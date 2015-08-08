package com.jact.jactfirstdemo;

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
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.List;

public class FaqActivity extends JactActionBarActivity implements ProcessUrlResponseCallback {
  private static String url_;
  private static String faq_url_;
  private static String title_;
  private static int title_resource_;
  
  public static synchronized void SetUrlAndTitle(String url, String title, int resource_id) {
    url_ = url;
    title_ = title;
    title_resource_ = resource_id;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState, R.string.faq_label,
		           R.layout.faq_layout,
		           JactNavigationDrawer.ActivityIndex.FAQ);
    faq_url_ = GetUrlTask.GetJactDomain() + "/faq-page";
    
    // Initialize url_ and title_ to dummy values if necessary (they should get re-written
    // to valid values before they are actually used).
    if (url_ == null || url_.isEmpty()) {
      url_ = faq_url_;
    }
    if (title_ == null || title_.isEmpty()) {
      title_ = "FAQ";
      title_resource_ = R.string.faq_label;
    }
  }
    
  @Override
  protected void onResume() {
    SetActivityId(title_resource_);
    super.onResume();
    //TextView ab_title = (TextView) findViewById(R.id.toolbar_title_tv);
    //ab_title.setText(title_);
    if (title_.equalsIgnoreCase(getString(R.string.menu_about_jact))) {
      navigation_drawer_.setActivityIndex(ActivityIndex.ABOUT_JACT);
    } else if (title_.equalsIgnoreCase(getString(R.string.menu_jact_my_profile))) {
      navigation_drawer_.setActivityIndex(ActivityIndex.PROFILE);
    } else if (title_.equalsIgnoreCase(getString(R.string.menu_contact_us))) {
        navigation_drawer_.setActivityIndex(ActivityIndex.CONTACT_US);
    } else if (title_.equalsIgnoreCase(getString(R.string.menu_faq))) {
      navigation_drawer_.setActivityIndex(ActivityIndex.FAQ);
    } else if (title_.equalsIgnoreCase(getString(R.string.menu_user_agreement))) {
        navigation_drawer_.setActivityIndex(ActivityIndex.USER_AGREEMENT);
    } else if (title_.equalsIgnoreCase(getString(R.string.menu_privacy_policy))) {
        navigation_drawer_.setActivityIndex(ActivityIndex.PRIVACY_POLICY);
    } else {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "FaqActivity::onResume. Unrecognized activity: " + title_);
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
              url_,
              cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain());
    }

    // Get url, and set webview from it.
    WebView web_view = (WebView) findViewById(R.id.faq_webview);
    web_view.loadUrl(url_);
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
    ProgressBar spinner = (ProgressBar) findViewById(R.id.faq_progress_bar);
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
    RelativeLayout layout = (RelativeLayout) findViewById(R.id.faq_content_frame);
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
