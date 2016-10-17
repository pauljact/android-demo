package com.jact.jactapp;

import java.net.HttpCookie;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.jact.jactapp.GetUrlTask.FetchStatus;
import com.jact.jactapp.ShoppingUtils.Amount;
import com.jact.jactapp.ShoppingUtils.LineItem;
import com.jact.jactapp.ShoppingUtils.ShoppingCartInfo;

public class ShoppingCartActivity extends JactActionBarActivity implements ProcessUrlResponseCallback {
	private static final int MAX_CART_ITEMS = 10;
	// TODO(PHB): Update string below with actual target.
	private static String rewards_url_;
	private static String jact_shopping_cart_url_;
	private static String mobile_cart_url_;
	private static final String GET_SHIPPING_INFO_TASK = "get_shipping_info_task";
	private static final String GET_REWARDS_PAGE_TASK = "get_rewards_page_task";
	private static final String DATE_PREFIX = "Drawing Date: ";
	private static final String CLEAR_CART_TAG = "clear_cart";
	private static final int MAX_FAILED_REQUESTS = 5;
	
	public static JactAddress shipping_address_;
	public static JactAddress billing_address_;
	
	private static ShoppingUtils.ShoppingCartInfo shopping_cart_;
	private static ShoppingUtils.ShoppingCartInfo quantity_positive_shopping_cart_;
	private static boolean view_has_been_loaded_;
	private static int num_csrf_requests_;
	private static int num_failed_requests_;
	private static int current_cart_icon_pos_;
	private static ArrayList<String> items_to_remove_;
	private ListView items_listview_;
	private CheckoutAdapter adapter_;

	private Tracker mTracker;  // For google analytics
	
	public enum CardType {
		NO_TYPE,
		VISA,
		MC,
		AMEX
	}
	
	public enum CartAccessType {
	  PRINT_CART,
	  GET_LINE_ITEM,
	  INITIALIZE_CART,
	  ITEM_TO_ADD_STATUS,
	  ENFORCE_CART_RULES,
	  GET_NUM_CART_ITEMS,
	  GET_NUM_DISTINCT_CART_ITEMS,
	  GET_TOTAL_CART_PRICE,
	  GET_QUANTITY_POSITIVE_ITEMS,
	  GET_ORDER_ID,
	  ADD_INFO_FROM_REWARDS,
	  SET_CART_FROM_WEBPAGE,
	  UPDATE_LINE_ITEM,
	  UPDATE_CART,
	  REMOVE_QP_CART_ITEM,
	  GET_PRODUCT_QUANTITY,
	}
	
	public static class CartAccessResponse {
		ItemToAddStatus to_add_status_;
		ItemStatus item_status_;
		String printed_cart_;
		int order_id_;
		int num_cart_items_;
		int num_distinct_cart_items_;
		TotalCartPrice total_cart_price_;
		LineItem line_item_;
		ShoppingUtils.ShoppingCartInfo cart_;
		ArrayList<LineItem> line_items_;
	}
	
	public static class JactAddress {
		public String first_name_;
		public String last_name_;
		public String street_addr_;
		public String street_addr_extra_;
		public String city_;
		public String state_;
		public String zip_;
		public String country_;
		public CardType cc_type_;
		public String cc_number_;
		public int profile_id_;
		public int cc_exp_date_month_;
		public int cc_exp_date_year_;
		public int cc_crv_;
		
		public JactAddress() {
		  cc_crv_ = 0;
		  cc_exp_date_month_ = 0;
		  cc_exp_date_year_ = 0;
		  cc_type_ = CardType.NO_TYPE;
		}
		
		public String Print() {
			return "first_name: " + this.first_name_ + ", last_name: " + this.last_name_ +
					", street: " + this.street_addr_ + ", street extra: " + this.street_addr_extra_ +
					", city: " + this.city_ + ", state: " + this.state_ + ", zip: " + this.zip_ +
					", card type: " + this.cc_type_.toString() + ", number: " + this.cc_number_ +
					", exp month: " + this.cc_exp_date_month_ + ", exp year: " + this.cc_exp_date_year_ +
					", crv: " + this.cc_crv_;
		}
	}
	
	public static class TotalCartPrice {
		int bux_;
		int points_;
		int usd_;
		int bux_purchased_;
		
		public TotalCartPrice() {
			bux_ = 0;
			points_ = 0;
			usd_ = 0;
			bux_purchased_ = 0;
		}
	}
	
	public static class JactUserCompleteInfo {
		int bux_;
		int points_;
		ArrayList<JactAddress> shipping_addresses_;
		ArrayList<JactAddress> billing_addresses_;
		
		public JactUserCompleteInfo() {
			bux_ = 0;
			points_ = 0;
		}
	}
	
	public enum ItemStatus {
		VALID,
		NO_TITLE,
		NO_PID,
		NO_PRICE,
		INVALID_PRICE
	}
	
	public enum ItemToAddStatus {
		NEW,
		INCREMENTED,
		OK,
		CART_FULL,
		ITEM_MAX,
		NO_PID,
		CART_NOT_READY,
		REWARDS_NOT_FETCHED,
		INCOMPATIBLE_TYPE,
		MAX_QUANTITY_EXCEEDED,
		EXPIRED_DATE,
		NO_DATE
	}
	
	private static final Map<Integer, Integer> cart_icon_map_;
    static {
        Map<Integer, Integer> temp = new HashMap<Integer, Integer>();
        temp.put(0, R.drawable.cart_transparent);
        temp.put(1, R.drawable.cart_one);
        temp.put(2, R.drawable.cart_two);
        temp.put(3, R.drawable.cart_three);
        temp.put(4, R.drawable.cart_four);
        temp.put(5, R.drawable.cart_five);
        temp.put(6, R.drawable.cart_five_plus_yellow);
        cart_icon_map_ = Collections.unmodifiableMap(temp);
    }
    
  // A master accessor/modifier function that will serve as the entry point to
  // all shopping_cart_ functions. This way, access to the cart is controlled
  // via a single synchronized method, preventing various multi-threading/synchronization
  // issues.
  public static synchronized boolean AccessCart(
	  CartAccessType type, int pid, int quantity, String product_type, String webpage,
	  LineItem line_item, ShoppingUtils.ShoppingCartInfo cart,
	  CartAccessResponse response) {
	if (!JactActionBarActivity.IS_PRODUCTION) Log.i("ShoppingCartActivity::AccessCart", "Type: " + type);
	if (type == CartAccessType.INITIALIZE_CART) {
	  boolean to_return = InitializeOnce(); 
	  return to_return;
	} else if (type == CartAccessType.ITEM_TO_ADD_STATUS) {
	  response.to_add_status_ = GetCartItemToAddStatus(line_item); 
	  return true;
	} else if (type == CartAccessType.ENFORCE_CART_RULES) {
      response.to_add_status_ = EnforceCartRules(pid, quantity, product_type); 
	  return true;
	} else if (type == CartAccessType.PRINT_CART) {
	  response.printed_cart_ = PrintCart(); 
	  return true;
	} else if (type == CartAccessType.GET_PRODUCT_QUANTITY) {
	  response.num_cart_items_ = GetProductQuantity(pid);
      return true;
	} else if (type == CartAccessType.GET_NUM_DISTINCT_CART_ITEMS) {
	  response.num_distinct_cart_items_ = GetNumberDistinctCartItems();
	  return true;
	} else if (type == CartAccessType.GET_NUM_CART_ITEMS) {
	  response.num_cart_items_ = GetTotalCartQuantity();
	  return true;
	} else if (type == CartAccessType.GET_TOTAL_CART_PRICE) {
	  if (response == null) {
		return false;
	  }
	  GetTotalCartPrice(response.total_cart_price_);
	  return true;
	} else if (type == CartAccessType.GET_LINE_ITEM) {
	  response.line_item_ = GetCartItem(pid);
	  return true;
	} else if (type == CartAccessType.GET_ORDER_ID) {
	  if (shopping_cart_ == null) return false;
	  response.order_id_ = shopping_cart_.order_id_;
	  return true;
	} else if (type == CartAccessType.ADD_INFO_FROM_REWARDS) {
	  FillExtraProductDetailsFromRewardsPage();
	  return true;
	} else if (type == CartAccessType.SET_CART_FROM_WEBPAGE) {
	  boolean to_return = SetShoppingCartFromGetCartStatic(webpage);
	  return to_return;
	} else if (type == CartAccessType.UPDATE_LINE_ITEM) {
	  boolean to_return = UpdateLineItemStatic(line_item);
	  return to_return;
	} else if (type == CartAccessType.UPDATE_CART) {
	  boolean to_return = UpdateServerShoppingCart(cart, response);
	  return to_return;
	} else if (type == CartAccessType.REMOVE_QP_CART_ITEM) {
	  RemoveQuantityPositiveCartItem(pid);
	  return true;
    } else if (type == CartAccessType.GET_QUANTITY_POSITIVE_ITEMS) {
      boolean to_return = GetQuantityPositiveCartItems(response);
	  return to_return;
	} else {
	  if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ShoppingCartActivity::AccessCart", "Unexpected cart action: " + type);
	  return false;
	}
  }
  
