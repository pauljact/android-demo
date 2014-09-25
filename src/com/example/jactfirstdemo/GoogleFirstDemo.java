package com.example.jactfirstdemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.example.jactfirstdemo.R;
import com.example.jactfirstdemo.GoogleSearchResultsActivity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

public class GoogleFirstDemo extends Activity {
	private String query_;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.google_pre_query);
		// Show the Up button in the action bar.
		setupActionBar();
	}
	
	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.jact_login, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	// Called when user clicks 'Google Search' button from Jact Home Screen.
    public void doGoogleSearchButtonClick(View view) {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
        	EditText query = (EditText) findViewById(R.id.google_query_edittext);
        	query_ = query.getText().toString().trim();
        	new GoogleSearchTask().execute(query_);
        } else {
            // display error
        }
    }
    
	private class GoogleSearchTask extends AsyncTask<String, Void, String> {
	     protected String doInBackground(String... params) {
             try {
               String url = "https://plus.google.com/complete/search?client=es-main-search&hl=en&gs_rn=18&gs_ri=es-main-search&q=" + params[0];
               return downloadUrl(url);
             } catch (IOException e) {
               return "Unable to retrieve web page. URL may be invalid.";
             }
	     }
	     
	     private String downloadUrl(String urlStr) throws IOException {
    		 InputStream is = null;
    		      
    		 try {
    		     URL url = new URL(urlStr);
    		     HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    		     conn.setReadTimeout(20000 /* milliseconds */);
    		     conn.setConnectTimeout(25000 /* milliseconds */);
    		     conn.setRequestProperty("Accept", "text/html");
    		     conn.setRequestMethod("GET");
    		     conn.setDoInput(true);
    				    				
    		     // Starts the query
    		     conn.connect();
    		     int response = conn.getResponseCode();
    		     //Log.d(DEBUG_TAG, "The response is: " + response);
    		     is = conn.getInputStream();

    		     BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
    		     String webPage = "",data="";
    		     int loops = 0;
    		     while ((data = reader.readLine()) != null && loops < 100) {
    		    	loops++;
    		        webPage += data + "\n";
    		     }
    		     return webPage;
    		        
    		 // Makes sure that the InputStream is closed after the app is
    		 // finished using it.
    		 } finally {
    		   if (is != null) {
    		     is.close();
    		   } 
    		 }
	     }

	     protected void onPostExecute(String result) {
	    	 Intent google_intent = new Intent(GoogleFirstDemo.this, GoogleSearchResultsActivity.class);
    		 google_intent.putExtra("search_results", result);
    		 google_intent.putExtra("search_query", query_);
    	     startActivity(google_intent);
	     }
	}
}