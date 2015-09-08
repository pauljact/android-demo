package com.jact.jactapp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.jact.jactapp.ShoppingCartActivity.ItemToAddStatus;

import android.graphics.Bitmap;
import android.util.Log;

public class ShoppingUtils {
  // TODO(PHB): Determine if I should use any of the commented out tags below.
  //private static final String EMAIL = "mail";
  //private static final String PRODUCT_IDS = "product_ids";
  //private static final String UNIT_PRICE = "commerce_unit_price";
  //private static final String SUB_AREA = "sub_administrative_area";
  //private static final String SUB_CITY = "dependent_locality";
  //private static final String SUB_APT = "sub_premise";
  //private static final String ORG = "organisation_name";
  //private static final String FULL_NAME = "name_line";
  //private static final String INCLUDED = "included";
  //private static final String BILLING_ID = "commerce_customer_billing";
  //private static final String SHIPPING_ID = "commerce_customer_shipping";
  //private static final String NAME = "name";
  //private static final String LINE_ITEMS = "commerce_line_items";
  //private static final String ORDER_TOTAL = "commerce_order_total";
  private static final String ORDER_NUMBER = "order_number";
  private static final String ORDER_ID = "order_id";
  private static final String REVISION_ID = "revision_id";
  private static final String STATUS = "status";
  private static final String REVISION_TIME = "revision_timestamp";
  private static final String TYPE = "type";
  private static final String AMOUNT = "amount";
  private static final String CURRENCY = "currency_code";
  private static final String DATA = "data";
  private static final String COMPONENTS = "components";
  private static final String PRICE = "price";
  private static final String LINE_ITEMS_BLOCK = "commerce_line_items_entities";
  private static final String LINE_ITEM_ID = "line_item_id";
  private static final String LINE_ITEM_LABEL = "line_item_label";
  private static final String QUANTITY = "quantity";
  private static final String PRODUCT_ID = "commerce_product";
  //private static final String COMMERCE_TOTAL = "commerce_total";
  private static final String COMMERCE_UNIT_PRICE = "commerce_unit_price";
  private static final String LINE_ITEM_TITLE = "line_item_title";
  private static final String NODE_ID = "commerce_display_path";
  private static final String POINTS_PRICE = "field_point_price";
  private static final String BILLING_ADDRESSES = "commerce_customer_billing_entities";
  private static final String SHIPPING_ADDRESSES = "commerce_customer_shipping_entities";
  private static final String PROFILE_ID = "profile_id";
  private static final String ADDRESS = "commerce_customer_address";
  private static final String COUNTRY = "country";
  private static final String STATE = "administrative_area";
  private static final String CITY = "locality";
  private static final String ZIP = "postal_code";
  private static final String STREET = "thoroughfare";
  private static final String APT = "premise";
  private static final String FIRST_NAME = "first_name";
  private static final String LAST_NAME = "last_name";
  
  private static final String LINE_ITEM_FIELD_SEPARATOR = "_PHB_FIELD_PHB_";
  private static final String LINE_ITEM_VALUE_SEPARATOR = "_PHB_VALUE_PHB_";
  private static final String COST_ITEM_SEPARATOR = "_PHB_COST_ITEM_PHB_";
  private static final String COST_FIELD_SEPARATOR = "_PHB_COST_FIELD_PHB_";
  private static final String COST_VALUE_SEPARATOR = "_PHB_COST_VALUE_PHB_";
  
  // The various GET/POST/PUT target urls and task names.
  // Create new (empty) cart.
  private static String cart_url_ = "/rest/cart.json";
  public static final String GET_CART_TASK = "get_cart_task";
  public static final String CREATE_CART_TASK = "create_cart_task";
  public static final String CLEAR_CART_TASK = "clear_cart_task";
  public static final String TASK_CART_SEPARATOR = "_PHB_TASK_CART_PHB_";
  // Add or Update a line-item (the latter requires the line-item id to be appended to the url).
  private static final String line_item_url_ = "/rest/line-item";
  public static final String ADD_LINE_ITEM_TASK = "add_line_item_task";
  public static final String UPDATE_LINE_ITEM_TASK = "update_line_item_task";
  // Get CSRF Token, and then perform a follow-up task next.
  public static final String get_csrf_url_ = "/services/session/token";
  public static final String GET_CSRF_THEN_CLEAR_CART_TASK = "get_csrf_then_clear_cart_task";
  public static final String GET_CSRF_THEN_CREATE_CART_TASK = "get_csrf_then_create_cart_task";
  public static final String GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK = "get_csrf_then_update_line_item_task";
  public static final String TASK_TASK_SEPARATOR = "_PHB_TASK_TASK_PHB_";
  // Get Cookies, and then perform a follow-up task next.
  public static final String get_cookies_url_ = "/rest/user/login";
  public static final String GET_COOKIES_THEN_CLEAR_CART_TASK = "get_cookies_then_clear_cart_task";
  public static final String GET_COOKIES_THEN_GET_CART_TASK = "get_cookies_then_get_cart_task";
  public static final String GET_COOKIES_THEN_CREATE_CART_TASK = "get_cookies_then_create_cart_task";
  public static final String GET_COOKIES_THEN_UPDATE_LINE_ITEM_TASK = "get_cookies_then_update_line_item_task";
  
  public static final int MAX_OUTSTANDING_SHOPPING_REQUESTS = 5;
  
  public enum CurrencyCode {
	BUX,
	USD,
	POINTS
  }
  public static class Amount {
    public double price_;
    public CurrencyCode type_;
  }
  
  public static class LineItem {
	public int id_;
	public int order_id_;
	public int quantity_;
	public int max_quantity_;
	public int pid_;
	public int entity_id_;
	public boolean is_drawing_;
	public String drawing_date_;
	public String type_;
	public String label_;
	public String title_;
	public Bitmap product_icon_;
	public String img_url_;
	public ArrayList<Amount> cost_;
  }
  
  public enum CheckoutStatus {
    EMPTY,
	CREATE,
	CHECKOUT,
	REVIEW,
    PENDING,
    COMPLETE
  }
  
