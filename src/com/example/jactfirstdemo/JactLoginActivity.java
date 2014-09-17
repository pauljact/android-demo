package com.example.jactfirstdemo;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.view.View;
import android.os.AsyncTask;
import android.support.v4.app.NavUtils;
//import android.util.Log;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.jactfirstdemo.GoogleFirstDemo;
import com.example.jactfirstdemo.GoBroncosActivity;
import com.example.jactfirstdemo.JactLoggedInHomeActivity;
import com.example.jactfirstdemo.R;

public class JactLoginActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.jact_welcome_screen);
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
	
	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	private class JactLoginTask extends AsyncTask<String, Void, String> {
	     protected String doInBackground(String... params) {
            try {
              String url = "http://us7.jact.com:3080/rest/user/login";
              return doJactLogin(url);
            } catch (IOException e) {
              return "Unable to retrieve web page. URL may be invalid.";
            }
	     }
	     
	     private String doJactLogin(String urlStr) throws IOException {
   		   InputStream is = null;
   		      
   		   try {
   		     URL url = new URL(urlStr);
   		     HttpURLConnection connection = (HttpURLConnection) url.openConnection();
   		     connection.setReadTimeout(20000 /* milliseconds */);
   		     connection.setConnectTimeout(25000 /* milliseconds */);
   		     connection.setRequestMethod("POST");
   		     connection.setDoInput(true);
   		     connection.setDoOutput(true);
   		     connection.setRequestProperty("Content-Type", "application/json");
   		     connection.setUseCaches (false);

    		 EditText username = (EditText) findViewById(R.id.username_edittext);
    		 EditText password = (EditText) findViewById(R.id.jact_password);
    		 JSONObject json = new JSONObject();
    		 String json_str = "";
    		 try {
    		   json.put("username", username.getText().toString().trim());
    		   json.put("password", password.getText().toString().trim());
    		   json_str = json.toString();
    		 } catch (JSONException e) {
    		 }

   		     DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
   		     outputStream.writeBytes(json_str);
   		     outputStream.flush();
   		     outputStream.close();
   				    				
   		     // Starts the query
   		     connection.connect();
   		     int response = connection.getResponseCode();
   		     if (response != 200) {
   		       return "";
   		     }
   		     //Log.d(DEBUG_TAG, "The response is: " + response);
   		     is = connection.getInputStream();
   		     
   		     // Convert the InputStream into a string
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
	       if (result == "") {
	    	   // Handle failed POST
	    	   TextView tv = (TextView) JactLoginActivity.this.findViewById(R.id.login_error_textview);
	           tv.setVisibility(View.VISIBLE);
	       } else {
	    	   TextView tv = (TextView) JactLoginActivity.this.findViewById(R.id.login_error_textview);
	           tv.setVisibility(View.INVISIBLE);
	           Intent logged_in_intent = new Intent(JactLoginActivity.this,
	        		                                JactLoggedInHomeActivity.class);
	       	   logged_in_intent.putExtra("server_login_response", result);
	       	   startActivity(logged_in_intent);
	       }
	     }
	}
		
	public void doLoginButtonClick(View view) {
		new JactLoginTask().execute();
	}
    
	// Called when user clicks 'Google Search' button from Jact Home Screen.
    public void doGoogleHomeButtonClick(View view) {
    	Intent google_intent = new Intent(this, GoogleFirstDemo.class);
    	startActivity(google_intent);
    }
	
    // Called when user clicks 'Go Broncos!' button.
    public void goToBroncosActivityButtonClick(View view) {
    	Intent broncos_intent = new Intent(this, GoBroncosActivity.class);
    	startActivity(broncos_intent);
    }
}
