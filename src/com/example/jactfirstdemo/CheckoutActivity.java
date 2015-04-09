package com.example.jactfirstdemo;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.List;

import org.apache.http.cookie.Cookie;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;
import com.example.jactfirstdemo.JactNavigationDrawer.ActivityIndex;

import android.app.Activity;
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
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CheckoutActivity extends JactActionBarActivity implements ProcessUrlResponseCallback {
  public static Cookie cookie_ = null;
  private static int order_id_;
  private JactDialogFragment dialog_;
  //private static final String checkout_url_ = "https://us7.jact.com:3081/checkout/";
  private static final String checkout_url_ = "https://m.jact.com:3081/checkout/";
  //private static final String checkout_url_ = "http://us7.jact.com:3080/checkout/";

  public static synchronized void SetOrderId(int order_id) {
    order_id_ = order_id;
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState, R.string.checkout_label,
    		       R.layout.checkout_mobile_layout,
    		       JactNavigationDrawer.ActivityIndex.CHECKOUT_VIA_MOBILE_SITE);
  }
    
  @Override
  protected void onResume() {
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
    Log.e("PHB TEMP", "CheckoutActivity::onResume. Loading Checkout webpage with order id: " + order_id_);
    WebView web_view = (WebView) findViewById(R.id.checkout_mobile_webview);
    web_view.loadUrl(url_to_load);
    web_view.setWebViewClient(new WebViewClient() {
    	@Override
    	public void onPageFinished(WebView view, String url) {
    		fadeAllViews(false);
    	}
    });
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
}