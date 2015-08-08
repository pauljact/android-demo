package com.jact.jactfirstdemo;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.List;

import com.jact.jactfirstdemo.GetUrlTask.FetchStatus;
import com.jact.jactfirstdemo.JactNavigationDrawer.ActivityIndex;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public class ViewOrdersActivity extends JactActionBarActivity implements ProcessUrlResponseCallback {
  private static String order_id_;
  private static String orders_url_;

  public static synchronized void SetOrderId(String order_id) {
    order_id_ = order_id;
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState, R.string.checkout_label,
    		       R.layout.checkout_mobile_layout,
    		       JactNavigationDrawer.ActivityIndex.CHECKOUT_VIA_MOBILE_SITE);
    
    // Get User Id.
    if (order_id_ == null) order_id_ = "";
    orders_url_ = "";
    SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    String jact_user_id = user_info.getString(getString(R.string.ui_user_id), "");
    if (!jact_user_id.isEmpty()) {
      orders_url_ = GetUrlTask.GetJactDomain() + "/user/" + jact_user_id + "/orders/";
    }
  }
    
  @Override
  protected void onResume() {
	super.onResume();
    navigation_drawer_.setActivityIndex(ActivityIndex.VIEW_ORDERS);
    
    // Sanity Check we're here legitimately.
    if (orders_url_.isEmpty()) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ViewOrdersActivity::onResume", "No user id found. Aborting call to View Orders.");
      // TODO(PHB): Handle this failure case (i.e. should abort, going back to main Profile page).
      // For now, I just do onBackPressed, which maybe is correct?
      onBackPressed();
      return;
    }
    
    // Set cookies for WebView.
	SharedPreferences user_info = getSharedPreferences(
        getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
    String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
    List<String> cookie_headers = Arrays.asList(cookies.split(GetUrlTask.COOKIES_SEPERATOR));
    HttpCookie cookie = null;
    for (String cookie_str : cookie_headers) {
      cookie = HttpCookie.parse(cookie_str).get(0);
    }
    if (cookie != null) {
      CookieManager cookie_manager = CookieManager.getInstance();
      cookie_manager.setCookie(
    		  orders_url_ + order_id_,
    		  cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain());
    }
    // Set webview from order_url_.
    if (!JactActionBarActivity.IS_PRODUCTION) Log.i("ViewOrdersActivity::onResume", "Loading View Orders webpage with url: " + orders_url_ + order_id_);
    WebView web_view = (WebView) findViewById(R.id.checkout_mobile_webview);
    web_view.loadUrl(orders_url_ + order_id_);
    web_view.setWebViewClient(new JactWebViewClient(this));
    
    // Set spinner (and hide WebView) until page has finished loading.
    SetCartIcon(this);
    fadeAllViews(num_server_tasks_ > 0);
  }

  @Override
  public void fadeAllViews(boolean should_fade) {
    ProgressBar spinner = (ProgressBar) findViewById(R.id.checkout_mobile_progress_bar);
    AlphaAnimation alpha;
    if (should_fade) {
      spinner.setVisibility(View.VISIBLE);
      alpha = new AlphaAnimation(0.5F, 0.5F);
    } else {
      spinner.setVisibility(View.INVISIBLE);
      alpha = new AlphaAnimation(1.0F, 1.0F);
    }
    // The AlphaAnimation will make the whole content frame transparent
    // (so that none of the views show).
    alpha.setDuration(0); // Make animation instant
    alpha.setFillAfter(true); // Tell it to persist after the animation ends
    RelativeLayout layout = (RelativeLayout) findViewById(R.id.checkout_mobile_content_frame);
    layout.startAnimation(alpha); // Add animation to the layout.
  }

  @Override
  public void ProcessUrlResponse(String webpage, String cookies, String extra_params) {
    ProcessCartResponse(this, webpage, cookies, extra_params);
  }

  @Override
  public void ProcessUrlResponse(Bitmap pic, String cookies, String extra_params) {
	// TODO Auto-generated method stub
  }

  @Override
  public void ProcessFailedResponse(FetchStatus status, String extra_params) {
	ProcessFailedCartResponse(this, status, extra_params);
  }
  
  @Override
  public boolean doSomething(String info) {
	return true;
  }
}