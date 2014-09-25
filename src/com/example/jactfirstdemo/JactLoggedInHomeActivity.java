package com.example.jactfirstdemo;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class JactLoggedInHomeActivity extends Activity {
	
	private String jact_session_name_;
	private String jact_session_id_;
	private String jact_token_;
	private String jact_user_;
	private String jact_user_id_;
	private String jact_user_name_;
	private String jact_user_email_;
	private String jact_user_signature_;
	private String jact_user_pic_;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
	  setContentView(R.layout.jact_logged_in_home_screen);
	  Intent intent = getIntent();
	  String server_response = intent.getStringExtra("server_login_response");
	  try {
		JSONObject response_json = new JSONObject(server_response);
		jact_session_name_ = response_json.getString("session_name");
		jact_session_id_ = response_json.getString("sessid");
		jact_token_ = response_json.getString("token");
		jact_user_ = response_json.getString("user");
		JSONObject user_json = new JSONObject(jact_user_);
		jact_user_id_ = user_json.getString("uid");
		jact_user_name_ = user_json.getString("name");
		jact_user_email_ = user_json.getString("mail");
		jact_user_signature_ = user_json.getString("signature");
		jact_user_pic_ = user_json.getString("picture");
		String phb_foo = user_json.getString("foo");
		phb_foo = "";
	  } catch (Exception e) {
	  }
	  TextView tv = 
		  (TextView) this.findViewById(R.id.logged_in_home_welcome_textview);
	  if (!jact_user_name_.isEmpty()) {
		tv.setText("Welcome, " + jact_user_name_);
	  } else {
	    tv.setText("Welcome, Unknown Jact User");
	  }
	}
	  
	private class JactNodeTask extends AsyncTask<String, Void, String> {
	    protected String doInBackground(String... params) {
	      try {
	        String url = "http://us7.jact.com:3080/rest/userpoints/retrieve";
	        return doJactLogin(url);
	      } catch (IOException e) {
	        return ("Unable to retrieve web page (Error Response Code 403).\n" + e.getMessage());
	      }
		}
		     
		private String doJactLogin(String urlStr) throws IOException {
	   	  InputStream is = null;
	   	  try {
	   		URL url = new URL(urlStr);
	   		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	   		connection.setReadTimeout(20000 /* milliseconds */);
	   		connection.setConnectTimeout(25000 /* milliseconds */);
	   		connection.setRequestMethod("GET");
	   		connection.setDoInput(true);
	   		String cookies =
	   		    "session_name=" + jact_session_name_.trim() + "; sessid=" + jact_session_id_.trim();
	   		//connection.setRequestProperty("Cookie", cookies);
	   		connection.setRequestProperty("session_name", jact_session_name_);
	   		connection.setRequestProperty("sessid", jact_session_id_);
	   			    				
	   		// Starts the query
	   		connection.connect();
	   		int response = connection.getResponseCode();
	   		if (response != 200) {
	   			// Handle error gracefully here.
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
		     TextView tv = (TextView) JactLoggedInHomeActivity.this.findViewById(R.id.test_textview);
		     tv.setText("phb_foo");
	         tv.setVisibility(View.VISIBLE);
		   } else {
		     TextView tv = (TextView) JactLoggedInHomeActivity.this.findViewById(R.id.test_textview);
		     tv.setText(result);
		     tv.setVisibility(View.VISIBLE);
		   }
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
	
	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}
	
	public void doFetchNodeButtonClick(View view) {
		new JactNodeTask().execute();
	}
}
