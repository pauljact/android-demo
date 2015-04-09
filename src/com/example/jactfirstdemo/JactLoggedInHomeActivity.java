package com.example.jactfirstdemo;

import java.text.NumberFormat;
import java.util.Locale;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
  
  private static String jact_website_ = "https://us7.jact.com:3081/rest/userpoints/";
  //private static String jact_website_ = "http://us7.jact.com:3080/rest/userpoints/";
  private String jact_user_name_;
  private String jact_user_id_;
  private String jact_profile_pic_url_;
  private String jact_user_points_;
  private boolean init_once_;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState, R.string.app_name,
    		       R.layout.jact_logged_in_home_screen,
    		       JactNavigationDrawer.ActivityIndex.PROFILE);
    init_once_ = false;
  }
  
  @Override
  protected void onResume() {
    // Check if the Logged-In State is ready (all user info has already been fetched).
    SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    String is_logged_off = user_info.getString(getString(R.string.logged_off_key), "");
    // Logged-In State is not ready. Fetch requisite items.
    if (!init_once_ || num_server_tasks_ != 0 || !is_logged_off.equalsIgnoreCase("false")) {
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
	super.onResume();
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
    editor.putString(getString(R.string.logged_off_key), "false");
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