  public static class ShoppingCartInfo {
	public int order_id_;
	public int revision_id_;
	public long timestamp_;
	public CheckoutStatus status_;
	public ArrayList<LineItem> line_items_;
	public ArrayList<ShoppingCartActivity.JactAddress> shipping_addresses_;
	public ArrayList<ShoppingCartActivity.JactAddress> billing_addresses_;
	
	public ShoppingCartInfo() {
	  order_id_ = 0;
	  revision_id_ = 0;
	  timestamp_ = 0;
	}
	
	public ShoppingCartInfo(ShoppingCartInfo other) {
	  if (other == null) return;
	  order_id_ = other.order_id_;
	  revision_id_ = other.revision_id_;
	  timestamp_ = other.timestamp_;
	  status_ = other.status_;
	  if (other.line_items_ != null) {
	    line_items_ = (ArrayList<LineItem>) other.line_items_.clone();
	  }
	  if (other.shipping_addresses_ != null) {
		shipping_addresses_ = (ArrayList<ShoppingCartActivity.JactAddress>) other.shipping_addresses_.clone();
	  }
	  if (other.billing_addresses_ != null) {
		billing_addresses_ = (ArrayList<ShoppingCartActivity.JactAddress>) other.billing_addresses_.clone();
	  }
	}
	
	public String toString() {
	  String line_items = "";
	  if (line_items_ != null) {
	    for (LineItem line_item : line_items_) {
		  line_items += "line item: ";
	      line_items += PrintLineItemHumanReadable(line_item) + ", ";
	    }
	  }
	  return "order id: " + order_id_ + ", revision id: " + revision_id_ + ", timestamp: " + timestamp_ +
			 ", status: " + status_ + ", line_items: " + line_items;
	}
  }
  
  public static void RefreshCookies(
	  ProcessUrlResponseCallback parent_class, String username, String password, String next_task) {
  	if (parent_class.GetNumRequestsCounter() >= MAX_OUTSTANDING_SHOPPING_REQUESTS) {
	  parent_class.DisplayPopup("Unable to Reach Jact Server. Please Try Again.");
	  return;
	}
	parent_class.IncrementNumRequestsCounter();
    GetUrlTask task = new GetUrlTask(parent_class, GetUrlTask.TargetType.JSON);
	GetUrlTask.UrlParams params = new GetUrlTask.UrlParams();
	params.url_ = GetUrlTask.JACT_DOMAIN + get_cookies_url_;
	params.connection_type_ = "POST";
	params.extra_info_ = next_task;
  	ArrayList<String> header_info = new ArrayList<String>();
    header_info.add(GetUrlTask.CreateHeaderInfo("Content-Type", "application/json"));
    ArrayList<String> form_info = new ArrayList<String>();
	form_info.add(GetUrlTask.CreateFormInfo("username", username));
	form_info.add(GetUrlTask.CreateFormInfo("password", password));
  	params.post_string_ = GetUrlTask.CreatePostString(header_info, form_info);
	task.execute(params);
  }
  
  public static boolean GetCsrfToken(
      ProcessUrlResponseCallback parent_class, String cookies, String next_task) {
	ShoppingCartActivity.IncrementNumCsrfRequests();
	if (ShoppingCartActivity.GetNumCsrfRequests() > 2) {
      // TODO(PHB): Handle this.
	  // Do not execute another CSRF request if the previous ones have failed (avoids
	  // infinite loop to server for CSRF requests.
	  Log.e("ShoppingUtils::GetCsrfToken", "Abandoning request: too many failed attempts: " +
			             ShoppingCartActivity.GetNumCsrfRequests());
	  return false;
	}
  	if (parent_class.GetNumRequestsCounter() >= MAX_OUTSTANDING_SHOPPING_REQUESTS) {
	  parent_class.DisplayPopup("Unable to Reach Jact Server. Please Try Again.");
	  return false;
	}
	parent_class.IncrementNumRequestsCounter();
	GetUrlTask task = new GetUrlTask(parent_class, GetUrlTask.TargetType.JSON);
	GetUrlTask.UrlParams params = new GetUrlTask.UrlParams();
	params.url_ = GetUrlTask.JACT_DOMAIN + get_csrf_url_;
	params.connection_type_ = "GET";
	params.extra_info_ = next_task;
  	params.cookies_ = cookies;
  	ArrayList<String> header_info = new ArrayList<String>();
    header_info.add(GetUrlTask.CreateHeaderInfo("Content-Type", "application/json"));
  	params.post_string_ = GetUrlTask.CreatePostString(header_info, null);
	task.execute(params);
	return true;
  }
  
  public static boolean LineItemsAreEquivalent(LineItem first, LineItem second) {
	if (first == null || second == null) return false;
	boolean same_cost = (first.cost_ == null) == (second.cost_ == null);
	if (same_cost && first.cost_ != null) {
	  if (first.cost_.size() != second.cost_.size()) return false;
	  for (Amount first_amount : first.cost_) {
	    boolean found_match = false;
	    for (Amount second_amount : second.cost_){
	      if (first_amount.type_ == second_amount.type_ && first_amount.price_ == second_amount.price_) {
	        found_match = true;
	        break;
	      }
	    }
	    if (!found_match) return false;
	  }
	}
    boolean to_return = (same_cost && (first.id_ == second.id_) && (first.order_id_ == second.order_id_) &&
    		(first.quantity_ == second.quantity_) && (first.pid_ == second.pid_) &&
    		(first.entity_id_ == second.entity_id_) && (first.is_drawing_ == second.is_drawing_) &&
    		first.type_.equals(second.type_) && first.label_.equals(second.label_) &&
    		first.title_.equals(second.title_));
	Log.w("ShoppingUtils::LineItemsAreEquivalent", "Returning: " + to_return + "; first:\n" +
	          PrintLineItemHumanReadable(first) + ";\nSecond: " +
	          PrintLineItemHumanReadable(second));
    return to_return;
  }
	
