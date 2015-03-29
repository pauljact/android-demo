package com.example.jactfirstdemo;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public abstract class JactActionBarActivity extends ActionBarActivity {
  protected Menu menu_bar_;
  protected static final String jact_shopping_cart_url_ = "https://us7.jact.com:3081/rest/cart.json";
  protected int num_server_tasks_;
	
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }
  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return super.onOptionsItemSelected(item);
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
	} else {
      Log.e("PHB ERROR", "JactActionBarActivity::ProcessFailedResponse. Status: " + status +
	                     "; extra_params: " + extra_params);
    }
  }
  
  // Dummy function that each extending class can use to do something.
  public void doSomething(String info) {}
  }