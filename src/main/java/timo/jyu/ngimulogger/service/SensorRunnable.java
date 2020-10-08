package timo.jyu.ngimulogger.service;

//Debugging
import android.util.Log;

//Listening to NGIMU
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.util.Arrays;

//Dumping the data into a file
import java.util.ArrayList;
import java.util.List;
import java.io.File;	//For saving results to a file
import java.util.TimeZone;
import java.util.Locale;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;

//Visualisation
import java.util.ArrayDeque;
import android.content.Intent;

//Custom classes
import timo.jyu.ngimulogger.util.Data;
import timo.jyu.ngimulogger.util.OSC;
import timo.jyu.ngimulogger.util.OSCObject;
import timo.jyu.ngimulogger.util.OSCBundle;
import timo.jyu.ngimulogger.util.OSCControl;
import timo.jyu.ngimulogger.util.Constants;

public class SensorRunnable implements Runnable{
	private static final String TAG = SensorRunnable.class.getName();
	
	private CaptureService a;
	private int b;
	private DatagramSocket socket = null;
	private DatagramPacket packet = null;
	//Log file handles
	File externalStorageDir;
	//Signals of interest and corresponding file headers. Have to be hard coded. Implement new signals here, and in the @handleControl method. An output file will be created for each signal
	String[] fileNames = {"sensors","quaternion","analogue","temperature","battery","rssi","humidity"};
	String[] headings = {"Timestamp (s);Gyroscope X (deg/s);Gyroscope Y (deg/s);Gyroscope Z (deg/s);Accelerometer X (g);Accelerometer Y (g);Accelerometer Z (g);Magnetometer X (uT);Magnetometer Y (uT);Magnetometer Z (uT);Barometer (hPa)",	//Sensors
						"Timestamp (s);W;X;Y;Z",	//quaternion
						"Timestamp (s);Channel 1 (V);Channel 2 (V);Channel 3 (V);Channel 4 (V);Channel 5 (V);Channel 6 (V);Channel 7 (V);Channel 8 (V)",	//Analogue
						"Timestamp (s);Processor (degC);Gyroscope And Accelerometer (degC);Environmental Sensor (degC)",	//Temperature
						"Timestamp (s);Percentage (%);Time To Empty (minutes);Voltage (V);Current (mA)",	//Battery
						"Timestamp (s);Power (dBm);Percentage (%)",
						"Timestamp (s);Humidity (%)"};
	HashMap<String,OutputFile> outputFiles =  new HashMap<>();
	
	ArrayList<ArrayDeque<Float>> accdata = null;
	private int updateCnt = 0;
	private double sFreq = 100;
	private String battery = "Unknown";
	private Locale locale = null;
	
	/**Constructor*/
	public SensorRunnable(CaptureService a,int b,Locale locale,TimeZone tz,String date){
		this.a = a;
		this.b = b;
		this.locale = locale;
		//Create output folder
		String logFolder = String.format("Port%d",b);
		externalStorageDir = new File(a.getExternalFilesDir(null), logFolder+File.separator+date);
		if (!externalStorageDir.exists()) {
			 boolean success = externalStorageDir.mkdirs();	//Create the folder structure
			 //Log.e(TAG,String.format("CREATING OUTPUT FOLDER %s %b",logFolder,success));
		}
		
		//Create output files
		for (int f = 0;f<fileNames.length;++f){
			outputFiles.put(fileNames[f],
				new OutputFile(externalStorageDir,fileNames[f]+".txt",tz.getID()+" "+headings[f],locale)
				);	//Add file into hashmap
		}
		
		//Prep visualisation
		accdata = new ArrayList<ArrayDeque<Float>>();
		int visualLength = (int) Math.ceil(Constants.visualLength*sFreq);
		for (int i =0;i<3;++i){
			accdata.add(new ArrayDeque<Float>(visualLength));
			for (int j = 0;j<visualLength;++j){
				accdata.get(i).addLast(0f);
			}
		}
		
	}
	
	/**Implement Runnable to listen to the UDP socket of the given sensor*/
	@Override
	public void run(){
		//Create sockets in the thread
		try{
			socket = new DatagramSocket(b,InetAddress.getByName("192.168.43.1"));//,InetAddress.getLocalHost());
			socket.setSoTimeout(100);	//Set timeout to 100 ms at a time -> any sensor not present won't hang the programme
			//Log.e(TAG,"Created socket "+socket.toString());
		}catch(Exception e){Log.e(TAG,"Could not create socket "+e.toString());}
		packet = new DatagramPacket(new byte[5000],5000);
		//Sample here until the main program is closed down
		while (a.keepGoing() && socket != null){

			boolean gotPackage = false;
			try{
				socket.receive(packet);	//Blocks until a package is received
				gotPackage = true;
				//String senderIP = packet.getAddress().getHostAddress();
				//String senderName = packet.getAddress().getHostName();
				//Log.e(TAG,senderIP+" "+senderName);
			}catch(Exception e){
				//Log.e(TAG,"COULD NOT RECEIVE PACKET "+e.toString());
			}
			
			if (gotPackage){
				byte[] inBound = null;
				try{	
					inBound = Arrays.copyOfRange(packet.getData(),0,packet.getLength());
				}catch(Exception e){
					Log.e(TAG,"Handling BUNDLE FAILED "+e.toString());
				}
				//String message = new String(inBound).trim();
				
				try{
					//Decode packet here
					OSCObject oObj = OSC.decode(inBound);
					
					//Handle controls
					if (oObj instanceof timo.jyu.ngimulogger.util.OSCControl){
						OSCControl oCon = (OSCControl) oObj;
						Log.e(TAG,"GOT CONTROL SIGNAL");
						//Implement control handling here. NGIMUs do not use controls (messages) to transmit data, it's all in bundles.
					}
				
					//Handle bundles. Need to recurse into the nested bundles
					if (oObj instanceof timo.jyu.ngimulogger.util.OSCBundle){
						OSCBundle oBun = (OSCBundle) oObj;
						handleBundle(oBun);	//Handle the bundle using a recursive function
					}
					
				}catch(Exception e){
					Log.e(TAG,"Decoding FAILED "+e.toString());
				}
			}

		}
		
		try{
			socket.close();	//Close the socket
		}catch(Exception e){
			Log.e(TAG,"Could not receive packet "+e.toString());
		}
		closeOutputFiles();	//Close the output files
	}

