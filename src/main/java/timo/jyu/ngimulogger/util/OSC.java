package timo.jyu.ngimulogger.util;

//import android.util.Log;

import java.util.Arrays;
import java.util.ArrayList;

/**
	OSC byte stream may have controls (OSC Message) and bundles. Bundles may contain Bundles so may be recursive
	NGIMU data is encapsulated in a bundle, i.e. the total size of the bundle is the byte array size!
*/

public class OSC{
	private static final String TAG = OSC.class.getName();
	public static final double fractionalSecond = 1d/(Math.pow(2d,32d)-1d);
	public static OSCObject decode(byte[] frame){
		switch (frame[0]){
			case (byte) 47:
				//System.out.println("dControl "+String.valueOf(frame[0]));
				return decodeControl(frame);
			case (byte) 35:
				//System.out.println("dBundle "+String.valueOf(frame[0]));
				return decodeBundle(frame);			
			default:
				return null;
		}
	}
	
	private static OSCControl decodeControl(byte[] frame){
		OSCControl returnVal = null;
		OSCString address = new OSCString(frame);
		OSCString fmt = new OSCString(address.remain);
		//System.out.println("CONTROL "+address.string+" fmt "+fmt.string);
		//Log.e(TAG,"CONTROL "+address.string+" "+fmt.string);
		returnVal = new OSCControl(address.string,fmt.string);
		//System.out.println(address.string+"_"+fmt.string+"_");
		
		byte[] remData = fmt.remain;
		for (int i = 1;i<fmt.string.length();++i){
			switch (fmt.string.charAt(i)){
				case 'F':
					//No data associated with this
					returnVal.bools.add(false);
					break;
				case 'T':
					//No data associated with this
					returnVal.bools.add(true);
					break;
				case 'f':
					
					float fl = Float.intBitsToFloat(
						((int) (remData[0] & 0xff)) << 24	|
						((int) (remData[1] & 0xff)) << 16	| 
						((int) (remData[2] & 0xff)) << 8	| 
						((int) (remData[3] & 0xff))
						);
					returnVal.doubles.add((double) fl);
					remData = Arrays.copyOfRange(remData,4,remData.length);
					
					break;
				case 'i':
					int inte = (int) (
						((int) (remData[0] & 0xff)) << 24	|
						((int) (remData[1] & 0xff)) << 16	| 
						((int) (remData[2] & 0xff)) << 8	| 
						((int) (remData[3] & 0xff))
						);
					
					returnVal.ints.add(inte);
					remData = Arrays.copyOfRange(remData,4,remData.length);
					break;
				case 's':
					OSCString temp = new OSCString(remData);
					returnVal.strings.add(temp.string);
					//System.out.println("decodeControl i "+i+" got s "+temp.string);
					remData = temp.remain;
					break;
				default:
					break;
			}
			
		}
		//System.out.println("Control address "+address.string);
		return returnVal;
		

	}
	
	private static OSCBundle decodeBundle(byte[] frame){
		OSCString bundle = new OSCString(frame);
		//System.out.println("BUNDLE "+bundle.string);
		//Log.e(TAG,"BUNDLE "+bundle.string);
		
		
		byte[] remData = bundle.remain;
		long seconds = (long) (
			((long) (remData[0] & 0xff)) << 24	|
			((long) (remData[1] & 0xff)) << 16	| 
			((long) (remData[2] & 0xff)) << 8	| 
			((long) (remData[3] & 0xff))
			);
		
		remData = Arrays.copyOfRange(remData,4,remData.length); //Advance past seconds
		long fractional = (long) (
			((long) (remData[0] & 0xff)) << 24	|
			((long) (remData[1] & 0xff)) << 16	| 
			((long) (remData[2] & 0xff)) << 8	| 
			((long) (remData[3] & 0xff))
			);
		
		remData = Arrays.copyOfRange(remData,4,remData.length);		//Advance past factorial
		
		/*
		int size = (int) (
			( (remData[0] & 0xff)) << 24	|
			((remData[1] & 0xff)) << 16	| 
			( (remData[2] & 0xff)) << 8	| 
			(( (remData[3] & 0xff))
			);
		
		remData = Arrays.copyOfRange(remData,4,remData.length);	//Advance past size
		*/
		
		double tStamp = ((double) seconds)+((double) fractional)*fractionalSecond;
		//System.out.println("Bundle "+bundle.string+" tStamp "+tStamp+" seconds "+seconds);
		OSCBundle returnVal = new OSCBundle(tStamp,seconds,fractional);
		
		//Get the messages (controls) or bundles
		while (remData != null && true){
			//try{
				if (remData.length < 4){
					break;
				}
				int blobLength = (int) (
							((int) (remData[0] & 0xff)) << 24	|
							((int) (remData[1] & 0xff)) << 16	| 
							((int) (remData[2] & 0xff)) << 8	| 
							((int) (remData[3] & 0xff))
					);
				//Log.e(TAG,String.format("blobLength %d remData %d",blobLength,remData.length));
				//System.out.println(String.format("blobLength %d remData %d",blobLength,remData.length));
				remData = Arrays.copyOfRange(remData,4,remData.length);
				if (remData == null || blobLength > remData.length){
					break;	//Something has gone wrong here, bloblength cannot be longer than remaining data
				}
				//Message or bundle
				switch (remData[0]){
					case (byte) 47:
						//System.out.println("dControl "+String.valueOf(frame[0]));
						
						
						
						
						returnVal.addControl(decodeControl(Arrays.copyOfRange(remData,0,blobLength)));
						//Debugging
						if (false && returnVal.controls != null){
							OSCControl temp = returnVal.controls.get(returnVal.controls.size()-1);
							ArrayList<Double> doubles = temp.doubles;
							if (doubles != null){
								String vals = temp.address;
								for (int i = 0; i<doubles.size();++i){
									vals+="\t"+String.format("%.1f",doubles.get(i));
								}
								//Log.e(TAG,vals);
							}
						}
												
					case (byte) 35:
						//System.out.println("dBundle "+String.valueOf(frame[0]));
						returnVal.addBundle(decodeBundle(Arrays.copyOfRange(remData,0,blobLength)));
					default:
				}
				
				
				
				
				
				if (blobLength < remData.length-11){
					remData = Arrays.copyOfRange(remData,blobLength,remData.length);
				}else{
					break;
				}
			//}catch(Exception e){
				//Log.e(TAG,"Could not decodeControl "+e.toString());
			//	break;
			//}

		}
		return returnVal;
	}

	private static class OSCString{
		byte[] remain;
		String string;
		public OSCString(byte[] in){
			ArrayList<FindIndices> nullIndices = find(in,(byte) 0);
			string = new String(Arrays.copyOfRange(in,0,nullIndices.get(0).init));
			int startInd = 0;
			int remainder =(nullIndices.get(0).init+1) % 4;
			if (remainder == 0){
				startInd = 	nullIndices.get(0).init+1;
			}else{
				startInd = 	nullIndices.get(0).init+4-remainder+1;
			}
			remain = Arrays.copyOfRange(in,startInd,in.length);
		}
	}
	
	public static ArrayList<FindIndices> find(byte[] dataIn, byte lookFor){
		ArrayList<FindIndices> matches = new ArrayList<FindIndices>(100);
		for (int i =0;i<dataIn.length;++i){
			if (dataIn[i] == lookFor){
				matches.add(new FindIndices(i));
			}
		}
		return matches;
	}
	
}