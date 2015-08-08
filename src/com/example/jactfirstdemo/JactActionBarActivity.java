package com.jact.jactfirstdemo;

import com.jact.jactfirstdemo.GetUrlTask.FetchStatus;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public abstract class JactActionBarActivity extends ActionBarActivity implements ProcessUrlResponseCallback {
  protected Menu menu_bar_;
  protected static String jact_shopping_cart_url_;
  protected int num_server_tasks_;
  protected JactNavigationDrawer navigation_drawer_;
  protected boolean can_show_dialog_;
  protected JactDialogFragment dialog_;
	
  protected void onCreate(Bundle savedInstanceState, int activity_id,
		                  int layout, JactNavigationDrawer.ActivityIndex index) {
    super.onCreate(savedInstanceState);

    can_show_dialog_ = false;
	if (!IsLoggedOn()) {
	  Log.w("JactActionBarActivity::onCreate", "Not logged on. Returning to Login Activity...");
	  onBackPressed();
	  finish();
	}
	
    jact_shopping_cart_url_ = GetUrlTask.JACT_DOMAIN + "/rest/cart.json";
    num_server_tasks_ = 0;
    setContentView(layout);
    Toolbar toolbar = (Toolbar) findViewById(R.id.jact_toolbar);
    TextView ab_title = (TextView) findViewById(R.id.toolbar_title_tv);
    ab_title.setText(activity_id);
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
	  Log.w("JactActionBarActivity::onResume", "Not logged on. Returning to Login Activity...");
	  onBackPressed();
	  finish();
	}
	  
	num_server_tasks_ = 0;
	super.onResume();
    can_show_dialog_ = true;
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
		Log.e("PHB ERROR", "JactActionBarActivity::ProcessCartResponse. Unable to parse cart response:\n" + webpage);
		return;
	  }
	  SetCartIcon(callback);
	} else {
      Log.e("PHB ERROR", "JactActionBarActivity::ProcessCartResponse. Returning from " +
                         "unrecognized task: " + extra_params);
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
	} else {
	  // TODO(PHB): Handle this: should do the right thing for all cases we get here; in
	  // particular, should never get here (all cases should be accounted for in the 'else if'
	  // statements above.
      Log.e("JactActionBarActivity::ProcessFailedResponse",
    		"Status: " + status + "; extra_params: " + extra_params);
    }
  }

  protected void DisplayPopupFragment(String title, String message, String id) {
	if (can_show_dialog_) {
  	  dialog_ = new JactDialogFragment(title, message);
  	  dialog_.show(getSupportFragmentManager(), id);
	} else {
	  Log.e("JactActionBarActivity::DisplayPopupFragment",
			"Unable to show Popup with title: " + title + " and message: " + message);	
	}
  }
  
  protected void DisplayPopupFragment(String title, String id) {
	if (can_show_dialog_) {
  	  dialog_ = new JactDialogFragment(title);
  	  dialog_.show(getSupportFragmentManager(), id);
	} else {
	  Log.e("JactActionBarActivity::DisplayPopupFragment", "Unable to show Popup with title: " + title);	
	}
  }
  
  public void doDialogOkClick(View view) {
	  // Close Dialog window.
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