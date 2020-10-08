package timo.jyu.ngimulogger;

//Debugging
//import android.util.Log;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;


import android.widget.Button;

//ActionBar
import android.os.Build;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;

//Broadcasting from thread to another
import android.content.Intent;
import android.content.Context;
import android.widget.Toast;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

//TextViews for results
import android.widget.TextView;
import android.widget.LinearLayout;	//Visualisation

//Requesting file write permission
import android.os.PowerManager;
import android.provider.Settings;
import android.net.Uri;
import androidx.core.content.ContextCompat;

//Custom classes
import timo.jyu.ngimulogger.util.Constants;
import timo.jyu.ngimulogger.graphicsView.GraphicsSurfaceRawView;
import timo.jyu.ngimulogger.service.CaptureService;


public class NgimuLogger extends AppCompatActivity {
	private static final String TAG = NgimuLogger.class.getName();//"NgimuLogger";
	
   Button shutdownButton;

	//Graphics view
	private GraphicsSurfaceRawView[] graphicsViewAcc;

	
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);
      
      //Get actionbar
     Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
     setSupportActionBar(myToolbar);
     
	 //Graphics views
	graphicsViewAcc = new GraphicsSurfaceRawView[10];
	//int[] colours = new int[]{Color.RED,Color.GREEN,Color.BLUE,Color.YELLOW};
	for (int i = 0;i<graphicsViewAcc.length;++i){
		graphicsViewAcc[i] =  new GraphicsSurfaceRawView(this);
		graphicsViewAcc[i].setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,1f)); //Set the layout parameters here
		//graphicsViewAcc[i].bgColor = colours[i];
		//Create a LinearLayout to hold the view
		LinearLayout temp = new LinearLayout(this);
		temp.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,1f));
		temp.setOrientation(LinearLayout.VERTICAL);    
		temp.addView(graphicsViewAcc[i]);
		((LinearLayout) findViewById(R.id.visualLayout)).addView(temp);	//Add the view into the underlying layout
	}
	
	//Add the graphicsViews into the layout
	
	
	
    
     //Get reference to shutdownButton
     shutdownButton = (Button) findViewById(R.id.shutdownButton);
     
     //Bind GUI listeners
		shutdownButton.setOnClickListener( new View.OnClickListener() {
			public void onClick(View v) {
					//Shutdown services
					sendBroadcast(new Intent().setAction(Constants.STOP_SERVICE));
					finish();	//Shutdown the program
				 }
     		}
      );
      
  
	//BROADCASTSERVICE register the receiver
	registerReceiver();
	
     
    	   
	 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !((PowerManager) getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName())){
		//Request ignoring battery optimizations
		 startActivityForResult(
			 new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:" + getPackageName()))
			 ,Constants.REQUEST_DISABLE_BATTERY);//, Uri.parse("package:"+getPackageName())));
		 Toast.makeText(this, R.string.disable_battery_optimisation, Toast.LENGTH_SHORT).show();
		 //finish();	//The user has to re-start the app with GPS enabled
	 }
     
    //Launch service if it is not yet running
    ContextCompat.startForegroundService(this,new Intent(NgimuLogger.this, CaptureService.class).setAction(Constants.START_SERVICE));

   }
     
	/*Register the broadcast listener*/
   private void registerReceiver(){
	   if (epReceiver != null) {
			IntentFilter intentFilter = new IntentFilter(Constants.UPDATE_ACC_GRAPH);	//Create an intent filter to listen to the broadcast sent with the action 
			 registerReceiver(epReceiver, intentFilter);	//Map the intent filter to the receiver
		}
   }


	
	//Broadcast receiver
    private BroadcastReceiver epReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
				//Visualisation
				if (intent.getAction().equals(Constants.UPDATE_ACC_GRAPH)){
					//Log.e(TAG,"Received IMU");
					
					float[][] rawData = new float[3][];
					rawData[0] = intent.getFloatArrayExtra(Constants.UPDATE_GRAPH_X);
					rawData[1] = intent.getFloatArrayExtra(Constants.UPDATE_GRAPH_Y); 
					rawData[2] = intent.getFloatArrayExtra(Constants.UPDATE_GRAPH_Z); 
					int accView = intent.getIntExtra(Constants.ACCVIEW,0);
					if (graphicsViewAcc[accView].surfaceCreated){  
						graphicsViewAcc[accView].updateData(rawData,Constants.normalisation[0],"Acc [m/s2] "+intent.getStringExtra(Constants.BATTERY));
					}
					
				}

        }
    };
 
	
	/**Toolbar*/
	/*Inflate the toolbar menu*/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		 MenuInflater inflater = getMenuInflater();
		 inflater.inflate(R.menu.toolbar, menu);
		 return true;
	}
	
	/*Listen to the toolbar*/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		 switch (item.getItemId()) {
		     case R.id.get_about:
		         // Launch Terms of use and policy
		         Intent intent = new Intent(NgimuLogger.this, TermsOfUse.class);
			  		startActivity(intent);
		         return true;

		     default:
		         // If we got here, the user's action was not recognized.
		         // Invoke the superclass to handle it.
		         return super.onOptionsItemSelected(item);

		 }
	}
	
	/**unregister broadcast listeners*/
	private void cleanUp(){
		try{
			unregisterReceiver(epReceiver);
		}catch (Exception e){}
	}
	
	@Override
    public void onPause(){
		cleanUp();	//Detach broadcast listener
		super.onPause();
    }
	
	@Override
    protected void onResume() {
		//Log.e(LOG_TAG,"onResume called");
		try{
			 registerReceiver();	//Re-attach broadcast listener
		}catch (Exception e){}
        super.onResume();
    }
   
   @Override
   public void onDestroy(){
		//Log.e(LOG_TAG,"onDestroy called");
		cleanUp();
		super.onDestroy();
   }
}

