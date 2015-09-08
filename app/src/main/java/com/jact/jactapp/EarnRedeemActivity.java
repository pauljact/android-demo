package com.jact.jactapp;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.List;

import com.jact.jactapp.GetUrlTask.FetchStatus;

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

public class EarnRedeemActivity extends JactActionBarActivity implements ProcessUrlResponseCallback {
  private static String url_;
  JactWebViewClient webview_client_;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState, R.string.earn_label,
		       R.layout.earn_redeem_layout,
		       JactNavigationDrawer.ActivityIndex.EARN_REDEEMED);
    
    // Initialize url_ and title_ to dummy values if necessary (they should get re-written
    // to valid values before they are actually used).
    url_ = getIntent().getStringExtra(getString(R.string.earn_url_key));
    if (!JactActionBarActivity.IS_PRODUCTION) Log.w("PHB TEMP", "EarnRedeemActivity::onCreate. Set redeem url to: " + url_);
    if (url_ == null || url_.isEmpty()) {
      EarnActivity.SetShouldRefreshEarnItems(true);
      startActivity(new Intent(this, EarnActivity.class));
    }
    webview_client_ = new JactWebViewClient(this);
  }
    
  @Override
  protected void onResume() {
	super.onResume();

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
    		  url_,
    		  cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain());
    }

    // Get url, and set webview from it.
    WebView web_view = (WebView) findViewById(R.id.earn_redeem_webview);
    web_view.loadUrl(url_);
    web_view.setWebViewClient(webview_client_);
    // Enable javascript, to display video.
    // UPDATE: Enabling javascript below still didn't allow the video to display. 
    WebSettings ws = web_view.getSettings();
    ws.setJavaScriptEnabled(true);
    
    // Set spinner (and hide WebView) until page has finished loading.
    GetCart(this);
    fadeAllViews(num_server_tasks_ > 0);
  }

  @Override
  public void onBackPressed() {
    EarnActivity.SetShouldRefreshEarnItems(true);
    startActivity(new Intent(this, EarnActivity.class));	  
  }

  @Override
  public void fadeAllViews(boolean should_fade) {
    ProgressBar spinner = (ProgressBar) findViewById(R.id.earn_redeem_progress_bar);
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
    RelativeLayout layout = (RelativeLayout) findViewById(R.id.earn_redeem_content_frame);
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
    if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB TEMP", "EarnRedeemActivity::doSomething. String: " + info);
    EarnActivity.SetShouldRefreshEarnItems(true);
    startActivity(new Intent(this, EarnActivity.class));
    return true;
  }
}
