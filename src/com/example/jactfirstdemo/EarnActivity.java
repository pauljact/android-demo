package com.jact.jactapp;

import java.util.ArrayList;

import com.jact.jactapp.GetUrlTask.FetchStatus;
import com.jact.jactapp.JactNavigationDrawer.ActivityIndex;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class EarnActivity extends JactActionBarActivity implements ProcessUrlResponseCallback {
  private class EarnSiteAndYoutubeId {
    String jact_site_;
    String youtube_id_;
    
    private EarnSiteAndYoutubeId() {
      jact_site_ = "";
      youtube_id_ = "";
    }
  }
  private ArrayList<EarnSiteAndYoutubeId> earn_activity_urls_;
  private static boolean should_refresh_earn_items_;
  private ListView list_;
  private EarnAdapter adapter_;
  private static ArrayList<EarnPageParser.EarnItem> earn_list_;
  
  private static final String FETCH_EARN_PAGE_TASK = "fetch_earn_page_task";
  private static final String FETCH_YOUTUBE_URLS_TASK = "fetch_youtube_urls_task";
  private static final String GET_COOKIES_THEN_FETCH_YOUTUBE_URLS_TASK = "get_cookies_then_youtube_urls_task";
  private static String earn_url_;
  // DEPRECATED. The following strings are no longer needed.
  //private static final String QUOTE = "\"";
  //private static final String HREF_MARKER = "<a href=" + QUOTE + "earn/";
  //private static final String YOUTUBE_URL_IDENTIFIER =
  //		      "/sites/default/files/styles/product_page/public/video_embed_field_thumbnails/youtube/";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState, R.string.earn_label,
		       R.layout.earn_layout,
		       JactNavigationDrawer.ActivityIndex.EARN);
    should_refresh_earn_items_ = true;
    earn_url_ = GetUrlTask.JACT_DOMAIN + "/rest/earn";
  }
  
  @Override
  protected void onResume() {
	super.onResume();
    navigation_drawer_.setActivityIndex(ActivityIndex.EARN);
    // Set spinner (and hide WebView) until page has finished loading.
    SetCartIcon(this);
    if (should_refresh_earn_items_) {
      FetchEarnPage();
      should_refresh_earn_items_ = false;
    }
    if (num_server_tasks_ == 0) {
      fadeAllViews(false);
    } else {
      fadeAllViews(true);
    }
  }
  
  public static void SetShouldRefreshEarnItems(boolean value) {
	should_refresh_earn_items_ = value;
  }
  
  private void FetchEarnPage() {
	num_server_tasks_++;
    GetUrlTask task = new GetUrlTask(this, GetUrlTask.TargetType.JSON);
	GetUrlTask.UrlParams params = new GetUrlTask.UrlParams();
	params.url_ = earn_url_;
	params.connection_type_ = "GET";
  	ArrayList<String> header_info = new ArrayList<String>();
    header_info.add(GetUrlTask.CreateHeaderInfo("Content-Type", "text/html"));
  	params.extra_info_ = FETCH_EARN_PAGE_TASK;
	task.execute(params);
  }

  public synchronized void ParseYoutubeVideoList(String webpage) {
    earn_list_ = new ArrayList<EarnPageParser.EarnItem>();
    EarnPageParser.ParseEarnPage(webpage, earn_list_);
    
    list_ = (ListView) findViewById(R.id.earn_list);

    // Getting adapter by passing xml data ArrayList.
    adapter_ = new EarnAdapter(
  	    this, R.layout.earn_item, earn_list_, "Earn_Activity");
    list_.setAdapter(adapter_);
  }
  
  private void StartYoutubeActivity(String youtube_id, int nid) {
    Intent youtube_intent = new Intent(this, YouTubePlayerActivity.class);
    Log.w("PHB TEMP", "Setting youtube id: " + youtube_id);
    youtube_intent.putExtra(getString(R.string.youtube_id), youtube_id);
    YouTubePlayerActivity.SetEarnId(nid);
    startActivity(youtube_intent);
  }

  private void Popup(String title, String message) {
	DisplayPopupFragment(title, message, title);
  }

  private String GetYoutubeUrlViaNodeId(int nid) {
	if (earn_list_ == null) {
	  Log.e("EarnActivity::GetYoutubeUrlViaNodeId", "Null nid");
	  return "";
	}
	
	// Look up nid.
	for (EarnPageParser.EarnItem item : earn_list_) {
	  if (item.nid_ == nid) {
		return item.youtube_url_;
	  }
	}
	Log.e("EarnActivity::GetYoutubeUrlViaNodeId", "Unable to find nid " + Integer.toString(nid));
	return "";
  }
  
  public void doEarnNowClick(View view) {
    TextView nid_tv = (TextView) ((LinearLayout) view.getParent()).findViewById(R.id.earn_nid);
    String nid_str = nid_tv.getText().toString();
	int nid = -1;
	try {
	  nid = Integer.parseInt(nid_str);
	} catch (NumberFormatException e) {
	  Log.e("EarnActivity::GetYoutubeUrlViaNodeId", "Unable to parse nid " + nid_str + ":" + e.getMessage());
      Popup("Unable to find Video", "Try again later.");
	  return;
	}
	
    String youtube_url = GetYoutubeUrlViaNodeId(nid);
    if (youtube_url.isEmpty()) {
      Log.e("EarnActivity::GetYoutubeUrlViaNodeId", "Unable to find nid " + nid + " in earn_list_");
      Popup("Unable to find Video", "Try again later.");
    } else {
      StartYoutubeActivity(youtube_url, nid);
    }
  }
  
  public void doEarnNowVideoClick(View view) {
    TextView nid_tv = (TextView) ((LinearLayout) view.getParent().getParent()).findViewById(R.id.earn_nid);
    String nid_str = nid_tv.getText().toString();
	int nid = -1;
	try {
	  nid = Integer.parseInt(nid_str);
	} catch (NumberFormatException e) {
	  Log.e("EarnActivity::GetYoutubeUrlViaNodeId", "Unable to parse nid " + nid_str + ":" + e.getMessage());
      Popup("Unable to find Video", "Try again later.");
	  return;
	}
	
    String youtube_url = GetYoutubeUrlViaNodeId(nid);
    if (youtube_url.isEmpty()) {
      Log.e("EarnActivity::GetYoutubeUrlViaNodeId", "Unable to find nid " + nid + " in earn_list_");
      Popup("Unable to find Video", "Try again later.");
    } else {
      StartYoutubeActivity(youtube_url, nid);
    }
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
	if (extra_params.indexOf(FETCH_EARN_PAGE_TASK) == 0) {
	  num_server_tasks_--;
	  ParseYoutubeVideoList(webpage);
  	  if (num_server_tasks_ == 0) {
  		SetCartIcon(this);
  		if (num_server_tasks_ == 0) {
  		  fadeAllViews(false);
  		}
  	  }
	// DEPRECATED. These values shouldn't ever be present anymore.
	/*
	} else if (extra_params.equals(FETCH_YOUTUBE_URLS_TASK) ||
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
  	  }*/
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
	if (extra_params.indexOf(FETCH_EARN_PAGE_TASK) == 0) {
      num_server_tasks_--;
      Log.e("EarnActivity::ProcessFailedResponse", "Failed to fetch Earn Page.");
      Popup("Unable to Reach Jact", "Check Internet Connection, and try again.");
  	  if (num_server_tasks_ == 0) {
  		SetCartIcon(this);
  		if (num_server_tasks_ == 0) {
  		  fadeAllViews(false);
  		}
  	  }
  	// DEPRECATED.
  	/*
	} else if (extra_params.equals(FETCH_YOUTUBE_URLS_TASK) ||
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
  	  }*/
    } else {
	  ProcessFailedCartResponse(this, status, extra_params);
    }
  }
  
  // DEPRECATED. The following methods were called when parsing html from jact .../earn page, to scrape the
  // urls for the youtube videos. They are no longer needed, since we get these directly from
  // the .../rest/earn page
  /*
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
  
  private void ParseYoutubeUrls(String webpage) {
    earn_activity_urls_ = new ArrayList<EarnSiteAndYoutubeId>();
	int youtube_marker = webpage.indexOf(GetUrlTask.JACT_DOMAIN + YOUTUBE_URL_IDENTIFIER);
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
	  suffix = suffix.substring(youtube_marker + GetUrlTask.JACT_DOMAIN.length() + YOUTUBE_URL_IDENTIFIER.length());
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
	  youtube_marker = suffix.indexOf(GetUrlTask.JACT_DOMAIN + YOUTUBE_URL_IDENTIFIER);
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
  */
}