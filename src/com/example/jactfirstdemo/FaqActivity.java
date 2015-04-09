package com.example.jactfirstdemo;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;
import com.example.jactfirstdemo.JactNavigationDrawer.ActivityIndex;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FaqActivity extends JactActionBarActivity implements ProcessUrlResponseCallback {
  private static String url_;
  private static final String faq_url_ = "https://m.jact.com:3081/faq-page";
  private static String title_;
  
  public static synchronized void SetUrlAndTitle(String url, String title) {
    url_ = url;
    title_ = title;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState, R.string.faq_label,
		       R.layout.faq_layout,
		       JactNavigationDrawer.ActivityIndex.FAQ);
    
    // Initialize url_ and title_ to dummy values if necessary (they should get re-written
    // to valid values before they are actually used).
    if (url_ == null || url_.isEmpty()) {
      url_ = faq_url_;
    }
    if (title_ == null || title_.isEmpty()) {
      title_ = "FAQ";
    }
  }
    
  @Override
  protected void onResume() {
	super.onResume();
    TextView ab_title = (TextView) findViewById(R.id.toolbar_title_tv);
    ab_title.setText(title_);
    if (title_.equalsIgnoreCase(getString(R.string.menu_about_jact))) {
      navigation_drawer_.setActivityIndex(ActivityIndex.ABOUT_JACT);
    } else if (title_.equalsIgnoreCase(getString(R.string.menu_contact_us))) {
        navigation_drawer_.setActivityIndex(ActivityIndex.CONTACT_US);
    } else if (title_.equalsIgnoreCase(getString(R.string.menu_faq))) {
      navigation_drawer_.setActivityIndex(ActivityIndex.FAQ);
    } else if (title_.equalsIgnoreCase(getString(R.string.menu_user_agreement))) {
        navigation_drawer_.setActivityIndex(ActivityIndex.USER_AGREEMENT);
    } else if (title_.equalsIgnoreCase(getString(R.string.menu_privacy_policy))) {
        navigation_drawer_.setActivityIndex(ActivityIndex.PRIVACY_POLICY);
    } else {
      Log.e("PHB ERROR", "FaqActivity::onResume. Unrecognized activity: " + title_);
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
    SetCartIcon(this);
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
