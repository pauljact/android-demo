package com.example.jactfirstdemo;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class JactLoggedInHomeActivity extends JactActionBarActivity implements ProcessUrlResponseCallback {  
  static final String PROFILE_PIC = "profile_pic";
  static final String BUX_LOGO = "bux_logo";
  static final String USER_POINTS = "user_points";
  
  private static String jact_website_;
  private String jact_user_name_;
  private String jact_user_id_;
  private String jact_profile_pic_url_;
  private String jact_user_points_;
  private boolean init_once_;
  
  // The follow fields used for GCM.
  public static final String PROPERTY_REG_ID = "gcm_registration_id";
  private static final String PROPERTY_APP_VERSION = "Jact_v1.0.0";
  private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
  private final static String SENDER_ID = "404003292102";  // From GCM Project Number
  GoogleCloudMessaging gcm_;
  AtomicInteger msg_id_ = new AtomicInteger();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState, R.string.app_name,
    		       R.layout.jact_logged_in_home_screen,
    		       JactNavigationDrawer.ActivityIndex.PROFILE);
    jact_website_ = GetUrlTask.JACT_DOMAIN + "/rest/userpoints/";
    init_once_ = false;
    
    // Start the Service that will run in background and handle GCM interactions.
    if (CheckPlayServices()) {
      gcm_ = GoogleCloudMessaging.getInstance(this);
      String reg_id = GetRegistrationId(getApplicationContext());

      if (reg_id.isEmpty()) {
        RegisterInBackground();
      }
    }
    startService(new Intent(this, GcmIntentService.class));
  }
  
  @Override
  protected void onResume() {
	super.onResume();	
	
	// Make sure Google Play is on user's device (so they can use GCM).
	CheckPlayServices();
    
	// Check if the Logged-In State is ready (all user info has already been fetched).
    SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    String was_logged_off = user_info.getString(getString(R.string.was_logged_off_key), "");
    String was_logged_off_other_check = getIntent().getStringExtra(getString(R.string.was_logged_off_key));
    boolean was_logged_off_other_check_bool =
    	(was_logged_off_other_check != null) && was_logged_off_other_check.equalsIgnoreCase("true");
    // Logged-In State is not ready. Fetch requisite items.
    if (!init_once_ || num_server_tasks_ != 0 || !was_logged_off.equalsIgnoreCase("false") ||
    	was_logged_off_other_check_bool) {
      num_server_tasks_ = 0;
      init_once_ = true;
      
      // Retrieve information that was retrieved from Jact Server on inital login.
      jact_user_name_ = user_info.getString(getString(R.string.ui_user_name), "");
      jact_user_id_ = user_info.getString(getString(R.string.ui_user_id), "");
      jact_profile_pic_url_ = user_info.getString(getString(R.string.ui_user_icon_url), "");
      jact_user_points_ = user_info.getString(getString(R.string.ui_user_points), "");
      if (jact_user_name_.isEmpty() || jact_user_id_.isEmpty() || jact_profile_pic_url_.isEmpty()) {
        Log.e("PHB ERROR", "JactLoggedInHomeActivity.onCreate:\n" +
                           "User Info file is missing the requisite info.");
      }
      
      // Set Username in UI.
      SetUserName();
      // Get UserPoints from Jact Server.
      GetUserPoints();
      // Get User Avatar from Jact Server.
      GetAvatar();
    }
    
    // Set Cart Icon.
	SetCartIcon(this);
    
    // Re-enable parent activity before transitioning to the next activity.
    // This ensures e.g. that when user hits 'back' button, the screen
    // is 'active' (not faded) when the user returns.
    fadeAllViews(num_server_tasks_ != 0);
  }
  
  @Override
  public void fadeAllViews(boolean should_fade) {
    ProgressBar spinner = (ProgressBar) findViewById(R.id.my_profile_progress_bar);
    AlphaAnimation alpha;
    if (should_fade) {
      spinner.setVisibility(View.VISIBLE);
      alpha = new AlphaAnimation(0.5F, 0.5F);
    } else {
      spinner.setVisibility(View.INVISIBLE);
      alpha = new AlphaAnimation(1.0F, 1.0F);
    }
    alpha.setDuration(0); // Make animation instant
    alpha.setFillAfter(true); // Tell it to persist after the animation ends
    RelativeLayout layout = (RelativeLayout) findViewById(R.id.my_profile_content_frame);
    layout.startAnimation(alpha); // Add animation to the layout.
  }
  
  private void GetAvatar() {
    if (jact_profile_pic_url_.isEmpty() ||
    	jact_profile_pic_url_.equalsIgnoreCase(getString(R.string.null_str))) {
      Log.i("PHB", "JLIHA::GetAvatar. Empty pic, displaying default");
      ImageView image = (ImageView) this.findViewById(R.id.user_profile_pic);
      image.setImageResource(R.drawable.ic_launcher_transparent);
    } else {
      num_server_tasks_++;
      ImageView image = (ImageView) this.findViewById(R.id.user_profile_pic);
      image.setImageResource(R.drawable.no_image);
	  new GetUrlTask(this, GetUrlTask.TargetType.IMAGE).
	     execute(jact_profile_pic_url_, "GET", "", "", PROFILE_PIC);
    }
  }
  
  private void GetUserPoints() {
    if (jact_user_points_.isEmpty()) {
      num_server_tasks_++;
      TextView tv = (TextView) this.findViewById(R.id.current_points);
      tv.setText(" User Points:  ...");
      new GetUrlTask(this, GetUrlTask.TargetType.JSON).execute(
          jact_website_ + jact_user_id_ + ".json",
          "GET", "", "", USER_POINTS);
    } else {
      TextView tv = (TextView) this.findViewById(R.id.current_points);
      tv.setText(" User Points:  " + jact_user_points_);
    }
  }
  
  private void SetLoginTrue() {
    SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    SharedPreferences.Editor editor = user_info.edit();
    editor.putString(getString(R.string.was_logged_off_key), "false");
    editor.commit();
  }
  
  private void SetUserName() {
    TextView tv =
        (TextView) this.findViewById(R.id.logged_in_home_welcome_textview);
    if (!jact_user_name_.isEmpty()) {
      tv.setText("Welcome, " + jact_user_name_);
    } else {
      tv.setText("Welcome, Unknown Jact User");
    }
  }
  
  private void SetProfilePic(Bitmap profile_pic_bitmap) {
    ImageView image = (ImageView) this.findViewById(R.id.user_profile_pic);
    image.setImageBitmap(profile_pic_bitmap);
  }

  private void SetUserPointsPic(Bitmap userpoints_pic_bitmap) {
    ImageView image = (ImageView) this.findViewById(R.id.userpoints_pic);
    image.setImageBitmap(userpoints_pic_bitmap);
  }
  
  private void SetUserPoints(String points) {
    points = points.replace("[", "");
    points = points.replace("]", "");
    points = points.replaceAll("\n", "");
    String points_str = "";
    try {
      int user_points = Integer.parseInt(points);
      points_str = NumberFormat.getNumberInstance(Locale.US).format(user_points);
      String to_display = " User Points:  " + points_str;
      TextView tv = (TextView) this.findViewById(R.id.current_points);
      tv.setText(to_display);
    } catch (NumberFormatException e) {
      Log.e("PHB ERROR", "JLIHA::SetUserPoints. Error: could not parse points into an int: " +
                   points + "\n" + e.getMessage());
      // TODO(PHB): Handle exception.
      return;
    }
    SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    SharedPreferences.Editor editor = user_info.edit();
    editor.putString(getString(R.string.ui_user_points), points_str);
    editor.commit();
  }

