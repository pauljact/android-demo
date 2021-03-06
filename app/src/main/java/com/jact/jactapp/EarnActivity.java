package com.jact.jactapp;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.jact.jactapp.GetUrlTask.FetchStatus;
import com.jact.jactapp.JactNavigationDrawer.ActivityIndex;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
  private static Map<Integer, Boolean> play_items_by_nid_;

  private static final String EARN_DIALOG_WARNING = "earn_dialog_warning";
  private static final String FETCH_EARN_PAGE_TASK = "fetch_earn_page_task";
  private static final String FETCH_YOUTUBE_URLS_TASK = "fetch_youtube_urls_task";
  private static final String GET_COOKIES_THEN_FEATURED_EARN_TASK = "get_cookies_then_featured_earn";
  private static final String GET_COOKIES_THEN_FETCH_YOUTUBE_URLS_TASK = "get_cookies_then_youtube_urls_task";
  private static String earn_url_;
  private static String mobile_earn_url_;
  private static boolean is_current_already_earned_;
  private static String current_youtube_url_;
  private static int current_earn_nid_;
  private static int num_failed_requests_;

  // Only needed if we play Earn Videos via external video player, instead of via
  // YouTubePlayerActivity
  private static int earn_video_watched_;
  private static final String EARN_REDEEM_URL_BASE = "/node/";
  private static final boolean stream_video_via_youtube_player_api_ = true;
  private static final boolean stream_video_via_youtube_ = false;
  private static final boolean stream_video_via_webview_ = false;

  private Tracker mTracker;  // For google analytics

  // DEPRECATED. The following strings are no longer needed.
  //private static final String QUOTE = "\"";
  //private static final String HREF_MARKER = "<a href=" + QUOTE + "earn/";
  //private static final String YOUTUBE_URL_IDENTIFIER =
  //		      "/sites/default/files/styles/product_page/public/video_embed_field_thumbnails/youtube/";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if (JactActionBarActivity.USE_MOBILE_SITE) {
      // Set layout.
      super.onCreate(savedInstanceState, R.string.earn_label,
              R.layout.earn_wrapper_layout,
              JactNavigationDrawer.ActivityIndex.EARN);
    } else {
      // Set layout.
      super.onCreate(savedInstanceState, R.string.earn_label,
              R.layout.earn_layout,
              JactNavigationDrawer.ActivityIndex.EARN);
    }
    should_refresh_earn_items_ = true;
    earn_url_ = GetUrlTask.GetJactDomain() + "/rest/earn";
    is_current_already_earned_ = false;
    current_youtube_url_ = "";
    earn_video_watched_ = -1;
    mobile_earn_url_ = GetUrlTask.GetJactDomain() + "/earn";

    // For Google Analytics tracking.
    // Obtain the shared Tracker instance.
    JactAnalyticsApplication application = (JactAnalyticsApplication) getApplication();
    mTracker = application.getDefaultTracker();
  }
  
  @Override
  protected void onResume() {
	super.onResume();
    mobile_earn_url_ = GetUrlTask.GetJactDomain() + "/earn";
    earn_url_ = GetUrlTask.GetJactDomain() + "/rest/earn";
    num_failed_requests_ = 0;

    // For Google Analytics.
    mTracker.setScreenName("Image~Earn");
    mTracker.send(new HitBuilders.ScreenViewBuilder().build());

    //PHBnavigation_drawer_.setActivityIndex(ActivityIndex.EARN);
    if (JactActionBarActivity.USE_MOBILE_SITE) {
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
                mobile_earn_url_,
                cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain());
      }

      // Set webview from mobile_earn_url_.
      WebView web_view = (WebView) findViewById(R.id.earn_wrapper_webview);
      web_view.loadUrl(mobile_earn_url_);
      web_view.setWebViewClient(new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
          fadeAllViews(false);
        }
      });
      // Set spinner (and hide WebView) until page has finished loading.
      GetCart(this);
      fadeAllViews(num_server_tasks_ > 0);
    } else {
      if (earn_video_watched_ > 0) {
        String url = GetUrlTask.GetJactDomain() + EARN_REDEEM_URL_BASE + Integer.toString(earn_video_watched_);
        earn_video_watched_ = -1;
        Intent intent = new Intent(this, EarnRedeemActivity.class);
        intent.putExtra(getString(R.string.earn_url_key), url);
        startActivity(intent);
        return;
      }
      // Set spinner (and hide WebView) until page has finished loading.
      GetCart(this);
      if (should_refresh_earn_items_) {
        FetchEarnPage();
      }
      should_refresh_earn_items_ = true;
      if (num_server_tasks_ == 0) {
        fadeAllViews(false);
      } else {
        fadeAllViews(true);
      }
    }
  }

  public static void SetEarnVideoWatched(int nid) {
    earn_video_watched_ = nid;
  }

  public static int GetEarnVideoWatched() {
    return earn_video_watched_;
  }
  
  public static void SetShouldRefreshEarnItems(boolean value) {
	should_refresh_earn_items_ = value;
  }
  
  private void FetchEarnPage() {
    // Need cookies to fetch featured earn (for already_earned_flag).
    SharedPreferences user_info = getSharedPreferences(
            getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
    String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
    if (cookies.isEmpty()) {
      String username = user_info.getString(getString(R.string.ui_username), "");
      String password = user_info.getString(getString(R.string.ui_password), "");
      ShoppingUtils.RefreshCookies(this, username, password, GET_COOKIES_THEN_FEATURED_EARN_TASK);
      return;
    }
    FetchEarnPage(cookies);
  }

  private void FetchEarnPage(String cookies) {
	num_server_tasks_++;
    GetUrlTask task = new GetUrlTask(this, GetUrlTask.TargetType.JSON);
	GetUrlTask.UrlParams params = new GetUrlTask.UrlParams();
    earn_url_ = GetUrlTask.GetJactDomain() + "/rest/earn";
	params.url_ = earn_url_;
	params.connection_type_ = "GET";
    params.cookies_ = cookies;
  	ArrayList<String> header_info = new ArrayList<String>();
    header_info.add(GetUrlTask.CreateHeaderInfo("Content-Type", "text/html"));
  	params.extra_info_ = FETCH_EARN_PAGE_TASK;
	task.execute(params);
  }

  public synchronized void ParseYoutubeVideoList(String webpage) {
    earn_list_ = new ArrayList<EarnPageParser.EarnItem>();
    play_items_by_nid_ = Collections.synchronizedMap(new HashMap<Integer, Boolean>());
    EarnPageParser.ParseEarnPage(webpage, earn_list_, play_items_by_nid_);
    
    list_ = (ListView) findViewById(R.id.earn_list);

    // Getting adapter by passing xml data ArrayList.
    adapter_ = new EarnAdapter(
  	    this, R.layout.earn_item, earn_list_, "Earn_Activity");
    list_.setAdapter(adapter_);
  }

  public static boolean GetYouTubePlayerFlag() {return stream_video_via_youtube_player_api_;}
  public static boolean GetYouTubeFlag() { return stream_video_via_youtube_;}
  public static boolean GetWebViewFlag() {return stream_video_via_webview_;}

  private void StartYoutubeActivity(String youtube_id, int nid) {
    if (stream_video_via_youtube_player_api_) {
      Intent youtube_intent = new Intent(this, YouTubePlayerActivity.class);
      youtube_intent.putExtra(getString(R.string.youtube_id), youtube_id);
      YouTubePlayerActivity.SetEarnId(nid);
      startActivity(youtube_intent);
    } else if (stream_video_via_youtube_) {
      earn_video_watched_ = nid;
      startActivity(new Intent(Intent.ACTION_VIEW,
                               Uri.parse("http://www.youtube.com/watch?v=" + youtube_id)));
    } else if (stream_video_via_webview_) {
      Intent intent = new Intent(this, YouTubeWebViewActivity.class);
      intent.putExtra(getString(R.string.earn_page_url), "star-trek-beyond-trailer");
      startActivity(intent);
    } else if (!JactActionBarActivity.IS_PRODUCTION) {
      Log.e("EarnActivity::StartYouTubeActivity", "All flags false.");
    }
  }

  private void StartPlayActivity(String url) {
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
  }

  private void Popup(String title, String message) {
	DisplayPopupFragment(title, message, title);
  }

  private boolean GetYoutubeUrlViaNodeId(int nid) {
	if (earn_list_ == null) {
	  if (!JactActionBarActivity.IS_PRODUCTION) Log.e("EarnActivity::GetYoutubeUrlViaNodeId", "Null nid");
	  return false;
	}
	
	// Look up nid.
	for (EarnPageParser.EarnItem item : earn_list_) {
	  if (item.nid_ == nid) {
		current_youtube_url_ = item.youtube_url_;
        is_current_already_earned_ = item.already_earned_;
        if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB TEMP", "GetYoutubeUrlViaNodeId. is_current_already_earned_: " + is_current_already_earned_);
        return true;
	  }
	}
	if (!JactActionBarActivity.IS_PRODUCTION) Log.e("EarnActivity::GetYoutubeUrlViaNodeId", "Unable to find nid " + Integer.toString(nid));
	return false;
  }

  public void doDialogCancelClick(View view) {
    // Close Dialog window.
    super.doDialogOkClick(view);
  }

  public void doDialogOkClick(View view) {
    if (dialog_ == null) return;
    // Close Dialog window.
    super.doDialogOkClick(view);
    // Check to see if this was the "Item Already Earned" dialog. If so, proceed to Earn Item.
    if (dialog_.getTag() != null && dialog_.getTag().equals(EARN_DIALOG_WARNING)) {
      StartYoutubeActivity(current_youtube_url_, current_earn_nid_);
    }
  }

  public void doEarnNowClick(View view) {
    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    vibe.vibrate(JactConstants.VIBRATION_DURATION);
    TextView nid_tv = (TextView) ((LinearLayout) view.getParent()).findViewById(R.id.earn_nid);
    String nid_str = nid_tv.getText().toString();
	int nid = -1;
	try {
	  nid = Integer.parseInt(nid_str);
	} catch (NumberFormatException e) {
	  if (!JactActionBarActivity.IS_PRODUCTION) Log.e("EarnActivity::doEarnNowClick", "Unable to parse nid " + nid_str + ":" + e.getMessage());
      Popup("Unable to find Video", "Try again later.");
	  return;
	}

    current_earn_nid_ = nid;
    if (!GetYoutubeUrlViaNodeId(current_earn_nid_) || current_youtube_url_.isEmpty()) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("EarnActivity::doEarnNowClick", "Unable to find nid " + nid + " in earn_list_");
      Popup("Unable to find Video", "Try again later.");
    } else if (is_current_already_earned_) {
      if (can_show_dialog_) {
        dialog_ = new JactDialogFragment();
        dialog_.SetTitle("Already Earned Points for this Item");
        dialog_.SetMessage("Watching again will not earn you more points. Watch Anyway?");
        dialog_.SetButtonOneText("Cancel");
        dialog_.SetButtonTwoText("OK");
        dialog_.show(getSupportFragmentManager(), EARN_DIALOG_WARNING);
        is_popup_showing_ = true;
      }
    } else {
      if (play_items_by_nid_.get(nid)) {
        StartPlayActivity(current_youtube_url_);
      } else {
        StartYoutubeActivity(current_youtube_url_, current_earn_nid_);
      }
    }
  }
  
  public void doEarnNowVideoClick(View view) {
    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    vibe.vibrate(JactConstants.VIBRATION_DURATION);
    TextView nid_tv = (TextView) ((LinearLayout) view.getParent().getParent()).findViewById(R.id.earn_nid);
    String nid_str = nid_tv.getText().toString();
	int nid = -1;
	try {
	  nid = Integer.parseInt(nid_str);
	} catch (NumberFormatException e) {
	  if (!JactActionBarActivity.IS_PRODUCTION) Log.e("EarnActivity::doEarnNowVideoClick",
            "Unable to parse nid " + nid_str + ":" + e.getMessage());
      Popup("Unable to find Video", "Try again later.");
	  return;
	}

    current_earn_nid_ = nid;
    if (!GetYoutubeUrlViaNodeId(current_earn_nid_) || current_youtube_url_.isEmpty()) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("EarnActivity::doEarnNowVideoClick", "Unable to find nid " + nid + " in earn_list_");
      Popup("Unable to find Video", "Try again later.");
    } else if (is_current_already_earned_) {
      if (can_show_dialog_) {
        dialog_ =  new JactDialogFragment();
        dialog_.SetTitle("Already Earned Points for this Item");
        dialog_.SetMessage("Watching again will not earn you more points. Watch Anyway?");
        dialog_.SetButtonOneText("Cancel");
        dialog_.SetButtonTwoText("OK");
        dialog_.show(getSupportFragmentManager(), EARN_DIALOG_WARNING);
        is_popup_showing_ = true;
      }
    } else {
      if (play_items_by_nid_.get(nid)) {
        StartPlayActivity(current_youtube_url_);
      } else {
        StartYoutubeActivity(current_youtube_url_, current_earn_nid_);
      }
    }
  }

  @Override
  public void fadeAllViews(boolean should_fade) {
    if (JactActionBarActivity.USE_MOBILE_SITE) {
      ProgressBar spinner = (ProgressBar) findViewById(R.id.earn_wrapper_progress_bar);
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
      RelativeLayout layout = (RelativeLayout) findViewById(R.id.earn_wrapper_content_frame);
      layout.startAnimation(alpha); // Add animation to the layout.
    } else {
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
  }
  
  @Override
  public void ProcessUrlResponse(String webpage, String cookies, String extra_params) {
    if (JactActionBarActivity.USE_MOBILE_SITE) {
      ProcessCartResponse(this, webpage, cookies, extra_params);
    } else {
      if (extra_params.indexOf(FETCH_EARN_PAGE_TASK) == 0) {
        num_server_tasks_--;
        ParseYoutubeVideoList(webpage);
        if (num_server_tasks_ == 0) {
          SetCartIcon(this);
          if (num_server_tasks_ == 0) {
            fadeAllViews(false);
          }
        }
      } else if (extra_params.indexOf(GET_COOKIES_THEN_FEATURED_EARN_TASK) == 0) {
        SaveCookies(cookies);
        FetchEarnPage(cookies);
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
  }

  @Override
  public void ProcessUrlResponse(Bitmap pic, String cookies, String extra_params) {
	// TODO Auto-generated method stub
  }

  @Override
  public void ProcessFailedResponse(FetchStatus status, String extra_params) {
    if (JactActionBarActivity.USE_MOBILE_SITE) {
      ProcessFailedCartResponse(this, status, extra_params);
    } else {
      num_failed_requests_++;
      if (extra_params.indexOf(FETCH_EARN_PAGE_TASK) == 0) {
        num_server_tasks_--;
        if (!JactActionBarActivity.IS_PRODUCTION)
          Log.e("EarnActivity::ProcessFailedResponse", "Failed to fetch Earn Page.");
        Popup("Unable to Reach Jact", "Check Internet Connection, and try again.");
        if (num_server_tasks_ == 0) {
          SetCartIcon(this);
          if (num_server_tasks_ == 0) {
            fadeAllViews(false);
          }
        }
      } else if (extra_params.indexOf(GET_COOKIES_THEN_FEATURED_EARN_TASK) == 0) {
        if (num_failed_requests_ > 5) return;
        FetchEarnPage();
        // DEPRECATED.
  	/*
	} else if (extra_params.equals(FETCH_YOUTUBE_URLS_TASK) ||
    	extra_params.equals(GET_COOKIES_THEN_FETCH_YOUTUBE_URLS_TASK)) {
      if (extra_params.equals(FETCH_YOUTUBE_URLS_TASK)) {
        if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "EarnActivity::ProcessFailedResponse. Failed to get youtube urls. Status: " + status);
      } else {
        if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "EarnActivity::ProcessFailedResponse. Failed to get cookies. Status: " + status);
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
	int youtube_marker = webpage.indexOf(GetUrlTask.GetJactDomain() + YOUTUBE_URL_IDENTIFIER);
	String suffix = webpage;
	while (youtube_marker != -1) {
	  // Get Jact Url landing site for a click on this item.
	  String prefix = suffix.substring(0, youtube_marker);
	  int earn_item_finder = prefix.lastIndexOf(HREF_MARKER);
	  if (earn_item_finder == -1) {
		if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "EarnActivity::ParseYouTubeUrls. Unable to find 'a href' in:\n" +
	                       prefix + "\nOriginal webpage:\n" + webpage);
		return;
	  }
	  String relevant_prefix = prefix.substring(earn_item_finder + HREF_MARKER.length());
	  int earn_item_end_marker = relevant_prefix.indexOf(QUOTE);
	  if (earn_item_end_marker == -1) {
		if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "EarnActivity::ParseYouTubeUrls. Unable to find |" + QUOTE + "| closing a href quote in:\n" +
				           relevant_prefix + "\nOriginal webpage:\n" + webpage);
		return;
	  }
	  String jact_landing_site = "earn/";
	  jact_landing_site += relevant_prefix.substring(0, earn_item_end_marker);
	  
	  // Get youtube (URL) ID.
	  suffix = suffix.substring(youtube_marker + GetUrlTask.GetJactDomain().length() + YOUTUBE_URL_IDENTIFIER.length());
	  int youtube_id_end_marker = suffix.indexOf(".jpg?");
	  // Youtube IDs should be relatively short (~10-15 characters). Choose 100 as sufficiently high.
	  int max_youtube_id_length = 100;
	  if (youtube_id_end_marker == -1 || youtube_id_end_marker > max_youtube_id_length) {
		if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "EarnActivity::ParseYouTubeUrls. Unable to find '.jpg?' in:\n" +
	                       suffix + "\nOriginal webpage:\n" + webpage);
		return;
	  }
	  String youtube_id = suffix.substring(0, youtube_id_end_marker);
	  EarnSiteAndYoutubeId new_item = new EarnSiteAndYoutubeId();
	  new_item.jact_site_ = jact_landing_site;
	  new_item.youtube_id_ = youtube_id;
	  if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB TEMP", "EarnActivity::ParseYouTubeUrls. Adding new item with jact site: " +
			            new_item.jact_site_ + ", and youtube id: " + new_item.youtube_id_);
	  earn_activity_urls_.add(new_item);
	
	  // Proceed to next marker.
	  youtube_marker = suffix.indexOf(GetUrlTask.GetJactDomain() + YOUTUBE_URL_IDENTIFIER);
	}
  }
  
  private String GetYoutubeId(String url) {
    if (earn_activity_urls_ == null) {
      // TODO(PHB): Handle this case.
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "EarnActivity.StartYoutubeActivity. earn_activity_urls_ not yet populated.");
      return "";
    }
    
    // Find url in earn_activity_urls_ to get corresponding youtube id (url).
    // First need to trim url.
    int earn_start = url.indexOf("earn/");
    if (earn_start == -1) {
      // TODO(PHB): Handle this case.
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "EarnActivity.StartYoutubeActivity. unrecognized url: " + url);
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