  // Various API for AccessCart.
  public static synchronized boolean AccessCart(
		  CartAccessType type, int pid, int quantity, String product_type, LineItem line_item,
		  CartAccessResponse response) {
    return AccessCart(type, pid, quantity, product_type, "", line_item, null, response);
  }
  public static synchronized boolean AccessCart(
		  CartAccessType type, int pid, int quantity, String product_type,
		  CartAccessResponse response) {
    return AccessCart(type, pid, quantity, product_type, null, response);
  }
  public static synchronized boolean AccessCart(
		  CartAccessType type, LineItem line_item,
		  CartAccessResponse response) {
    return AccessCart(type, -1, -1, "", line_item, response);
  }
  public static synchronized boolean AccessCart(CartAccessType type, CartAccessResponse response) {
    return AccessCart(type, -1, -1, "", null, response);
  }
  public static synchronized boolean AccessCart(CartAccessType type, String webpage) {
    return AccessCart(type, -1, -1, "", webpage, null, null, null);
  }
  public static synchronized boolean AccessCart(CartAccessType type, int pid) {
    return AccessCart(type, pid, -1, "", "", null, null, null);
  }
  public static synchronized boolean AccessCart(CartAccessType type, int pid, CartAccessResponse response) {
    return AccessCart(type, pid, -1, "", "", null, null, response);
  }
  public static synchronized boolean AccessCart(CartAccessType type, LineItem item) {
    return AccessCart(type, -1, -1, "", item, null);
  }
  public static synchronized boolean AccessCart(
	  CartAccessType type, ShoppingUtils.ShoppingCartInfo cart, CartAccessResponse response) {
    return AccessCart(type, -1, -1, "", "", null, cart, response);
  }
  public static synchronized boolean AccessCart(CartAccessType type) {
    return AccessCart(type, -1, -1, "", null, null);
  }
  
  public synchronized void GetInitialShoppingCart() {
	// Need cookies and csrf_token to update server's cart.
	SharedPreferences user_info = getSharedPreferences(
        getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
    String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
    if (cookies.isEmpty()) {
      String username = user_info.getString(getString(R.string.ui_username), "");
      String password = user_info.getString(getString(R.string.ui_password), "");
      ShoppingUtils.RefreshCookies(
          this, username, password, ShoppingUtils.GET_COOKIES_THEN_GET_CART_TASK);
      return;
    }

    IncrementNumRequestsCounter();
    GetUrlTask task = new GetUrlTask(this, GetUrlTask.TargetType.JSON);
	GetUrlTask.UrlParams params = new GetUrlTask.UrlParams();
	params.url_ = jact_shopping_cart_url_;
	params.connection_type_ = "GET";
	params.extra_info_ = ShoppingUtils.GET_CART_TASK;
	params.cookies_ = cookies;
	task.execute(params);
  }
  
  private static synchronized boolean InitializeOnce() {
	boolean shopping_cart_was_null = (shopping_cart_ == null);
    if (shopping_cart_ == null) {
	  shopping_cart_ = new ShoppingUtils.ShoppingCartInfo();
	  shopping_cart_.line_items_ = new ArrayList<ShoppingUtils.LineItem>();
	  quantity_positive_shopping_cart_ = new ShoppingUtils.ShoppingCartInfo();
	  quantity_positive_shopping_cart_.line_items_ = new ArrayList<ShoppingUtils.LineItem>();
	}
	if (items_to_remove_ == null) {
	  items_to_remove_ = new ArrayList<String>();
  	}
	return shopping_cart_was_null;
  }
  
  private synchronized void LoadView() {
	if (view_has_been_loaded_) return;
	view_has_been_loaded_ = true;
    if (!IsCartEmpty() && SetCartItemsUI()) {
      adapter_.notifyDataSetChanged();
    }
    fadeAllViews(false);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
	  if (JactActionBarActivity.USE_MOBILE_SITE) {
		  // Set layout.
		  super.onCreate(savedInstanceState, R.string.cart_activity_label,
				  R.layout.checkout_wrapper_layout,
				  JactNavigationDrawer.ActivityIndex.CHECKOUT_MAIN);
	  } else {
		  // Set layout.
		  super.onCreate(savedInstanceState, R.string.cart_activity_label,
				  R.layout.checkout_layout,
				  JactNavigationDrawer.ActivityIndex.CHECKOUT_MAIN);
	  }
	  rewards_url_ = GetUrlTask.GetJactDomain() + "/rest/rewards.json";
	  jact_shopping_cart_url_ = GetUrlTask.GetJactDomain() + "/rest/cart.json";
	  mobile_cart_url_ = GetUrlTask.GetJactDomain() + "/cart";

	  // For Google Analytics tracking.
	  // Obtain the shared Tracker instance.
	  JactAnalyticsApplication application = (JactAnalyticsApplication) getApplication();
	  mTracker = application.getDefaultTracker();
  }

  @Override
  protected void onResume() {
	  super.onResume();
	  rewards_url_ = GetUrlTask.GetJactDomain() + "/rest/rewards.json";
	  jact_shopping_cart_url_ = GetUrlTask.GetJactDomain() + "/rest/cart.json";
	  mobile_cart_url_ = GetUrlTask.GetJactDomain() + "/cart";

	  // For Google Analytics.
	  mTracker.setScreenName("Image~Shopping_Cart");
	  mTracker.send(new HitBuilders.ScreenViewBuilder().build());

	  //PHBnavigation_drawer_.setActivityIndex(JactNavigationDrawer.ActivityIndex.CHECKOUT_MAIN);
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
					  mobile_cart_url_,
					  cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain());
		  }