// =============================================================================
// GCM Methods.
//=============================================================================
  
  /**
   * Check the device to make sure it has the Google Play Services APK. If
   * it doesn't, display a dialog that allows users to download the APK from
   * the Google Play Store or enable it in the device's system settings.
   */
  private boolean CheckPlayServices() {
    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
    if (resultCode != ConnectionResult.SUCCESS) {
      if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
        GooglePlayServicesUtil.getErrorDialog(
        	resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
      } else {
        Log.i("JactLoginActivity::CheckPlayServices", "This device is not supported.");
        finish();
      }
      return false;
    }
    return true;
  }
  
  /**
   * Gets the current registration ID for application on GCM service.
   * <p>
   * If result is empty, the app needs to register.
   *
   * @return registration ID, or empty string if there is no existing
   *         registration ID.
   */
  private String GetRegistrationId(Context context) {
      final SharedPreferences prefs = GetGCMPreferences(context);
      String registrationId = prefs.getString(PROPERTY_REG_ID, "");
      if (registrationId.isEmpty()) {
          Log.i("JactLoginActivity::CheckPlayServices", "Registration not found.");
          return "";
      }
      // Check if app was updated; if so, it must clear the registration ID
      // since the existing registration ID is not guaranteed to work with
      // the new app version.
      int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
      int currentVersion = GetAppVersion(context);
      if (registeredVersion != currentVersion) {
          Log.i("JactLoginActivity::CheckPlayServices", "App version changed.");
          return "";
      }
      return registrationId;
  }

  /**
   * @return Application's {@code SharedPreferences}.
   */
  private SharedPreferences GetGCMPreferences(Context context) {
      // This sample app persists the registration ID in shared preferences, but
      // how you store the registration ID in your app is up to you.
      return getSharedPreferences(JactLoginActivity.class.getSimpleName(),
              Context.MODE_PRIVATE);
  }
  
  /**
   * @return Application's version code from the {@code PackageManager}.
   */
  private static int GetAppVersion(Context context) {
      try {
        PackageInfo packageInfo =
        	context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        return packageInfo.versionCode;
      } catch (NameNotFoundException e) {
        // should never happen
        throw new RuntimeException("Could not get package name: " + e.getMessage());
      }
  }
  
  /**
   * Registers the application with GCM servers asynchronously.
   * <p>
   * Stores the registration ID and app versionCode in the application's
   * shared preferences.
   */
  private void RegisterInBackground() {
    new AsyncTask<Void, String, String>() {
        @Override
        protected String doInBackground(Void... params) {
          Log.e("PHB TEMP", "JLHA::RegisterInBackground::doInBackground.");
          String msg = "";
          try {
            if (gcm_ == null) {
              gcm_ = GoogleCloudMessaging.getInstance(getApplicationContext());
            }
            String regid = gcm_.register(SENDER_ID);
            msg = "Device registered, registration ID=" + regid;

            // You should send the registration ID to your server over HTTP,
            // so it can use GCM/HTTP or CCS to send messages to your app.
            // The request to your server should be authenticated if your app
            // is using accounts.
            SendRegistrationIdToBackend();

            // For this demo: we don't need to send it because the device
            // will send upstream messages to a server that echo back the
            // message using the 'from' address in the message.

            // Persist the registration ID - no need to register again.
            StoreRegistrationId(getApplicationContext(), regid);
          } catch (IOException ex) {
            msg = "Error :" + ex.getMessage();
            // If there is an error, don't just keep trying to register.
            // Require the user to click a button again, or perform
            // exponential back-off.
          }
          return msg;
        }

        @Override
        protected void onPostExecute(String msg) {
          Log.e("PHB TEMP", "JLHA::RegisterInBackground::onPostExecute. msg: " + msg);
          //mDisplay.append(msg + "\n");
        }
    }.execute(null, null, null);
  }
  
  /**
   * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP
   * or CCS to send messages to your app. Not needed for this demo since the
   * device sends upstream messages to a server that echoes back the message
   * using the 'from' address in the message.
   */
  private void SendRegistrationIdToBackend() {
	Log.e("PHB TEMP", "JLHA::SendRegistrationIdToBackend");
	Intent registration_intent = new Intent("com.google.android.c2dm.intent.REGISTER");
	registration_intent.putExtra(
		"app", PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(), 0));
	registration_intent.putExtra("sender", jact_user_name_);
	getApplicationContext().startService(registration_intent);
  }
  
  /**
   * Stores the registration ID and app versionCode in the application's
   * {@code SharedPreferences}.
   *
   * @param context application's context.
   * @param regId registration ID
   */
  private void StoreRegistrationId(Context context, String regId) {
	  Log.e("PHB TEMP", "JLHA::StoreRegistrationId");
      final SharedPreferences prefs = GetGCMPreferences(context);
      int appVersion = GetAppVersion(context);
      Log.i("JactLoginActivity::CheckPlayServices", "Saving regId on app version " + appVersion);
      SharedPreferences.Editor editor = prefs.edit();
      editor.putString(PROPERTY_REG_ID, regId);
      editor.putInt(PROPERTY_APP_VERSION, appVersion);
      editor.commit();
  }
  
  // Temporarily testing sending upstream message from App to Jact GCM.
  public void doSendUpstreamMessageClick(final View view) {
	new AsyncTask<Void, String, String>() {
	    @Override
	    protected String doInBackground(Void... params) {
	      Log.e("PHB TEMP", "JLHA::doSendUpstreamMessageClick");
	      String msg = "";
	      try {
	        Bundle data = new Bundle();
	        data.putString("my_message", "Hello World");
	        data.putString("my_action", "com.google.android.gcm.demo.app.ECHO_NOW");
	        String id = Integer.toString(msg_id_.incrementAndGet());
	        gcm_.send(SENDER_ID + "@gcm.googleapis.com", id, data);
	        msg = "Sent message";
	      } catch (IOException ex) {
	        msg = "Error :" + ex.getMessage();
	      }
	      return msg;
	    }

	    @Override
	    protected void onPostExecute(String msg) {
	      Log.e("PHB TEMP", "JLHA::onSendUpstreamMessageClick::onPostExecute. msg: " + msg);
	      //mDisplay.append(msg + "\n");
	    }
	}.execute(null, null, null);
  }

