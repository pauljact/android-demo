package com.example.jactfirstdemo;

import com.example.jactfirstdemo.JactNavigationDrawer.ActivityIndex;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public class FaqActivity extends JactActionBarActivity {
  private JactNavigationDrawer navigation_drawer_;
  private String url_;
  private String title_;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState);
    setContentView(R.layout.faq_layout);
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    // Set Navigation Drawer.
    navigation_drawer_ =
        new JactNavigationDrawer(this,
        		                 findViewById(R.id.faq_drawer_layout),
        		                 findViewById(R.id.faq_left_drawer),
        		                 JactNavigationDrawer.ActivityIndex.FAQ);
  }
    
  @Override
  protected void onResume() {
	super.onResume();
    // Get title, and set Action Bar with this title.
    title_ = getIntent().getStringExtra(getString(R.string.webview_title_key));
    getSupportActionBar().setTitle(title_);
    if (title_.equalsIgnoreCase(getString(R.string.menu_about_jact))) {
      navigation_drawer_.setActivityIndex(ActivityIndex.ABOUT_JACT);
    } else if (title_.equalsIgnoreCase(getString(R.string.menu_faq))) {
      navigation_drawer_.setActivityIndex(ActivityIndex.FAQ);
    } else if (title_.equalsIgnoreCase(getString(R.string.menu_privacy_policy))) {
        navigation_drawer_.setActivityIndex(ActivityIndex.PRIVACY_POLICY);
    } else if (title_.equalsIgnoreCase(getString(R.string.menu_user_agreement))) {
        navigation_drawer_.setActivityIndex(ActivityIndex.USER_AGREEMENT);
    } else {
      Log.e("PHB ERROR", "FaqActivity::onResume. Unrecognized activity: " + title_);
    }
    
    // Get url, and set webview from it.
    url_ = getIntent().getStringExtra(getString(R.string.webview_url_key));
    WebView web_view = (WebView) findViewById(R.id.faq_webview);
    web_view.loadUrl(url_);
    web_view.setWebViewClient(new WebViewClient() {
    	@Override
    	public void onPageFinished(WebView view, String url) {
    		fadeAllViews(false);
    	}
    });
    // Set spinner (and hide WebView) until page has finished loading.
    fadeAllViews(true);
  }
  
  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // Sync the toggle state after onRestoreInstanceState has occurred.
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
	ShoppingCartActivity.SetCartIcon(menu);
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
}