	//Function to handle bundles. If additional bundles are included in this bundle recurse into it, otherwise handle controls of the current bundle
	private void handleBundle(OSCBundle in){
		//pop the additional bundles back into this function
		for (int b = 0;b<in.bundles.size();++b){
			handleBundle(in.bundles.get(b));
		}
		//Handle controls of this bundle here
		for (int c = 0; c<in.controls.size();++c){
			Data data = handleControl(in.controls.get(c),in.tStamp);
			if (data != null){
				handleData(data);	//Store data here
			}
		}
	}
	
	//Handle incoming data here
	private void handleData(Data data){
		outputFiles.get(data.getType()).writeData(data.gettStamp(),data.getData());	//Write the data into a file
	}
	
	/**Map the OSC messages (controls) here. Unused as is since sensors are configured manually */
	private void handleControl(OSCControl in){
		//Log.e(TAG,String.format("handleControl %s",in.address));
		switch (in.address){
			case "/rate/quaternion":
				double quaternionRate = in.doubles.get(0);
				Log.e(TAG,String.format("/rate/quaternion %.0f",quaternionRate));
				break;
			case "/rate/sensors":
				double sensorRate = in.doubles.get(0);
				Log.e(TAG,String.format("/rate/sensors %.0f",sensorRate));
				break;
			//Implement additional controls here
			default:
				break;
		}
	}
	
	//Map the controls into the output files based on the control signal type
	private Data handleControl(OSCControl in, double tStamp){

		ArrayList<Double> data = new ArrayList<Double>();
		if (in.doubles != null){
			for (int i = 0;i<in.doubles.size();++i){
					data.add(in.doubles.get(i));
			}

			switch (in.address){
				case "/sensors":
					//Pop accelerations into visualisation
					for (int i = 0;i<accdata.size();++i){
						accdata.get(i).removeFirst();
						accdata.get(i).addLast((float) ((double) (in.doubles.get(i+3))));
					}
					++updateCnt;
					if (updateCnt >= (int) Math.ceil(Constants.updateInterval*sFreq)){
						//Send data for visualisation
						float[][] tempdata = new float[3][];
						for (int i = 0;i<accdata.size();++i){
							Float[] temp = accdata.get(i).toArray(new Float[accdata.get(i).size()]);
							tempdata[i] = new float[temp.length];
							for (int j = 0;j<temp.length;++j){
								tempdata[i][j] = temp[j];
							}
						}
						
						Intent new_intent = new Intent();
						new_intent.setAction(Constants.UPDATE_ACC_GRAPH);
						new_intent.putExtra(Constants.UPDATE_GRAPH_X,tempdata[0]);
						new_intent.putExtra(Constants.UPDATE_GRAPH_Y,tempdata[1]);
						new_intent.putExtra(Constants.UPDATE_GRAPH_Z,tempdata[2]);
						new_intent.putExtra(Constants.BATTERY,battery);
						new_intent.putExtra(Constants.ACCVIEW,b-8002);	//Use the port to figure out which visualisation graph to use
						
						a.sendBroadcast(new_intent);
						updateCnt = 0;
					}
					
					return new Data("sensors",tStamp,data);
				case "/quaternion":
					return new Data("quaternion",tStamp,data);
				case "/analogue":
					return new Data("analogue",tStamp,data);
				case "/temperature":
					return new Data("temperature",tStamp,data);
				case "/battery":
					//Set battery string
					battery = String.format(locale,"%.0f", in.doubles.get(1));
					return new Data("battery",tStamp,data);
				case "/rssi":
					return new Data("rssi",tStamp,data);
				case "/humidity":
					return new Data("humidity",tStamp,data);
				//Implement other data types here!
				default:
					break;
			}
		}
		return null;
	}
	
	//Close output files
	public void closeOutputFiles(){
		try{
			Set<String> keyso = outputFiles.keySet();
			Iterator<String> ito = keyso.iterator();
			ArrayList<String> keyListo = new ArrayList<String>();
			while (ito.hasNext()) {
				keyListo.add(ito.next());
			}
			
			for (int i = 0; i<keyListo.size();++i){
				outputFiles.get(keyListo.get(i)).closeFile();
				outputFiles.remove(keyListo.get(i));
			}
		}catch(Exception e){}
	}
	
}