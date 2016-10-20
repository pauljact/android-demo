package com.jact.jactapp;

import com.jact.jactapp.GetUrlTask.FetchStatus;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.Locale;

public abstract class JactActionBarActivity extends ActionBarActivity implements ProcessUrlResponseCallback {
  protected Menu menu_bar_;
  protected static String jact_shopping_cart_url_;
  protected int num_server_tasks_;
  protected JactNavigationDrawer navigation_drawer_;
  protected boolean can_show_dialog_;
  protected static boolean is_popup_showing_;
  protected JactDialogFragment dialog_;
  protected int activity_id_;
  protected static final String USER_POINTS = "user_points";
  public static final boolean IS_PRODUCTION = false;
  public static final boolean USE_MOBILE_SITE = true;
	
  protected void onCreate(Bundle savedInstanceState, int activity_id,
		                  int layout, JactNavigationDrawer.ActivityIndex index) {
    super.onCreate(savedInstanceState);

    can_show_dialog_ = false;
	if (!IsLoggedOn()) {
	  if (!JactActionBarActivity.IS_PRODUCTION) Log.w("JactActionBarActivity::onCreate", "Not logged on. Returning to Login Activity...");
	  onBackPressed();
	  finish();
	}
	
    jact_shopping_cart_url_ = GetUrlTask.GetJactDomain() + "/rest/cart.json";
    num_server_tasks_ = 0;
    is_popup_showing_ = false;
    setContentView(layout);
    Toolbar toolbar = (Toolbar) findViewById(R.id.jact_toolbar);
    TextView ab_title = (TextView) findViewById(R.id.toolbar_title_tv);
    activity_id_ = activity_id;
    //PHB_OLDab_title.setText(activity_id);
    ab_title.setText("");
    setSupportActionBar(toolbar);
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    // Set Navigation Drawer.
    navigation_drawer_ =
        new JactNavigationDrawer(this,
          		                 findViewById(R.id.activity_drawer_layout),
          		                 findViewById(R.id.nav_left_drawer),
          		                 index);

  }
  
  @Override
  protected void onResume() {
	// There may be ways to get here even when user has 'signed off'; for example, a GCM
	// Notification may have set a PendingIntent to open this. In that case, make sure we
	// are logged in; otherwise, return to home screen.
	if (!IsLoggedOn()) {
	  if (!JactActionBarActivity.IS_PRODUCTION) Log.w("JactActionBarActivity::onResume", "Not logged on. Returning to Login Activity...");
	  onBackPressed();
	  finish();
	}
	  
	num_server_tasks_ = 0;
	super.onResume();
    jact_shopping_cart_url_ = GetUrlTask.GetJactDomain() + "/rest/cart.json";
    can_show_dialog_ = true;
    GetUserPoints();
  }
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    can_show_dialog_ = true;
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    navigation_drawer_.onPostCreate(savedInstanceState);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    navigation_drawer_.onConfigurationChanged(newConfig);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu items for use in the action bar.
    getMenuInflater().inflate(R.menu.action_bar, menu);
    menu_bar_ = menu;
	ShoppingCartActivity.SetCartIcon(menu_bar_);
	return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (navigation_drawer_.onOptionsItemSelected(item)) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
  
  @Override
  protected void onPause() {
    super.onPause();
    can_show_dialog_ = false;
  }

  public void SetActivityId(int id) {
    activity_id_ = id;
  }

  public boolean IsLoggedOn() {
	SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    return user_info.getBoolean(getString(R.string.ui_is_logged_in), false);
  }
  
  public void SetLoggedOff() {
	SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    SharedPreferences.Editor editor = user_info.edit();
    editor.putBoolean(getString(R.string.ui_is_logged_in), false);
    editor.commit();
  }
  
  public abstract void fadeAllViews(boolean should_fade);

