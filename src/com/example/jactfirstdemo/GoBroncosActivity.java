package com.example.jactfirstdemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v4.app.NavUtils;
import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

public class GoBroncosActivity extends Activity {

	private String phb_temp;
	private Bitmap image_bitmap_;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.go_broncos);
		// Show the Up button in the action bar.
		setupActionBar();
		new getBroncosIconTask().execute();
	}
		  
		private class getBroncosIconTask extends AsyncTask<String, Void, String> {
		    protected String doInBackground(String... params) {
		      try {
		        String url = "http://www.sports-logos-screensavers.com/user/Denver_Broncos_Old.jpg";
		        return doBroncosIcon(url);
		      } catch (IOException e) {
		        return ("Unable to retrieve web page. URL may be invalid.\n" + e.getMessage());
		      }
			}
			     
			private String doBroncosIcon(String urlStr) throws IOException {
		   	  InputStream is = null;
		   	  try {
		   		URL url = new URL(urlStr);
		   		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		   		connection.setReadTimeout(20000 /* milliseconds */);
		   		connection.setConnectTimeout(25000 /* milliseconds */);
		   		//connection.setRequestProperty("Accept", "text/html");
		   		connection.setRequestMethod("GET");
		   		connection.setDoInput(true);
		   		//connection.setUseCaches (false);
		   			    				
		   		// Starts the query
		   		connection.connect();
		   		int response = connection.getResponseCode();
		   		if (response != 200) {
		   			// Handle error here.
		   		}
		   		//Log.d(DEBUG_TAG, "The response is: " + response);
		   		is = connection.getInputStream();
		   	    image_bitmap_ = BitmapFactory.decodeStream(is);
   		        phb_temp = "zoo";
		   		
		   		return "";
		   		// Makes sure that the InputStream is closed after the app is
		   		// finished using it.
		   	 } finally {
		   	   if (is != null) {
		   	     is.close();
		   	   }
		   	 }
			 }
			     
			 protected void onPostExecute(String result) {
				 GoBroncosActivity.this.setDrawingImage();
			 }
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
	
    // Called when user clicks 'Jact Home' button from Go Broncos Activity.
    public void broncosToHomeButtonClick(View view) {
    	Intent jact_home_intent = new Intent(this, JactLoginActivity.class);
    	startActivity(jact_home_intent);
    }
    
    private void setDrawingImage() {
    	ImageView image = (ImageView) this.findViewById(R.id.broncos_image);
    	image.setImageBitmap(image_bitmap_);
    }
}