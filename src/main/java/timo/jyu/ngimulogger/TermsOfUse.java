package timo.jyu.ngimulogger;

//import android.util.Log;

//Activity
import android.app.Activity;
import android.os.Bundle;


//webview
import android.webkit.WebView;


//Utils
import timo.jyu.ngimulogger.util.RawResourceReader;

public class TermsOfUse extends Activity{
	private String TAG = TermsOfUse.class.getName();
	     /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
  			setContentView(R.layout.terms);
		TAG = this.getClass().getName();
			  
		//WebView for EULA  			
		WebView wv = (WebView) findViewById(R.id.terms);
		String eula = RawResourceReader.readTextFileFromRawResource(this, R.raw.terms);
		wv.loadData(eula, "text/html", null);
    }
	
	
	
	/*Power saving*/
     protected void onResume() {
     		super.onResume();
     }

     protected void onPause() {
      		super.onPause();
     }
	
	protected void onDestroy(){
		//BROADCAST the selected files and/or set the return intent here
      super.onDestroy();
	}
    
}
