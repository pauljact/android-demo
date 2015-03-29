package com.example.jactfirstdemo;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;
/*import com.examples.youtubeapidemo.DeveloperKey;
import com.examples.youtubeapidemo.R;
import com.examples.youtubeapidemo.PlayerControlsDemoActivity.ListEntry;
import com.examples.youtubeapidemo.PlayerControlsDemoActivity.MyPlaybackEventListener;
import com.examples.youtubeapidemo.PlayerControlsDemoActivity.MyPlayerStateChangeListener;
import com.examples.youtubeapidemo.PlayerControlsDemoActivity.MyPlaylistEventListener;
*/
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.google.android.youtube.player.YouTubePlayer.ErrorReason;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.PlaybackEventListener;
import com.google.android.youtube.player.YouTubePlayer.PlayerStateChangeListener;
import com.google.android.youtube.player.YouTubePlayer.PlaylistEventListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerFragment;
//import com.google.android.youtube.player.YouTubePlayer.OnFullscreenListener;
//import com.google.android.youtube.player.YouTubePlayerSupportFragment;

public class YouTubePlayerActivity extends JactActionBarActivity
                                   implements ProcessUrlResponseCallback {
  private JactNavigationDrawer navigation_drawer_;
  private YouTubePlayer youtube_player_;
  private String youtube_id_;
  public static final String DEVELOPER_KEY = "AIzaSyBoJypQxKePzlnt_4kVbRbAez64hbULcqI";
  
  //private YouTubePlayerView youTubePlayerView;
  //private TextView stateText;
  //private ArrayAdapter<ListEntry> videoAdapter;
  private EarnPlaylistEventListener playlist_event_listener_;
  private EarnPlayerStateChangeListener player_state_change_listener_;
  private EarnPlaybackEventListener playback_event_listener_;

  //private int currentlySelectedPosition;
  //private String currentlySelectedId;
  
  private void LoadYoutubePlayer() {
	if (youtube_player_ == null || youtube_id_ == null || youtube_id_.isEmpty()) {
	  // TODO(PHB): Handle this.
	  Log.e("PHB ERROR", "YouTubePlayerActivity::LoadYoutubePlayer. Null parameters.");
	  return;
	}
    youtube_player_.setFullscreen(true);
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
    
    playlist_event_listener_ = new EarnPlaylistEventListener();
    player_state_change_listener_ = new EarnPlayerStateChangeListener();
    playback_event_listener_ = new EarnPlaybackEventListener();

    setControlsEnabled(false);
  }
    
  @Override
  protected void onResume() {
	super.onResume();
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
            player.setPlaylistEventListener(playlist_event_listener_);
            player.setPlayerStateChangeListener(player_state_change_listener_);
            player.setPlaybackEventListener(playback_event_listener_);

            //if (!wasRestored) {
            //  playVideoAtSelection();
            //}
            setControlsEnabled(true);
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
    //FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
    //transaction.add(R.id.youtube_fragment, youTubePlayerFragment).commit();
    
    // Set spinner (and hide WebView) until page has finished loading.
    SetCartIcon(this);
    fadeAllViews(num_server_tasks_ > 0);
  }
  
  protected void ReturnToEarn() {
	onBackPressed();
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
  private String updateText() {
    return String.format("Current state: %s %s %s",
        player_state_change_listener_.playerState, playback_event_listener_.playbackState,
        playback_event_listener_.bufferingState);
  }
  //private void updateText() {
  //  stateText.setText(String.format("Current state: %s %s %s",
  //      player_state_change_listener_.playerState, playback_event_listener_.playbackState,
  //      playback_event_listener_.bufferingState));
  //}

  //private void log(String message) {
  //  logString.append(message + "\n");
  //  eventLog.setText(logString);
  //}

  private void setControlsEnabled(boolean enabled) {
    //playButton.setEnabled(enabled);
    //pauseButton.setEnabled(enabled);
    //skipTo.setEnabled(enabled);
    //videoChooser.setEnabled(enabled);
    //for (int i = 0; i < styleRadioGroup.getChildCount(); i++) {
    //  styleRadioGroup.getChildAt(i).setEnabled(enabled);
    //}
  }

  private static final int parseInt(String intString, int defaultValue) {
    try {
      return intString != null ? Integer.valueOf(intString) : defaultValue;
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private String formatTime(int millis) {
    int seconds = millis / 1000;
    int minutes = seconds / 60;
    int hours = minutes / 60;

    return (hours == 0 ? "" : hours + ":")
        + String.format("%02d:%02d", minutes % 60, seconds % 60);
  }

  private String getTimesText() {
    int currentTimeMillis = youtube_player_.getCurrentTimeMillis();
    int durationMillis = youtube_player_.getDurationMillis();
    return String.format("(%s/%s)", formatTime(currentTimeMillis), formatTime(durationMillis));
  }
  
  private final class EarnPlaylistEventListener implements PlaylistEventListener {
    @Override
    public void onNext() {
      Log.w("PHB TEMP", "YouTubePlayerActivity::onNext. NEXT VIDEO");
    }

    @Override
    public void onPrevious() {
      Log.w("PHB TEMP", "YouTubePlayerActivity::onPrevious. PREVIOUS VIDEO");
    }

    @Override
    public void onPlaylistEnded() {
      Log.w("PHB TEMP", "YouTubePlayerActivity::onPlaylistEnded. PLAYLIST ENDED");
    }
  }

  private final class EarnPlaybackEventListener implements PlaybackEventListener {
    String playbackState = "NOT_PLAYING";
    String bufferingState = "";
    @Override
    public void onPlaying() {
      playbackState = "PLAYING";
      updateText();
      Log.w("PHB TEMP", "YouTubePlayerActivity::onPlaying. \tPLAYING " + getTimesText());
    }

    @Override
    public void onBuffering(boolean isBuffering) {
      bufferingState = isBuffering ? "(BUFFERING)" : "";
      updateText();
      Log.w("PHB TEMP", "YouTubePlayerActivity::onBuffering. \t\t" +
                        (isBuffering ? "BUFFERING " : "NOT BUFFERING ") + getTimesText());
    }

    @Override
    public void onStopped() {
      playbackState = "STOPPED";
      updateText();
      Log.w("PHB TEMP", "YouTubePlayerActivity::onStopped. \tSTOPPED");
    }

    @Override
    public void onPaused() {
      playbackState = "PAUSED";
      updateText();
      Log.w("PHB TEMP", "YouTubePlayerActivity::onPaused. \tPAUSED " + getTimesText());
    }

    @Override
    public void onSeekTo(int endPositionMillis) {
    	Log.w("PHB TEMP", "YouTubePlayerActivity::onSeekTo. " + String.format("\tSEEKTO: (%s/%s)",
          formatTime(endPositionMillis),
          formatTime(youtube_player_.getDurationMillis())));
    }
  }

  private final class EarnPlayerStateChangeListener implements PlayerStateChangeListener {
    String playerState = "UNINITIALIZED";

    @Override
    public void onLoading() {
      playerState = "LOADING";
      updateText();
      Log.w("PHB TEMP", "YouTubePlayerActivity::onLoading. " + playerState);
    }

    @Override
    public void onLoaded(String videoId) {
      playerState = String.format("LOADED %s", videoId);
      updateText();
      Log.w("PHB TEMP", "YouTubePlayerActivity::onLoaded. " + playerState);
    }

    @Override
    public void onAdStarted() {
      playerState = "AD_STARTED";
      updateText();
      Log.w("PHB TEMP", "YouTubePlayerActivity::onAdStarted. " + playerState);
    }

    @Override
    public void onVideoStarted() {
      playerState = "VIDEO_STARTED";
      updateText();
      Log.w("PHB TEMP", "YouTubePlayerActivity::onVideoStarted. " + playerState);
    }

    @Override
    public void onVideoEnded() {
      playerState = "VIDEO_ENDED";
      updateText();
      Log.w("PHB TEMP", "YouTubePlayerActivity::onVideoEnded. " + playerState);
    }

    @Override
    public void onError(ErrorReason reason) {
      playerState = "ERROR (" + reason + ")";
      if (reason == ErrorReason.UNEXPECTED_SERVICE_DISCONNECTION) {
        // When this error occurs the player is released and can no longer be used.
        youtube_player_ = null;
        setControlsEnabled(false);
      }
      updateText();
      Log.w("PHB TEMP", "YouTubePlayerActivity::onError. " + playerState);
    }
  }
  
  private static final class ListEntry {
    public final String title;
    public final String id;
    public final boolean isPlaylist;

    public ListEntry(String title, String videoId, boolean isPlaylist) {
      this.title = title;
      this.id = videoId;
      this.isPlaylist = isPlaylist;
    }

    @Override
    public String toString() {
      return title;
    }

  }
}