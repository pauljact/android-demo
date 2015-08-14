package com.jact.jactfirstdemo;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.util.Log;

public class ProductsPageParser {
  // Website node keys.
  private static final String WEBSITE_NID = "nid";
  private static final String WEBSITE_TITLE = "node_title";
  private static final String WEBSITE_SUMMARY_BODY = "Body";
  // As of June 2015, we decided to just use "value" tag instead of "summary"
  private static final String WEBSITE_SUMMARY = "value";
  //private static final String WEBSITE_SUMMARY = "summary";
  private static final String WEBSITE_PID = "commerce_product_field_data_field_product_product_id";
  private static final String WEBSITE_NODE_TYPE = "node_type";
  private static final String WEBSITE_DATE = "drawing date";
  private static final String WEBSITE_DRAWING_DATE = "Drawing_Ends";
  private static final String WEBSITE_SKU = "commerce_product_field_data_field_product_sku";
  private static final String WEBSITE_PROMOTE = "Promote";
  private static final String WEBSITE_MAX_QUANTITY_OLD = "Max order quantity";
  private static final String WEBSITE_MAX_QUANTITY = "Max_order_quantity";
  private static final String WEBSITE_VALUE = "value";
  private static final String WEBSITE_IMAGE = "field_product_image";
  private static final String WEBSITE_IMAGE_URI = "uri";
  private static final String WEBSITE_ORIG_PRICE = "Original_Price";
  private static final String WEBSITE_PRICE = "Price";
  private static final String WEBSITE_PRICE_AMOUNT = "amount";
  private static final String WEBSITE_PRICE_CURRENCY = "currency_code";
  private static final String WEBSITE_POINT_PRICE = "Point_Price";
  private static final String WEBSITE_PRODUCT_TYPE = "Product_Type";
	
  private static final String jact_icons_website_ = "/sites/default/files/styles/medium/public/";
  
