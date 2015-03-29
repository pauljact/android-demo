package com.example.jactfirstdemo;

import java.util.ArrayList;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;
import com.example.jactfirstdemo.JactNavigationDrawer.ActivityIndex;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class EarnRedeemActivity extends JactActionBarActivity implements ProcessUrlResponseCallback {
  private class EarnSiteAndYoutubeId {
    String jact_site_;
    String youtube_id_;
    
    private EarnSiteAndYoutubeId() {
      jact_site_ = "";
      youtube_id_ = "";
    }
  }
  private JactNavigationDrawer navigation_drawer_;
  private ArrayList<EarnSiteAndYoutubeId> earn_activity_urls_;
  private static final String FETCH_YOUTUBE_URLS_TASK = "fetch_youtube_urls_task";
  private static final String GET_COOKIES_THEN_FETCH_YOUTUBE_URLS_TASK = "get_cookies_then_youtube_urls_task";
  private static final String earn_url_ = "https://us7.jact.com:3081/earn";
  //private static final String earn_url_ = "http://us7.jact.com:3080/earn";
  private static final String QUOTE = "\"";
  private static final String HREF_MARKER = "<a href=" + QUOTE + "earn/";
  private static final String YOUTUBE_URL_IDENTIFIER =
		      "https://us7.jact.com:3081/sites/default/files/styles/product_page/public/video_embed_field_thumbnails/youtube/";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState);
    num_server_tasks_ = 0;
    setContentView(R.layout.earn_redeem_layout);
    Toolbar toolbar = (Toolbar) findViewById(R.id.jact_toolbar);
    TextView ab_title = (TextView) findViewById(R.id.toolbar_title_tv);
    ab_title.setText(R.string.earn_label);
    setSupportActionBar(toolbar);
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    
    // Fetch all youtube urls on this page.
    FetchYoutubeUrls();

    // Set Navigation Drawer.
    navigation_drawer_ =
        new JactNavigationDrawer(this,
        		                 findViewById(R.id.earn_redeem_drawer_layout),
        		                 findViewById(R.id.earn_redeem_left_drawer),
        		                 JactNavigationDrawer.ActivityIndex.EARN);
  }
  
  private void FetchYoutubeUrls() {
	SharedPreferences user_info = getSharedPreferences(
          getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
    String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
    if (cookies.isEmpty()) {
      String username = user_info.getString(getString(R.string.ui_username), "");
      String password = user_info.getString(getString(R.string.ui_password), "");
      num_server_tasks_++;
      ShoppingUtils.RefreshCookies(
          this, username, password, GET_COOKIES_THEN_FETCH_YOUTUBE_URLS_TASK);
      return;
    }
    
  	num_server_tasks_++;
    GetUrlTask task = new GetUrlTask(this, GetUrlTask.TargetType.JSON);
	GetUrlTask.UrlParams params = new GetUrlTask.UrlParams();
	params.url_ = earn_url_;
	params.connection_type_ = "GET";
  	ArrayList<String> header_info = new ArrayList<String>();
    header_info.add(GetUrlTask.CreateHeaderInfo("Content-Type", "text/html"));
  	params.cookies_ = cookies;
  	params.extra_info_ = FETCH_YOUTUBE_URLS_TASK;
	task.execute(params);
  }
    
  @Override
  protected void onResume() {
	super.onResume();
    navigation_drawer_.setActivityIndex(ActivityIndex.EARN);
    
    // Set webview from earn_url_.
    WebView web_view = (WebView) findViewById(R.id.earn_redeem_webview);
    web_view.loadUrl(earn_url_);
    web_view.setWebViewClient(new WebViewClient() {
    	@Override
    	public void onPageFinished(WebView view, String url) {
    		fadeAllViews(false);
    	}
    	
    	@Override
    	public boolean shouldOverrideUrlLoading(WebView view, String url) {
    	  Log.e("PHB TEMP", "EarnActivity::shouldOverrideUrlLoading. url: " + url);
    	  String youtube_id = GetYoutubeId(url);
    	  Log.e("PHB TEMP", "EarnActivity::shouldOverrideUrlLoading. youtube_id|" + youtube_id + "|");
          if (!youtube_id.isEmpty()) {
        	StartYoutubeActivity(youtube_id);
            //MailTo mt = MailTo.parse(url);
            //Intent i = newEmailIntent(MyActivity.this, mt.getTo(), mt.getSubject(), mt.getBody(), mt.getCc());
            //startActivity(i);
            //view.reload();
            return true;
          } else {
            view.loadUrl(url);
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
  
  private void ParseYoutubeUrls(String webpage) {
    earn_activity_urls_ = new ArrayList<EarnSiteAndYoutubeId>();
	int youtube_marker = webpage.indexOf(YOUTUBE_URL_IDENTIFIER);
	String suffix = webpage;
	while (youtube_marker != -1) {
	  // Get Jact Url landing site for a click on this item.
	  String prefix = suffix.substring(0, youtube_marker);
	  int earn_item_finder = prefix.lastIndexOf(HREF_MARKER);
	  if (earn_item_finder == -1) {
		Log.e("PHB ERROR", "EarnActivity::ParseYouTubeUrls. Unable to find 'a href' in:\n" +
	                       prefix + "\nOriginal webpage:\n" + webpage);
		return;
	  }
	  String relevant_prefix = prefix.substring(earn_item_finder + HREF_MARKER.length());
	  int earn_item_end_marker = relevant_prefix.indexOf(QUOTE);
	  if (earn_item_end_marker == -1) {
		Log.e("PHB ERROR", "EarnActivity::ParseYouTubeUrls. Unable to find |" + QUOTE + "| closing a href quote in:\n" +
				           relevant_prefix + "\nOriginal webpage:\n" + webpage);
		return;
	  }
	  String jact_landing_site = "earn/";
	  jact_landing_site += relevant_prefix.substring(0, earn_item_end_marker);
	  
	  // Get youtube (URL) ID.
	  suffix = suffix.substring(youtube_marker + YOUTUBE_URL_IDENTIFIER.length());
	  int youtube_id_end_marker = suffix.indexOf(".jpg?");
	  // Youtube IDs should be relatively short (~10-15 characters). Choose 100 as sufficiently high.
	  int max_youtube_id_length = 100;
	  if (youtube_id_end_marker == -1 || youtube_id_end_marker > max_youtube_id_length) {
		Log.e("PHB ERROR", "EarnActivity::ParseYouTubeUrls. Unable to find '.jpg?' in:\n" +
	                       suffix + "\nOriginal webpage:\n" + webpage);
		return;
	  }
	  String youtube_id = suffix.substring(0, youtube_id_end_marker);
	  EarnSiteAndYoutubeId new_item = new EarnSiteAndYoutubeId();
	  new_item.jact_site_ = jact_landing_site;
	  new_item.youtube_id_ = youtube_id;
	  Log.e("PHB TEMP", "EarnActivity::ParseYouTubeUrls. Adding new item with jact site: " +
			            new_item.jact_site_ + ", and youtube id: " + new_item.youtube_id_);
	  earn_activity_urls_.add(new_item);
	
	  // Proceed to next marker.
	  youtube_marker = suffix.indexOf(YOUTUBE_URL_IDENTIFIER);
	}
  }
  
  @Override
  public void ProcessUrlResponse(String webpage, String cookies, String extra_params) {
    if (extra_params.equals(FETCH_YOUTUBE_URLS_TASK) ||
        extra_params.equals(GET_COOKIES_THEN_FETCH_YOUTUBE_URLS_TASK)) {
      if (extra_params.equals(FETCH_YOUTUBE_URLS_TASK)) {
        ParseYoutubeUrls(webpage);
      } else {
    	SaveCookies(cookies);
    	FetchYoutubeUrls();
      }
      num_server_tasks_--;
  	  if (num_server_tasks_ == 0) {
  		SetCartIcon(this);
  		if (num_server_tasks_ == 0) {
  		  fadeAllViews(false);
  		}
  	  }
    } else {
	  ProcessCartResponse(this, webpage, cookies, extra_params);
    }
  }

  @Override
  public void ProcessUrlResponse(Bitmap pic, String cookies, String extra_params) {
	// TODO Auto-generated method stub
  }

  @Override
  public void ProcessFailedResponse(FetchStatus status, String extra_params) {
    if (extra_params.equals(FETCH_YOUTUBE_URLS_TASK) ||
    	extra_params.equals(GET_COOKIES_THEN_FETCH_YOUTUBE_URLS_TASK)) {
      if (extra_params.equals(FETCH_YOUTUBE_URLS_TASK)) {
        Log.e("PHB ERROR", "EarnActivity::ProcessFailedResponse. Failed to get youtube urls. Status: " + status);
      } else {
        Log.e("PHB ERROR", "EarnActivity::ProcessFailedResponse. Failed to get cookies. Status: " + status);
      }
      num_server_tasks_--;
  	  if (num_server_tasks_ == 0) {
  		SetCartIcon(this);
  		if (num_server_tasks_ == 0) {
  		  fadeAllViews(false);
  		}
  	  }
    } else {
	  ProcessFailedCartResponse(this, status, extra_params);
    }
  }
  
  private String GetYoutubeId(String url) {
    if (earn_activity_urls_ == null) {
      // TODO(PHB): Handle this case.
      Log.e("PHB ERROR", "EarnActivity.StartYoutubeActivity. earn_activity_urls_ not yet populated.");
      return "";
    }
    
    // Find url in earn_activity_urls_ to get corresponding youtube id (url).
    // First need to trim url.
    int earn_start = url.indexOf("earn/");
    if (earn_start == -1) {
      // TODO(PHB): Handle this case.
      Log.e("PHB ERROR", "EarnActivity.StartYoutubeActivity. unrecognized url: " + url);
      return "";
    }
    String key = url.substring(earn_start);
    for (EarnSiteAndYoutubeId item : earn_activity_urls_) {
      if (item.jact_site_.equals(key)) {
    	return item.youtube_id_;
      }
    }
    return "";
  }
  
  private void StartYoutubeActivity(String youtube_id) {
    Intent youtube_intent = new Intent(this, YouTubePlayerActivity.class);
    youtube_intent.putExtra(getString(R.string.youtube_id), youtube_id);
    startActivity(youtube_intent);
  }
}