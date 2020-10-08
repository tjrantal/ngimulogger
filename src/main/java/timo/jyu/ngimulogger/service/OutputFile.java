package timo.jyu.ngimulogger.service;

//Debugging
//import android.util.Log;

import java.io.File;	//For saving results to a file
import java.io.FileOutputStream;	//Output stream to write to a file
import java.io.OutputStreamWriter;	//Buffered output stream
import java.util.Locale;
import java.util.ArrayList;

public class OutputFile{
	private static final String TAG = OutputFile.class.getName();
	public FileOutputStream fOut;
	public OutputStreamWriter myOutWriter;
	private Locale locale;
	
	public OutputFile(File externalStorageDir, String fileName, String headerLine,Locale locale){
		this.locale = locale;
		//Create the file and write the header
		fOut = null;
		myOutWriter = null;
		try{
			//Log.d(TAG,"Write header "+headerLine);
			fOut = new FileOutputStream(new File(externalStorageDir , fileName));
			myOutWriter = new OutputStreamWriter(fOut,"UTF-8");
			myOutWriter.append(headerLine+"\n");
		}catch(Exception e){
			//Log.e(TAG,"Could not open outputstream "+e.toString());
		}
	}
	
	//Write ArrayList of Doubles to output file
	public void writeData(double tStamp,ArrayList<Double> values){
		String dataString =String.format(locale,"%f",tStamp);
		for (int f = 0; f<values.size();++f){
			dataString+=String.format(locale,";%f",values.get(f));
		}
		try{
			myOutWriter.append(dataString+"\n");
		}catch(Exception e){
			//Log.e(TAG,"Could not write data "+e.toString());
		}
	}
	  
	public void closeFile(){
		try{
			//of.myOutWriter.flush();
			myOutWriter.close();
			fOut.close();
		}catch(Exception e){}
	}
}