  public static class ProductItem {
    public String nid_;
    public String title_;
    public String summary_;
    public String pid_;
    public String max_quantity_;
    public String node_type_;
    public String date_;
    public String sku_;
    public boolean promote_;
    public String img_url_;
    public String drawing_url_;
    public String orig_bux_;
    public String orig_usd_;
    public String orig_points_;
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
      return "NID: " + nid_ + ", title: " + title_ + ", summary: " + summary_ + 
    		 ", pid: " + pid_ + ", max quantity: " + max_quantity_ + ", node_type: " +
             node_type_ + ", date: " + date_ + ", sku: " + sku_ + ", image_url_: " +
    	     img_url_ + ", drawing url: " + drawing_url_ + ", promote: " + promote_ +
             ", orig_price: " + orig_usd_ + ", bux: " + bux_ +
    		 ", usd: " + usd_ + ", points: " + points_ + ", types: " + types;
    }
  }

  @SuppressLint("LongLogTag")
  static private String ParseNode(JSONObject node, String tag) {
	if (!node.has(tag) || node.isNull(tag)) {
	  return "";
	}
	try {
	  // When tag is present, it is a JSONObject; but when it is absent, it is an empty JSONArray.
	  // This makes handling them annoying.
	  JSONObject block = new JSONObject(node.getString(tag));
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
		  if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParse::ParseNode", "For tag: " + tag + ", Promote array " +
				             "should have size at most one, but found: " + block_array.length()); 
		}
	  } catch (JSONException ex) {
		  if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParse::ParseNode", "For tag: " + tag + ", Failed to parse " +
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
    if (node.isEmpty()) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParser::ParseRewardsNodeType", "Empty node type.");
      return;
    }
    if (node.equals("collect_and_win")) {
      item.node_type_ = "Collect and Win";
    } else if (node.equals("jact_prize_drawing")) {
      item.node_type_ = "Prize Drawings";
    } else if (node.equals("premium_product")) {
      item.node_type_ = "Premium Rewards";
    } else if (node.equals("product")) {
      item.node_type_ = "Points Only";
    } else {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParser::ParseRewardsNodeType",
            "Unable to parse product type: '" + node + "'");
    }
  }
  @SuppressLint("LongLogTag")
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
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParse::ParseRewardsPromote", "Unrecognize Promote->Value tag: " + value);
	}
  }

  static private void ParseRewardsMaxQuantity(JSONObject node, ProductItem item) {
	String value = ParseNode(node, WEBSITE_MAX_QUANTITY);
	// Rest/Rewards was returning a bad string for this node title.
	// The below makes this code compatible for either version.
	if (value.isEmpty()) {
	  value = ParseNode(node, WEBSITE_MAX_QUANTITY_OLD);
	}
	if (value.isEmpty() || value.equals("-1") || value.equals("0")) {
	  item.max_quantity_ = "-1";
	} else {
	  item.max_quantity_ = value;
	}
    if (!JactActionBarActivity.IS_PRODUCTION) Log.w("ProductsPageParser", "MaxQuantity: " + value);
  }
  
  static private void ParseRewardsDrawingDate(JSONObject node, ProductItem item) {
    String value = ParseNode(node, WEBSITE_DRAWING_DATE);
	if (!value.isEmpty()) {
	  item.date_ = value;
	}
  }
  
  static private void ParseRewardsImageFromString(String value, JSONObject node, ProductItem item) {
	if (value.isEmpty()) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParser::ParseRewardsImage", "Unable to find Image uri:\n" + node.toString());
      // TODO(PHB): Handle missing image.
      return;
	}
	int prefix_index = value.indexOf("public://");
	if (prefix_index != 0) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParser::ParseRewardsImage", "Unable to parse Image uri:\n" + node.toString());
      // TODO(PHB): Handle missing image.
      return;
	}
	String image_url = value.substring(9);  // 9 is the length of prefix "public://" that should be removed.
	item.img_url_ = GetUrlTask.GetJactDomain() + jact_icons_website_ +
	                image_url.replace(" ", "%20");  // Replace whitespace in url with %20.
  }
  static private void ParseRewardsImage(JSONObject node, ProductItem item) {
	if (!node.has(WEBSITE_IMAGE) || node.isNull(WEBSITE_IMAGE)) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParser::ParseRewardsImage", "Unable to find Image tag:\n" + node.toString());
      // TODO(PHB): Handle missing image.
	  return;
	}
	try {
	  JSONObject image = new JSONObject(node.getString(WEBSITE_IMAGE));
	  String value = image.getString(WEBSITE_IMAGE_URI);
	  ParseRewardsImageFromString(value, node, item);
	} catch (JSONException e) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.w("ProductsPageParser::ParseRewardsImage",
    	  	"Unable to parse Image tag using old format, trying new format");
	  try {
        String value = node.getString(WEBSITE_IMAGE);
	    ParseRewardsImageFromString(value, node, item);
	  } catch (JSONException ex) {
	      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParser::ParseRewardsImage",
	    	  	"Unable to parse Image tag using new format. Node:\n" + node.toString());
          // TODO(PHB): Handle exception gracefully.
	  }
    }
  }
  
  static private void ParseRewardsSummary(JSONObject node, ProductItem item) {
	if (!node.has(WEBSITE_SUMMARY_BODY) || node.isNull(WEBSITE_SUMMARY_BODY)) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParse::ParseRewardsSummary", "Unable to find Body tag:\n" + node.toString());
	  return;
	}
	try {
	  JSONObject summary_body = new JSONObject(node.getString(WEBSITE_SUMMARY_BODY));
	  String summary = summary_body.getString(WEBSITE_SUMMARY);
	  if (summary.isEmpty()) {
        if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParse::ParseRewardsSummary", "Unable to find summary:\n" + node.toString());
        return;
	  }
	  item.summary_ = summary;
	} catch (JSONException e) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParse::ParseRewardsSummary", "Unable to parse Body tag:\n" + node.toString());
    }
  }
	
  static private void ParseRewardsPrice(JSONObject node, ProductItem item, boolean isOriginalPrice) {
	if (!node.has(WEBSITE_PRICE) || node.isNull(WEBSITE_PRICE)) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParse::ParseRewardsPrice", "Unable to find Price tag:\n" + node.toString());
      // TODO(PHB): Handle missing price.
	  return;
	}
	try {
	  JSONObject price = new JSONObject(node.getString(isOriginalPrice ? WEBSITE_ORIG_PRICE : WEBSITE_PRICE));
	  String amount = price.getString(WEBSITE_PRICE_AMOUNT);
	  String currency = price.getString(WEBSITE_PRICE_CURRENCY);
	  if (amount.isEmpty() || currency.isEmpty()) {
        if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParse::ParseRewardsPrice", "Unable to find Price amount:\n" + node.toString());
        // TODO(PHB): Handle missing price.
        return;
	  }
	  if (!currency.equalsIgnoreCase("BUX") &&
	      !currency.equalsIgnoreCase("USD") &&
		  !currency.equalsIgnoreCase("Points")) {
        if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParse::ParseRewardsPrice", "Unrecognize currency: " + currency);
        // TODO(PHB): Handle missing price.
        return;
	  }
	  if (currency.equalsIgnoreCase("BUX")) {
	    if (amount.substring(amount.length() - 2).equalsIgnoreCase("00")) {
	      if (isOriginalPrice) {
	    	item.orig_bux_ = amount.substring(0, amount.length() - 2);
	      } else {
		    item.bux_ = amount.substring(0, amount.length() - 2);
	      }
	    } else {
	      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParser::ParseRewardsPrice", "BUX should be divisible by 100: " + amount);
	      if (isOriginalPrice) {
	    	item.orig_bux_ = amount;
	      } else {
		   	item.bux_ = amount;
	      }
	    }
	  } else if (currency.equalsIgnoreCase("USD")) {
		  if (isOriginalPrice) {
			item.orig_usd_ = amount.substring(0, amount.length() - 2) + "." + amount.substring(amount.length() - 2);
		  } else {
			item.usd_ = amount.substring(0, amount.length() - 2) + "." + amount.substring(amount.length() - 2);
		  }
	  } else {
		if (isOriginalPrice) {
		  item.orig_points_ = amount;
		} else {
		  item.points_ = amount;
		}
	  }
	} catch (JSONException e) {
	  if (isOriginalPrice) {
		if (!JactActionBarActivity.IS_PRODUCTION) Log.w("ProductsPageParse::ParseRewardsPrice", "Unable to find Original price for item:\n" + node.toString());  
	  } else {
		if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParse::ParseRewardsPrice", "Unable to parse Price tag:\n" + node.toString());
        // TODO(PHB): Handle exception gracefully.
	  }
    }
  }
  
  static private void ParseRewardsPointPrice(JSONObject node, ProductItem item) {
	String value = ParseNode(node, WEBSITE_POINT_PRICE);
	if (!value.isEmpty()) {
	  if (item.points_ != null && !item.points_.isEmpty()) {
		if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParse::ParseRewardsPointPrice",
			  "Duplicate entries for Price in points:\n" + node.toString());	
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
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParse::ParseRewardsProductType", "Unable to parse ProductType:\n" + node.toString());
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
        
        // Parse Summary.
        ParseRewardsSummary(product, item);
        
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

        // Parse Original Price.
        ParseRewardsPrice(product, item, true);
        
        // Parse Price.
        ParseRewardsPrice(product, item, false);
        
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
        if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParser.ParseRewardsPage", "Failed to parse response");
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
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsPageParser.ParseDrawingsPage", "Failed to parse response");
      // TODO(PHB): Handle exception.
    }
  }
    
  static private String ParseDateFromDrawingDate(String date) {
    if (date == null || date.isEmpty()) return "";
    int expected_start = date.indexOf("<span class=\"");
    if (expected_start < 0) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsActivity.ParseDateFromDrawingsDate", "Unexpected format of node_title: " + date);
      return "";
    }
    String first_cut = date.substring(expected_start);
    int end_link = first_cut.indexOf("\">");
    if (end_link < 0) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsActivity.ParseDateFromDrawingsDate", "Unexpected format of node_title: " + date);
      return "";
    }
    String second_cut = first_cut.substring(end_link + 2);
    int close_tag = second_cut.indexOf("</span>");
    if (close_tag < 0) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsActivity.ParseDateFromDrawingsDate", "Unexpected format of node_title: " + date);
      return "";
    }
    return second_cut.substring(0, close_tag);
  }

  static private String ParseTitleFromNodeTitle(String node_title) {
    if (node_title == null || node_title.isEmpty()) return "";
    int expected_start = node_title.indexOf("<a href=\"");
    if (expected_start < 0) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsActivity.ParseTitleFromNodeTitle", "Unexpected format of node_title: " + node_title);
      return "";
    }
    String first_cut = node_title.substring(expected_start); 
    int end_link = first_cut.indexOf("\">");
    if (end_link < 0) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsActivity.ParseTitleFromNodeTitle", "Unexpected format of node_title: " + node_title);
      return "";
    }
    String second_cut = first_cut.substring(end_link + 2);
    int close_tag = second_cut.indexOf("</a>");
    if (close_tag < 0) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsActivity.ParseTitleFromNodeTitle", "Unexpected format of node_title: " + node_title);
      return "";
    }
    return second_cut.substring(0, close_tag);
  }
    
  static private String ParseImageUrlFromNodeId(String nid) {
    if (nid == null || nid.isEmpty()) return "";
    int expected_start = nid.indexOf("<img ");
    if (expected_start < 0) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsActivity.ParseImageUrlFromNodeId", "Unexpected format of nid: " + nid);
      return "";
    }
    String first_cut = nid.substring(expected_start);
    int start_src = first_cut.indexOf("src=\"");
    if (start_src < 0) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsActivity.ParseImageUrlFromNodeId", "Unexpected format of node_title: " + nid);
      return "";
    }
    String second_cut = first_cut.substring(start_src + 5);
    int end_src = second_cut.indexOf("\"");
    if (end_src < 0) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsActivity.ParseImageUrlFromNodeId", "Unexpected format of node_title: " + nid);
      return "";
    }
    return second_cut.substring(0, end_src);
  }
    
  static private String ParseDrawingUrlFromNodeId(String nid) {
    if (nid == null || nid.isEmpty()) return "";
    int expected_start = nid.indexOf("<a href=\"");
    if (expected_start < 0) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsActivity.ParseDrawingUrlFromNodeId", "Unexpected format of nid: " + nid);
      return "";
    }
    String first_cut = nid.substring(expected_start + 8);
    int end_link = first_cut.indexOf("\">");
    if (end_link < 0) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsActivity.ParseDrawingUrlFromNodeId", "Unexpected format of node_title: " + nid);
      return "";
    }
    return first_cut.substring(0, end_link);
  }

  public static String PrintProductItem(ProductItem item) {
    if (item == null) return "";
    String to_return = "";
    if (item.nid_ != null) {
      to_return += "Node ID: " + item.nid_;
    }
    if (item.title_ != null) {
      to_return += "Title: " + item.title_;
    }
    if (item.summary_ != null) {
      to_return += "Summary: " + item.summary_;
    }
    if (item.pid_ != null) {
      to_return += "PID: " + item.pid_;
    }
    if (item.max_quantity_ != null) {
      to_return += "Max. Quantity: " + item.max_quantity_;
    }
    if (item.node_type_ != null) {
      to_return += "Node Type: " + item.node_type_;
    }
    if (item.date_ != null) {
      to_return += "Date: " + item.date_;
    }
    if (item.img_url_ != null) {
      to_return += "Image Url: " + item.img_url_;
    }
    if (item.drawing_url_ != null) {
      to_return += "Drawing Url: " + item.drawing_url_;
    }
    if (item.orig_bux_ != null) {
      to_return += "Orig bux: " + item.orig_bux_;
    }
    if (item.orig_usd_ != null) {
      to_return += "Orig usd: " + item.orig_usd_;
    }
    if (item.bux_ != null) {
      to_return += "Jact bux: " + item.bux_;
    }
    if (item.usd_ != null) {
      to_return += "Jact usd: " + item.usd_;
    }
    if (item.types_ != null && !item.types_.isEmpty()) {
      for (String type : item.types_) to_return += "; Type: " + type;
    }
    return to_return;
  }
}
