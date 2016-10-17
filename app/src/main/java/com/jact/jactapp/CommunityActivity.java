package com.jact.jactapp;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.jact.jactapp.GetUrlTask.FetchStatus;
import com.jact.jactapp.JactNavigationDrawer.ActivityIndex;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.List;

public class CommunityActivity extends JactActionBarActivity implements ProcessUrlResponseCallback {
  private static String community_url_;
  private Tracker mTracker;  // For google analytics

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState, R.string.community_label,
		       R.layout.community_layout,
		       JactNavigationDrawer.ActivityIndex.COMMUNITY);
    community_url_ = GetUrlTask.GetJactDomain() + "/community";

    // For Google Analytics tracking.
    // Obtain the shared Tracker instance.
    JactAnalyticsApplication application = (JactAnalyticsApplication) getApplication();
    mTracker = application.getDefaultTracker();
  }
    
  @Override
  protected void onResume() {
	super.onResume();

    // For Google Analytics.
    mTracker.setScreenName("Image~Community");
    mTracker.send(new HitBuilders.ScreenViewBuilder().build());


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
              community_url_,
              cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain());
    }

    community_url_ = GetUrlTask.GetJactDomain() + "/community";
    //PHBnavigation_drawer_.setActivityIndex(ActivityIndex.COMMUNITY);
    
    // Set webview from community_url_.
    WebView web_view = (WebView) findViewById(R.id.community_webview);
    web_view.loadUrl(community_url_);
    web_view.setWebViewClient(new WebViewClient() {
    	@Override
    	public void onPageFinished(WebView view, String url) {
    		fadeAllViews(false);
    	}
    });
    // Set spinner (and hide WebView) until page has finished loading.
    GetCart(this);
    fadeAllViews(num_server_tasks_ > 0);
  }

  @Override
  public void fadeAllViews(boolean should_fade) {
    ProgressBar spinner = (ProgressBar) findViewById(R.id.community_progress_bar);
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
    RelativeLayout layout = (RelativeLayout) findViewById(R.id.community_content_frame);
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