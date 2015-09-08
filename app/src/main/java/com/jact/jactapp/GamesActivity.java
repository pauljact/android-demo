package com.jact.jactapp;

import com.jact.jactapp.GetUrlTask.FetchStatus;
import com.jact.jactapp.JactNavigationDrawer.ActivityIndex;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public class GamesActivity extends JactActionBarActivity implements ProcessUrlResponseCallback {
  private static String games_url_;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState, R.string.games_label,
		       R.layout.games_layout,
		       JactNavigationDrawer.ActivityIndex.GAMES);
    games_url_ = GetUrlTask.GetJactDomain() + "/games";
  }
    
  @Override
  protected void onResume() {
	super.onResume();
    navigation_drawer_.setActivityIndex(ActivityIndex.GAMES);
    
    // Set webview from games_url_.
    WebView web_view = (WebView) findViewById(R.id.games_webview);
    web_view.loadUrl(games_url_);
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
    ProgressBar spinner = (ProgressBar) findViewById(R.id.games_progress_bar);
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
    RelativeLayout layout = (RelativeLayout) findViewById(R.id.games_content_frame);
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