package com.example.jactfirstdemo;

import android.app.Activity;
import java.util.ArrayList;
import java.util.HashMap;
 
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;
 
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class ProductsActivity extends Activity implements ProcessUrlResponseCallback {
    // XML node keys
    static final String PRODUCT_ID_KEY = "id";
    static final String PRODUCT_TITLE_KEY = "product_title";
    static final String PRODUCT_DATE_KEY = "product_date";
    static final String PRODUCT_BUX_KEY = "product_bux";
    static final String PRODUCT_IMAGE_KEY = "product_image";
 
    private ListView list_;
    private ProductsAdapter adapter_;
    private String products_page_url_;
    ArrayList<HashMap<String, String>> products_list_;
 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.products_main);
 
        // Get product from URL, store all product info in a hash map.
        products_list_ = new ArrayList<HashMap<String, String>>();
        ParseProductsPage(getIntent().getStringExtra("server_response"), products_list_);
 
        list_ = (ListView) findViewById(R.id.products_list);
 
        // Getting adapter by passing xml data ArrayList
        adapter_ = new ProductsAdapter(this, products_list_);
        list_.setAdapter(adapter_);
 
        // Click event for single list row
        list_.setOnItemClickListener(
        		new OnItemClickListener() {
        			@Override
        			public void onItemClick(
        					AdapterView<?> parent, View view, int position, long id) {}
        		}
        );
        //String url = "http://us7.jact.com:3080/rest/views/jact_prize_drawings.json";
        //PHB String url = getIntent().getStringExtra("products_page_url");
        //new GetUrlTask(this, GetUrlTask.TargetType.JSON).execute(url, "GET");
    }

    private void ParseProductsPage(String response,
    		                       ArrayList<HashMap<String, String>> products_list) {
        try {
          JSONArray products = new JSONArray(response);
          Log.e("PHB", "response_json valid, number of items: " + products.length());
          for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);
            HashMap<String, String> map = new HashMap<String, String>();
            String node_id = "";
            String node_title = "";
            String drawing_date = "";
            String price_in_bux = "";
            try {
              node_id = product.getString("nid");
              String image_url = ParseImageUrlFromNodeId(node_id);
              if (!image_url.isEmpty()) {
  	            map.put(PRODUCT_IMAGE_KEY, image_url);
              }
              String drawing_url  = ParseDrawingUrlFromNodeId(node_id);
              if (!drawing_url.isEmpty()) {
   	            map.put(PRODUCT_ID_KEY, "PHB");
              }
            } catch (JSONException e) {
            	Log.e("PHB ERROR", "Failed to find nid");
            }
            try {
              node_title = product.getString("node_title");
              String title = ParseTitleFromNodeTitle(node_title);
              if (!title.isEmpty()) {
  	            map.put(PRODUCT_TITLE_KEY, title);
              }
            } catch (JSONException e) {
            	Log.e("PHB ERROR", "Failed to find node_title");
            }
            try {
              drawing_date = product.getString("drawing date");
      		  String date = ParseDateFromDrawingDate(drawing_date);
      		  if (!date.isEmpty()) {
  	            map.put(PRODUCT_DATE_KEY, date);
      		  }
            } catch (JSONException e) {
            	Log.e("PHB Error", "Failed to find drawing_date");
            }
            /*
            if (drawing_date.isEmpty()) {
            	try {
            		drawing_date = product.getString("drawing_date");
            		Log.e("PHB", "drawing date 2: " + drawing_date);
            		String date = ParseDateFromDrawingDate(drawing_date);
            		if (!date.isEmpty()) {
        	          map.put(PRODUCT_DATE_KEY, date);
            		}
            	} catch (JSONException e) {
            		Log.e("PHB ERROR", "Failed to find drawing date");
            	}
            }
            */
            try {
              price_in_bux = product.getString(
            	  "commerce_product_field_data_field_product_product_id");
  	          map.put(PRODUCT_BUX_KEY, price_in_bux);
            } catch (JSONException e) {
            	Log.e("PHB ERROR", "Failed to find commerce.");
            }
            
            if (!map.isEmpty()) {
	  	      products_list.add(map);
            }
          }
        } catch (JSONException e) {
          Log.e("PHB ERROR", "Failed to parse response");
          // TODO(PHB): Handle exception.
        }
    }
    
    private String ParseDateFromDrawingDate(String date) {
        if (date == null || date.isEmpty()) return "";
        int expected_start = date.indexOf("<span class=\"");
        if (expected_start < 0) {
          Log.e("PHB Error", "Unexpected format of node_title: " + date);
          return "";
        }
        String first_cut = date.substring(expected_start);
        int end_link = first_cut.indexOf("\">");
        if (end_link < 0) {
          Log.e("PHB Error", "Unexpected format of node_title: " + date);
          return "";
        }
        String second_cut = first_cut.substring(end_link + 2);
        int close_tag = second_cut.indexOf("</span>");
        if (close_tag < 0) {
          Log.e("PHB Error", "Unexpected format of node_title: " + date);
          return "";
        }
        return second_cut.substring(0, close_tag);
    }
    
    private String ParseTitleFromNodeTitle(String node_title) {
      if (node_title == null || node_title.isEmpty()) return "";
      int expected_start = node_title.indexOf("<a href=\"");
      if (expected_start < 0) {
        Log.e("PHB Error", "Unexpected format of node_title: " + node_title);
        return "";
      }
      String first_cut = node_title.substring(expected_start); 
      int end_link = first_cut.indexOf("\">");
      if (end_link < 0) {
        Log.e("PHB Error", "Unexpected format of node_title: " + node_title);
        return "";
      }
      String second_cut = first_cut.substring(end_link + 2);
      int close_tag = second_cut.indexOf("</a>");
      if (close_tag < 0) {
        Log.e("PHB Error", "Unexpected format of node_title: " + node_title);
        return "";
      }
      return second_cut.substring(0, close_tag);
    }
    
    private String ParseImageUrlFromNodeId(String nid) {
      if (nid == null || nid.isEmpty()) return "";
      int expected_start = nid.indexOf("<img ");
      if (expected_start < 0) {
        Log.e("PHB Error", "Unexpected format of nid: " + nid);
        return "";
      }
      String first_cut = nid.substring(expected_start);
      int start_src = first_cut.indexOf("src=\"");
      if (start_src < 0) {
        Log.e("PHB Error", "Unexpected format of node_title: " + nid);
        return "";
      }
      String second_cut = first_cut.substring(start_src + 5);
      int end_src = second_cut.indexOf("\"");
      if (end_src < 0) {
        Log.e("PHB Error", "Unexpected format of node_title: " + nid);
        return "";
      }
      return second_cut.substring(0, end_src);
    }
    
    private String ParseDrawingUrlFromNodeId(String nid) {
      if (nid == null || nid.isEmpty()) return "";
      int expected_start = nid.indexOf("<a href=\"");
      if (expected_start < 0) {
        Log.e("PHB Error", "Unexpected format of nid: " + nid);
        return "";
      }
      String first_cut = nid.substring(expected_start + 8);
      int end_link = first_cut.indexOf("\">");
      if (end_link < 0) {
        Log.e("PHB Error", "Unexpected format of node_title: " + nid);
        return "";
      }
      return first_cut.substring(0, end_link);
    }

	@Override
	public void ProcessUrlResponse(String webpage, String cookies) {
		// TODO Auto-generated method stub
	}

	@Override
	public void ProcessUrlResponse(Bitmap pic, String cookies) {
		// TODO(PHB): Implement this.
	}

	@Override
	public void ProcessFailedResponse(FetchStatus status) {
		// TODO(PHB): Implement this.
	}
}

