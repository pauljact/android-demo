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

public class EarnActivity extends JactActionBarActivity implements ProcessUrlResponseCallback {
  private JactNavigationDrawer navigation_drawer_;
  private static final String earn_url_ = "https://us7.jact.com:3081/earn";
  //private static final String earn_url_ = "http://us7.jact.com:3080/earn";
  private static final String title_ = "Earn";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState);
    num_server_tasks_ = 0;
    setContentView(R.layout.earn_layout);
    Toolbar toolbar = (Toolbar) findViewById(R.id.jact_toolbar);
    TextView ab_title = (TextView) findViewById(R.id.toolbar_title_tv);
    ab_title.setText(R.string.earn_label);
    setSupportActionBar(toolbar);
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    // Set Navigation Drawer.
    navigation_drawer_ =
        new JactNavigationDrawer(this,
        		                 findViewById(R.id.earn_drawer_layout),
        		                 findViewById(R.id.earn_left_drawer),
        		                 JactNavigationDrawer.ActivityIndex.EARN);
  }
    
  @Override
  protected void onResume() {
	super.onResume();
    navigation_drawer_.setActivityIndex(ActivityIndex.EARN);
    
    // Set webview from earn_url_.
    WebView web_view = (WebView) findViewById(R.id.earn_webview);
    web_view.loadUrl(earn_url_);
    web_view.setWebViewClient(new WebViewClient() {
    	@Override
    	public void onPageFinished(WebView view, String url) {
    		fadeAllViews(false);
    	}
    	
    	@Override
    	public boolean shouldOverrideUrlLoading(WebView view, String url) {
    	  Log.e("PHB", "EarnActivity::shouldOverrideUrlLoading. url: " + url);
          if(url.startsWith("mailto:")){
            //MailTo mt = MailTo.parse(url);
            //Intent i = newEmailIntent(MyActivity.this, mt.getTo(), mt.getSubject(), mt.getBody(), mt.getCc());
            //startActivity(i);
            //view.reload();
            return true;
          } else {
            //view.loadUrl(url);
          }
          return true;
        }
    });
    // Set spinner (and hide WebView) until page has finished loading.
    SetCartIcon(this);
    fadeAllViews(num_server_tasks_ > 0);
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
    boolean set_cart_icon = false;
    if (menu_bar_ == null) set_cart_icon = true;
    menu_bar_ = menu;
    if (set_cart_icon) {
      SetCartIcon(this);
    }
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
    ProgressBar spinner = (ProgressBar) findViewById(R.id.earn_progress_bar);
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
    RelativeLayout layout = (RelativeLayout) findViewById(R.id.earn_content_frame);
    layout.startAnimation(alpha); // Add animation to the layout.
  }
  
  @Override
  public void ProcessUrlResponse(String webpage, String cookies, String extra_params) {
	ProcessCartResponse(webpage, cookies, extra_params);
  }

  @Override
  public void ProcessUrlResponse(Bitmap pic, String cookies, String extra_params) {
	// TODO Auto-generated method stub
  }

  @Override
  public void ProcessFailedResponse(FetchStatus status, String extra_params) {
	ProcessFailedCartResponse(status, extra_params);
  }
}