package timo.jyu.ngimulogger.util;
import java.util.ArrayList;

/*Helper class to hold decoded data from OSC*/
public class Data{
	private double tStamp;
	private String type;
	private ArrayList<Double> data;
	public Data(String type,double tStamp, ArrayList<Double> data){
		this.type = type;
		this.tStamp = tStamp;
		this.data = data;
	}
	public String getType(){return type;}
	public double gettStamp(){return tStamp;}
	public ArrayList<Double> getData(){return data;}
}