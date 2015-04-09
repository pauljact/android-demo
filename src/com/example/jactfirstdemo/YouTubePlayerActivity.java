package com.example.jactfirstdemo;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ProgressBar;import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.ErrorReason;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.PlaybackEventListener;
import com.google.android.youtube.player.YouTubePlayer.PlayerStateChangeListener;
import com.google.android.youtube.player.YouTubePlayer.PlayerStyle;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerFragment;
//import com.google.android.youtube.player.YouTubePlayer.OnFullscreenListener;
//import com.google.android.youtube.player.YouTubePlayerSupportFragment;

public class YouTubePlayerActivity extends JactActionBarActivity
                                   implements ProcessUrlResponseCallback {
  private YouTubePlayer youtube_player_;
  private String youtube_id_;
  private static int earn_id_;
  private boolean has_skipped_ahead_;
  private JactDialogFragment dialog_;
  private static final String EARN_REDEEM_URL_BASE = "https://m.jact.com:3081/node/";
  //private static final String EARN_REDEEM_URL_BASE = "https://us7.jact.com:3081/node/";
  public static final String DEVELOPER_KEY = "AIzaSyBoJypQxKePzlnt_4kVbRbAez64hbULcqI";
  private EarnPlayerStateChangeListener player_state_change_listener_;
  private EarnPlaybackEventListener playback_event_listener_;
  
  private void LoadYoutubePlayer() {
	if (youtube_player_ == null || youtube_id_ == null || youtube_id_.isEmpty()) {
	  // TODO(PHB): Handle this.
	  Log.e("PHB ERROR", "YouTubePlayerActivity::LoadYoutubePlayer. Null parameters.");
	  return;
	}
	Log.w("PHB TEMP", "YouTubePlayerActivity::LoadYoutubePlayer. About to load url: " + youtube_id_);
    youtube_player_.setFullscreen(true);
    youtube_player_.loadVideo(youtube_id_);
    youtube_player_.play();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState, R.string.earn_label,
		       R.layout.youtube_layout,
		       JactNavigationDrawer.ActivityIndex.EARN);
    //playlist_event_listener_ = new EarnPlaylistEventListener();
    player_state_change_listener_ = new EarnPlayerStateChangeListener();
    playback_event_listener_ = new EarnPlaybackEventListener();
  }
    
  @Override
  protected void onResume() {
	super.onResume();
	// Reset has_skipped_ahead_.
	has_skipped_ahead_ = false;
	// Get Youtube ID to load.
    youtube_id_ = getIntent().getStringExtra(getString(R.string.youtube_id));
    // Setup youtube player fragment.
    YouTubePlayerFragment youTubePlayerFragment =
        (YouTubePlayerFragment) getFragmentManager().findFragmentById(R.id.youtube_fragment);
    youTubePlayerFragment.initialize(DEVELOPER_KEY, new OnInitializedListener() {
        @Override
        public void onInitializationSuccess(
        	YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
          youtube_player_ = player;
          //player.setPlaylistEventListener(playlist_event_listener_);
          youtube_player_.setPlayerStateChangeListener(player_state_change_listener_);
          youtube_player_.setPlaybackEventListener(playback_event_listener_);
          youtube_player_.setPlayerStyle(PlayerStyle.MINIMAL);
          Log.e("PHB TEMP", "YouTubePlayerActivity::onInitializationSucess. wasRestored: " + wasRestored);
          if (!wasRestored) {
            youtube_player_ = player;
        	LoadYoutubePlayer();
          }
        }
        
        @Override
        public void onInitializationFailure(Provider provider, YouTubeInitializationResult result) {
          Log.e("PHB ERROR", "YouTubePlayerActivity::onInitializationFailure. result: " + result.toString());
          ReturnToEarn();
            // PHB Copied.
        }
    });
    
    // Set spinner (and hide WebView) until page has finished loading.
    SetCartIcon(this);
    fadeAllViews(num_server_tasks_ > 0);
  }
  
  public static void SetEarnId(int nid) {
    earn_id_ = nid;
  }
  
  protected void ReturnToEarn() {
	onBackPressed();
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

  private void PopupWarning() {
	dialog_ = new JactDialogFragment("Warning", "You won't earn points if you skip ahead");
	dialog_.SetButtonOneText("Cancel");
	dialog_.SetButtonTwoText("Ok");
	dialog_.show(getSupportFragmentManager(), "Unable_to_update_cart");
  }
  
  protected void StartRedeemActivity() {
    Intent intent = new Intent(this, EarnRedeemActivity.class);
    intent.putExtra(getString(R.string.earn_url_key), EARN_REDEEM_URL_BASE + Integer.toString(earn_id_));
    Log.w("PHB TEMP", "Starting Earn redeem activity.");
    startActivity(intent);
  }
  
  public void doDialogOkClick(View view) {
  	// Close Dialog window.
  	dialog_.dismiss();
  	has_skipped_ahead_ = true;
  }

  public void doDialogCancelClick(View view) {
	// Close Dialog window.
	dialog_.dismiss();
  }

  private final class EarnPlaybackEventListener implements PlaybackEventListener {
    @Override
    public void onPlaying() {
      Log.w("PHB TEMP", "YouTubePlayerActivity::onPlaying.");
    }

    @Override
    public void onBuffering(boolean isBuffering) {
      Log.w("PHB TEMP", "YouTubePlayerActivity::onBuffering. \t\t" +
                        (isBuffering ? "BUFFERING " : "NOT BUFFERING "));
    }

    @Override
    public void onStopped() {
      Log.w("PHB TEMP", "YouTubePlayerActivity::onStopped. \tSTOPPED");
    }

    @Override
    public void onPaused() {
      Log.w("PHB TEMP", "YouTubePlayerActivity::onPaused. \tPAUSED");
    }

    @Override
    public void onSeekTo(int endPositionMillis) {
    	Log.w("PHB TEMP", "YouTubePlayerActivity::onSeekTo. Current time: " +
                          youtube_player_.getCurrentTimeMillis() + ". See to time: " +
    			          endPositionMillis);
    	PopupWarning();
    }
  }

  private final class EarnPlayerStateChangeListener implements PlayerStateChangeListener {
    String playerState = "UNINITIALIZED";

    @Override
    public void onLoading() {
      playerState = "LOADING";
      Log.w("PHB TEMP", "YouTubePlayerActivity::onLoading. " + playerState);
    }

    @Override
    public void onLoaded(String videoId) {
      playerState = String.format("LOADED %s", videoId);
      Log.w("PHB TEMP", "YouTubePlayerActivity::onLoaded. " + playerState);
    }

    @Override
    public void onAdStarted() {
      playerState = "AD_STARTED";
      Log.w("PHB TEMP", "YouTubePlayerActivity::onAdStarted. " + playerState);
    }

    @Override
    public void onVideoStarted() {
      playerState = "VIDEO_STARTED";
      Log.w("PHB TEMP", "YouTubePlayerActivity::onVideoStarted. " + playerState);
    }

    @Override
    public void onVideoEnded() {
      playerState = "VIDEO_ENDED";
      Log.w("PHB TEMP", "YouTubePlayerActivity::onVideoEnded. " + playerState);
      if (has_skipped_ahead_) {
    	ReturnToEarn();
      } else {
    	StartRedeemActivity();
      }
    }

    @Override
    public void onError(ErrorReason reason) {
      playerState = "ERROR (" + reason + ")";
      if (reason == ErrorReason.UNEXPECTED_SERVICE_DISCONNECTION) {
        // When this error occurs the player is released and can no longer be used.
        youtube_player_ = null;
      }
      Log.e("YouTubePlayerActivity::onError", playerState);
    }
  }
}