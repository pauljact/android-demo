package com.jact.jactapp;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.List;

import org.apache.http.cookie.Cookie;

import com.jact.jactapp.GetUrlTask.FetchStatus;
import com.jact.jactapp.JactNavigationDrawer.ActivityIndex;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public class CheckoutActivity extends JactActionBarActivity implements ProcessUrlResponseCallback {
  JactWebViewClient webview_client_;
  public static Cookie cookie_ = null;
  private static int order_id_;
  private static String checkout_url_;
  private static String order_url_;
  private static String jact_home_url_;
  private static String jact_user_id_;

  public static synchronized void SetOrderId(int order_id) {
    order_id_ = order_id;
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState, R.string.checkout_label,
    		       R.layout.checkout_mobile_layout,
    		       JactNavigationDrawer.ActivityIndex.CHECKOUT_VIA_MOBILE_SITE);
    
    // Set urls.
    checkout_url_ = GetUrlTask.JACT_DOMAIN + "/checkout/";
    SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    jact_user_id_ = user_info.getString(getString(R.string.ui_user_id), "");
    order_url_ = GetUrlTask.JACT_DOMAIN + "/user/" + jact_user_id_ + "/orders/";
    jact_home_url_ = GetUrlTask.JACT_DOMAIN;
    
    // Set JactWebViewClient.
    webview_client_ = new JactWebViewClient(this);
  }
    
  @Override
  protected void onResume() {
	Log.e("PHB TEMP", "CheckoutActivity::onResume");
	super.onResume();
    navigation_drawer_.setActivityIndex(ActivityIndex.CHECKOUT_VIA_MOBILE_SITE);
    

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
    		  checkout_url_,
    		  cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain());
    }
    // Set webview from checkout_url_.
    String url_to_load = checkout_url_ + Integer.toString(order_id_);
    if (order_id_ <= 0) {
      Log.e("PHB ERROR", "CheckoutActivity::onResume. Order id: " + order_id_);
      onBackPressed();
      return;
    }
    Log.i("CheckoutActivity::onResume", "Loading Checkout webpage with order id: " + order_id_);
    WebView web_view = (WebView) findViewById(R.id.checkout_mobile_webview);
    web_view.loadUrl(url_to_load);
    web_view.setWebViewClient(webview_client_);
    // Enable javascript, to have tables display properly.
    WebSettings ws = web_view.getSettings();
    ws.setJavaScriptEnabled(true);
    
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
  // We overload doSomething to handle clicks on links in the WebView.
  public boolean doSomething(String info) {
	Log.e("PHB TEMP", "CheckoutActivity::doSomething. url: " + info +
			          ", orders_url: " + order_url_ + Integer.toString(order_id_)); 
	if (info.equals(order_url_ + Integer.toString(order_id_))) {
	  Log.e("PHB TEMP", "CheckoutActivity::doSomething. Going to View Orders with order id: " + order_id_);
      if (jact_user_id_.isEmpty()) {
    	// TODO(PHB): Determine if I need to do any additional handling here, e.g. returning to main
    	// checkout activity, or to home activity.
    	Log.e("CheckoutActivity::doSomething", "Cannot view orders without user id. Aborting.");
    	return false;
      }
	  ViewOrdersActivity.SetOrderId(Integer.toString(order_id_));
	  startActivity(new Intent(this, ViewOrdersActivity.class));
	  return true;
	} else if (info.equals(jact_home_url_) ||
			   info.equals(jact_home_url_ + "/")) {
	  Log.e("PHB TEMP", "CheckoutActivity::doSomething. Going back to Rewards Page.");
	  startActivity(new Intent(this, JactLoggedInHomeActivity.class));
	} else if (info.equals(checkout_url_ + Integer.toString(order_id_) + "/complete")) {
	  GetCart(this);
	  Log.e("PHB TEMP", "CheckoutActivity::doSomething. Should clear cart, then go to Checkout complete page.");		
	}
	return webview_client_.CallSuperLoadUrl(info);
  }
}