// =============================================================================
// END GCM Methods.
// =============================================================================
 
  
// =============================================================================
// ProcessUrlResponse Override Methods.
// =============================================================================

  @Override
  public void ProcessUrlResponse(String webpage, String cookies, String extra_params) {
    if (extra_params.isEmpty()) {
      Log.e("PHB ERROR", "JLIHA::ProcessUrlResponse. Error: JactLoggedInHomeActivity has multiple calls " +
                         "to GetUrlTask; in order to properly handle the response, " +
    		             "must specify desired action via extra_params");
    } else if (extra_params.equalsIgnoreCase(USER_POINTS)) {
    	SetUserPoints(webpage);
    } else if (extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_GET_CART_TASK) == 0) {
  	  SaveCookies(cookies);
  	  GetCart(this);
  	} else if (extra_params.indexOf(ShoppingUtils.GET_CART_TASK) == 0) {
  	  if (!ShoppingCartActivity.AccessCart(ShoppingCartActivity.CartAccessType.SET_CART_FROM_WEBPAGE, webpage)) {
  		// TODO(PHB): Handle this gracefully (popup a dialog).
  		Log.e("PHB ERROR", "JactActionBarActivity::ProcessCartResponse. Unable to parse cart response:\n" + webpage);
  	  }
  	} else {
      Log.e("PHB ERROR", "JLIHA::ProcessUrlResponse. Error: Unrecognized extra params: " + extra_params);
    }
    num_server_tasks_--;
	if (num_server_tasks_ == 0) {
	  SetLoginTrue();
	  SetCartIcon(this);
	  if (num_server_tasks_ == 0) {
	    fadeAllViews(false);
	  }
	}
  }
  
  @Override
  public void ProcessUrlResponse(Bitmap pic, String cookies, String extra_params) {
    if (extra_params.isEmpty()) {
      Log.e("PHB ERROR", "JLIHA::ProcessUrlResponse. Error: JactLoggedInHomeActivity has multiple calls " +
                         "to GetUrlTask; in order to properly handle the response, " +
      		             "must specify desired action via extra_params");
    } else if (extra_params == BUX_LOGO) {
    	SetUserPointsPic(pic);
    } else if (extra_params == PROFILE_PIC) {
    	SetProfilePic(pic);
  	} else {
        Log.e("PHB ERROR", "JLIHA::ProcessUrlResponse. Error: Unrecognized extra params: " + extra_params);
    }
    num_server_tasks_--;
	if (num_server_tasks_ == 0) {
	  SetLoginTrue();
	  SetCartIcon(this);
	  if (num_server_tasks_ == 0) {
	    fadeAllViews(false);
	  }
	}
  }

  @Override
  public void ProcessFailedResponse(FetchStatus status, String extra_params) {
	// TODO(PHB): Implement this.
	Log.e("PHB ERROR", "JLIHA::ProcessFailedResponse. Status: " + status);
    if (extra_params.indexOf(ShoppingUtils.GET_CART_TASK) == 0) {
  	  GetCookiesThenGetCart(this);
  	  return;
    }
	num_server_tasks_--;
	if (num_server_tasks_ == 0) {
	  fadeAllViews(false);
	  SetLoginTrue();
	}
  }
}
// =============================================================================
// END ProcessUrlResponse Override Methods.
// =============================================================================
