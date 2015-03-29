package com.example.jactfirstdemo;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class ProductsPageParser {	  
  // Website node keys.
  static final String WEBSITE_NID = "nid";
  static final String WEBSITE_TITLE = "node_title";
  static final String WEBSITE_PID = "commerce_product_field_data_field_product_product_id";
  static final String WEBSITE_NODE_TYPE = "node_type";
  static final String WEBSITE_DATE = "drawing date";
  static final String WEBSITE_DRAWING_DATE = "Drawing_Ends";
  static final String WEBSITE_SKU = "commerce_product_field_data_field_product_sku";
  static final String WEBSITE_PROMOTE = "Promote";
  static final String WEBSITE_MAX_QUANTITY = "Max_order_quantity";
  static final String WEBSITE_VALUE = "value";
  static final String WEBSITE_IMAGE = "field_product_image";
  static final String WEBSITE_IMAGE_URI = "uri";
  static final String WEBSITE_PRICE = "Price";
  static final String WEBSITE_PRICE_AMOUNT = "amount";
  static final String WEBSITE_PRICE_CURRENCY = "currency_code";
  static final String WEBSITE_POINT_PRICE = "Point_Price";
  static final String WEBSITE_PRODUCT_TYPE = "Product_Type";
	
  static final String jact_icons_website_ = "https://us7.jact.com:3081/sites/default/files/styles/medium/public/";
  
  public static class ProductItem {
    public String nid_;
    public String title_;
    public String pid_;
    public String max_quantity_;
    public String node_type_;
    public String date_;
    public String sku_;
    public boolean promote_;
    public String img_url_;
    public String drawing_url_;
    public String bux_;
    public String usd_;
    public String points_;
    public ArrayList<String> types_;
    
    public String toString() {
      String types = "";
      if (types_ != null) {
        for (String type : types_) {
    	  types += type + ", ";
        }
      }
      return "NID: " + nid_ + ", title: " + title_ + ", pid: " + pid_ + ", node_type: " +
             node_type_ + ", date: " + date_ + ", sku: " + sku_ + ", image_url_: " +
    	     img_url_ + ", drawing url: " + drawing_url_ + ", bux: " + bux_ +
    		 ", usd: " + usd_ + ", points: " + points_ + ", types: " + types;
    }
  }

  static private String ParseNode(JSONObject node, String tag) {
	if (!node.has(tag) || node.isNull(tag)) {
	  return "";
	}
	try {
	  // When tag is present, it is a JSONObject; but when it is absent, it is an empty JSONArray.
	  // This makes handling them annoying.
	  JSONObject block = new JSONObject(node.getString(tag));
	  if (block == null || block.length() == 0) {
	    return "";
	  }
	  return block.getString(WEBSITE_VALUE);
	} catch (JSONException e) {
	  try {
		JSONArray block_array = new JSONArray(node.getString(tag));
		if (block_array.length() == 0) {
		  return "";
		} else if (block_array.length() == 1) {
		  JSONObject block_ob = block_array.getJSONObject(0);
		  return block_ob.getString(WEBSITE_VALUE);
		} else {
		  Log.e("PHB ERROR", "ProductsPageParse::ParseNode. For tag: " + tag + ", Promote array " +
				             "should have size at most one, but found: " + block_array.length()); 
		}
	  } catch (JSONException ex) {
		  Log.e("PHB ERROR", "ProductsPageParse::ParseNode. For tag: " + tag + ", Failed to parse " +
	                         "Drawing_Ends tag as both a JSONObject and JSONArray.\n" +
                             node.toString() + "\nException: " + ex.getMessage());
        // TODO(PHB): Handle exception gracefully.
	  }
	}
    return "";
  }
  
  static private void ParseRewardsNodeTitle(String node, ProductItem item) {
    if (!node.isEmpty()) {
      item.title_ = node;
    }
  }
  
  static private void ParseDrawingsNodeTitle(String node, ProductItem item) {
    String title = ParseTitleFromNodeTitle(node);
    if (!title.isEmpty()) {
      item.title_ = title;
    }
  }
  
  static private void ParseRewardsNodeId(String node, ProductItem item) {
    if (!node.isEmpty()) {
      item.nid_ = node;
    }
  }
	  
  static private void ParseDrawingsNodeId(String node_id, ProductItem item) {
    // Parse Image URL.
    String image_url = ParseImageUrlFromNodeId(node_id);
    if (!image_url.isEmpty()) {
      item.img_url_ = image_url;
    }

    // Parse Drawing URL.
    String drawing_url  = ParseDrawingUrlFromNodeId(node_id);
    if (!drawing_url.isEmpty()) {
      item.drawing_url_ = drawing_url;
    }
  }
  
  static private void ParseRewardsProductId(String node, ProductItem item) {
    if (!node.isEmpty()) {
      item.pid_ = node;
    }
  }
  
  static private void ParseDrawingsProductId(String node, ProductItem item) {
    if (!node.isEmpty()) {
      item.pid_ = node;
    }
  }
  
  static private void ParseDrawingsDate(String node, ProductItem item) {
    String date = ParseDateFromDrawingDate(node);
	if (!date.isEmpty()) {
      item.date_ = date;
	}
  }
  
  static private void ParseRewardsSku(String node, ProductItem item) {
    if (!node.isEmpty()) {
      item.sku_ = node;
    }
  }
  
  static private void ParseRewardsNodeType(String node, ProductItem item) {
    if (!node.isEmpty()) {
      item.node_type_ = node;
    }
  }
  static private void ParseRewardsPromote(JSONObject node, ProductItem item) {
	String value = ParseNode(node, WEBSITE_PROMOTE);
	if (value.isEmpty()) {
	  item.promote_ = false;
	} else if (value.equalsIgnoreCase("0")) {
	  item.promote_ = false;
	} else if (value.equalsIgnoreCase("1")) {
	  item.promote_ = true;
	} else {
	  item.promote_ = false;
      Log.e("PHB ERROR", "ProductsPageParse::ParseRewardsPromote. Unrecognize Promote->Value tag: " + value);
	}
  }

  static private void ParseRewardsMaxQuantity(JSONObject node, ProductItem item) {
	String value = ParseNode(node, WEBSITE_MAX_QUANTITY);
	if (value.isEmpty() || value.equals("-1") || value.equals("0")) {
	  item.max_quantity_ = "-1";
	} else {
	  item.max_quantity_ = value;
	}
  }
  
  static private void ParseRewardsDrawingDate(JSONObject node, ProductItem item) {
    String value = ParseNode(node, WEBSITE_DRAWING_DATE);
	if (!value.isEmpty()) {
	  item.date_ = value;
	}
  }
  
  static private void ParseRewardsImage(JSONObject node, ProductItem item) {
	if (!node.has(WEBSITE_IMAGE) || node.isNull(WEBSITE_IMAGE)) {
      Log.e("PHB ERROR", "ProductsPageParser::ParseRewardsImage. Unable to find Image tag:\n" + node.toString());
      // TODO(PHB): Handle missing image.
	  return;
	}
	try {
	  JSONObject image = new JSONObject(node.getString(WEBSITE_IMAGE));
	  String value = image.getString(WEBSITE_IMAGE_URI);
	  if (value.isEmpty()) {
        Log.e("PHB ERROR", "ProductsPageParser::ParseRewardsImage. Unable to find Image uri:\n" + node.toString());
        // TODO(PHB): Handle missing image.
        return;
	  }
	  int prefix_index = value.indexOf("public://");
	  if (prefix_index != 0) {
        Log.e("PHB ERROR", "ProductsPageParser::ParseRewardsImage. Unable to parse Image uri:\n" + node.toString());
        // TODO(PHB): Handle missing image.
        return;
	  }
	  String image_url = value.substring(9);  // 9 is the length of prefix "public://" that should be removed.
	  item.img_url_ = jact_icons_website_ + image_url.replace(" ", "%20");  // Replace whitespace in url with %20.
	} catch (JSONException e) {
      Log.e("PHB ERROR", "ProductsPageParser::ParseRewardsImage. Unable to parse Image tag:\n" + node.toString());
      // TODO(PHB): Handle exception gracefully.
    }
  }
  
  static private void ParseRewardsPrice(JSONObject node, ProductItem item) {
	if (!node.has(WEBSITE_PRICE) || node.isNull(WEBSITE_PRICE)) {
      Log.e("PHB ERROR", "ProductsPageParse::ParseRewardsPrice. Unable to find Price tag:\n" + node.toString());
      // TODO(PHB): Handle missing price.
	  return;
	}
	try {
	  JSONObject price = new JSONObject(node.getString(WEBSITE_PRICE));
	  String amount = price.getString(WEBSITE_PRICE_AMOUNT);
	  String currency = price.getString(WEBSITE_PRICE_CURRENCY);
	  if (amount.isEmpty() || currency.isEmpty()) {
        Log.e("PHB ERROR", "ProductsPageParse::ParseRewardsPrice. Unable to find Price amount:\n" + node.toString());
        // TODO(PHB): Handle missing price.
        return;
	  }
	  if (!currency.equalsIgnoreCase("BUX") &&
	      !currency.equalsIgnoreCase("USD") &&
		  !currency.equalsIgnoreCase("Points")) {
        Log.e("PHB ERROR", "ProductsPageParse::ParseRewardsPrice. Unrecognize currency: " + currency);
        // TODO(PHB): Handle missing price.
        return;
	  }
	  if (currency.equalsIgnoreCase("BUX")) {
	    if (amount.substring(amount.length() - 2).equalsIgnoreCase("00")) {
	      item.bux_ = amount.substring(0, amount.length() - 2);
	    } else {
	      Log.e("PHB ERROR", "ProductsPageParser::ParseRewardsPrice. BUX should be divisible by 100: " + amount);
	      item.bux_ = amount;
	    }
	  } else if (currency.equalsIgnoreCase("USD")) {
	    item.usd_ = amount.substring(0, amount.length() - 2) + "." + amount.substring(amount.length() - 2);
	  } else {
	    item.points_ = amount;
	  }
	} catch (JSONException e) {
      Log.e("PHB ERROR", "ProductsPageParse::ParseRewardsPrice. Unable to parse Price tag:\n" + node.toString());
      // TODO(PHB): Handle exception gracefully.
    }
  }
  
  static private void ParseRewardsPointPrice(JSONObject node, ProductItem item) {
	String value = ParseNode(node, WEBSITE_POINT_PRICE);
	if (!value.isEmpty()) {
	  if (item.points_ != null && !item.points_.isEmpty()) {
		Log.e("PHB ERROR", "ProductsPageParse::ParseRewardsPointPrice. Duplicate entries for Price in points:\n" + node.toString());	
	  } else {
	    item.points_ = value;
	  }
	}
  }
  
  static private void ParseRewardsProductType(JSONObject node, ProductItem item) {
	if (!node.has(WEBSITE_PRODUCT_TYPE) || node.isNull(WEBSITE_PRODUCT_TYPE)) {
	  return;
	}
	try {
	  JSONArray product_types = new JSONArray(node.getString(WEBSITE_PRODUCT_TYPE));
	  if (product_types == null || product_types.length() < 1) return;
	  item.types_ = new ArrayList<String>(product_types.length());
      for (int i = 0; i < product_types.length(); i++) {
        item.types_.add(product_types.getString(i));
      }
	} catch (JSONException e) {
      Log.e("PHB ERROR", "ProductsPageParse::ParseRewardsProductType. Unable to parse ProductType:\n" + node.toString());
      // TODO(PHB): Handle exception gracefully.
    }
  }
  
  static public void ParseRewardsPage(String response, ArrayList<ProductItem> products_list) {
    try {
      JSONArray products = new JSONArray(response);
      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);
        ProductItem item = new ProductItem();
        
        // Parse node_title.
        ParseRewardsNodeTitle(product.getString(WEBSITE_TITLE), item);
        
        // Parse sku.
        ParseRewardsSku(product.getString(WEBSITE_SKU), item);
        
        // Parse node_type.
        ParseRewardsNodeType(product.getString(WEBSITE_NODE_TYPE), item);
        
        // Parse nid.
        ParseRewardsNodeId(product.getString(WEBSITE_NID), item);
        
        // Parse product_id.
        ParseRewardsProductId(product.getString(WEBSITE_PID), item);
        
        // Parse Promote.
        ParseRewardsPromote(product, item);
        
        // Parse Max Quantity.
        ParseRewardsMaxQuantity(product, item);
        
        // Parse Image.
        ParseRewardsImage(product, item);
        
        // Parse Price.
        ParseRewardsPrice(product, item);
        
        // Parse Point_Price.
        ParseRewardsPointPrice(product, item);
        
        // Parse Product_Type.
        ParseRewardsProductType(product, item);
        
        // Parse Drawing Date.
        ParseRewardsDrawingDate(product, item);
        
        // Add product info to products_list.
        products_list.add(item);
      }
    } catch (JSONException e) {
        Log.e("PHB ERROR", "ProductsPageParser.ParseRewardsPage: Failed to parse response");
        // TODO(PHB): Handle exception.
    }
  }
  
  static public void ParseDrawingsPage(String response, ArrayList<ProductItem> products_list) {
    try {
      JSONArray products = new JSONArray(response);
      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);
        ProductItem item = new ProductItem();
        
        // Parse nid.
        ParseDrawingsNodeId(product.getString(WEBSITE_NID), item);
        
        // Parse node_title.
        ParseDrawingsNodeTitle(product.getString(WEBSITE_TITLE), item);
        
        // Parse product_id.
        ParseDrawingsProductId(product.getString(WEBSITE_PID), item);
       
        // Parse Date.
        // TODO(PHB): Note the space in 'drawing date' below. If the json tag gets correctly
        // updated to 'drawing_date', you'll need to update the string below accordingly.
        ParseDrawingsDate(product.getString(WEBSITE_DATE), item);
        
        // Add product info to products_list.
	  	products_list.add(item);
      }
    } catch (JSONException e) {
      Log.e("PHB ERROR", "ProductsPageParser.ParseDrawingsPage: Failed to parse response");
      // TODO(PHB): Handle exception.
    }
  }
    
  static private String ParseDateFromDrawingDate(String date) {
    if (date == null || date.isEmpty()) return "";
    int expected_start = date.indexOf("<span class=\"");
    if (expected_start < 0) {
      Log.e("PHB ERROR", "ProductsActivity.ParseDateFromDrawingsDate: Unexpected format of node_title: " + date);
      return "";
    }
    String first_cut = date.substring(expected_start);
    int end_link = first_cut.indexOf("\">");
    if (end_link < 0) {
      Log.e("PHB ERROR", "ProductsActivity.ParseDateFromDrawingsDate: Unexpected format of node_title: " + date);
      return "";
    }
    String second_cut = first_cut.substring(end_link + 2);
    int close_tag = second_cut.indexOf("</span>");
    if (close_tag < 0) {
      Log.e("PHB ERROR", "ProductsActivity.ParseDateFromDrawingsDate: Unexpected format of node_title: " + date);
      return "";
    }
    return second_cut.substring(0, close_tag);
  }

  static private String ParseTitleFromNodeTitle(String node_title) {
    if (node_title == null || node_title.isEmpty()) return "";
    int expected_start = node_title.indexOf("<a href=\"");
    if (expected_start < 0) {
      Log.e("PHB ERROR", "ProductsActivity.ParseTitleFromNodeTitle: Unexpected format of node_title: " + node_title);
      return "";
    }
    String first_cut = node_title.substring(expected_start); 
    int end_link = first_cut.indexOf("\">");
    if (end_link < 0) {
      Log.e("PHB ERROR", "ProductsActivity.ParseTitleFromNodeTitle: Unexpected format of node_title: " + node_title);
      return "";
    }
    String second_cut = first_cut.substring(end_link + 2);
    int close_tag = second_cut.indexOf("</a>");
    if (close_tag < 0) {
      Log.e("PHB ERROR", "ProductsActivity.ParseTitleFromNodeTitle: Unexpected format of node_title: " + node_title);
      return "";
    }
    return second_cut.substring(0, close_tag);
  }
    
  static private String ParseImageUrlFromNodeId(String nid) {
    if (nid == null || nid.isEmpty()) return "";
    int expected_start = nid.indexOf("<img ");
    if (expected_start < 0) {
      Log.e("PHB ERROR", "ProductsActivity.ParseImageUrlFromNodeId: Unexpected format of nid: " + nid);
      return "";
    }
    String first_cut = nid.substring(expected_start);
    int start_src = first_cut.indexOf("src=\"");
    if (start_src < 0) {
      Log.e("PHB ERROR", "ProductsActivity.ParseImageUrlFromNodeId: Unexpected format of node_title: " + nid);
      return "";
    }
    String second_cut = first_cut.substring(start_src + 5);
    int end_src = second_cut.indexOf("\"");
    if (end_src < 0) {
      Log.e("PHB ERROR", "ProductsActivity.ParseImageUrlFromNodeId: Unexpected format of node_title: " + nid);
      return "";
    }
    return second_cut.substring(0, end_src);
  }
    
  static private String ParseDrawingUrlFromNodeId(String nid) {
    if (nid == null || nid.isEmpty()) return "";
    int expected_start = nid.indexOf("<a href=\"");
    if (expected_start < 0) {
      Log.e("PHB ERROR", "ProductsActivity.ParseDrawingUrlFromNodeId: Unexpected format of nid: " + nid);
      return "";
    }
    String first_cut = nid.substring(expected_start + 8);
    int end_link = first_cut.indexOf("\">");
    if (end_link < 0) {
      Log.e("PHB ERROR", "ProductsActivity.ParseDrawingUrlFromNodeId: Unexpected format of node_title: " + nid);
      return "";
    }
    return first_cut.substring(0, end_link);
  }
}