  protected void GetUserPoints() {
    SharedPreferences user_info =
        getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    String user_id = user_info.getString(getString(R.string.ui_user_id), "");
    if (user_id.isEmpty()) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("JactActionBarActivity::GetUserPoints", "Empty User Id.");
    }
    String points_url = GetUrlTask.GetJactDomain() + "/rest/userpoints/" + user_id + ".json";
    IncrementNumRequestsCounter();
    new GetUrlTask(this, GetUrlTask.TargetType.JSON).execute(
            points_url,
            "GET", "", "", USER_POINTS);
  }

  protected void SetUserPoints(String points) {
    points = points.replace("[", "");
    points = points.replace("]", "");
    points = points.replaceAll("\n", "");
    String points_str = "";
    try {
      int user_points = Integer.parseInt(points);
      points_str = NumberFormat.getNumberInstance(Locale.US).format(user_points);
      SetUserPointsInActionBar(points_str);
    } catch (NumberFormatException e) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("JactActionBarActivity::SetUserPoints",
              "Unable to parse points as an int: '" + points + "'");
      // TODO(PHB): Handle exception.
      return;
    }
    SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    SharedPreferences.Editor editor = user_info.edit();
    editor.putString(getString(R.string.ui_user_points), points_str);
    editor.commit();
  }

  protected void SetUserPointsInActionBar(String points) {
    if (menu_bar_ == null) {
      return;
    }
    TextView ab_points = (TextView) findViewById(R.id.toolbar_title_points);
    ab_points.setText(points);
    TextView ab_title = (TextView) findViewById(R.id.toolbar_title_tv2);
    ab_title.setText(activity_id_);
    //PHB_OLD menu_bar_.findItem(R.id.menu_jact_points).setTitle(points);
  }

  protected void SetUserPoints() {
    if (menu_bar_ == null) {
      return;
    }
    SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    String user_points = user_info.getString(getString(R.string.ui_user_points), "");
    if (user_points.isEmpty()) return;
    SetUserPointsInActionBar(user_points);
  }

  protected void SetCartIcon(ProcessUrlResponseCallback callback) {
    if (menu_bar_ == null) {
      return;
    }
  	// Make sure shopping cart has been initialized. If not, fetch it from server.
    if (!ShoppingCartActivity.AccessCart(
		    ShoppingCartActivity.CartAccessType.INITIALIZE_CART)) {
  	  // Cart is fresh. Simply display cart icon.
  	  ShoppingCartActivity.SetCartIcon(menu_bar_);
  	  return;
  	}
  	GetCart(callback);
  }
  
  protected void GetCart(ProcessUrlResponseCallback callback) {
    // Initialize cart, in case it hasn't been created yet.
    ShoppingCartActivity.AccessCart(
            ShoppingCartActivity.CartAccessType.INITIALIZE_CART);
  	// Need cookies to fetch server's cart.
    SharedPreferences user_info = getSharedPreferences(
        getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
    String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
    if (cookies.isEmpty()) {
      String username = user_info.getString(getString(R.string.ui_username), "");
      String password = user_info.getString(getString(R.string.ui_password), "");
      num_server_tasks_++;
      ShoppingUtils.RefreshCookies(
          callback, username, password, ShoppingUtils.GET_COOKIES_THEN_GET_CART_TASK);
      return;
    }
  	 
    // Fetch cart from server.
    num_server_tasks_++;
    GetUrlTask task = new GetUrlTask(callback, GetUrlTask.TargetType.JSON);
  	GetUrlTask.UrlParams params = new GetUrlTask.UrlParams();
  	params.url_ = jact_shopping_cart_url_;
  	params.connection_type_ = "GET";
  	params.extra_info_ = ShoppingUtils.GET_CART_TASK;
  	params.cookies_ = cookies;
  	task.execute(params);
  	return;
  }
  
  protected void SaveCookies(String cookies) {
      SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
      SharedPreferences.Editor editor = user_info.edit();
      editor.putString(getString(R.string.ui_session_cookies), cookies);
      editor.commit();
  }
  
  protected void SaveCsrfToken(String token) {
	  // TODO(PHB): I have temporarily? commented this out, as it can end in an infinite loop, e.g.
	  // when the actual error is NOT an issue with the CSRF token, but GetUrlTask identifies
	  // CSRF as the issue. Instead, may be safer to reset this only when re-starting one of
	  // the shopping-related activities (ProductsActivity and ShoppingCartActivity), which
	  // is what I do now.
	  // ShoppingCartActivity.ResetNumCsrfRequests();
      SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
      SharedPreferences.Editor editor = user_info.edit();
    editor.putString(getString(R.string.ui_csrf_token), token);
      editor.commit();
  }
  protected void GetCookiesThenGetCart(ProcessUrlResponseCallback callback) {
   	SharedPreferences user_info = getSharedPreferences(
         getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
   	String username = user_info.getString(getString(R.string.ui_username), "");
   	String password = user_info.getString(getString(R.string.ui_password), "");
    ShoppingUtils.RefreshCookies(
            callback, username, password, ShoppingUtils.GET_COOKIES_THEN_GET_CART_TASK);
  }
  
  protected void GetCookiesThenClearCart(ProcessUrlResponseCallback callback) {
   	SharedPreferences user_info = getSharedPreferences(
         getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
   	String username = user_info.getString(getString(R.string.ui_username), "");
   	String password = user_info.getString(getString(R.string.ui_password), "");
    ShoppingUtils.RefreshCookies(
            callback, username, password, ShoppingUtils.GET_COOKIES_THEN_CLEAR_CART_TASK);
  }
  
  protected void ProcessCartResponse(ProcessUrlResponseCallback callback,
		                             String webpage, String cookies, String extra_params) {
	num_server_tasks_--;
	if (extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_GET_CART_TASK) == 0) {
	  SaveCookies(cookies);
	  GetCart(callback);
	} else if (extra_params.indexOf(ShoppingUtils.GET_CART_TASK) == 0) {
	  if (!ShoppingCartActivity.AccessCart(ShoppingCartActivity.CartAccessType.SET_CART_FROM_WEBPAGE, webpage)) {
		// TODO(PHB): Handle this gracefully (popup a dialog).
		if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "JactActionBarActivity::ProcessCartResponse. Unable to parse cart response:\n" + webpage);
		return;
	  }
	  SetCartIcon(callback);
    } else if (extra_params.equalsIgnoreCase(USER_POINTS)) {
      SetUserPoints(webpage);
	} else {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "JactActionBarActivity::ProcessCartResponse. Returning from " +
                         "unrecognized task: " + GetUrlTask.PrintExtraParams(extra_params));
    }
	if (num_server_tasks_ == 0) {
	  fadeAllViews(false);
	}
  }

  protected void ProcessFailedCartResponse(ProcessUrlResponseCallback callback,
		                                   FetchStatus status, String extra_params) {
	num_server_tasks_--;
	if (extra_params.indexOf(ShoppingUtils.GET_CART_TASK) == 0) {
	  GetCookiesThenGetCart(callback);
	} else if (extra_params.indexOf(ShoppingUtils.CLEAR_CART_TASK) == 0) {
	  GetCookiesThenClearCart(callback);
    } else if (extra_params.equalsIgnoreCase(USER_POINTS)) {
      // TODO(PHB): Handle this: should do the right thing for all cases we get here; in
      // particular, should never get here (all cases should be accounted for in the 'else if'
      // statements above.
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("JactActionBarActivity::ProcessFailedResponse",
              "Status: " + status + ", extra_params: " + GetUrlTask.PrintExtraParams(extra_params));
	} else {
	  // TODO(PHB): Handle this: should do the right thing for all cases we get here; in
	  // particular, should never get here (all cases should be accounted for in the 'else if'
	  // statements above.
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("JactActionBarActivity::ProcessFailedResponse",
    		"Status: " + status + "; extra_params: " + GetUrlTask.PrintExtraParams(extra_params));
    }
  }


  public boolean GetGcmDisabled() {
    SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    String user_disabled_gcm = user_info.getString(getString(R.string.gcm_disabled_key), "false");
    return (user_disabled_gcm != null) && user_disabled_gcm.equalsIgnoreCase("true");
  }

  private void SetGcmDisabled(boolean disabled) {
    SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    SharedPreferences.Editor editor = user_info.edit();
    String is_disabled = disabled ? "true" : "false";
    editor.putString(getString(R.string.gcm_disabled_key), is_disabled);
    editor.commit();
  }

  protected void DisplayPopupWithButtonsFragment(String title, String message, String id, String button_one, String button_two) {
    if (can_show_dialog_ && !is_popup_showing_) {
      is_popup_showing_ = true;
      dialog_ = new JactDialogFragment();
      dialog_.SetTitle(title);
      dialog_.SetMessage(message);
      if (button_one != null && !button_one.isEmpty()) {
        dialog_.SetButtonOneText(button_one);
      }
      if (button_two != null && !button_two.isEmpty()) {
        dialog_.SetButtonTwoText(button_two);
      }
      dialog_.show(getSupportFragmentManager(), id);
    } else {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("JactActionBarActivity::DisplayPopupWithButtonsFragment",
              "Unable to show PopUp with title: " + title + " and message: " + message);
    }
  }

  protected void DisplayPopupFragment(String title, String message, String id) {
	if (can_show_dialog_ && !is_popup_showing_) {
      is_popup_showing_ = true;
  	  dialog_ = new JactDialogFragment();
      dialog_.SetTitle(title);
      dialog_.SetMessage(message);
  	  dialog_.show(getSupportFragmentManager(), id);
	} else {
	  if (!JactActionBarActivity.IS_PRODUCTION) Log.e("JactActionBarActivity::DisplayPopupFragment",
			"Unable to show PopUp with title: " + title + " and message: " + message);
	}
  }
  
  protected void DisplayPopupFragment(String title, String id) {
	if (can_show_dialog_ && !is_popup_showing_) {
      is_popup_showing_ = true;
  	  dialog_ = new JactDialogFragment();
      dialog_.SetTitle(title);
  	  dialog_.show(getSupportFragmentManager(), id);
	} else {
	  if (!JactActionBarActivity.IS_PRODUCTION) Log.e("JactActionBarActivity::DisplayPopupFragment",
            "Unable to show PopUp with title: " + title);
	}
  }
  
  public void doDialogOkClick(View view) {
    // Handle "OK" pressed on Enable/Disable Jact Notifications.
    if (dialog_.getTag() != null && dialog_.getTag().equals(getString(R.string.gcm_disable_dialog))) {
      // Get current state of GCM.
      boolean currently_disabled = GetGcmDisabled();

      if (currently_disabled) {
        // Old state was disabled; re-enable.
        SetGcmDisabled(false);
        LocalBroadcastManager.getInstance(this).registerReceiver(JactLoggedInHomeActivity.GetGcmReceiver(),
                new IntentFilter("registrationComplete"));
        if (!JactActionBarActivity.IS_PRODUCTION) {
          Log.e("PHB TEMP", "DIALOG OK: GCM Re-Enabled");
        }
      } else {
        // Old state was enabled; disable.
        SetGcmDisabled(true);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(JactLoggedInHomeActivity.GetGcmReceiver());
        if (!JactActionBarActivity.IS_PRODUCTION) {
          Log.e("PHB TEMP", "DIALOG OK: GCM Disabled");
        }
      }
    }

	// Close Dialog window.
    is_popup_showing_ = false;
	dialog_.dismiss();
  }

  public void doDialogCancelClick(View view) {
    // Close Dialog window.
    is_popup_showing_ = false;
    dialog_.dismiss();
  }

  @Override
  public void IncrementNumRequestsCounter() {
	num_server_tasks_++;
  }

  @Override
  public void DecrementNumRequestsCounter() {
	num_server_tasks_--;
  }
  
  @Override
  public int GetNumRequestsCounter() {
	return num_server_tasks_;
  }
  
  @Override
  public void DisplayPopup(String message) {
	DisplayPopupFragment(message, message);
  }
  
  @Override
  public void DisplayPopup(String title, String message) {
	DisplayPopupFragment(title, message, title);
  }
  
  // Dummy function that each extending class can use to do something.
  public boolean doSomething(String info) { return true; }
  }