  // This creates a new Cart on the server. If there was an existing cart/order, it will be deleted.
  // Additionally, this will add the 'cart' to the new cart/order; however, it is assumed that the
  // 'cart' to add has only a single line item. If you want to add multiple items, you'll have to
  // first create the cart here, and then call UpdateLineItem(). If 'cart' is null, simply creates
  // an empty cart on the server.
  public static boolean CreateServerCart(
	  ProcessUrlResponseCallback parent_class, String cookies, String csrf_token, LineItem line_item) {
  	if (parent_class.GetNumRequestsCounter() >= MAX_OUTSTANDING_SHOPPING_REQUESTS) {
	  parent_class.DisplayPopup("Unable to Reach Jact Server. Please Try Again.");
	  return false;
	}
	parent_class.IncrementNumRequestsCounter();
	GetUrlTask task = new GetUrlTask(parent_class, GetUrlTask.TargetType.JSON);
	GetUrlTask.UrlParams params = new GetUrlTask.UrlParams();
	String extra_info = CREATE_CART_TASK;
	
	// When creating a new cart, you can optionally initialize it with an item.
	if (line_item != null) {
      extra_info += TASK_CART_SEPARATOR + PrintLineItem(line_item);
	}
	
	// Construct request that will create the cart on the server.
	params.url_ = GetUrlTask.JACT_DOMAIN + cart_url_;
	params.connection_type_ = "POST";
	params.extra_info_ = extra_info;
  	params.cookies_ = cookies;
  	ArrayList<String> header_info = new ArrayList<String>();
    //PHBheader_info.add(GetUrlTask.CreateHeaderInfo("Content-Type", "application/json"));
    header_info.add(GetUrlTask.CreateHeaderInfo("X-CSRF-Token", csrf_token));
  	params.post_string_ = GetUrlTask.CreatePostString(header_info, null);
	task.execute(params);
	return true;
  }
  
  public static boolean CreateServerCart(
	  ProcessUrlResponseCallback parent_class, String cookies, String csrf_token) {
	return CreateServerCart(parent_class, cookies, csrf_token, null);
  }

  // Adds or Updates a line-item in the cart:
  //   - If line_item <= 0: Creates a new line-item with the relevant parameters as set by input
  //   - If line_item > 0: Updates that line-item with the relevant parameters as set by input
  public static boolean UpdateLineItem(
	  ProcessUrlResponseCallback parent_class, String cookies, String csrf_token,
	  int line_item, int order_id, int pid, String type, int node_id, int point_price, int quantity) {
	GetUrlTask task = new GetUrlTask(parent_class, GetUrlTask.TargetType.JSON);
  	GetUrlTask.UrlParams params = new GetUrlTask.UrlParams();
  	
	// Check if this is updating an existing line-item, or creating a new one. In the latter case,
	// line_item should be non-positive.
	String extra_info = "";
  	if (line_item <= 0) {
	  // For new line-items, we must specify order_id, pid, type, and node_id. Make sure these are all set.
	  if (order_id < 0 || pid < 0 || node_id < 0 || type == null || type.isEmpty()) {
	    Log.e("ShoppingUtils::UpdateServerCart", "New line item must specify order_id (" + order_id +
	    		           "), pid (" + pid + "), type (" + type + "), and node_id (" + node_id + ").");
	    return false;
	  }
	  // Create new line-item via POSTing to /line-item
  	  params.url_ = GetUrlTask.JACT_DOMAIN + line_item_url_;
  	  params.connection_type_ = "POST";
  	  extra_info = ADD_LINE_ITEM_TASK;
	} else {
	  // Update existing line-item via PUTing to /line-item	  
  	  params.url_ = GetUrlTask.JACT_DOMAIN + line_item_url_ + "/" + Integer.toString(line_item);
  	  params.connection_type_ = "PUT";
  	  extra_info = UPDATE_LINE_ITEM_TASK;
	}
  	if (parent_class.GetNumRequestsCounter() >= MAX_OUTSTANDING_SHOPPING_REQUESTS) {
	  parent_class.DisplayPopup("Unable to Reach Jact Server. Please Try Again.");
	  return false;
	}
	parent_class.IncrementNumRequestsCounter();
	
  	params.cookies_ = cookies;
  	ArrayList<String> header_info = new ArrayList<String>();
  	header_info.add(GetUrlTask.CreateHeaderInfo("Content-Type", "application/json"));
    header_info.add(GetUrlTask.CreateHeaderInfo("X-CSRF-Token", csrf_token));
    ArrayList<String> form_info = new ArrayList<String>();
    // Field 'order_id' is only required when adding a NEW item (as opposed to updating
    // an existing line-item); indeed, in the latter case, the controller doesn't even
    // recognize the 'order-id' field. So only include it in the former case, which
    // we distinguish based on whether the request is 'POST' or 'PUT'.
    if (order_id > 0 && params.connection_type_.equals("POST")) {
      form_info.add(GetUrlTask.CreateFormInfo("order_id", Integer.toString(order_id)));
    }
  	if (pid > 0) {
  	  form_info.add(GetUrlTask.CreateFormInfo("commerce_product", Integer.toString(pid)));
  	}
  	if (!type.isEmpty()) {
  	  String cart_type = "product";
  	  if (type.equals("premium_product")) cart_type = type;
  	  form_info.add(GetUrlTask.CreateFormInfo("type", cart_type));
  	}
  	if (node_id > 0) {
  	  form_info.add(GetUrlTask.CreateFormInfo("commerce_display_path", "node/" + Integer.toString(node_id)));
  	}
  	if (point_price > 0) {
  	  form_info.add(GetUrlTask.CreateFormInfo("field_point_price", Integer.toString(point_price)));
  	}
  	if (quantity > 0 || (quantity == 0 && line_item > 0)) {
  	  form_info.add(GetUrlTask.CreateFormInfo("quantity", Integer.toString(quantity)));
  	}
  	params.post_string_ = GetUrlTask.CreatePostString(header_info, form_info);
  	params.extra_info_ = extra_info + TASK_CART_SEPARATOR +
  		PrintLineItem(CreateLineItem(line_item, order_id, pid, type, node_id, point_price, quantity));
	task.execute(params);
	return true;
  }

