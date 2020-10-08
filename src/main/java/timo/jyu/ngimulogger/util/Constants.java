/**Strings to be used to communicate/relay intent between ActivityMonitor and IMUCaptureService*/
package timo.jyu.ngimulogger.util;

import android.graphics.Color;
import java.util.UUID;

public final class Constants{
	
	public static final String APP_CLASS = "timo.jyu.ngimulogger.NgimuLogger"; 	//Used to get preferences
	public static final String ACCDATA = "timo.jyu.ngimulogger.util.Constants.ACCDATA";		//
	public static final int REQUEST_DISABLE_BATTERY = 158;
	
	//SERVICE
	public static final String START_SERVICE = "timo.jyu.ngimulogger.util.Constants.START_SERVICE";
	public static final String STOP_SERVICE = "timo.jyu.ngimulogger.util.Constants.STOP_SERVICE";
	public static final String CAPTURE_NOTIFICATION_CHANNEL_ID = "timo.jyu.activityMonitor.util.Constants.CAPTURE_NOTIFICATION_CHANNEL_ID";
	
	//Communication through Broadcasts
	public static final String BATTERY = "timo.jyu.ngimulogger.BATTERY";
	public static final String ACCVIEW = "timo.jyu.ngimulogger.ACCVIEW";

	public static final String UPDATE_ACC_GRAPH = "timo.jyu.ngimulogger.UPDATE_ACC_GRAPH";
	public static final String UPDATE_GRAPH_X = "timo.jyu.ngimulogger.UPDATE_GRAPH_X";
	public static final String UPDATE_GRAPH_Y = "timo.jyu.ngimulogger.UPDATE_GRAPH_Y";
	public static final String UPDATE_GRAPH_Z = "timo.jyu.ngimulogger.UPDATE_GRAPH_Z";
	public static final int fgServiceInt = 132;

	
	//Colors
	public static final int[] colours = {0xFF000000	/*0 = black*/,
													0xFFFFFFFF	/*1 = white*/,
													0XFFFF3232	/*2 = RED*/,
													0XFFFF9632	/*3 = ORANGE*/,
													0XFFFFFF46	/*4 = YELLOW*/,
													0XFF32FF32	/*5 = GREEN*/,
													0XFF3232FF	/*6 = BLUE*/,
													Color.TRANSPARENT /*7 = Transparent*/
													};
	public static final double[] normalisation = {3d, 2048d,200d};
	public static final double updateInterval = 1d/5d;
	public static final double visualLength = 4;
}