		  // Set webview from mobile_cart_url_.
		  WebView web_view = (WebView) findViewById(R.id.checkout_wrapper_webview);
		  web_view.loadUrl(mobile_cart_url_);
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
		  num_failed_requests_ = 0;
		  if (InitializeOnce()) {
			  GetInitialShoppingCart();
		  }
		  // Re-enable parent activity before transitioning to the next activity.
		  // This ensures e.g. that when user hits 'back' button, the screen
		  // is 'active' (not faded) when the user returns.
		  view_has_been_loaded_ = false;
		  ResetNumCsrfRequests();
		  GetCart(this);
		  if (!ProductsActivity.IsProductsListInitialized()) {
			  new GetUrlTask(this, GetUrlTask.TargetType.JSON).execute(
					  rewards_url_, "GET", "", "", GET_REWARDS_PAGE_TASK);
			  IncrementNumRequestsCounter();
		  }
		  if (GetNumRequestsCounter() == 0) {
			  LoadView();
		  } else {
			  fadeAllViews(true);
		  }
	  }
  }
  
  @Override
  public void onPause() {
	if (!JactActionBarActivity.USE_MOBILE_SITE) {
		if (items_to_remove_ != null) {
			Iterator<String> itr = items_to_remove_.iterator();
			while (itr.hasNext()) {
				String pid_str = itr.next();
				try {
					int pid = Integer.parseInt(pid_str);
					AccessCart(CartAccessType.REMOVE_QP_CART_ITEM, pid);
				} catch (NumberFormatException e) {
					if (!JactActionBarActivity.IS_PRODUCTION)
						Log.e("ShoppingCartActivity::onPause", "Unable to parse as pid: " + pid_str);
				}
			}
		}
	}
    super.onPause();
  }

  @Override
  public void fadeAllViews(boolean should_fade) {
	if (JactActionBarActivity.USE_MOBILE_SITE) {
		ProgressBar spinner = (ProgressBar) findViewById(R.id.checkout_wrapper_progress_bar);
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
		RelativeLayout layout = (RelativeLayout) findViewById(R.id.checkout_wrapper_content_frame);
		layout.startAnimation(alpha); // Add animation to the layout.
	} else {
		ProgressBar spinner = (ProgressBar) findViewById(R.id.checkout_progress_bar);
		Button proceed_button = (Button) findViewById(R.id.proceed_to_checkout_button);
		Button clear_button = (Button) findViewById(R.id.clear_cart_button);
		AlphaAnimation alpha;
		if (should_fade) {
			proceed_button.setEnabled(false);
			proceed_button.setTextColor(getResources().getColor(R.color.translucent_black));
			clear_button.setEnabled(false);
			clear_button.setTextColor(getResources().getColor(R.color.translucent_black));
			spinner.setVisibility(View.VISIBLE);
			alpha = new AlphaAnimation(0.5F, 0.5F);
		} else {
			spinner.setVisibility(View.GONE);
			alpha = new AlphaAnimation(1.0F, 1.0F);
			if (GetNumberCartItems() == 0) {
				proceed_button.setEnabled(false);
				proceed_button.setTextColor(getResources().getColor(R.color.translucent_black));
				clear_button.setEnabled(false);
				clear_button.setTextColor(getResources().getColor(R.color.translucent_black));
			} else {
				proceed_button.setEnabled(true);
				proceed_button.setTextColor(getResources().getColor(R.color.white));
				clear_button.setEnabled(true);
				clear_button.setTextColor(getResources().getColor(R.color.white));
			}
		}
		// The AlphaAnimation will make the whole content frame transparent
		// (so that none of the views show).
		alpha.setDuration(0); // Make animation instant
		alpha.setFillAfter(true); // Tell it to persist after the animation ends
		RelativeLayout layout = (RelativeLayout) findViewById(R.id.checkout_content_frame);
		layout.startAnimation(alpha); // Add animation to the layout.
	}
  }
  
  private boolean IsCartEmpty() {
    if (GetNumberCartItems() == 0) {
      TextView empty_msg = (TextView) findViewById(R.id.checkout_no_items_tv);
      empty_msg.setVisibility(View.VISIBLE);
      Button proceed_button = (Button) findViewById(R.id.proceed_to_checkout_button);
      proceed_button.setEnabled(false);
      proceed_button.setTextColor(getResources().getColor(R.color.translucent_black));
      Button clear_button = (Button) findViewById(R.id.clear_cart_button);
      clear_button.setEnabled(false);
      clear_button.setTextColor(getResources().getColor(R.color.translucent_black));
      return true;
    } else {
      TextView empty_msg = (TextView) findViewById(R.id.checkout_no_items_tv);
      empty_msg.setVisibility(View.GONE);
      Button proceed_button = (Button) findViewById(R.id.proceed_to_checkout_button);
      proceed_button.setEnabled(true);
      proceed_button.setTextColor(getResources().getColor(R.color.white));
      Button clear_button = (Button) findViewById(R.id.clear_cart_button);
      clear_button.setEnabled(true);
      clear_button.setTextColor(getResources().getColor(R.color.white));
      return false;
    }
  }
  
  public void EnableProceedToCheckoutButton() {
    Button proceed_button = (Button) findViewById(R.id.proceed_to_checkout_button);
    proceed_button.setEnabled(true);
    proceed_button.setTextColor(getResources().getColor(R.color.white));
    Button clear_button = (Button) findViewById(R.id.clear_cart_button);
    clear_button.setEnabled(true);
    clear_button.setTextColor(getResources().getColor(R.color.white));
  }
  
  public void DisableProceedToCheckoutButton() {
    Button proceed_button = (Button) findViewById(R.id.proceed_to_checkout_button);
    proceed_button.setEnabled(false);
    proceed_button.setTextColor(getResources().getColor(R.color.translucent_black));
    Button clear_button = (Button) findViewById(R.id.clear_cart_button);
    clear_button.setEnabled(false);
    clear_button.setTextColor(getResources().getColor(R.color.translucent_black));
  }
  
  public static synchronized void IncrementNumCsrfRequests() {
	num_csrf_requests_++;
  }
  
  public static synchronized void ResetNumCsrfRequests() {
	num_csrf_requests_ = 0;
  }
  
  public static synchronized int GetNumCsrfRequests() {
	return num_csrf_requests_;
  }
  
  private static synchronized String PrintCart() {
    if (shopping_cart_ == null) return "";
	return shopping_cart_.toString();
  }
  
  private static synchronized int GetProductQuantity(int product_id) {
	if (shopping_cart_ == null || shopping_cart_.line_items_ == null) return -1;
  	Iterator<ShoppingUtils.LineItem> itr = shopping_cart_.line_items_.iterator();
    while (itr.hasNext()) {
	  ShoppingUtils.LineItem item = itr.next();
	  if (item.pid_ == product_id) return item.quantity_;
    }
	return -1;
  }
  
  private static synchronized int GetNumberCartItems() {
    CartAccessResponse response = new CartAccessResponse();
    if (!AccessCart(CartAccessType.GET_NUM_DISTINCT_CART_ITEMS, response)) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "ShoppingCartActivity::GetNumberCartItems. Failed Cart Access.");
      return 0;
    }
    return response.num_distinct_cart_items_;
  }
	
  private static synchronized int GetNumberDistinctCartItems() {
	if (shopping_cart_ == null || shopping_cart_.line_items_ == null) return 0;
	int num_items = 0;
  	Iterator<ShoppingUtils.LineItem> itr = shopping_cart_.line_items_.iterator();
    while (itr.hasNext()) {
	  ShoppingUtils.LineItem item = itr.next();
	  if (item.quantity_ != 0) ++num_items;
    }
	return num_items;
  }
  
  private static synchronized void GetTotalCartPrice(TotalCartPrice total) {
	if (shopping_cart_ == null || shopping_cart_.line_items_ == null) return;
	int bux = 0;
	int points = 0;
	int usd = 0;
  	Iterator<ShoppingUtils.LineItem> itr = shopping_cart_.line_items_.iterator();
    while (itr.hasNext()) {
	  ShoppingUtils.LineItem item = itr.next();
	  if (item.quantity_ == 0) continue;
	  Iterator<Amount> cost_itr = item.cost_.iterator();
	  while (cost_itr.hasNext()) {
	    Amount amount = cost_itr.next();
	    if (amount.type_ == ShoppingUtils.CurrencyCode.BUX) {
		  bux += item.quantity_ * Double.valueOf(amount.price_).intValue();
	      //OLD bux += item.quantity_ * NumberFormat.getNumberInstance(java.util.Locale.US).parse(amount.price_).intValue();
	    }
	    if (amount.type_ == ShoppingUtils.CurrencyCode.POINTS) {
	      points += item.quantity_ * Double.valueOf(amount.price_).intValue();
	    }
	    if (amount.type_ == ShoppingUtils.CurrencyCode.USD) {
	      usd += item.quantity_ * Double.valueOf(amount.price_).intValue();
	    }
	  }
    }
    total.bux_ = bux;
    total.points_ = points;
    total.usd_ = usd;
    // BUX are no longer in existence. If they come back, we'll need to re-implement code
    // that populates the amount of BUX being purchased in the Cart.
    // total.bux_purchased_ = bux_purchased;
  }

  private static synchronized int GetTotalCartQuantity() {
    if (shopping_cart_ == null || shopping_cart_.line_items_ == null) return 0;
    int count = 0;
    Iterator<ShoppingUtils.LineItem> itr = shopping_cart_.line_items_.iterator();
    while (itr.hasNext()) {
      ShoppingUtils.LineItem item = itr.next();
	  count += item.quantity_;
    }
    return count;
  }
  
  private boolean SetCartItemsUI() {
	items_listview_ = (ListView) findViewById(R.id.checkout_list);

    // Getting adapter by passing xml data ArrayList
	CartAccessResponse response = new CartAccessResponse();
	response.line_items_ = new ArrayList<ShoppingUtils.LineItem>();
	if (!AccessCart(CartAccessType.GET_QUANTITY_POSITIVE_ITEMS, response)) {
	  if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "ShoppingCartActivity::SetCartItemsUI. Unable to get QP cart items.");
	  return false;
	}
	if (response.line_items_ == null || response.line_items_.size() == 0) {
	  // No items to display.
	  return false;
	}
    adapter_ = new CheckoutAdapter(this, R.layout.checkout_item, response.line_items_);
    items_listview_.setAdapter(adapter_);
    return true;
  }
  
  private static boolean GetQuantityPositiveCartItems(CartAccessResponse response) {
	if (response == null || response.line_items_ == null) return false;
	response.line_items_ = (ArrayList<LineItem>) quantity_positive_shopping_cart_.line_items_.clone();
	return true;
  }
  
  public static int GetCartIconResource(int pos) {
	  int pos_to_check = pos;
	  if (pos >= cart_icon_map_.size()) {
	    pos_to_check = cart_icon_map_.size() - 1;
	  }
	  if (cart_icon_map_.containsKey(pos_to_check)) {
	    return cart_icon_map_.get(pos_to_check);
	  } else {
	    if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB Error", "ShoppingCartActivity::GetCartIconResource. Unable to get icon for position: " + pos);
	  }
	  return -1;
  }
  
  public synchronized int GetCartIconPos() {
    return current_cart_icon_pos_;
  }
  
  public static synchronized void SetCartIcon(Menu menu_bar) {
    if (menu_bar == null) return;
    int num_items = GetNumberCartItems();
    if (num_items >= 0) {
      int resource_id = GetCartIconResource(num_items);
      if (resource_id != -1) {
        menu_bar.findItem(R.id.menu_shopping_cart).setIcon(resource_id);
        current_cart_icon_pos_ = num_items;
      }
    }
  }
  
  public static synchronized void SetCartIcon(Menu menu_bar, int num_items) {
    if (num_items >= 0) {
      int resource_id = GetCartIconResource(num_items);
      if (resource_id != -1) {
        menu_bar.findItem(R.id.menu_shopping_cart).setIcon(resource_id);
        current_cart_icon_pos_ = num_items;
      }
    } 
  }
  
  private static synchronized ShoppingUtils.LineItem GetCartItem(int product_id) {
    if (shopping_cart_ == null || shopping_cart_.line_items_ == null) {
	  return null;
	}
    Iterator<ShoppingUtils.LineItem> itr = shopping_cart_.line_items_.iterator();
    while (itr.hasNext()) {
      ShoppingUtils.LineItem item = itr.next();
	  if (item.pid_ == product_id) {
	    return item;
	  }
    }
    return null;
  }
  
    // In some places (e.g. on Cart checkout page), it may be desirable
    // to mark an item for removal from the cart, but not actually remove
    // it (yet). For example, setting an item's quantity to zero on Cart page.
    // This method marks an item for removal; items marked for removal will
    // be removed from the Cart when ShoppingCartActivity::onPause() is called
    // (e.g. when ShoppingCartActivity loses focus).
    public static synchronized void AddItemToRemove(String product_id) {
      if (items_to_remove_ == null) items_to_remove_ = new ArrayList<String>();
      if (!items_to_remove_.contains(product_id)) {
    	items_to_remove_.add(product_id);
      }
    }
    
    public static synchronized void RemoveItemFromToRemoveList(String product_id) {
      if (items_to_remove_ == null || items_to_remove_.isEmpty()) return;
      Iterator<String> itr = items_to_remove_.iterator();
      while(itr.hasNext()) {
        String item = itr.next();
        if (item.equals(product_id)) {
          itr.remove();
          return;
        }
      }
    }
    
    private synchronized static void RemoveQuantityZeroItems() {
      if (quantity_positive_shopping_cart_ == null ||
    	  quantity_positive_shopping_cart_.line_items_ == null) return;
      Iterator<ShoppingUtils.LineItem> qp_itr = quantity_positive_shopping_cart_.line_items_.iterator();
      while (qp_itr.hasNext()) {
    	ShoppingUtils.LineItem item = qp_itr.next(); 
    	if (item.quantity_ == 0) {
    	  qp_itr.remove();
    	}
      }
    }
    
    public synchronized static boolean RemoveQuantityPositiveCartItem(int product_id) {
      if (quantity_positive_shopping_cart_ == null ||
    	  quantity_positive_shopping_cart_.line_items_ == null) {
        return false;
      }
      Iterator<ShoppingUtils.LineItem> qp_itr = quantity_positive_shopping_cart_.line_items_.iterator();
      while (qp_itr.hasNext()) {
  	    ShoppingUtils.LineItem item = qp_itr.next(); 
  	    if (item.pid_ == product_id) {
  	      qp_itr.remove();
  	      return true;
  	    }
      }
  	  return false;
    }
    
    private synchronized void ClearCart() {
  	  for (ShoppingUtils.LineItem item : shopping_cart_.line_items_) {
  		item.quantity_ = 0;
		AddItemToRemove(Integer.toString(item.pid_));
  	  }
      TextView empty_msg = (TextView) findViewById(R.id.checkout_no_items_tv);
      empty_msg.setVisibility(View.VISIBLE);
      if (items_listview_ == null) {
  	    items_listview_ = (ListView) findViewById(R.id.checkout_list);
      }
  	  items_listview_.setVisibility(View.GONE);
      
	  SetCartIcon(menu_bar_, 0);
	  DisableProceedToCheckoutButton();
	  
      // Need cookies and csrf_token to update server's cart.
	  SharedPreferences user_info = getSharedPreferences(
          getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
      String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
      if (cookies.isEmpty()) {
    	String username = user_info.getString(getString(R.string.ui_username), "");
    	String password = user_info.getString(getString(R.string.ui_password), "");
        ShoppingUtils.RefreshCookies(
            this, username, password, ShoppingUtils.GET_COOKIES_THEN_CLEAR_CART_TASK);
        return;
      }
      String csrf_token = user_info.getString(getString(R.string.ui_csrf_token), "");
      if (csrf_token.isEmpty()) {
        if (!ShoppingUtils.GetCsrfToken(this, cookies, ShoppingUtils.GET_CSRF_THEN_CLEAR_CART_TASK)) {
          // Nothing to do.
        }
        return;
      }
      
      // Update server's cart.
      for (ShoppingUtils.LineItem item : shopping_cart_.line_items_) {
    	ShoppingUtils.UpdateLineItem(this, cookies, csrf_token, item);
      }
    }
	
	private static synchronized void FillExtraProductDetailsFromRewardsPage() {
		FillExtraProductDetailsFromRewardsPage(shopping_cart_);
		FillExtraProductDetailsFromRewardsPage(quantity_positive_shopping_cart_);
	}
	
	private static synchronized void FillExtraProductDetailsFromRewardsPage(ShoppingUtils.LineItem item) {
		FillExtraProductDetailsFromRewardsPage(shopping_cart_, item);
		FillExtraProductDetailsFromRewardsPage(quantity_positive_shopping_cart_, item);
	}
	
	private static synchronized void FillExtraProductDetailsFromRewardsPage(ShoppingUtils.ShoppingCartInfo cart_info) {
	  if (cart_info == null || cart_info.line_items_ == null) return;
	  for (ShoppingUtils.LineItem item : cart_info.line_items_) {
		ProductsActivity.FillItemDetails(item);
	  }
	}
	
	private static synchronized void FillExtraProductDetailsFromRewardsPage(
	    ShoppingUtils.ShoppingCartInfo cart_info, ShoppingUtils.LineItem item) {
	  if (cart_info == null || cart_info.line_items_ == null || item == null ) return;
	  for (ShoppingUtils.LineItem cart_item : cart_info.line_items_) {
		if (cart_item.pid_ == item.pid_) {
		  ProductsActivity.FillItemDetails(cart_item);
		  break;
		}
	  }
	}
	
	// Adds a item to the cart (or updates the item, in case it's already present in the cart).
	private static synchronized ItemToAddStatus GetCartItemToAddStatus(ShoppingUtils.LineItem item) {
		if (item == null) {
			if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "ShoppingCartActivity::ItemToAddStatus. 1");
			return ItemToAddStatus.NO_PID;
		}
		if (item.pid_ == 0) {
			if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "ShoppingCartActivity::ItemToAddStatus. 2");
			return ItemToAddStatus.NO_PID;
		}
		int max_quantity = ProductsActivity.GetMaxQuantity(item.pid_);
		if (max_quantity != -2 && max_quantity > 0 && max_quantity < item.quantity_) {
	      return ItemToAddStatus.MAX_QUANTITY_EXCEEDED;
	    }
		if (GetNumberCartItems() >= MAX_CART_ITEMS) {
			return ItemToAddStatus.CART_FULL;
		}
		if (item.is_drawing_) {
		  // Check that item has drawing date, and that this date is in future.
		  if (item.drawing_date_ == null) {
		    return ItemToAddStatus.NO_DATE;
		  } else if (!IsFutureDate(item.drawing_date_)) {
		    return ItemToAddStatus.EXPIRED_DATE;
		  }
		}
		// If item already exists in cart, increment it. Otherwise, enforce remaining rules.
		ShoppingUtils.LineItem temp_item = GetCartItem(item.pid_);
		if (temp_item != null) {
		  return ItemToAddStatus.INCREMENTED;
		}
		return EnforceCartRules(item.pid_, item.quantity_, item.type_);
	}
	
	private static boolean IsFutureDate(String date_str) {
	  if (date_str == null) return false;
	  DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
	  int prefix = date_str.indexOf(DATE_PREFIX);
	  String date_to_parse = date_str;
	  if (prefix >= 0) {
		date_to_parse = date_str.substring(prefix + DATE_PREFIX.length());
	  }
	  try {
		Date drawing_date = format.parse(date_to_parse);
		DateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
		Calendar cal = Calendar.getInstance();
		Date current_date = date_format.parse(date_format.format(cal.getTime()));
		return drawing_date.after(current_date);
	  } catch (ParseException e) {
		if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ShoppingCartActivity::IsFutureDate",
			  "ParseException for date_to_parse " + date_to_parse + ": " + e.getMessage());
		return false;
	  }
	}
	  
	private static synchronized ItemToAddStatus EnforceCartRules(int pid, int new_quantity, String type) {
	  if (shopping_cart_ == null) return ItemToAddStatus.CART_NOT_READY;
	  int max_quantity = ProductsActivity.GetMaxQuantity(pid);
	  if (max_quantity == -2) {
	    return ItemToAddStatus.REWARDS_NOT_FETCHED;
	  } else if (max_quantity > 0 && max_quantity < new_quantity) {
	    return ItemToAddStatus.MAX_QUANTITY_EXCEEDED;
	  }
	  if (shopping_cart_.line_items_ != null) {
	    for (ShoppingUtils.LineItem item : shopping_cart_.line_items_) {
	      if (item.quantity_ > 0 && item.type_ != type) {
	        return ItemToAddStatus.INCOMPATIBLE_TYPE;
	      }
	    }
	  }
	  return ItemToAddStatus.OK;
	}
	  
	public static JactAddress GetShippingAddress() {
		return shipping_address_;
	}
	
	public static void SetShippingAddress(JactAddress address) {
		shipping_address_ = address;
	}
	
	public static JactAddress GetBillingAddress() {
		return billing_address_;
	}
	
	public static void SetBillingAddress(JactAddress address) {
		billing_address_ = address;
	}
	
	public void DisplayPopupTitle(String title) {
	  DisplayPopupFragment(title, title);
	}
	
	public void DisplayPopup(String title, String message) {
	  DisplayPopupFragment(title, message, title);
	}

	public void onQuantityClick(View view) {
		adapter_.onQuantityClick(view);
	}

	public void doClearCartButtonClick(View view) {
	  Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	  vibe.vibrate(JactConstants.VIBRATION_DURATION);
	  if (can_show_dialog_) {
	    dialog_ = new JactDialogFragment();
		dialog_.SetTitle("Are You Sure You Want to Remove All Items From Your Cart?");
	    dialog_.SetButtonOneText("Cancel");
	    dialog_.SetButtonTwoText("OK");
	    dialog_.show(getSupportFragmentManager(), CLEAR_CART_TAG);
	  }
	}
	
	public void doProceedToShippingButtonClick(View view) {
		Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vibe.vibrate(JactConstants.VIBRATION_DURATION);
	    CartAccessResponse response = new CartAccessResponse();
	    if (!AccessCart(CartAccessType.GET_NUM_CART_ITEMS, response)) {
	      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "ShoppingCartActivity::doProceedToShippingButtonClick. Failed Cart Access.");
	    } else if (response.num_cart_items_ == 0) {
	      DisplayPopupFragment("No Items in Cart.", "No_items_in_cart");
		  return;
	    }
	    
	    CartAccessResponse response_two = new CartAccessResponse();
		if (!AccessCart(CartAccessType.GET_ORDER_ID, response_two)) {
		  if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "ShoppingCartActivity::doProceedToShippingButtonClick. " +
		                     "Unable to get order id.");
		  return;
		}
	    if (response_two.order_id_ == 0) {
	      DisplayPopupFragment("Order Id not found.", "No_order_id");
		  return;
		}
		fadeAllViews(true);
		CheckoutActivity.SetOrderId(response_two.order_id_);
		startActivity(new Intent(this, CheckoutActivity.class));

		// PHB Old: The following code will execute the checkout process within the App
		// (i.e. it won't use a WebView of the Mobile website).
		//fadeAllViews(true);
		//new GetUrlTask(this, GetUrlTask.TargetType.JSON).execute(
		//		jact_shipping_info_url_, "GET", "", "", GET_SHIPPING_INFO_TASK);
	}
	
	public void doDialogCancelClick(View view) {
	  super.doDialogCancelClick(view);
	}
	
	public void doDialogOkClick(View view) {
	  if (dialog_ == null) return;
	  // Check to see if this was the "Clear Cart" dialog. If so, clear cart.
	  if (dialog_.getTag() != null && dialog_.getTag().equals(CLEAR_CART_TAG)) {
		ClearCart();
	  }
	  super.doDialogOkClick(view);
	}
	
	private void ProcessShippingInfoResponse(String webpage, String cookies) {
		// TODO(PHB): Parse webpage to get billing info:
		//   - Shipping Address(es)
		//   - Billing info (Credit Card and addresses)
		//   - User Points
		//   - User Bux
		
		// Parse Shipping and Billing addresses, credit card, and user's bux and
		// points from 'webpage', and write them to SharedPreferences.
		JactUserCompleteInfo user_info = new JactUserCompleteInfo();
		UserPageParser.ParseUserInfoFromWebpage(webpage, user_info);
		ShippingActivity.WriteAddresses(user_info.shipping_addresses_);
		BillingActivity.WriteAddresses(user_info.billing_addresses_);
		
		// Get total price for all items in cart, and make sure User has enough BUX and Points.
		CartAccessResponse response = new ShoppingCartActivity.CartAccessResponse();
		response.total_cart_price_ = new TotalCartPrice();
		AccessCart(CartAccessType.GET_TOTAL_CART_PRICE, response);
		TotalCartPrice total_price = response.total_cart_price_; 
		if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB", "Total cart value: " + total_price.toString());
		if (user_info.points_ < total_price.points_) {
			fadeAllViews(false);
		  DisplayPopupFragment("Insufficient Points",
				               "Remove items or earn more points to complete transaction",
				               "insufficient_points");
		  return;
		}
		if (user_info.bux_ + total_price.bux_purchased_ < total_price.bux_) {
		  fadeAllViews(false);
		  DisplayPopupFragment("Insufficient Bux",
				               "Remove items or purchase more BUX to complete transaction",
				               "insufficent_bux");
		  return;
		}
		
		if (user_info.shipping_addresses_ == null || user_info.shipping_addresses_.size() == 0) {
		  startActivity(new Intent(this, ShippingNewActivity.class));
		} else {
		  startActivity(new Intent(this, ShippingActivity.class));
		}
	}
	
	private static synchronized boolean SetShoppingCartFromGetCartStatic(String webpage) {
	  if (!ShoppingUtils.ParseCartFromGetCartPage(webpage, shopping_cart_)) return false;
	  
	  // Copy shopping_cart_ over to quantity_positive_shopping_cart_.
	  if (quantity_positive_shopping_cart_ == null) {
		quantity_positive_shopping_cart_ = new ShoppingUtils.ShoppingCartInfo();
	  }
	  if (quantity_positive_shopping_cart_.line_items_ == null) {
		quantity_positive_shopping_cart_.line_items_ = new ArrayList<LineItem>();
	  }
	  if (shopping_cart_.line_items_ != null) {
	    quantity_positive_shopping_cart_.line_items_ =
	        (ArrayList<LineItem>) shopping_cart_.line_items_.clone();
	  }
	  
	  //PHBIterator<ShoppingUtils.LineItem> sc_itr = shopping_cart_.line_items_.iterator();
      //PHBwhile (sc_itr.hasNext()) {
      //PHB	ShoppingUtils.LineItem sc_item = sc_itr.next();
      //PHB	quantity_positive_shopping_cart_.line_items_.add(sc_item);
      //PHB}
	  
	  // Remove any items with quantity zero.
	  if (quantity_positive_shopping_cart_.line_items_ == null) return true;
	  Iterator<ShoppingUtils.LineItem> qp_itr = quantity_positive_shopping_cart_.line_items_.iterator();
      while (qp_itr.hasNext()) {
    	ShoppingUtils.LineItem qp_item = qp_itr.next();
    	if (qp_item.quantity_ == 0) {
    		qp_itr.remove();
    	}
      }
      return true;
	}
	
	public static synchronized boolean UpdateLineItemStatic(ShoppingUtils.LineItem line_item) {
	  if (shopping_cart_ == null || line_item == null) return false;
	  if (quantity_positive_shopping_cart_ == null) {
		if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ShoppingCartActivity::UpdateLineItemStatic",  "Unexpected null qp_shopping_cart.");
		quantity_positive_shopping_cart_ = new ShoppingUtils.ShoppingCartInfo();
	  }
	  
	  // Create shopping cart line_items array if they are null.
	  if (shopping_cart_.line_items_ == null) {
	    shopping_cart_.line_items_ = new ArrayList<LineItem>(1);
	    quantity_positive_shopping_cart_.line_items_ = new ArrayList<LineItem>(1);
	  }
	  
	  // Update quantity_positive_shopping_cart_ first. Check if this line_item already exists
	  // there, if so, update quantity.
      Iterator<ShoppingUtils.LineItem> qp_itr = quantity_positive_shopping_cart_.line_items_.iterator();
      boolean found_pid = false;
      while (qp_itr.hasNext()) {
    	ShoppingUtils.LineItem item = qp_itr.next();
    	if (item.pid_ == line_item.pid_) {
    	  // Found item. Update quantity.
    	  found_pid = true;
    	  if (line_item.quantity_ == 0) {
    	    qp_itr.remove();
    	    break;
    	  } else {
	        item = line_item;
			FillExtraProductDetailsFromRewardsPage(item);
			break;
    	  }
	    }
	  }
	  if (!found_pid && line_item.quantity_ > 0) {
		quantity_positive_shopping_cart_.line_items_.add(line_item);
	  }
	  
	  // Now update shopping_cart_.
	  for (ShoppingUtils.LineItem item : shopping_cart_.line_items_) {
	    if (item.pid_ == line_item.pid_) {
	      item = line_item;
		  FillExtraProductDetailsFromRewardsPage(item);
	      return true;
	    }
	  }
	  // No existing line-item matched the PID. Add a new line-item.
	  shopping_cart_.line_items_.add(line_item);
	  return true;
	}
	
	private synchronized static boolean UpdateServerShoppingCart(
		ShoppingUtils.ShoppingCartInfo temp_cart, CartAccessResponse response) {
	  if (temp_cart == null) return false;
	  
	  // App has no knowledge of a current shopping cart, copy parsed cart into shopping_cart_.
	  if (shopping_cart_.revision_id_ == 0) {
	    shopping_cart_ = new ShoppingCartInfo(temp_cart);
	    quantity_positive_shopping_cart_ = new ShoppingCartInfo(temp_cart);
	    RemoveQuantityZeroItems();
	    FillExtraProductDetailsFromRewardsPage();
	    return false;
	  }

	  // App already has a cart on hand. Compare App's version to server's version, and update
	  // whichever one is older.
	  if (temp_cart.timestamp_ >= shopping_cart_.timestamp_) {
	    shopping_cart_ = temp_cart;
	    quantity_positive_shopping_cart_ = temp_cart;
	    RemoveQuantityZeroItems();
	    FillExtraProductDetailsFromRewardsPage();
	    return false;
	  }
	  // App's version of cart is more up-to-date. Update server's cart.
	  response.cart_ = new ShoppingUtils.ShoppingCartInfo();
	  response.cart_ = shopping_cart_;
	  return true;
	}
	
    // Updates server's cart (represented by old_cart) to reflect the changes in new_cart.
    // Note that since this method is only called when timestamp on new_cart is more recent
    // than on old_cart, this method should only be called when the App wants to update the
    // cart, by changing quantity of an item (i.e. there should only be one difference between
    // the old_cart and the new_cart).
	private synchronized void UpdateCart(
		ShoppingUtils.ShoppingCartInfo new_cart, ShoppingUtils.ShoppingCartInfo old_cart) {
	  if (new_cart == null) {
	    if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "ShoppingCartActivity::UpdateCart. Null new cart.");
	    return;
	  }
	  
	  // First check that we have an old cart to compare/update.
      if (old_cart == null) {
	  	if (GetNumRequestsCounter() >= ShoppingUtils.MAX_OUTSTANDING_SHOPPING_REQUESTS) {
		  DisplayPopupTitle("Unable to Reach Jact Server. Please Try Again.");
		  return;
		}
    	IncrementNumRequestsCounter();
	    if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "ShoppingCartActivity::UpdateCart. Null old cart.");
	    new GetUrlTask(this, GetUrlTask.TargetType.JSON).execute(
			jact_shopping_cart_url_, "GET", "", "", ShoppingUtils.GET_CART_TASK);
        return;
      }
    
      // Check if there is an existing cart.
      if (old_cart.order_id_ == 0) {
        if (new_cart == null || new_cart.line_items_ == null || new_cart.line_items_.isEmpty()) {
          CreateEmptyCart();
          return;
        } else if (new_cart.line_items_.size() == 1) {
          CreateCartWithLineItem(new_cart.line_items_.get(0));
          return;
        } else {
    	  // New cart should differ from the old cart by at most one line item. Log error and abort.
    	  if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "ShoppingCartActivity::UpdateCart. Expected at most one line item, but found " +
    	                     new_cart.line_items_.size() + ". Cart:\n" + new_cart.toString());
    	  return;
        }
      }
    
      // There is an existing cart. Verify order_id's match.
      if (old_cart.order_id_ != new_cart.order_id_) {
    	if (new_cart.order_id_ == 0) {
    	  // Will enter here when user has completed a checkout. In this case, clear the cart.
    	  ClearCart();
    	  return;
    	}
        if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "ShoppingCartActivity::UpdateCart. Old order id (" + old_cart.order_id_ +
    	  	               ") does not match new order id: " + new_cart.order_id_);
        return;
      }
    
      // Go through new_cart's line items. They should exactly match the old_cart's line items,
      // except possibly for one line item that needs to be updated.
      int line_item_to_update = -1;
      int updated_quantity = -1;
      Iterator<LineItem> new_items_itr = new_cart.line_items_.iterator();
  	  while (new_items_itr.hasNext()) {
        LineItem new_item = new_items_itr.next();
        updated_quantity = -1;
        boolean found_match = false;
        Iterator<LineItem> old_items_itr = old_cart.line_items_.iterator();
        while (!found_match && old_items_itr.hasNext()) {
          LineItem old_item = old_items_itr.next();
         if (new_item.id_ == old_item.id_) {
            // Sanity check line item is consistent with PID, Entity ID, and order_id.
            if (new_item.order_id_ != old_item.order_id_ || new_item.pid_ != old_item.pid_ ||
        	    new_item.entity_id_ != old_item.entity_id_) {
              if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "ShoppingCartActivity::UpdateCart. Mismatching line item " +
        	                     new_item.id_ + ". New item: " + new_item.toString() +
        	                     ". Old item: " + old_item.toString());
              return;
            }
            found_match = true;
            if (new_item.quantity_ != old_item.quantity_) {
              updated_quantity = new_item.quantity_;
            }
          }
        }
        // Check if we need to update this line item.
        if (!found_match || updated_quantity != -1) {
          // As mentioned above, we only allow a single update in this method.
    	  if (line_item_to_update != -1) {
    	    if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "ShoppingCartActivity::UpdateCart. Multiple line items to update: " +
	                           line_item_to_update + " and " + new_item.id_);
    	    return;
    	  }
    	  line_item_to_update = new_item.id_;
    	  updated_quantity = new_item.quantity_;
        }
      }
  	
  	  // Check if there were any mismatching items; return without additional work if not; otherwise
  	  // update server's cart.
  	  if (line_item_to_update == -1) {
  		return;
  	  }
  	  
      // Need cookies and csrf_token to update server's cart.
	  SharedPreferences user_info = getSharedPreferences(
          getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
      String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
      if (cookies.isEmpty()) {
    	String username = user_info.getString(getString(R.string.ui_username), "");
    	String password = user_info.getString(getString(R.string.ui_password), "");
        ShoppingUtils.RefreshCookies(
            this, username, password, ShoppingUtils.GET_COOKIES_THEN_UPDATE_LINE_ITEM_TASK);
        return;
      }
      String csrf_token = user_info.getString(getString(R.string.ui_csrf_token), "");
      if (csrf_token.isEmpty()) {
        if (!ShoppingUtils.GetCsrfToken(this, cookies, ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK)) {
          // Nothing to do.
        }
        return;
      }
      
      // Update server's cart.
  	  ShoppingUtils.UpdateLineItem(this, cookies, csrf_token, line_item_to_update, updated_quantity);
	}
	/* TODO(PHB): Remove below function (no longer used)
	public synchronized void UpdateLineItemIfModified(ShoppingUtils.LineItem line_item) {
	  if (line_item == null || line_item.pid_ == 0) return;
	  
	  // First check if line_item differs from shopping_cart_. 
	  ShoppingUtils.LineItem existing_item = GetCartItem(line_item.pid_);
	  if (existing_item != null &&
		  ShoppingUtils.LineItemsAreEquivalent(line_item, existing_item)) {
	    // Found an existing item in cart. Compare it to new item; if any differences, call update
		// cart. Otherwise, existing item is up-to-date, so nothing to do.
		if (!JactActionBarActivity.IS_PRODUCTION) Log.w("PHB TEMP", "ShoppingCartActivity::UpdateLineItemIfModified. Not updating line item " +
		                  "as it is already present in cart. Line item:\n" +
				          ShoppingUtils.PrintLineItemHumanReadable(line_item));
		return;
	  }
	  
	  // No matching line-item found, else if was out of date. Update it.
	  if (!JactActionBarActivity.IS_PRODUCTION) Log.w("PHB TEMP", "ShoppingCartActivity::UpdateLineItemIfModified. Not updating line item " +
                        "as it is already present in cart. New Line item:\n" +
		                ShoppingUtils.PrintLineItemHumanReadable(line_item) + "Old line item:\n" +
                        ShoppingUtils.PrintLineItemHumanReadable(existing_item));
	  UpdateLineItem(line_item);
	}*/
	
	public synchronized void UpdateLineItem(ShoppingUtils.LineItem line_item) {
      // Need cookies and csrf_token to update server's cart.
   	  SharedPreferences user_info = getSharedPreferences(
         getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
      String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
      if (cookies.isEmpty()) {
   	    String username = user_info.getString(getString(R.string.ui_username), "");
   	    String password = user_info.getString(getString(R.string.ui_password), "");
        ShoppingUtils.RefreshCookies(
            this, username, password, ShoppingUtils.GET_COOKIES_THEN_UPDATE_LINE_ITEM_TASK +
            ShoppingUtils.TASK_CART_SEPARATOR + ShoppingUtils.PrintLineItem(line_item));
        return;
      }
      String csrf_token = user_info.getString(getString(R.string.ui_csrf_token), "");
      if (csrf_token.isEmpty()) {
        if (!ShoppingUtils.GetCsrfToken(
        	    this, cookies, ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK +
        	    ShoppingUtils.TASK_CART_SEPARATOR + ShoppingUtils.PrintLineItem(line_item))) {
          // Nothing to do.
        }
        return;
      }
      
      // Update line-item in server's cart.
   	  if (!ShoppingUtils.UpdateLineItem(this, cookies, csrf_token, line_item)) {
   		// Nothing to do.
   		//PHB_OLDDisplayPopupFragment("Unable to update cart on server. Check connection and try again.",
   		//PHB_OLD                     "Unable_to_update_cart");
   	  }
	}
	
	public synchronized void CreateEmptyCart() {
      // Need cookies and csrf_token to create server's cart.
   	  SharedPreferences user_info = getSharedPreferences(
         getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
      String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
      if (cookies.isEmpty()) {
   	    String username = user_info.getString(getString(R.string.ui_username), "");
   	    String password = user_info.getString(getString(R.string.ui_password), "");
        ShoppingUtils.RefreshCookies(
            this, username, password, ShoppingUtils.GET_COOKIES_THEN_CREATE_CART_TASK);
        return;
      }
      String csrf_token = user_info.getString(getString(R.string.ui_csrf_token), "");
      if (csrf_token.isEmpty()) {
        if (!ShoppingUtils.GetCsrfToken(
        	    this, cookies, ShoppingUtils.GET_CSRF_THEN_CREATE_CART_TASK)) {
          // Nothing to do.
        }
        return;
      }
      
      // Update line-item in server's cart.
   	  if (!ShoppingUtils.CreateServerCart(this, cookies, csrf_token, null)) {
   		// Nothing to do.
   		//PHB_OLDDisplayPopupFragment("Unable to update cart on server. Check connection and try again.",
   		//PHB_OLD                     "Unable_to_update_cart");
   	  }
	}
	
	public synchronized void CreateCartWithLineItem(LineItem line_item) {
      // Need cookies and csrf_token to update server's cart.
   	  SharedPreferences user_info = getSharedPreferences(
         getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
      String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
      if (cookies.isEmpty()) {
   	    String username = user_info.getString(getString(R.string.ui_username), "");
   	    String password = user_info.getString(getString(R.string.ui_password), "");
        ShoppingUtils.RefreshCookies(
            this, username, password, ShoppingUtils.GET_COOKIES_THEN_CREATE_CART_TASK +
            ShoppingUtils.TASK_CART_SEPARATOR + ShoppingUtils.PrintLineItem(line_item));
        return;
      }
      String csrf_token = user_info.getString(getString(R.string.ui_csrf_token), "");
      if (csrf_token.isEmpty()) {
        if (!ShoppingUtils.GetCsrfToken(
        	    this, cookies, ShoppingUtils.GET_CSRF_THEN_CREATE_CART_TASK +
        	    ShoppingUtils.TASK_CART_SEPARATOR + ShoppingUtils.PrintLineItem(line_item))) {
          // Nothing to do.
        }
        return;
      }
      
      // Update line-item in server's cart.
   	  if (!ShoppingUtils.CreateServerCart(this, cookies, csrf_token, line_item)) {
   		// Nothing to do.
   		//PHB_OLDDisplayPopupFragment("Unable to update cart on server. Check connection and try again.",
   		//PHB_OLD                     "Unable_to_update_cart");
   	  }
	}

	@Override
	public void ProcessUrlResponse(String webpage, String cookies, String extra_params) {
	  if (JactActionBarActivity.USE_MOBILE_SITE) {
		  ProcessCartResponse(this, webpage, cookies, extra_params);
	  } else {
		  if (!JactActionBarActivity.IS_PRODUCTION)
			  Log.d("ShoppingCartActivity::ProcessUrlResponse", "Response:\n" + webpage);
		  DecrementNumRequestsCounter();
		  if (extra_params.equalsIgnoreCase(GET_REWARDS_PAGE_TASK)) {
			  ProductsActivity.SetProductsList(webpage);
			  AccessCart(CartAccessType.ADD_INFO_FROM_REWARDS);
		  } else if (extra_params.equalsIgnoreCase(GET_SHIPPING_INFO_TASK)) {
			  ProcessShippingInfoResponse(webpage, cookies);
		  } else if (extra_params.equalsIgnoreCase(ShoppingUtils.CLEAR_CART_TASK) ||
				  extra_params.equalsIgnoreCase(ShoppingUtils.GET_CART_TASK)) {
			  // Parse webpage to a new cart.
			  ShoppingUtils.ShoppingCartInfo temp_cart = new ShoppingUtils.ShoppingCartInfo();
			  if (!ShoppingUtils.ParseCartFromGetCartPage(webpage, temp_cart)) {
				  DisplayPopupFragment("Unable to fetch cart from server",
						  "Check connection and try again.",
						  "Unable_to_fetch_cart");
				  return;
			  }
			  CartAccessResponse response = new CartAccessResponse();
			  if (AccessCart(CartAccessType.UPDATE_CART, temp_cart, response)) {
				  UpdateCart(temp_cart, response.cart_);
			  }
		  } else if (extra_params.equalsIgnoreCase(ShoppingUtils.GET_COOKIES_THEN_CLEAR_CART_TASK)) {
			  SaveCookies(cookies);
			  ClearCart();
		  } else if (extra_params.equalsIgnoreCase(ShoppingUtils.GET_CSRF_THEN_CLEAR_CART_TASK)) {
			  SaveCsrfToken(webpage);
			  ClearCart();
		  } else if (extra_params.equalsIgnoreCase(ShoppingUtils.GET_COOKIES_THEN_GET_CART_TASK)) {
			  SaveCookies(cookies);
			  GetInitialShoppingCart();
		  } else if (extra_params.equalsIgnoreCase(ShoppingUtils.CREATE_CART_TASK)) {
			  // Parse webpage to a new cart.
			  ShoppingUtils.ShoppingCartInfo temp_cart = new ShoppingUtils.ShoppingCartInfo();
			  if (!ShoppingUtils.ParseCartFromCreateCartPage(webpage, temp_cart)) {
				  DisplayPopupFragment("Unable to fetch cart from server",
						  "Check connection and try again.",
						  "Unable_to_fetch_cart");
				  return;
			  }
			  CartAccessResponse response = new CartAccessResponse();
			  if (AccessCart(CartAccessType.UPDATE_CART, temp_cart, response)) {
				  UpdateCart(temp_cart, response.cart_);
			  }
		  } else if (extra_params.equalsIgnoreCase(ShoppingUtils.GET_COOKIES_THEN_CREATE_CART_TASK)) {
			  SaveCookies(cookies);
			  CreateEmptyCart();
		  } else if (extra_params.equalsIgnoreCase(ShoppingUtils.GET_CSRF_THEN_CREATE_CART_TASK)) {
			  SaveCsrfToken(webpage);
			  CreateEmptyCart();
		  } else if (extra_params.indexOf(ShoppingUtils.ADD_LINE_ITEM_TASK) == 0 ||
				  extra_params.indexOf(ShoppingUtils.UPDATE_LINE_ITEM_TASK) == 0) {
			  try {
				  JSONObject line_item = new JSONObject(webpage);
				  ArrayList<ShoppingUtils.LineItem> new_line_items = new ArrayList<ShoppingUtils.LineItem>();
				  if (extra_params.indexOf(ShoppingUtils.ADD_LINE_ITEM_TASK) == 0 &&
						  !ShoppingUtils.ParseLineItemFromAddLineItemPage(line_item, new_line_items)) {
					  // TODO(PHB): Handle this error (e.g. popup warning to user).
					  if (!JactActionBarActivity.IS_PRODUCTION)
						  Log.e("PHB ERROR", "ProductsActivity::ProcessUrlResponse. Unable to parse add line-item response:\n" + webpage);
					  return;
				  } else if (extra_params.indexOf(ShoppingUtils.UPDATE_LINE_ITEM_TASK) == 0 &&
						  !ShoppingUtils.ParseLineItemFromUpdateLineItemPage(line_item, new_line_items)) {
					  // TODO(PHB): Handle this error (e.g. popup warning to user).
					  if (!JactActionBarActivity.IS_PRODUCTION)
						  Log.e("PHB ERROR", "ProductsActivity::ProcessUrlResponse. Unable to parse add line-item response:\n" + webpage);
					  return;
				  }
				  if (new_line_items.size() != 1 ||
						  !AccessCart(CartAccessType.UPDATE_LINE_ITEM, new_line_items.get(0))) {
					  // TODO(PHB): Handle this error (e.g. popup warning to user).
					  if (!JactActionBarActivity.IS_PRODUCTION)
						  Log.e("PHB ERROR", "ProductsActivity::ProcessUrlResponse. Unable to parse cart response. " +
								  "Num new line items: " + new_line_items.size() + "; Webpage response:\n" + webpage);
				  }
				  // PHB Temp.
				  //if (!JactActionBarActivity.IS_PRODUCTION) Log.w("PHB TEMP", "ProductsActivity::ProcessUrlResponse. Updated line item. Now shopping cart is: " +
				  //                  ShoppingCartActivity.PrintCart());
			  } catch (JSONException e) {
				  // TODO(PHB): Handle this error (e.g. popup warning to user).
				  if (!JactActionBarActivity.IS_PRODUCTION)
					  Log.e("PHB ERROR", "ProductsActivity::ProcessUrlResponse. Unable to parse add line-item response " +
							  "from server. Exception: " + e.getMessage() + "; webpage response:\n" + webpage);
			  }
		  } else if (extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) > 0 &&
				  (extra_params.indexOf(ShoppingUtils.CREATE_CART_TASK) == 0 ||
						  extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_CREATE_CART_TASK) == 0 ||
						  extra_params.indexOf(ShoppingUtils.GET_CSRF_THEN_CREATE_CART_TASK) == 0 ||
						  extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_UPDATE_LINE_ITEM_TASK) == 0 ||
						  extra_params.indexOf(ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK) == 0)) {
			  // Create Cart was called to create a new cart/order, but the new item was not
			  // yet added to the cart. Parse the response (e.g. to get order_id), and then
			  // add the new item to the cart (the specifics of the item to add are in extra_params).
			  String parsed_line_item = extra_params.substring(
					  extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) +
							  ShoppingUtils.TASK_CART_SEPARATOR.length());
			  if (extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_UPDATE_LINE_ITEM_TASK) == 0 ||
					  extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_CREATE_CART_TASK) == 0) {
				  SaveCookies(cookies);
			  } else if (extra_params.indexOf(ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK) == 0 ||
					  extra_params.indexOf(ShoppingUtils.GET_CSRF_THEN_CREATE_CART_TASK) == 0) {
				  SaveCsrfToken(webpage);
			  } else if (extra_params.indexOf(ShoppingUtils.CREATE_CART_TASK) == 0) {
				  // Parse webpage to a new cart.
				  ShoppingUtils.ShoppingCartInfo temp_cart = new ShoppingUtils.ShoppingCartInfo();
				  if (!ShoppingUtils.ParseCartFromCreateCartPage(webpage, temp_cart)) {
					  DisplayPopupFragment("Unable to fetch cart from server",
							  "Check connection and try again.",
							  "Unable_to_fetch_cart");
					  return;
				  }
				  CartAccessResponse response = new CartAccessResponse();
				  if (AccessCart(CartAccessType.UPDATE_CART, temp_cart, response)) {
					  UpdateCart(temp_cart, response.cart_);
				  }
			  }
			  if (extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_CREATE_CART_TASK) == 0 ||
					  extra_params.indexOf(ShoppingUtils.GET_CSRF_THEN_CREATE_CART_TASK) == 0) {
				  CreateCartWithLineItem(ShoppingUtils.ParseLineItem(parsed_line_item));
			  } else {
				  UpdateLineItem(ShoppingUtils.ParseLineItem(parsed_line_item));
			  }
			  //} else if (extra_params.equalsIgnoreCase(ShoppingUtils.GET_COOKIES_THEN_UPDATE_LINE_ITEM_TASK)) {
			  //SaveCookies(cookies);
			  //UpdateCart();
			  //} else if (extra_params.equalsIgnoreCase(ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK)) {
			  //SaveCsrfToken(webpage);
			  //UpdateCart();
		  } else if (extra_params.equalsIgnoreCase(USER_POINTS)) {
			  IncrementNumRequestsCounter();
			  ProcessCartResponse(this, webpage, cookies, extra_params);
			  return;
		  } else {
			  if (!JactActionBarActivity.IS_PRODUCTION)
				  Log.e("PHB ERROR", "ShoppingCartActivity::ProcessUrlResponse. Returning from " +
						  "unrecognized task:\n" + GetUrlTask.PrintExtraParams(extra_params));
		  }
		  if (GetNumRequestsCounter() == 0) {
			  SetCartIcon(this);
			  if (GetNumRequestsCounter() == 0) {
				  LoadView();
			  }
		  }
	  }
	}

	@Override
	public void ProcessUrlResponse(Bitmap pic, String cookies, String extra_params) {
		// TODO Auto-generated method stub.
	}

	@Override
	public void ProcessFailedResponse(FetchStatus status, String extra_params) {
	  if (JactActionBarActivity.USE_MOBILE_SITE) {
		  ProcessFailedCartResponse(this, status, extra_params);
	  } else {
		  // TODO(PHB): Handle failure cases below by more than just a Log error (e.g. popup
		  // a dialog? Try query again (depending on status)?).
		  DecrementNumRequestsCounter();
		  num_failed_requests_++;
		  if (status == GetUrlTask.FetchStatus.ERROR_CSRF_FAILED ||
				  status == GetUrlTask.FetchStatus.ERROR_UNSUPPORTED_CONTENT_TYPE) {
			  String task = "";
			  if (extra_params.indexOf(ShoppingUtils.CREATE_CART_TASK) >= 0) {
				  task = ShoppingUtils.GET_CSRF_THEN_CREATE_CART_TASK;
			  } else if (extra_params.indexOf(ShoppingUtils.CLEAR_CART_TASK) >= 0) {
				  task = ShoppingUtils.GET_CSRF_THEN_CLEAR_CART_TASK;
			  } else if (extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) > 0 &&
					  (extra_params.indexOf(ShoppingUtils.ADD_LINE_ITEM_TASK) == 0 ||
							  extra_params.indexOf(ShoppingUtils.UPDATE_LINE_ITEM_TASK) == 0)) {
				  String parsed_line_item = extra_params.substring(
						  extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) +
								  ShoppingUtils.TASK_CART_SEPARATOR.length());
				  task = ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK +
						  ShoppingUtils.TASK_CART_SEPARATOR + parsed_line_item;
			  } else {
				  if (!JactActionBarActivity.IS_PRODUCTION)
					  Log.e("PHB ERROR", "ShoppingCartActivity::ProcessFailedResponse. CSRF Failed. Status: " + status +
							  "; extra_params: " + GetUrlTask.PrintExtraParams(extra_params));
				  return;
			  }
			  SharedPreferences user_info = getSharedPreferences(
					  getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
			  String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
			  if (!ShoppingUtils.GetCsrfToken(this, cookies, task)) {
				  // Nothing to do.
			  }
		  } else if (status == GetUrlTask.FetchStatus.ERROR_RESPONSE_CODE ||
				  status == GetUrlTask.FetchStatus.ERROR_UNABLE_TO_CONNECT) {
			  // Failed to Connect with Jact Server. Retry, if we haven't had too many consecutive failures.
			  if (num_failed_requests_ >= MAX_FAILED_REQUESTS) {
				  DisplayPopup("Unable to Reach Jact", "Your last action may not have been processed.");
				  return;
			  }
			  if (extra_params.indexOf(ShoppingUtils.CREATE_CART_TASK) >= 0 ||
					  extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_CREATE_CART_TASK) >= 0) {
				  CreateEmptyCart();
			  } else if (extra_params.indexOf(ShoppingUtils.GET_CART_TASK) >= 0 ||
					  extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_GET_CART_TASK) >= 0) {
				  GetInitialShoppingCart();
			  } else if (extra_params.indexOf(ShoppingUtils.CLEAR_CART_TASK) >= 0 ||
					  extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_CLEAR_CART_TASK) >= 0) {
				  ClearCart();
			  } else if (extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) > 0 &&
					  (extra_params.indexOf(ShoppingUtils.ADD_LINE_ITEM_TASK) == 0 ||
							  extra_params.indexOf(ShoppingUtils.UPDATE_LINE_ITEM_TASK) == 0 ||
							  extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_UPDATE_LINE_ITEM_TASK) >= 0)) {
				  String parsed_line_item = extra_params.substring(
						  extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) +
								  ShoppingUtils.TASK_CART_SEPARATOR.length());
				  UpdateLineItem(ShoppingUtils.ParseLineItem(parsed_line_item));
			  } else if (extra_params.indexOf(ShoppingUtils.GET_CSRF_THEN_CREATE_CART_TASK) >= 0 ||
					  extra_params.indexOf(ShoppingUtils.GET_CSRF_THEN_CLEAR_CART_TASK) >= 0 ||
					  extra_params.indexOf(ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK) >= 0) {
				  SharedPreferences user_info = getSharedPreferences(
						  getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
				  String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
				  ShoppingUtils.GetCsrfToken(this, cookies, extra_params);
			  } else {
				  if (!JactActionBarActivity.IS_PRODUCTION)
					  Log.e("ShoppingCartActivity::ProcessFailedResponse",
							  "Unrecognized task: " + GetUrlTask.PrintExtraParams(extra_params));
			  }
		  } else if (extra_params.equalsIgnoreCase(USER_POINTS)) {
			  IncrementNumRequestsCounter();
			  ProcessFailedCartResponse(this, status, extra_params);
		  } else {
			  if (!JactActionBarActivity.IS_PRODUCTION)
				  Log.w("ShoppingCartActivity::ProcessFailedResponse",
						  "Status: " + status + "; extra_params: " + GetUrlTask.PrintExtraParams(extra_params) +
								  ". Trying parent resolution.");
			  // ProcessFailedCartResponse will decrement num_server_tasks_, so re-increment it here so the net is no change.
			  IncrementNumRequestsCounter();
			  ProcessFailedCartResponse(this, status, extra_params);
		  }
		  if (GetNumRequestsCounter() == 0) {
			  SetCartIcon(this);
			  if (GetNumRequestsCounter() == 0) {
				  fadeAllViews(false);
			  }
		  }
	  }
	}
	
	@Override
	public void DisplayPopup(String message) {
	  DisplayPopupFragment(message, "too_many_server_requests");
	}
}