  // Clears Cart.
  /* PHB No longer used, as Clearing Server cart in one swoop is no longer possible?
     (POST to cart.json no longer seems to work).
  public static boolean ClearCart (
	  ProcessUrlResponseCallback parent_class, String cookies, String csrf_token) {
  	if (parent_class.GetNumRequestsCounter() >= MAX_OUTSTANDING_SHOPPING_REQUESTS) {
	  parent_class.DisplayPopup("Unable to Reach Jact Server. Please Try Again.");
	  return false;
	}
	parent_class.IncrementNumRequestsCounter();
	GetUrlTask task = new GetUrlTask(parent_class, GetUrlTask.TargetType.JSON);
  	GetUrlTask.UrlParams params = new GetUrlTask.UrlParams();

  	params.url_ = GetUrlTask.JACT_DOMAIN + cart_url_;
  	params.connection_type_ = "POST";
  	params.extra_info_ = CLEAR_CART_TASK;
	
  	params.cookies_ = cookies;
  	ArrayList<String> header_info = new ArrayList<String>();
  	header_info.add(GetUrlTask.CreateHeaderInfo("Content-Type", "application/json"));
    header_info.add(GetUrlTask.CreateHeaderInfo("X-CSRF-Token", csrf_token));
  	params.post_string_ = GetUrlTask.CreatePostString(header_info, null);
	task.execute(params);
	return true;
  }
  */
  
  private static LineItem CreateLineItem(
      int line_item, int order_id, int pid, String type, int node_id, int point_price, int quantity) {
    LineItem item = new LineItem();
    item.id_ = line_item;
    item.order_id_ = order_id;
    item.pid_ = pid;
    item.type_ = type;
    item.entity_id_ = node_id;
    item.quantity_ = quantity;
    item.cost_ = new ArrayList<Amount>();
    Amount amount = new Amount();
    amount.price_ = point_price;
    amount.type_ = CurrencyCode.POINTS;
    item.cost_.add(amount);
    return item;
  }
  
  //Same as above, but with API for the common use-case of updating the quantity of a line item.
  public static boolean UpdateLineItem(
	  ProcessUrlResponseCallback parent_class, String cookies, String csrf_token,
	  int line_item, int quantity) {
    return UpdateLineItem(parent_class, cookies, csrf_token, line_item, -1 /* order_id */,
     		                -1 /* pid */, "" /* type */, -1 /* node_id */, -1 /* point_price */, quantity);
  }
  
  // Same as above, but with API that has all information in the line_item.
  public static boolean UpdateLineItem(
	  ProcessUrlResponseCallback parent_class, String cookies, String csrf_token, LineItem line_item) {
	return UpdateLineItem(parent_class, cookies, csrf_token,
			              line_item.id_, line_item.order_id_, line_item.pid_, line_item.type_,
			              line_item.entity_id_, GetExtraPointPrice(line_item.cost_), line_item.quantity_);
  }
  
