package com.example.jactfirstdemo;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.example.jactfirstdemo.ProductsPageParser.ProductItem;

public class EarnPageParser {
  // Website node keys.
  private static final String TITLE_NODE = "node_title";
  private static final String NID_NODE = "nid";
  private static final String YOUTUBE_URL_NODE = "field_data_field_watch_field_watch_video_url";
  private static final String IMG_URL_NODE = "field_data_field_watch_field_watch_thumbnail_path";
  private static final String POINTS_NODE = "field_earn";
  private static final String POINTS_SUFFIX = " Points";
  static final String jact_icons_website_ = "https://us7.jact.com:3081/sites/default/files/styles/product_page/public/";

  public static class EarnItem {
	String title_;
	int nid_;
	String youtube_url_;
	String img_url_;
	int earn_points_;
  }
  
  static private boolean ParseTitle(String title, EarnItem item) {
    if (title == null || title.isEmpty()) return false;
    item.title_ = title;
    return true;
  }
  
  static private boolean ParseNodeId(String nid, EarnItem item) {
    if (nid == null || nid.isEmpty()) return false;
    try {
      item.nid_ = Integer.parseInt(nid);
    } catch (NumberFormatException e) {
      Log.e("PHB ERROR", "EarnPageParser::ParseNodeId. Unable to parse nid as an int: " + nid);
      return false;
    }
    return true;
  }
  
  static private boolean ParseYoutubeUrl(String url, EarnItem item) {
    if (url == null || url.isEmpty()) return false;
    item.youtube_url_ = url;
    return true;
  }
  
  static private boolean ParseImageUrl(String url, EarnItem item) {
    if (url == null || url.isEmpty()) return false;
    int prefix_index = url.indexOf("public://");
	if (prefix_index != 0) {
      Log.e("PHB ERROR", "EarnPageParser::ParseImageUrl. Unable to parse Image uri:\n" + url);
      return false;
	}
    String image_url = url.substring(9);  // 9 is the length of prefix "public://" that should be removed.
	item.img_url_ = jact_icons_website_ + image_url.replace(" ", "%20");  // Replace whitespace in url with %20.
    return true;
  }
  
  static private boolean ParsePoints(String points, EarnItem item) {
    if (points == null || points.isEmpty()) return false;
    int points_suffix = points.indexOf(POINTS_SUFFIX);
    if (points_suffix < 0 || (points_suffix + POINTS_SUFFIX.length() != points.length())) {
      Log.e("PHB ERROR", "EarnPageParser::ParsePoints. Unexpected points string: " + points);
    }
    try {
      item.earn_points_ = Integer.parseInt(points.substring(0, points_suffix));
    } catch (NumberFormatException e) {
      Log.e("PHB ERROR", "EarnPageParser::ParsePoints. Unable to parse points as an int: " + points);
      return false;
    }
    return true;
  }
  
  static public void ParseEarnPage(String response, ArrayList<EarnItem> earn_list) {
    try {
      JSONArray items = new JSONArray(response);
      for (int i = 0; i < items.length(); i++) {
        JSONObject item = items.getJSONObject(i);
        EarnItem earn_item = new EarnItem();
        
        // Parse title.
        if (!ParseTitle(item.getString(TITLE_NODE), earn_item)) {
          Log.e("PHB ERROR", "EarnPageParser::ParseEarnPage. Unable to parse title: " +
        		             item.getString(TITLE_NODE));
          return;
        }
        
        // Parse Node id.
        if (!ParseNodeId(item.getString(NID_NODE), earn_item)) {
            Log.e("PHB ERROR", "EarnPageParser::ParseEarnPage. Unable to parse title: " +
		                       item.getString(NID_NODE));
            return;
        }
        
        // Parse Youtube Url.
        if (!ParseYoutubeUrl(item.getString(YOUTUBE_URL_NODE), earn_item)) {
            Log.e("PHB ERROR", "EarnPageParser::ParseEarnPage. Unable to parse title: " +
 		                       item.getString(YOUTUBE_URL_NODE));
            return;
        }
        
        // Parse Image Url.
        if (!ParseImageUrl(item.getString(IMG_URL_NODE), earn_item)) {
            Log.e("PHB ERROR", "EarnPageParser::ParseEarnPage. Unable to parse title: " +
		                       item.getString(IMG_URL_NODE));
            return;
        }
        
        // Parse Points.
        if (!ParsePoints(item.getString(POINTS_NODE), earn_item))  {
            Log.e("PHB ERROR", "EarnPageParser::ParseEarnPage. Unable to parse title: " +
		                       item.getString(POINTS_NODE));
            return;
        }
        
        // Add earn_item to earn_list.
        earn_list.add(earn_item);
      }
    } catch (JSONException e) {
      Log.e("PHB ERROR", "EarnPageParser.ParseEarnPage: Failed to parse response");
      // TODO(PHB): Handle exception.
    }
  }

}
