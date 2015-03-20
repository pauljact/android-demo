package com.example.jactfirstdemo;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.google.android.youtube.player.YouTubePlayer.OnFullscreenListener;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;

public class YouTubePlayerActivity extends JactActionBarActivity
                                   implements ProcessUrlResponseCallback {
  private JactNavigationDrawer navigation_drawer_;
  private YouTubePlayer youtube_player_;
  private String youtube_id_;
  public static final String DEVELOPER_KEY = "AIzaSyBoJypQxKePzlnt_4kVbRbAez64hbULcqI";
  
  private void LoadYoutubePlayer() {
	if (youtube_player_ == null || youtube_id_ == null || youtube_id_.isEmpty()) {
	  // TODO(PHB): Handle this.
	  Log.e("PHB ERROR", "YouTubePlayerActivity::LoadYoutubePlayer. Null parameters.");
	  return;
	}
    youtube_player_.setFullscreen(true);
    //youtube_player_.loadVideo("2zNSgSzhBfM");  // From StackOverflow.
    //youtube_player_.loadVideo("VN2FU1F4m3A");  // First YT video on Jact Earn.
    youtube_player_.loadVideo(youtube_id_);
    youtube_player_.play();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState);
    num_server_tasks_ = 0;
    setContentView(R.layout.youtube_layout);
    Toolbar toolbar = (Toolbar) findViewById(R.id.jact_toolbar);
    TextView ab_title = (TextView) findViewById(R.id.toolbar_title_tv);
    ab_title.setText(R.string.earn_label);
    setSupportActionBar(toolbar);
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    // Set Navigation Drawer.
    navigation_drawer_ =
        new JactNavigationDrawer(this,
        		                 findViewById(R.id.youtube_drawer_layout),
        		                 findViewById(R.id.youtube_left_drawer),
        		                 JactNavigationDrawer.ActivityIndex.EARN);
  }
    
  @Override
  protected void onResume() {
	super.onResume();
	// Get Youtube ID to load.
    youtube_id_ = getIntent().getStringExtra(getString(R.string.youtube_id));
    // Setup youtube player fragment.
    YouTubePlayerFragment youTubePlayerFragment =
        (YouTubePlayerFragment) getFragmentManager().findFragmentById(R.id.youtube_fragment);
  //YouTubePlayerSupportFragment youTubePlayerFragment = YouTubePlayerSupportFragment.newInstance();
    youTubePlayerFragment.initialize(DEVELOPER_KEY, new OnInitializedListener() {
        @Override
        public void onInitializationSuccess(
        	YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
          Log.e("PHB TEMP", "YouTubePlayerActivity::onInitializationSucess. wasRestored: " + wasRestored);
          if (!wasRestored) {
            youtube_player_ = player;
        	LoadYoutubePlayer();
          }
        }
        
        @Override
        public void onInitializationFailure(Provider arg0, YouTubeInitializationResult arg1) {
            // PHB Copied.
        }
    });
    //FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
    //transaction.add(R.id.youtube_fragment, youTubePlayerFragment).commit();
    
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
    ProgressBar spinner = (ProgressBar) findViewById(R.id.youtube_progress_bar);
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
    RelativeLayout layout = (RelativeLayout) findViewById(R.id.youtube_content_frame);
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