  //////////////////////////////////////////////////////////////////////////////
  /////////////////////////// Webpage Parsers //////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  public static boolean ParseLineItem(JSONObject item, ShoppingCartInfo cart) {
	Iterator<String> item_itr = item.keys();
	while (item_itr.hasNext()) {
	  String key = item_itr.next();
	  if (!ParseLineItem(item, key, cart)) {
		return false;
	  }
	}
    return true;
  }
  
  public static boolean ParseLineItem(JSONObject item, String key, ShoppingCartInfo cart) {
  	try {
	  if (key.equals(ORDER_NUMBER)) {
	    // Get order id.
        cart.order_id_ = item.getInt(key);
	  } else if (key.equals(REVISION_ID)) { 
        // Get Revision Id.
        cart.revision_id_ = item.getInt(key);
	  } else if (key.equals(REVISION_TIME)) {
        // Get Timestamp.
        cart.timestamp_ = item.getLong(key);
	  } else if (key.equals(STATUS)) {
        // Get Status.
        // TODO(PHB): Implement these based on all possible statuses.
        String status = item.getString(key);
        if (status.equals("PHB foo")) {
          cart.status_ = CheckoutStatus.EMPTY;
        } else if (status.equals("PHB foo")) {
          cart.status_ = CheckoutStatus.CREATE;
        } else if (status.equals("checkout_review")) {
          cart.status_ = CheckoutStatus.REVIEW;
        } else if (status.equals("cart")) {
          cart.status_ = CheckoutStatus.PENDING;
        } else if (status.equals("checkout_checkout")) {
          cart.status_ = CheckoutStatus.CHECKOUT;
        } else if (status.equals("PHB foo")) {
          cart.status_ = CheckoutStatus.COMPLETE;
        } else {
          Log.e("ShoppingUtils::ParseCart", "Unrecognized status: " + status);
          //return PrintErrorAndAbort("Unable to parse status: " + status);
        }
	  } else if (key.equals(LINE_ITEMS_BLOCK)) {
        // Get Line Items.
        if (!GetLineItems(item, cart)) return false;
	  } else if (key.equals(BILLING_ADDRESSES)) {
        // Get Billing Info.
        if (!GetBillingInfo(item, cart)) return false;
	  } else if (key.equals(SHIPPING_ADDRESSES)) {
        // Get Shipping Info.
        if (!GetShippingInfo(item, cart)) return false;
	  } else {
	    // TODO(PHB): Determine if this is a troublesome case; if so, handle it.
	  }
	  return true;
	} catch (JSONException e) {
      return PrintErrorAndAbort("Unable to parse JSON item (cart) as JSON. |Exception: " +
                                e.getMessage() + "|Webpage:\n" + cart.toString());
    }
  }
  
  private static boolean ParseCartLineItemKey(JSONObject item, String key, ShoppingCartInfo cart) {
	// Sometimes, the parse cart page response has JSONObjects for this key, sometimes it is just
	// a string. Handle both cases here.
    try {
	  //JSONObject cart_item = (JSONObject) cart_item_value;
      JSONObject cart_item = (JSONObject) item.getJSONObject(key);
	  return ParseLineItem(cart_item, cart); 
    //} catch (ClassCastException cast_ex) {
    } catch (JSONException json_ex) {
	  //String actual_cart_value = (String) cart_item_value;
      try {
        String actual_cart_value = item.getString(key);
	    Log.w("PHB", "ShoppingUtils::ParseCartLineItemKey. JSON Exception: " + json_ex.getMessage() +
		  	         ", actual_cart_value: " + actual_cart_value);
	    if (actual_cart_value.trim().isEmpty() || actual_cart_value.trim().equalsIgnoreCase("[]")) {
	      return true;
	    } else {
	      return ParseLineItem(item, key, cart);
	    }
      } catch (JSONException second_json_ex) {
  	    return PrintErrorAndAbort("ParseCartLineItemKey. 2nd JSON Exception: " + second_json_ex.getMessage());
      }
    }
  }

  public static boolean ParseCartFromCreateCartPage(String webpage, ShoppingCartInfo cart) {
    // TODO(PHB): Update this for real, then remove duplicate behavior from ...FromAdd|UpdateLineItem|Create
    return ParseCartFromGetCartPage(webpage, cart);
  }
  
  public static boolean ParseCartFromGetCartPage(String webpage, ShoppingCartInfo cart) {
    if (webpage.trim().isEmpty() || webpage.trim().equalsIgnoreCase("[]")) {
      cart.line_items_ = new ArrayList<LineItem>();
      cart.order_id_ = 0;
      cart.status_ = CheckoutStatus.EMPTY;
      return true;
    }
    try {
      // Make sure webpage can be parsed as a list of cart items.
      JSONArray cart_items = new JSONArray(webpage);
      if (cart_items == null || cart_items.length() != 1) {
        String message = cart_items == null ? "Null cart_items" : Integer.toString(cart_items.length());
        Log.w("ShoppingUtils::ParseCartPage", "Unexpected number of cart items. " +
                             "Webpage|" + webpage + "| length: " + webpage.length());
        return PrintErrorAndAbort("Unexpected number of cart items: " + message);
      }
      JSONObject item = cart_items.getJSONObject(0);
      return ParseLineItem(item, cart);
    } catch (JSONException e) {
      try {
    	// Since parsing as JSONArray failed, try to parse directly as JSONObject.
    	JSONObject item = new JSONObject(webpage);
    	Iterator<String> item_itr = item.keys();
    	String keys_processed = "";
  	    while (item_itr.hasNext()) {
  	      String item_key = item_itr.next();
  	      if (!ParseCartLineItemKey(item, item_key, cart)) {
  	    	return false;
  	      }
	  	  keys_processed += item_key + ", ";
  	    }
    	return true;
      } catch (JSONException ex) {
          return PrintErrorAndAbort("Unable to parse cart response as JSON. |Exception: " +
                                    ex.getMessage() + "|Webpage:\n" + webpage);
      }
    }
  }
  
  public static boolean ParseLineItemFromUpdateLineItemPage(JSONObject item, ArrayList<LineItem> line_items) {
    // TODO(PHB): Update this for real, and then remove the dual functionality from the ...FromAddLineItem... below.
    return ParseLineItemFromAddLineItemPage(item, line_items);
  }
  
  public static boolean ParseLineItemFromAddLineItemPage(JSONObject item, ArrayList<LineItem> line_items) {
    // When this function is called, sometimes I'm already at the line-item level, and
	// sometimes I need to dig another level deeper to get the field info for the line-item.
	// I distinguish the cases based on whether I find all of the required fields; if so,
	// I return. Otherwise, I try to use the item's keys to generate a new JSONObject,
	// and then call this function on that new object.
    LineItem new_item = new LineItem();
	try {
      Iterator<String> field_itr = item.keys();
      ArrayList<String> unused_keys = new ArrayList<String>();
      int num_req_fields_found = 0;
  	  while (field_itr.hasNext()) {
  	    String key = field_itr.next();
  	    if (key.equals(LINE_ITEM_ID)) {
          // Get Line Item ID.
          new_item.id_ = item.getInt(key);
          num_req_fields_found++;
  	    } else if (key.equals(ORDER_ID)) {
          // Get Order Id.
          new_item.order_id_ = item.getInt(key);
          num_req_fields_found++;
  	    } else if (key.equals(QUANTITY)) {
          // Get Quantity.
          new_item.quantity_ = item.getInt(key);
          num_req_fields_found++;
    	} else if (key.equals(PRODUCT_ID)) {
          // Get Product Id.
          new_item.pid_ = item.getInt(key);
          num_req_fields_found++;
    	} else if (key.equals(TYPE)) {
          // Get Type, Label, and Title.
          new_item.type_ = item.getString(key);
          num_req_fields_found++;
        } else if (key.equals(LINE_ITEM_LABEL)) {
          new_item.label_ = item.getString(key);
          num_req_fields_found++;
        } else if (key.equals(LINE_ITEM_TITLE)) {
          new_item.title_ = item.getString(key);
          num_req_fields_found++;
        } else if (key.equals(COMMERCE_UNIT_PRICE)) {
          // We choose COMMERCE_UNIT_PRICE instead of COMMERCE_TOTAL (which are the same if quantity
          // is '1') because for products with multiple currency types (e.g. USD + POINTS), only
          // the non-points price is stored in either of these places, while POINTS is retrieved
          // via POINTS_PRICE, and the latter ONLY is given as a per-unit cost. Thus, we store
          // all prices in per-unit cost, and then when computing final cost, multiply everything
          // by the quantity.
          num_req_fields_found++;
          // Get Cost.
          JSONObject cost = item.getJSONObject(key);
          if (cost == null) return PrintErrorAndAbort("Unable to parse line item cost.");
          JSONObject data = cost.getJSONObject(DATA);
          if (data == null) return PrintErrorAndAbort("Unable to parse line item cost data.");
          JSONArray components = new JSONArray(data.getString(COMPONENTS));
          if (components == null || components.length() == 0) {
            return PrintErrorAndAbort("Unable to parse line item components.");
          }
          if (new_item.cost_ == null) {
            new_item.cost_ = new ArrayList<Amount>(components.length());
          }
          // Loop through all cost elements (e.g. POINTS, BUX, PRICE).
		  for (int j = 0; j < components.length(); j++) {
		    JSONObject component = components.getJSONObject(j);
		    JSONObject price = component.getJSONObject(PRICE);
		    if (price == null) return PrintErrorAndAbort("Unable to parse line item, cost element " + j);
		    Amount amount = new Amount();
		    amount.price_ = price.getDouble(AMOUNT);
		    String currency_type = price.getString(CURRENCY);
		    if (currency_type.equals("POINTS")) {
		      amount.type_ = CurrencyCode.POINTS;
		    } else if (currency_type.equals("USD")) {
		      amount.type_ = CurrencyCode.USD;
		    } else if (currency_type.equals("BUX")) {
		      amount.type_ = CurrencyCode.BUX;
		    } else {
		      return PrintErrorAndAbort("Unable to parse currency type: " + currency_type +
		       		                  " for line item, cost element " + j);
		    }
		    new_item.cost_.add(amount);
		  }
        } else if (key.equals(POINTS_PRICE)) {
          // For items that have both BUX and POINTS, the POINTS amount is present in a separate field.
          String points_price = item.getString(key);
          num_req_fields_found++;
		  if (points_price != null && !points_price.isEmpty()) {
		    Amount amount = new Amount();
		    amount.price_ = item.getInt(POINTS_PRICE);
		    amount.type_ = CurrencyCode.POINTS;
            if (new_item.cost_ == null) {
              new_item.cost_ = new ArrayList<Amount>();
            }
		    new_item.cost_.add(amount);
		  }
        } else if (key.equals(NODE_ID)) {
		  // Get Entity Id.
		  String entity_id = item.getString(key);
          num_req_fields_found++;
		  if (entity_id.indexOf("node/") == 0) {
		    try {
		      new_item.entity_id_ = Integer.parseInt(entity_id.substring(5));
		    } catch (NumberFormatException e) {
		      Log.e("ShoppingUtils::GetBillingInfo",
		    		"Skipping line item: Unable to parse line item with entity id: " + entity_id);
		      continue;
		    }
		  }
        } else {
          unused_keys.add(key);
        }
  	  }
  	  if (num_req_fields_found < 5 && !unused_keys.isEmpty()) {
  	    try {
  	      for (String unused_key : unused_keys) {
  	        JSONObject actual_line_item = item.getJSONObject(unused_key);
  	        if (!ParseLineItemFromAddLineItemPage(actual_line_item, line_items)) return false;
  	      }
  	      return true;
  	    } catch (JSONException js_ex) {
  	      Log.e("ShoppingUtils::ParseAddLineItemPage", "Failed to dig a level deeper. Error: " +
  	                         js_ex.getMessage());
  	      return false;
  	    }
  	  }
      line_items.add(new_item);
  	  return true;
    } catch (JSONException e) {
	  Log.e("ShoppingUtils::ParseAddLineItemPage", "JSON Error: " + e.getMessage());
	  return false;
    }
  }
  
  //////////////////////////////////////////////////////////////////////////////
  private static boolean GetLineItems(JSONObject cart_item, ShoppingCartInfo cart) {
    String error_msg = "";
    try {
      String line_items_block = cart_item.getString(LINE_ITEMS_BLOCK);
      error_msg = "first";
      if (line_items_block == null) {
    	Log.e("ShoppingUtils::GetLineItems", "Unexpected null line_items block. cart_item: " + cart_item.toString());
    	return false;
      }
      JSONArray line_items = new JSONArray(line_items_block);
      error_msg = "second";
      if (line_items == null || line_items.length() == 0) {
    	Log.w("PHB", "ShoppingUtils::GetLineItems. Null line_items. cart_item: " + cart_item.toString());
    	return true;
      }
      cart.line_items_ = new ArrayList<LineItem>(line_items.length());
      
      // Loop  through line items.
      for (int i = 0; i < line_items.length(); i++) {
        JSONObject item = line_items.getJSONObject(i);
        if (!ParseLineItemFromAddLineItemPage(item, cart.line_items_)) {
          Log.e("ShoppingUtils::GetLineItems", "Error caused by line item: " + i);
          return false;
        }
      }
      return true;
    } catch (JSONException e) {
      try {
        JSONObject l_item = new JSONObject(cart_item.getString(LINE_ITEMS_BLOCK));
        cart.line_items_ = new ArrayList<LineItem>(1);
        if (!ParseLineItemFromAddLineItemPage(l_item, cart.line_items_)) {
            Log.e("ShoppingUtils::GetLineItems", "Error caused by line item: " + l_item.toString());
            return false;
        }
        return true;
      } catch (JSONException ex) {
        return PrintErrorAndAbort("Unable to parse " + LINE_ITEMS_BLOCK + ". Exception: " + ex.getMessage() +
    		                      "Error msg: " + error_msg);
      }
    }
  }
  
  private static boolean GetBillingInfo(JSONObject cart_item, ShoppingCartInfo cart) {
    String error_msg = "";
    try {
      String billing_addresses = cart_item.getString(BILLING_ADDRESSES);
      if (billing_addresses == null) return true;
      JSONArray billing_items = new JSONArray(billing_addresses);
      if (billing_items == null || billing_items.length() == 0) return true;
      cart.billing_addresses_ = new ArrayList<ShoppingCartActivity.JactAddress>(billing_items.length());
      
      // Loop through all billing items.
      boolean to_return = true;
      for (int i = 0; i < billing_items.length(); i++) {
        JSONObject item = billing_items.getJSONObject(i);
        to_return &= GetAdressInfo(item, cart.billing_addresses_);
      }
      return to_return;
    } catch (JSONException e) {
      try {
    	JSONObject billing_item = new JSONObject(cart_item.getString(BILLING_ADDRESSES));
    	cart.billing_addresses_ = new ArrayList<ShoppingCartActivity.JactAddress>(1);
    	return GetAdressInfo(billing_item, cart.billing_addresses_);
      } catch (JSONException ex) {
    	return PrintErrorAndAbort("Unable to parse as JSONObject " + BILLING_ADDRESSES + ": " + error_msg);
      }
    }
  }
  
  private static boolean GetAdressInfo(JSONObject item, ArrayList<ShoppingCartActivity.JactAddress> addresses) {
	String error_msg = "First failure";
	try {
  	  Iterator<String> items_itr = item.keys();
	  while (items_itr.hasNext()) {  	  
		String unused_keys = "";
	  	String unused_addr_keys = "";
		JSONObject addr_item = new JSONObject(item.getString(items_itr.next()));
		ShoppingCartActivity.JactAddress addr = new ShoppingCartActivity.JactAddress();
		Iterator<String> item_itr = addr_item.keys();
	    while (item_itr.hasNext()) {
	      String key = item_itr.next();
	      error_msg = key;
          if (key.equals(STATUS) && !addr_item.getString(key).equals("1")) {
            // Make sure status indicates this is a valid address.
            Log.e("ShoppingUtils::GetAdressInfo", "Skipping shipping item " +
                               "with status: " + addr_item.getString(STATUS));
            return false;
          } else if (key.equals(PROFILE_ID)) {
            // Get Shipping ID.
            error_msg = "Profile Id: " + addr_item.getString(key); 
        	addr.profile_id_ = addr_item.getInt(key);
          } else if (key.equals(ADDRESS)) {
            // Get Address Details.
        	error_msg = "Trying to parse first address: " + addr_item.getString(key);
            JSONObject addr_info = addr_item.getJSONObject(key);
            Iterator<String> addr_itr = addr_info.keys();
    	    while (addr_itr.hasNext()) {
    	      String addr_key = addr_itr.next();
    	      error_msg = "within address key: " + addr_key;
    	      if (addr_key.equals(COUNTRY)) {
                addr.country_ = addr_info.getString(addr_key);
    	      } else if (addr_key.equals(STATE)) {
                addr.state_ = addr_info.getString(addr_key);
    	      } else if (addr_key.equals(CITY)) {
                addr.city_ = addr_info.getString(addr_key);
              } else if (addr_key.equals(ZIP)) {
                addr.zip_ = addr_info.getString(addr_key);
              } else if (addr_key.equals(STREET)) {
                addr.street_addr_ = addr_info.getString(addr_key);
              } else if (addr_key.equals(APT)) {
                addr.street_addr_extra_ = addr_info.getString(addr_key);
              } else if (addr_key.equals(FIRST_NAME)) {
                addr.first_name_ = addr_info.getString(addr_key);
              } else if (addr_key.equals(LAST_NAME)) {
                addr.last_name_ = addr_info.getString(addr_key);
              } else {
                unused_addr_keys += addr_key + ", ";
              }
    	    }
          } else {
            unused_keys += key + ", ";
          }
	    }
        // Add address.
        addresses.add(addr);
	  }
    } catch (JSONException e) {
      return PrintErrorAndAbort("Unable to parse GetAddressInfo. Error Msg: " + error_msg);
    }
    return true;
  }
  
  private static boolean GetShippingInfo(JSONObject cart_item, ShoppingCartInfo cart) {
    String error_msg = "";
    try {
      String shipping_addresses = cart_item.getString(SHIPPING_ADDRESSES);
      if (shipping_addresses == null) return true;
      JSONArray shipping_items = new JSONArray(shipping_addresses);
      if (shipping_items == null || shipping_items.length() == 0) return true;
      cart.shipping_addresses_ = new ArrayList<ShoppingCartActivity.JactAddress>(shipping_items.length());
      
      // Loop through all shipping items.
      boolean to_return = true;
      for (int i = 0; i < shipping_items.length(); i++) {
        JSONObject item = shipping_items.getJSONObject(i);
        to_return &= GetAdressInfo(item, cart.shipping_addresses_);
      }
      return to_return;
    } catch (JSONException e) {
      try {
	    JSONObject shipping_item = new JSONObject(cart_item.getString(SHIPPING_ADDRESSES));
	    cart.shipping_addresses_ = new ArrayList<ShoppingCartActivity.JactAddress>(1);
	    return GetAdressInfo(shipping_item, cart.shipping_addresses_);
      } catch (JSONException ex) {
	    return PrintErrorAndAbort("Unable to parse as JSONObject " + SHIPPING_ADDRESSES + ": " + error_msg);
      }
    }
  }
  
  public static String PrintCostHumanReadable(ArrayList<Amount> cost) {
    if (cost == null) return "";
    String to_return = "";
    for (Amount a : cost) {
      String type = "";
      if (a.type_ == CurrencyCode.BUX) {
        type = "BUX";
      } else if (a.type_ == CurrencyCode.POINTS) {
        type = "POINTS";
      } else if (a.type_ == CurrencyCode.USD) {
        type = "USD";
      } else {
        Log.e("ShoppingUtils::ParseCost", "Unrecognized CurrencyCode: " + a.type_);
        return "";
      }
      to_return += " (" + type + ", " + Double.toString(a.price_) + ")";
    }
    return to_return;
  }
  
  public static String PrintCost(ArrayList<Amount> cost) {
    if (cost == null) return "";
    String to_return = "";
    for (Amount a : cost) {
      if (!to_return.isEmpty()) to_return += COST_ITEM_SEPARATOR;
      String type = "";
      if (a.type_ == CurrencyCode.BUX) {
        type = "BUX";
      } else if (a.type_ == CurrencyCode.POINTS) {
        type = "POINTS";
      } else if (a.type_ == CurrencyCode.USD) {
        type = "USD";
      } else {
        Log.e("ShoppingUtils::ParseCost", "Unrecognized CurrencyCode: " + a.type_);
        return "";
      }
      to_return += "price" + COST_VALUE_SEPARATOR + Double.toString(a.price_) +
    		       COST_FIELD_SEPARATOR + "type" + COST_VALUE_SEPARATOR + type;
    }
    return to_return;
  }
  
  public static ArrayList<Amount> ParseCost(String cost) {
    ArrayList<Amount> to_return = new ArrayList<Amount>();
    ArrayList<String> items = new ArrayList<String>(Arrays.asList(cost.split(COST_ITEM_SEPARATOR)));
    for (String item : items) {
      Amount amount = new Amount();
      ArrayList<String> fields = new ArrayList<String>(Arrays.asList(item.split(COST_FIELD_SEPARATOR)));
      for (String field : fields) {
        ArrayList<String> name_value = new ArrayList<String>(Arrays.asList(field.split(COST_VALUE_SEPARATOR)));
        if (name_value.size() != 2) {
          Log.e("ShoppingUtils::ParseCost", "Unexpected format for header. " +
                             "size=" + name_value.size() + ", field: " + field);
          return null;
        }
        String value = name_value.get(1);
        try {
          if (name_value.get(0).equals("price")) {
            amount.price_ = Double.parseDouble(value);
          } else if (name_value.get(0).equals("type")) {
            if (value.equals("BUX")) {
              amount.type_ = CurrencyCode.BUX;
            } else if (value.equals("POINTS")) {
              amount.type_ = CurrencyCode.POINTS;
            } else if (value.equals("USD")) {
              amount.type_ = CurrencyCode.USD;
            } else {
              Log.e("ShoppingUtils::ParseLineItem", "Unrecognized currency type: " + value);
      	      return null;
            }
          } else {
  	        Log.e("ShoppingUtils::ParseLineItem", "Unidentifiable cost field|" +
                               name_value.get(0) + "|value|" + value + "|");
  	        return null;
          }
        } catch (NumberFormatException e) {
  	      Log.e("ShoppingUtils::ParseLineItem", "Unable to parse one of the cost fields as a double: " +
                             field);
  	      return null;
        }
      }
      to_return.add(amount);
    }
    return to_return;
  }
  
  public static String PrintLineItemHumanReadable(LineItem item) {
    if (item == null) return "";
    String to_return = "id: " + Integer.toString(item.id_);
    to_return += ", order_id: " + Integer.toString(item.order_id_);
    to_return += ", quantity: " + Integer.toString(item.quantity_);
    to_return += ", pid: " + Integer.toString(item.pid_);
    to_return += ", entity_id: " + Integer.toString(item.entity_id_);
    String is_drawing = item.is_drawing_ ? "true" : "false";
    to_return += ", drawing: " + is_drawing;
    to_return += ", type: " + item.type_;
    to_return += ", label: " + item.label_;
    to_return += ", title: " + item.title_;
    to_return += ", cost: " + PrintCostHumanReadable(item.cost_);
    return to_return;
  }
  
  public static String PrintLineItem(LineItem item) {
	if (item == null) return "";
    String to_return = "id" + LINE_ITEM_VALUE_SEPARATOR + Integer.toString(item.id_);
    to_return += LINE_ITEM_FIELD_SEPARATOR + "order_id" + LINE_ITEM_VALUE_SEPARATOR +
    		     Integer.toString(item.order_id_);
    to_return += LINE_ITEM_FIELD_SEPARATOR + "quantity" + LINE_ITEM_VALUE_SEPARATOR +
		         Integer.toString(item.quantity_);
    to_return += LINE_ITEM_FIELD_SEPARATOR + "pid" + LINE_ITEM_VALUE_SEPARATOR +
		         Integer.toString(item.pid_);
    to_return += LINE_ITEM_FIELD_SEPARATOR + "entity_id" + LINE_ITEM_VALUE_SEPARATOR +
		         Integer.toString(item.entity_id_);
    String is_drawing = item.is_drawing_ ? "true" : "false";
    to_return += LINE_ITEM_FIELD_SEPARATOR + "drawing" + LINE_ITEM_VALUE_SEPARATOR + is_drawing;
    to_return += LINE_ITEM_FIELD_SEPARATOR + "type" + LINE_ITEM_VALUE_SEPARATOR + item.type_;
    to_return += LINE_ITEM_FIELD_SEPARATOR + "label" + LINE_ITEM_VALUE_SEPARATOR + item.label_;
    to_return += LINE_ITEM_FIELD_SEPARATOR + "title" + LINE_ITEM_VALUE_SEPARATOR + item.title_;
    to_return += LINE_ITEM_FIELD_SEPARATOR + "cost" + LINE_ITEM_VALUE_SEPARATOR + PrintCost(item.cost_);
    return to_return;
  }
  
  public static LineItem ParseLineItem(String item) {
    if (item.isEmpty()) return null;
    LineItem to_return = new LineItem();
    ArrayList<String> fields = new ArrayList<String>(Arrays.asList(item.split(LINE_ITEM_FIELD_SEPARATOR)));
    for (String field : fields) {
      ArrayList<String> name_value = new ArrayList<String>(Arrays.asList(field.split(LINE_ITEM_VALUE_SEPARATOR)));
      if (name_value.size() != 2) {
        Log.e("ShoppingUtils::ParseLineItem", "Unexpected format for header. " +
                           "size=" + name_value.size() + ", field: " + field);
        return null;
      }
      String value = name_value.get(1);
      try {
        if (name_value.get(0).equals("id")) {
          to_return.id_ = Integer.parseInt(value);
        } else if (name_value.get(0).equals("order_id")) {
          to_return.order_id_ = Integer.parseInt(value);
        } else if (name_value.get(0).equals("quantity")) {
          to_return.quantity_ = Integer.parseInt(value);
        } else if (name_value.get(0).equals("pid")) {
          to_return.pid_ = Integer.parseInt(value);
        } else if (name_value.get(0).equals("entity_id")) {
          to_return.entity_id_ = Integer.parseInt(value);
        } else if (name_value.get(0).equals("type")) {
          to_return.type_ = value;
        } else if (name_value.get(0).equals("label")) {
          to_return.label_ = value;
        } else if (name_value.get(0).equals("title")) {
          to_return.title_ = value;
        } else if (name_value.get(0).equals("drawing")) {
          to_return.is_drawing_ = value.equals("true");
        } else if (name_value.get(0).equals("cost")) {
          to_return.cost_ = ParseCost(value);
          // PHB TEMP.
          if (to_return.cost_ == null) { 
        	Log.e("ShoppingUtils::ParseLineItem", "ParseCost fn is returning null cost.");
          }
        } else {
    	  Log.e("ShoppingUtils::ParseLineItem", "Unidentifiable line item field|" +
                             name_value.get(0) + "|value|" + value + "|");
    	  return null;
        }
      } catch (NumberFormatException e) {
    	  Log.e("ShoppingUtils::ParseLineItem",
    			"Unable to parse one of the line item fields as an int: " + field);
    	  return null;
      }
    }
    return to_return;
  }
  
  // Looks to see if amount has multiple price types, and if so, returns the POINTS price;
  // otherwise, returns -1 (even if cost has a single Amount that has type POINTS).
  // The idea is that this method is used to set 'field_point_price' when updating a cart's
  // line-item, and this field need only be set if it is a multi-currency product.
  private static int GetExtraPointPrice(ArrayList<Amount> cost) {
    if (cost == null || cost.size() < 2) {
	  return -1;	  
	}
    for (Amount amount : cost) {
      if (amount.type_ == CurrencyCode.POINTS) {
        return Integer.valueOf(Double.valueOf(amount.price_).intValue());
      }
    }
	return -1; 
  }
  
  private static boolean PrintErrorAndAbort(String message) {
	Log.e("ShoppingUtils::ParseCartPage", message); 
	return false;  
  }
}