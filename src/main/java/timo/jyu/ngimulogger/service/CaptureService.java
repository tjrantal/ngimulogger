package timo.jyu.ngimulogger.service;

//DEBUG
//import android.util.Log;

//To Implement a foreground service that will not die
import android.os.Binder;
import android.os.IBinder;
import android.app.Service;
import android.os.PowerManager;
import android.content.Intent;
import android.content.Context;

//A notification is required for a foreground service
import android.app.Notification;
import android.app.NotificationManager;
import androidx.core.app.NotificationCompat;
import android.app.NotificationChannel;
import android.os.Build;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.app.PendingIntent;
import timo.jyu.ngimulogger.R;	//Import R from this project...

//Communication from the other threads
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;

//Timing
import android.os.SystemClock;

//Time stamps
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.util.Locale;

//Listening to NGIMU
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

//Custom classes
import timo.jyu.ngimulogger.util.Constants;
import timo.jyu.ngimulogger.NgimuLogger;

public class CaptureService extends Service{
	private static final String TAG = CaptureService.class.getName();
	private PowerManager.WakeLock wl;
   private NotificationCompat.Builder nBuilder = null;

	private TimeZone tz = null;
	private String date = null;
	
	
	private ExecutorService es = null;
	private boolean goOn = true;

	
	/*This will get called whenever an intent for the service is created*/
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    		super.onStartCommand(intent, flags , startId);
    		
    		//For stopping the service, a must have...	    		
        if (intent.getAction().equals(Constants.STOP_SERVICE)){
     			//Send the close intent to the other service
	 			sendBroadcast(new Intent().setAction(Constants.STOP_SERVICE));
		     	shutdownService();
        }
        //Starting the service
        if(intent.getAction().equals(Constants.START_SERVICE)){
        	showNotification();
        }
        return START_STICKY; // If we get killed, after returning from here, restart
    }
    
    public void shutdownService(){
		stopForeground(true);
     	stopSelf();	//Call this to stop the service
   }
	
	/*This gets called, if the service has not been started before*/
	public void onCreate(){
		super.onCreate();
		getWakeLock();

		//Use system time for time stamps
		date = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new Date());
		tz = TimeZone.getDefault();

		//BROADCASTSERVICE register the receiver
		if (serviceReceiver != null) {
			IntentFilter intentFilter = new IntentFilter(Constants.STOP_SERVICE);	//Create an intent filter
			registerReceiver(serviceReceiver, intentFilter);	//Map the intent filter to the receiver
		}
		
		//Start listening to UDP
		es = Executors.newFixedThreadPool(10);	//Max 10 concurrent threads
		for (int t = 0;t<10;++t){
			es.execute(new SensorRunnable(this,8002+t,Locale.getDefault(),tz,date));	//Add threads to listen to the sockets
		}
	}
	
	public boolean keepGoing(){
		return goOn;
	}
	
	//BROADCAST RECEIVER
	private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
				
				//Shutdown
				if (intent.getAction().equals(Constants.STOP_SERVICE)){
			      shutdownService();
				}
        }
    };
    

	
	//For notification
  private void showNotification() {
	  createNotificationChannel();	//Required for Android 9 and above
     Intent notificationIntent = new Intent(this, NgimuLogger.class);
     notificationIntent.setAction("CaptureServiceNotification");
     notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
             | Intent.FLAG_ACTIVITY_CLEAR_TASK);

      //Launch ActivityReminder as the default action
       Intent arIntent = new Intent(this, NgimuLogger.class);
     PendingIntent parIntent = PendingIntent.getActivity(this, 0,
             arIntent, 0);      

		//Enable shutting down the monitor from taskbar
		Intent closeIntent = new Intent(this, CaptureService.class);
		closeIntent.setAction(Constants.STOP_SERVICE);
     PendingIntent pcloseIntent = PendingIntent.getService(this, 0,
             closeIntent, 0);
     Bitmap icon = BitmapFactory.decodeResource(getResources(),
             R.drawable.ic_launcher);          
             //.setContentIntent(parIntent);	//Launch activity monitor
      nBuilder = new NotificationCompat.Builder(this,Constants.CAPTURE_NOTIFICATION_CHANNEL_ID)
             .setContentTitle(getResources().getString(R.string.ep_sensor_capture))
             .setContentIntent(parIntent)
             .setTicker(getResources().getString(R.string.ep_sensor_capture))
             .setSmallIcon(R.drawable.ic_launcher)
             .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
             .setOngoing(true)
             .addAction(new NotificationCompat.Action.Builder(R.drawable.ic_close, getResources().getString(R.string.ep_close) ,pcloseIntent).build());
			//Start the service in foreground
		  startForeground(116, nBuilder.build());

    }
	
		//Create a nofitication channel. Required on SDK 26  (O) or higher
	private void createNotificationChannel(){
     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         NotificationChannel serviceChannel = new NotificationChannel(
                 Constants.CAPTURE_NOTIFICATION_CHANNEL_ID,
                 getResources().getString(R.string.app_name),
                 NotificationManager.IMPORTANCE_DEFAULT
         );
		serviceChannel.setSound( null, null );	//Turn of notification sound
         NotificationManager manager = getSystemService(NotificationManager.class);
         manager.createNotificationChannel(serviceChannel);
     }
	}

    private void getWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wl.acquire();	//Start wake lock
    }

	 @Override
    public void onDestroy() {
		goOn = false;
		
		if (es != null){
			es.shutdown();
		}

        wl.release();	//Release wakelock
        //Broadcast listener
        unregisterReceiver(serviceReceiver);	//Unregister BROADCAST receiver
        super.onDestroy();
    }
        
    //Implement abstract onBind (copied this straight off the internet..)
    public class LocalBinder extends Binder {
        CaptureService getService() {
            return CaptureService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
	
}
