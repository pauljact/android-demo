package com.example.jactfirstdemo;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.view.View;
import android.os.AsyncTask;
import android.support.v7.appcompat.*;
//import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;
import com.example.jactfirstdemo.JactLoggedInHomeActivity;
import com.example.jactfirstdemo.R;

public class JactLoginActivity extends Activity implements ProcessUrlResponseCallback {
  static final String COOKIES_HEADER = "Set-Cookie";

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
        // http://developer.android.com/design/patterns/navigation.html#up-vs-back
        //PHB NavUtils.navigateUpFromSameTask(this);
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

  public void doLoginButtonClick(View view) {
	String url = "http://us7.jact.com:3080/rest/user/login";
	EditText username = (EditText) findViewById(R.id.username_edittext);
    EditText password = (EditText) findViewById(R.id.jact_password);
    JSONObject json = new JSONObject();
    String json_str = "";
    try {
      json.put("username", username.getText().toString().trim());
      json.put("password", password.getText().toString().trim());
      json_str = json.toString();
    } catch (JSONException e) {
      Log.e("PHB", "Error: " + e.getMessage());
      // TODO(PHB): Implement this.
    }
    new GetUrlTask(this, GetUrlTask.TargetType.JSON).execute(url, "POST", "", json_str);
  }
  
  @Override
  public void ProcessUrlResponse(String webpage, String cookies) {
    if (webpage == "") {
      // Handle failed POST
      TextView tv = (TextView) JactLoginActivity.this.findViewById(R.id.login_error_textview);
      tv.setVisibility(View.VISIBLE);
    } else {
      TextView tv = (TextView) JactLoginActivity.this.findViewById(R.id.login_error_textview);
      tv.setVisibility(View.INVISIBLE);
      Intent logged_in_intent =
        new Intent(JactLoginActivity.this, JactLoggedInHomeActivity.class);
      logged_in_intent.putExtra("server_login_response", webpage);
      logged_in_intent.putExtra("session_cookies", cookies);
      startActivity(logged_in_intent);
    }
  }
/*
  public void doLoginButtonClick(View view) {
	String url = "http://us7.jact.com:3080/rest/views/jact_prize_drawings.json";
    new GetUrlTask(this, GetUrlTask.TargetType.JSON).execute(url, "GET");
  }

  @Override
  public void ProcessUrlResponse(String webpage, String cookies) {
    if (webpage == "") {
      // Handle failed POST
      TextView tv = (TextView) JactLoginActivity.this.findViewById(R.id.login_error_textview);
      tv.setVisibility(View.VISIBLE);
    } else {
      TextView tv = (TextView) JactLoginActivity.this.findViewById(R.id.login_error_textview);
      tv.setVisibility(View.INVISIBLE);
      Intent products_activity =
              new Intent(JactLoginActivity.this, ProductsActivity.class);
          products_activity.putExtra("server_response", webpage);
          startActivity(products_activity);
    }
  }
*/
  @Override
  public void ProcessUrlResponse(Bitmap pic, String cookies) {
	// TODO Auto-generated method stub
  }

  @Override
  public void ProcessFailedResponse(GetUrlTask.FetchStatus status) {
	// TODO(PHB): Implement this.
	Log.e("PHB ERROR", "Status: " + status);
    // Handle failed POST
    TextView tv = (TextView) JactLoginActivity.this.findViewById(R.id.login_error_textview);
    tv.setVisibility(View.VISIBLE);
  }
}
