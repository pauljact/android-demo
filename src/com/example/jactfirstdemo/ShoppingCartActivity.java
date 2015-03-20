package com.example.jactfirstdemo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

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
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;
import com.example.jactfirstdemo.ShoppingUtils.Amount;
import com.example.jactfirstdemo.ShoppingUtils.LineItem;

public class ShoppingCartActivity extends JactActionBarActivity implements ProcessUrlResponseCallback {
	private static final int MAX_CART_ITEMS = 10;
	// TODO(PHB): Update string below with actual target.
	//private static final String jact_shipping_info_url_ = "http://us7.jact.com:3080/rest/rewards.json";
	//private static final String jact_shopping_cart_url_ = "http://us7.jact.com:3080/rest/cart.json";
	private static final String rewards_url_ = "https://us7.jact.com:3081/rest/rewards.json";
	private static final String jact_shopping_cart_url_ = "https://us7.jact.com:3081/rest/cart.json";
	private static final String GET_SHIPPING_INFO_TASK = "get_shipping_info_task";
	private static final String GET_REWARDS_PAGE_TASK = "get_rewards_page_task";
	
	public static JactAddress shipping_address_;
	public static JactAddress billing_address_;
	
	private static ShoppingUtils.ShoppingCartInfo shopping_cart_;
	private static ShoppingUtils.ShoppingCartInfo temp_shopping_cart_;
	private static ShoppingUtils.ShoppingCartInfo quantity_positive_shopping_cart_;
	private static boolean hay_active_cart_request_;
	private static boolean view_has_been_loaded_;
	private static int num_csrf_requests_;
	private static int current_cart_icon_pos_;
	private static ArrayList<String> items_to_remove_;
	private ListView items_listview_;
	private CheckoutAdapter adapter_;
	private JactNavigationDrawer navigation_drawer_;
	private JactDialogFragment dialog_;
	
	public enum CardType {
		NO_TYPE,
		VISA,
		MC,
		AMEX
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
		MAX_QUANTITY_EXCEEDED
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
    
  public static synchronized ItemToAddStatus EnforceCartRules(int pid, int quantity, String type) {
	 if (shopping_cart_ == null) return ItemToAddStatus.CART_NOT_READY;
	 int max_quantity = ProductsActivity.GetMaxQuantity(pid);
	 if (max_quantity == -2) {
	   return ItemToAddStatus.REWARDS_NOT_FETCHED;
	 } else if (max_quantity > 0 && max_quantity < quantity) {
	   return ItemToAddStatus.MAX_QUANTITY_EXCEEDED;
	 }
	 for (ShoppingUtils.LineItem item : shopping_cart_.line_items_) {
	   if (item.type_ != type) {
	     return ItemToAddStatus.INCOMPATIBLE_TYPE;
	   }
	 }
	 return ItemToAddStatus.OK;
  }
  
  public synchronized void GetInitialShoppingCart() {
	// Need cookies and csrf_token to update server's cart.
	SharedPreferences user_info = getSharedPreferences(
        getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
    String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
    if (cookies.isEmpty()) {
      String username = user_info.getString(getString(R.string.ui_username), "");
      String password = user_info.getString(getString(R.string.ui_password), "");
      num_server_tasks_++;
      ShoppingUtils.RefreshCookies(
          this, username, password, ShoppingUtils.GET_COOKIES_THEN_GET_CART_TASK);
      return;
    }
    
    num_server_tasks_++;
    GetUrlTask task = new GetUrlTask(this, GetUrlTask.TargetType.JSON);
	GetUrlTask.UrlParams params = new GetUrlTask.UrlParams();
	params.url_ = jact_shopping_cart_url_;
	params.connection_type_ = "GET";
	params.extra_info_ = ShoppingUtils.GET_CART_TASK;
	params.cookies_ = cookies;
	task.execute(params);
  }
  
  public static synchronized boolean InitializeOnce() {
	boolean shopping_cart_was_null = (shopping_cart_ == null);
    if (shopping_cart_ == null) {
	  shopping_cart_ = new ShoppingUtils.ShoppingCartInfo();
	  shopping_cart_.line_items_ = new ArrayList<ShoppingUtils.LineItem>();
	  quantity_positive_shopping_cart_ = new ShoppingUtils.ShoppingCartInfo();
	  quantity_positive_shopping_cart_.line_items_ = new ArrayList<ShoppingUtils.LineItem>();
	  hay_active_cart_request_ = true;
	}
	if (items_to_remove_ == null) {
	  items_to_remove_ = new ArrayList<String>();
  	}
	return shopping_cart_was_null;
  }
  
  private synchronized void LoadView() {
	if (view_has_been_loaded_) return;
	Log.e("PHB TEMP", "SCA::LoadView. Loading view.");
	view_has_been_loaded_ = true;
    CheckForEmptyCart();
    SetCartItemsUI();
    adapter_.notifyDataSetChanged();
    fadeAllViews(false);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState);
    num_server_tasks_ = 0;
    setContentView(R.layout.checkout_layout);
    Toolbar toolbar = (Toolbar) findViewById(R.id.jact_toolbar);
    TextView ab_title = (TextView) findViewById(R.id.toolbar_title_tv);
    ab_title.setText(R.string.cart_activity_label);
    setSupportActionBar(toolbar);
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    // Set Navigation Drawer.
    navigation_drawer_ =
        new JactNavigationDrawer(this,
        		                 findViewById(R.id.checkout_drawer_layout),
        		                 findViewById(R.id.checkout_left_drawer),
        		                 JactNavigationDrawer.ActivityIndex.CHECKOUT_MAIN);
	hay_active_cart_request_ = false;
	ResetNumCsrfRequests();
    if (InitializeOnce()) {
    	GetInitialShoppingCart();
    }
  }
  

  @Override
  protected void onResume() {
	  // Re-enable parent activity before transitioning to the next activity.
	  // This ensures e.g. that when user hits 'back' button, the screen
	  // is 'active' (not faded) when the user returns.
	  view_has_been_loaded_ = false;
      InitializeCallback(this);
	  GetCart();
	  Log.e("PHB TEMP", "SCA::onResume. num_server_tasks_: " + num_server_tasks_);
	  if (!ProductsActivity.IsProductsListInitialized()) {
    	new GetUrlTask(this, GetUrlTask.TargetType.JSON).execute(
        		rewards_url_, "GET", "", "", GET_REWARDS_PAGE_TASK);
    	num_server_tasks_++;
	  }
	  if (num_server_tasks_ == 0) {
		LoadView();
	  } else {
		fadeAllViews(true);
	  }
	  Log.e("PHB TEMP", "SCA::onResume. num_server_tasks_: " + num_server_tasks_);
	  super.onResume();
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
    menu_bar_ = menu;
	SetCartIcon(menu_bar_);
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
  public void onPause() {
    if (items_to_remove_ != null) {
      Iterator<String> itr = items_to_remove_.iterator();
      while (itr.hasNext()) {
	    RemoveQuantityPositiveCartItem(itr.next());
      } 
    }
    super.onPause();
  }

  @Override
  public void fadeAllViews(boolean should_fade) {
    ProgressBar spinner = (ProgressBar) findViewById(R.id.checkout_progress_bar);
    Button proceed_button = (Button) findViewById(R.id.proceed_to_checkout_button);
    AlphaAnimation alpha;
    if (should_fade) {
      proceed_button.setEnabled(false);
      proceed_button.setTextColor(getResources().getColor(R.color.translucent_black));
      spinner.setVisibility(View.VISIBLE);
      alpha = new AlphaAnimation(0.5F, 0.5F);
    } else {
      spinner.setVisibility(View.GONE);
      alpha = new AlphaAnimation(1.0F, 1.0F);
      if (GetNumberCartItems() == 0) {
        proceed_button.setEnabled(false);
        proceed_button.setTextColor(getResources().getColor(R.color.translucent_black));
      } else {
        proceed_button.setEnabled(true);
        proceed_button.setTextColor(getResources().getColor(R.color.white));
      }
    }
    // The AlphaAnimation will make the whole content frame transparent
    // (so that none of the views show).
    alpha.setDuration(0); // Make animation instant
    alpha.setFillAfter(true); // Tell it to persist after the animation ends
    RelativeLayout layout = (RelativeLayout) findViewById(R.id.checkout_content_frame);
    layout.startAnimation(alpha); // Add animation to the layout.
  }
  
  private void CheckForEmptyCart() {
    if (GetNumberCartItems() == 0) {
      TextView empty_msg = (TextView) findViewById(R.id.checkout_no_items_tv);
      empty_msg.setVisibility(View.VISIBLE);
      Button proceed_button = (Button) findViewById(R.id.proceed_to_checkout_button);
      proceed_button.setEnabled(false);
      proceed_button.setTextColor(getResources().getColor(R.color.translucent_black));
    } else {
      TextView empty_msg = (TextView) findViewById(R.id.checkout_no_items_tv);
      empty_msg.setVisibility(View.GONE);
      Button proceed_button = (Button) findViewById(R.id.proceed_to_checkout_button);
      proceed_button.setEnabled(true);
      proceed_button.setTextColor(getResources().getColor(R.color.white));
    }
  }
  
  public void EnableProceedToCheckoutButton() {
    Button proceed_button = (Button) findViewById(R.id.proceed_to_checkout_button);
    proceed_button.setEnabled(true);
    proceed_button.setTextColor(getResources().getColor(R.color.white));
  }
  
  public void DisableProceedToCheckoutButton() {
    Button proceed_button = (Button) findViewById(R.id.proceed_to_checkout_button);
    proceed_button.setEnabled(false);
    proceed_button.setTextColor(getResources().getColor(R.color.translucent_black));
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
  
  public static synchronized String PrintCart() {
    if (shopping_cart_ == null) return "";
	return shopping_cart_.toString();
  }
	
  public static synchronized int GetNumberCartItems() {
	if (shopping_cart_ == null || shopping_cart_.line_items_ == null) return -1;
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
  
  private void SetCartItemsUI() {
	  items_listview_ = (ListView) findViewById(R.id.checkout_list);

      // Getting adapter by passing xml data ArrayList
      adapter_ = new CheckoutAdapter(this, R.layout.checkout_item, quantity_positive_shopping_cart_.line_items_);
      items_listview_.setAdapter(adapter_);
  }
  
  public static int GetCartIconResource(int pos) {
	  int pos_to_check = pos;
	  if (pos >= cart_icon_map_.size()) {
	    pos_to_check = cart_icon_map_.size() - 1;
	  }
	  if (cart_icon_map_.containsKey(pos_to_check)) {
	    return cart_icon_map_.get(pos_to_check);
	  } else {
	    Log.e("PHB Error", "ShoppingCartActivity::GetCartIconResource. Unable to get icon for position: " + pos);
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
  
  public static synchronized ShoppingUtils.LineItem GetCartItem(int product_id) {
    if (shopping_cart_ == null || shopping_cart_.line_items_ == null) return null;
    Log.e("PHB TEMP", "SCA::GetCartItem. Num line items: " + shopping_cart_.line_items_.size() +
    		          ", shopping cart: " + shopping_cart_.toString());
    Iterator<ShoppingUtils.LineItem> itr = shopping_cart_.line_items_.iterator();
    while (itr.hasNext()) {
      ShoppingUtils.LineItem item = itr.next();
      Log.e("PHB TEMP", "ShoppingCartActivity::GetCartItem. product_id: " + product_id +
    		            ", pid: " + item.pid_);
	  if (item.pid_ == product_id) {
	    return item;
	  }
    }
    Log.e("PHB TEMP", "ShoppingCartActivity::GetCartItem. No match found.");
    return null;
  }
  
  public static synchronized ShoppingUtils.LineItem GetCartItem(String product_id) {
    if (shopping_cart_ == null || shopping_cart_.line_items_ == null) return null;
    Iterator<ShoppingUtils.LineItem> itr = shopping_cart_.line_items_.iterator();
    while (itr.hasNext()) {
      ShoppingUtils.LineItem item = itr.next(); 
	  if (Integer.toString(item.pid_) == product_id) {
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
    
    private synchronized void RemoveQuantityZeroItems() {
      Iterator<ShoppingUtils.LineItem> qp_itr = quantity_positive_shopping_cart_.line_items_.iterator();
      while (qp_itr.hasNext()) {
    	  ShoppingUtils.LineItem item = qp_itr.next(); 
    	  if (item.quantity_ == 0) {
    	    qp_itr.remove();
    	  }
      }
    }
    
    public synchronized boolean RemoveQuantityPositiveCartItem(String product_id) {
        if (quantity_positive_shopping_cart_ == null ||
        	quantity_positive_shopping_cart_.line_items_ == null) {
          return false;
        }
        Iterator<ShoppingUtils.LineItem> qp_itr = quantity_positive_shopping_cart_.line_items_.iterator();
        while (qp_itr.hasNext()) {
      	  ShoppingUtils.LineItem item = qp_itr.next(); 
      	  if (Integer.toString(item.pid_) == product_id) {
      	    qp_itr.remove();
      	    return true;
      	  }
        }
      	return false;
      }
    
    public synchronized boolean RemoveCartItem(String product_id) {
      if (shopping_cart_ == null || shopping_cart_.line_items_ == null) return false;
      Iterator<ShoppingUtils.LineItem> qp_itr = quantity_positive_shopping_cart_.line_items_.iterator();
      while (qp_itr.hasNext()) {
    	ShoppingUtils.LineItem item = qp_itr.next(); 
    	if (Integer.toString(item.pid_) == product_id) {
    	  qp_itr.remove();
    	  break;
    	}
      }
      Iterator<ShoppingUtils.LineItem> itr = shopping_cart_.line_items_.iterator();
      while (itr.hasNext()) {
    	ShoppingUtils.LineItem item = itr.next(); 
    	if (Integer.toString(item.pid_) == product_id) {
     	  // Update server, if this represents a change in the item's quantity.
    	  if (item.quantity_ != 0) {
    	    item.quantity_ = 0;
     	    UpdateLineItem(item);
    	  }
    	  itr.remove();
    	  return true;
    	}
      }
      return false;
    }
    
    public synchronized void ClearCart() {
    	if (shopping_cart_ == null || shopping_cart_.line_items_ == null) return;
    	CreateEmptyCart();
    }
    
    public synchronized ItemToAddStatus GetCartItemToAddStatus(
    		String product_id, String title, Bitmap bitmap, int bux, int points, int usd) {
    	return GetCartItemToAddStatus(product_id, title, bitmap, bux, points, usd, 1);
    }
    
	public synchronized ItemToAddStatus GetCartItemToAddStatus(
			String product_id, String title, Bitmap bitmap, int bux, int points, int usd, int quantity) {
		ShoppingUtils.LineItem item = new ShoppingUtils.LineItem();
		try {
		  item.pid_ = Integer.parseInt(product_id);
		  item.title_ = title;
		  item.product_icon_ = bitmap;
		  item.cost_ = new ArrayList<Amount>();
		  if (bux > 0) {
		    Amount amount = new Amount();
		    amount.type_ = ShoppingUtils.CurrencyCode.BUX;
		    amount.price_ = bux;
		    item.cost_.add(amount);
		  }
		  if (points > 0) {
		    Amount amount = new Amount();
		    amount.type_ = ShoppingUtils.CurrencyCode.POINTS;
		    amount.price_ = points;
		    item.cost_.add(amount);
		  }
		  if (usd > 0) {
		    Amount amount = new Amount();
		    amount.type_ = ShoppingUtils.CurrencyCode.USD;
		    amount.price_ = usd;
		    item.cost_.add(amount);
		  }
		  item.quantity_ = quantity;
		} catch (NumberFormatException e) {
		  return ItemToAddStatus.NO_PID;
		}
		return GetCartItemToAddStatus(item);
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
	  if (cart_info == null) return;
	  for (ShoppingUtils.LineItem item : shopping_cart_.line_items_) {
		ProductsActivity.FillItemDetails(item);
	  }
	}
	
	private static synchronized void FillExtraProductDetailsFromRewardsPage(
	    ShoppingUtils.ShoppingCartInfo cart_info, ShoppingUtils.LineItem item) {
	  if (cart_info == null || item == null ) return;
	  for (ShoppingUtils.LineItem cart_item : shopping_cart_.line_items_) {
		if (cart_item.pid_ == item.pid_) {
		  ProductsActivity.FillItemDetails(cart_item);
		  break;
		}
	  }
	}
	
	// Adds a item to the cart (or updates the item, in case it's already present in the cart).
	public static synchronized ItemToAddStatus GetCartItemToAddStatus(ShoppingUtils.LineItem item) {
		if (item == null) {
			Log.e("PHB ERROR", "ShoppingCartActivity::ItemToAddStatus. 1");
			return ItemToAddStatus.NO_PID;
		}
		if (item.pid_ == 0) {
			Log.e("PHB ERROR", "ShoppingCartActivity::ItemToAddStatus. 2");
			return ItemToAddStatus.NO_PID;
		}
		ShoppingUtils.LineItem temp_item = GetCartItem(item.pid_);
		if (temp_item != null) {
			if (temp_item.quantity_ > 10) {
				return ItemToAddStatus.ITEM_MAX;
			}
			return ItemToAddStatus.INCREMENTED;
		}
		if (GetNumberCartItems() >= MAX_CART_ITEMS) {
			return ItemToAddStatus.CART_FULL;
		}
		return EnforceCartRules(item.pid_, 1 + item.quantity_, item.type_);
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
	
	public void DisplayPopup(String title) {
	  dialog_ = new JactDialogFragment(title);
	  dialog_.show(getSupportFragmentManager(), title);
	}
	
	public void DisplayPopup(String title, String message) {
	  dialog_ = new JactDialogFragment(title, message);
	  dialog_.show(getSupportFragmentManager(), title);
	}
	
	public void doProceedToShippingButtonClick(View view) {
		if (GetTotalCartQuantity() == 0) {
			dialog_ = new JactDialogFragment("No Items in Cart.");
			dialog_.show(getSupportFragmentManager(), "No_items_in_cart");
		} else {
			if (shopping_cart_ == null || shopping_cart_.order_id_ == 0) {
			  dialog_ = new JactDialogFragment("Order Id not found.");
			  dialog_.show(getSupportFragmentManager(), "No_order_id");
			} else {
			  fadeAllViews(true);
			  CheckoutActivity.SetOrderId(shopping_cart_.order_id_);
			  startActivity(new Intent(this, CheckoutActivity.class));
			}
		}

		// PHB Old: The following code will execute the checkout process within the App
		// (i.e. it won't use a WebView of the Mobile website).
		//fadeAllViews(true);
		//new GetUrlTask(this, GetUrlTask.TargetType.JSON).execute(
		//		jact_shipping_info_url_, "GET", "", "", GET_SHIPPING_INFO_TASK);
	}

	public void doDialogOkClick(View view) {
	  // Close Dialog window.
	  dialog_.dismiss();
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
		TotalCartPrice total_price = new TotalCartPrice();
		GetTotalCartPrice(total_price);
		Log.e("PHB", "Total cart value: " + total_price.toString());
		if (user_info.points_ < total_price.points_) {
			fadeAllViews(false);
			dialog_ = new JactDialogFragment(
				"Insufficient Points", "Remove items or earn more points to complete transaction");
			dialog_.show(getSupportFragmentManager(), "insufficient_points");
			return;
		}
		if (user_info.bux_ + total_price.bux_purchased_ < total_price.bux_) {
			fadeAllViews(false);
			dialog_ = new JactDialogFragment(
				"Insufficient Bux", "Remove items or purchase more BUX to complete transaction");
			dialog_.show(getSupportFragmentManager(), "insufficent_bux");
			return;
		}
		
		if (user_info.shipping_addresses_ == null || user_info.shipping_addresses_.size() == 0) {
		  startActivity(new Intent(this, ShippingNewActivity.class));
		} else {
		  startActivity(new Intent(this, ShippingActivity.class));
		}
	}
	
	public static synchronized int GetOrderId() {
		if (shopping_cart_ == null) {
			Log.e("PHB ERROR", "ShoppingCartActivity::GetOrderId. Unexpected call to GetOrderId: Shopping cart not intialized yet.");
			return -1;
		}
		return shopping_cart_.order_id_;
	}
	
	public static synchronized boolean SetShoppingCartFromGetCartStatic(String webpage) {
	  if (!ShoppingUtils.ParseCartFromGetCartPage(webpage, shopping_cart_)) return false;
	  
	  // Copy shopping_cart_ over to quantity_positive_shopping_cart_.
	  if (quantity_positive_shopping_cart_ == null) {
		quantity_positive_shopping_cart_ = new ShoppingUtils.ShoppingCartInfo();
	  }
	  if (quantity_positive_shopping_cart_.line_items_ == null) {
		quantity_positive_shopping_cart_.line_items_ = new ArrayList<LineItem>();
	  }
	  Iterator<ShoppingUtils.LineItem> sc_itr = shopping_cart_.line_items_.iterator();
      while (sc_itr.hasNext()) {
    	ShoppingUtils.LineItem sc_item = sc_itr.next();
    	quantity_positive_shopping_cart_.line_items_.add(sc_item);
      }
	  
	  // Remove any items with quantity zero.
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
	  if (shopping_cart_.line_items_ == null) {
	    shopping_cart_.line_items_ = new ArrayList<LineItem>(1);
	    quantity_positive_shopping_cart_.line_items_ = new ArrayList<LineItem>(1);
	  }
	  // Look to see if there is already a line_item with a matching PID; if so, update that
	  // line item.
      Iterator<ShoppingUtils.LineItem> qp_itr = quantity_positive_shopping_cart_.line_items_.iterator();
      boolean found_pid = false;
      while (qp_itr.hasNext()) {
    	ShoppingUtils.LineItem item = qp_itr.next();
    	if (item.pid_ == line_item.pid_) {
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
	
	private synchronized void UpdateShoppingCart(ShoppingUtils.ShoppingCartInfo temp_cart) {
	  if (temp_cart == null) return;
	  
	  temp_shopping_cart_ = temp_cart;
	  // App has no knowledge of a current shopping cart, copy parsed cart into shopping_cart_.
	  if (shopping_cart_.revision_id_ == 0) {
	    shopping_cart_ = temp_shopping_cart_;
	    quantity_positive_shopping_cart_ = temp_shopping_cart_;
	    RemoveQuantityZeroItems();
	    FillExtraProductDetailsFromRewardsPage();
	    return;
	  }

	  // App already has a cart on hand. Compare App's version to server's version, and update
	  // whichever one is older.
	  if (temp_shopping_cart_.timestamp_ >= shopping_cart_.timestamp_) {
	    shopping_cart_ = temp_shopping_cart_;
	    quantity_positive_shopping_cart_ = temp_shopping_cart_;
	    RemoveQuantityZeroItems();
	    FillExtraProductDetailsFromRewardsPage();
	    return;
	  }
	  // App's version of cart is more up-to-date. Update server's cart.
	  Log.w("PHB TEMP", "ShoppingCartActivity::UpdateShoppingCart. Calling UpdateCart. Old timestamp: " +
	                     shopping_cart_.revision_id_ + ", new timestamp: " +
	                     temp_shopping_cart_.revision_id_);
	  UpdateCart();
	}
	
	private synchronized void UpdateCart() {
	  hay_active_cart_request_ = true;
	  // First check that we have an old cart to compare/update.
      if (temp_shopping_cart_ == null) {
    	num_server_tasks_++;
	    new GetUrlTask(this, GetUrlTask.TargetType.JSON).execute(
			jact_shopping_cart_url_, "GET", "", "", ShoppingUtils.GET_CART_TASK);
        return;
      }
      
      // Need cookies and csrf_token to update server's cart.
	  SharedPreferences user_info = getSharedPreferences(
          getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
      String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
      if (cookies.isEmpty()) {
    	String username = user_info.getString(getString(R.string.ui_username), "");
    	String password = user_info.getString(getString(R.string.ui_password), "");
        num_server_tasks_++;
        ShoppingUtils.RefreshCookies(
            this, username, password, ShoppingUtils.GET_COOKIES_THEN_UPDATE_LINE_ITEM_TASK);
        return;
      }
      String csrf_token = user_info.getString(getString(R.string.ui_csrf_token), "");
      if (csrf_token.isEmpty()) {
    	num_server_tasks_++;
        if (!ShoppingUtils.GetCsrfToken(this, cookies, ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK)) {
          num_server_tasks_--;
        }
        return;
      }
      
      // Update server's cart.
      num_server_tasks_++;
	  if (!ShoppingUtils.UpdateServerCart(this, cookies, csrf_token, temp_shopping_cart_, shopping_cart_)) {
	    num_server_tasks_--;
		dialog_ = new JactDialogFragment("Unable to update cart on server. Check connection and try again.");
		dialog_.show(getSupportFragmentManager(), "Unable_to_update_cart");
	  }
	}
	
	public synchronized void UpdateLineItemIfModified(ShoppingUtils.LineItem line_item) {
	  if (line_item == null || line_item.pid_ == 0) return;
	  
	  // First check if line_item differs from shopping_cart_. 
	  ShoppingUtils.LineItem existing_item = GetCartItem(line_item.pid_);
	  if (existing_item != null &&
		  ShoppingUtils.LineItemsAreEquivalent(line_item, existing_item)) {
	    // Found an existing item in cart. Compare it to new item; if any differences, call update
		// cart. Otherwise, existing item is up-to-date, so nothing to do.
		Log.w("PHB TEMP", "ShoppingCartActivity::UpdateLineItemIfModified. Not updating line item " +
		                  "as it is already present in cart. Line item:\n" +
				          ShoppingUtils.PrintLineItemHumanReadable(line_item));
		return;
	  }
	  
	  // No matching line-item found, else if was out of date. Update it.
	  Log.w("PHB TEMP", "ShoppingCartActivity::UpdateLineItemIfModified. Not updating line item " +
                        "as it is already present in cart. New Line item:\n" +
		                ShoppingUtils.PrintLineItemHumanReadable(line_item) + "Old line item:\n" +
                        ShoppingUtils.PrintLineItemHumanReadable(existing_item));
	  UpdateLineItem(line_item);
	}
	
	public synchronized void UpdateLineItem(ShoppingUtils.LineItem line_item) {
      hay_active_cart_request_ = true;
      // Need cookies and csrf_token to update server's cart.
   	  SharedPreferences user_info = getSharedPreferences(
         getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
      String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
      if (cookies.isEmpty()) {
   	    String username = user_info.getString(getString(R.string.ui_username), "");
   	    String password = user_info.getString(getString(R.string.ui_password), "");
   	    num_server_tasks_++;
        ShoppingUtils.RefreshCookies(
            this, username, password, ShoppingUtils.GET_COOKIES_THEN_UPDATE_LINE_ITEM_TASK +
            ShoppingUtils.TASK_CART_SEPARATOR + ShoppingUtils.PrintLineItem(line_item));
        return;
      }
      String csrf_token = user_info.getString(getString(R.string.ui_csrf_token), "");
      if (csrf_token.isEmpty()) {
    	num_server_tasks_++;
        if (!ShoppingUtils.GetCsrfToken(
        	    this, cookies, ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK +
        	    ShoppingUtils.TASK_CART_SEPARATOR + ShoppingUtils.PrintLineItem(line_item))) {
          num_server_tasks_--;
        }
        return;
      }
      
      // Update line-item in server's cart.
      num_server_tasks_++;
   	  if (!ShoppingUtils.UpdateLineItem(
   			  this, cookies, csrf_token, line_item)) {
        num_server_tasks_--;
   		dialog_ = new JactDialogFragment("Unable to update cart on server. Check connection and try again.");
   		dialog_.show(getSupportFragmentManager(), "Unable_to_update_cart");
   	  }
	}
	
	public synchronized void CreateEmptyCart() {
	  hay_active_cart_request_ = true;
      // Need cookies and csrf_token to create server's cart.
   	  SharedPreferences user_info = getSharedPreferences(
         getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
      String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
      if (cookies.isEmpty()) {
   	    String username = user_info.getString(getString(R.string.ui_username), "");
   	    String password = user_info.getString(getString(R.string.ui_password), "");
   	    num_server_tasks_++;
        ShoppingUtils.RefreshCookies(
            this, username, password, ShoppingUtils.GET_COOKIES_THEN_CREATE_CART_TASK);
        return;
      }
      String csrf_token = user_info.getString(getString(R.string.ui_csrf_token), "");
      if (csrf_token.isEmpty()) {
    	num_server_tasks_++;
        if (!ShoppingUtils.GetCsrfToken(
        	    this, cookies, ShoppingUtils.GET_CSRF_THEN_CREATE_CART_TASK)) {
          num_server_tasks_--;
        }
        return;
      }
      
      // Update line-item in server's cart.
      num_server_tasks_++;
   	  if (!ShoppingUtils.CreateServerCart(
   			  this, cookies, csrf_token, null)) {
   		dialog_ = new JactDialogFragment("Unable to update cart on server. Check connection and try again.");
   		dialog_.show(getSupportFragmentManager(), "Unable_to_update_cart");
   	  }
	}

	@Override
	public void ProcessUrlResponse(String webpage, String cookies, String extra_params) {
	  // Look for TASK_TASK_SEPARATOR. If present, store token to SharedPreferences, then
      // strip it (and things before it) out of extra_params, and do next task.
	  Log.w("PHB Temp", "ShoppingCartActivity::ProcessUrlResponse. Response:\n" + webpage);
	  num_server_tasks_--;
	  if (extra_params.equalsIgnoreCase(GET_REWARDS_PAGE_TASK)) {
	    ProductsActivity.SetProductsList(webpage);
	    FillExtraProductDetailsFromRewardsPage();
	  } else if (extra_params.equalsIgnoreCase(GET_SHIPPING_INFO_TASK)) {
		ProcessShippingInfoResponse(webpage, cookies);
	  } else if (extra_params.equalsIgnoreCase(ShoppingUtils.GET_CART_TASK)) {
		// Parse webpage to a new cart.
		ShoppingUtils.ShoppingCartInfo temp_cart = new ShoppingUtils.ShoppingCartInfo();
		if (!ShoppingUtils.ParseCartFromGetCartPage(webpage, temp_cart)) {
		  dialog_ = new JactDialogFragment("Unable to fetch cart from server. Check connection and try again.");
		  dialog_.show(getSupportFragmentManager(), "Unable_to_fetch_cart");
		return;
		}
	    UpdateShoppingCart(temp_cart);
	  } else if (extra_params.equalsIgnoreCase(ShoppingUtils.GET_COOKIES_THEN_GET_CART_TASK)) {
		SaveCookies(cookies);
		GetInitialShoppingCart();
	  } else if (extra_params.equalsIgnoreCase(ShoppingUtils.CREATE_CART_TASK)) {
		// Parse webpage to a new cart.
		ShoppingUtils.ShoppingCartInfo temp_cart = new ShoppingUtils.ShoppingCartInfo();
		if (!ShoppingUtils.ParseCartFromCreateCartPage(webpage, temp_cart)) {
		  dialog_ = new JactDialogFragment("Unable to fetch cart from server. Check connection and try again.");
		  dialog_.show(getSupportFragmentManager(), "Unable_to_fetch_cart");
		return;
		}
	    UpdateShoppingCart(temp_cart);
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
		    Log.e("PHB ERROR", "ProductsActivity::ProcessUrlResponse. Unable to parse add line-item response:\n" + webpage);
		    return;
		  } else if (extra_params.indexOf(ShoppingUtils.UPDATE_LINE_ITEM_TASK) == 0 &&
				     !ShoppingUtils.ParseLineItemFromUpdateLineItemPage(line_item, new_line_items)) {
			// TODO(PHB): Handle this error (e.g. popup warning to user).
		    Log.e("PHB ERROR", "ProductsActivity::ProcessUrlResponse. Unable to parse add line-item response:\n" + webpage);
		    return;
		  }
		  if (new_line_items.size() != 1 || !UpdateLineItemStatic(new_line_items.get(0))) {
			// TODO(PHB): Handle this error (e.g. popup warning to user).
			Log.e("PHB ERROR", "ProductsActivity::ProcessUrlResponse. Unable to parse cart response. " +
			                   "Num new line items: " + new_line_items.size() + "; Webpage response:\n" + webpage);
		  }
		  // PHB Temp.
		  Log.w("PHB TEMP", "ProductsActivity::ProcessUrlResponse. Updated line item. Now shopping cart is: " +
		                    ShoppingCartActivity.PrintCart());
		} catch (JSONException e) {
		  // TODO(PHB): Handle this error (e.g. popup warning to user).
		  Log.e("PHB ERROR", "ProductsActivity::ProcessUrlResponse. Unable to parse add line-item response " +
		                     "from server. Exception: " + e.getMessage() + "; webpage response:\n" + webpage);
		}
	  } else if (extra_params.equalsIgnoreCase(ShoppingUtils.GET_COOKIES_THEN_UPDATE_LINE_ITEM_TASK)) {
	    SaveCookies(cookies);
	    UpdateCart();
	  } else if (extra_params.equalsIgnoreCase(ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK)) {
	    SaveCsrfToken(webpage);
	    UpdateCart();
	  } else if (extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) > 0 &&
			     (extra_params.indexOf(ShoppingUtils.CREATE_CART_TASK) == 0 ||
			      extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_UPDATE_LINE_ITEM_TASK) == 0 ||
			      extra_params.indexOf(ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK) == 0)) {
	    // Create Cart was called to create a new cart/order, but the new item was not
		// yet added to the cart. Parse the response (e.g. to get order_id), and then
		// add the new item to the cart (the specifics of the item to add are in extra_params).
		String parsed_line_item = extra_params.substring(
		    extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) +
		    ShoppingUtils.TASK_CART_SEPARATOR.length());
		if (extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_UPDATE_LINE_ITEM_TASK) == 0) {
		  SaveCookies(cookies);
		} else if (extra_params.indexOf(ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK) == 0) {
		  SaveCsrfToken(webpage);
		} else if (extra_params.indexOf(ShoppingUtils.CREATE_CART_TASK) == 0) {
		  // Parse webpage to a new cart.
		  ShoppingUtils.ShoppingCartInfo temp_cart = new ShoppingUtils.ShoppingCartInfo();
		  if (!ShoppingUtils.ParseCartFromCreateCartPage(webpage, temp_cart)) {
			dialog_ = new JactDialogFragment("Unable to fetch cart from server. Check connection and try again.");
			dialog_.show(getSupportFragmentManager(), "Unable_to_fetch_cart");
			return;
		  }
		  UpdateShoppingCart(temp_cart);
		}
		UpdateLineItem(ShoppingUtils.ParseLineItem(parsed_line_item));
	  } else {
	    Log.e("PHB ERROR", "ShoppingCartActivity::ProcessUrlResponse. Returning from " +
	                       "unrecognized task:\n" + extra_params);
	  }
	  if (num_server_tasks_ == 0) {
		SetCartIcon(this);
		if (num_server_tasks_ == 0) {
		  LoadView();
		}
	  }
	}

	@Override
	public void ProcessUrlResponse(Bitmap pic, String cookies, String extra_params) {
		// TODO Auto-generated method stub.
	}

	@Override
	public void ProcessFailedResponse(FetchStatus status, String extra_params) {
	  // TODO(PHB): Handle failure cases below by more than just a Log error (e.g. popup
	  // a dialog? Try query again (depending on status)?).
      num_server_tasks_--;
	  if (status == GetUrlTask.FetchStatus.ERROR_CSRF_FAILED) {
    	String task = "";
	    if (extra_params.indexOf(ShoppingUtils.CREATE_CART_TASK) >= 0) {
	      task = ShoppingUtils.GET_CSRF_THEN_CREATE_CART_TASK;
	    } else if (extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) > 0 &&
	    		   (extra_params.indexOf(ShoppingUtils.ADD_LINE_ITEM_TASK) == 0 ||
	    		    extra_params.indexOf(ShoppingUtils.UPDATE_LINE_ITEM_TASK) == 0)) {
		  String parsed_line_item = extra_params.substring(
		    extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) +
		    ShoppingUtils.TASK_CART_SEPARATOR.length());
	      task = ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK +
	    		 ShoppingUtils.TASK_CART_SEPARATOR + parsed_line_item;
	    } else {
	      Log.e("PHB ERROR", "ShoppingCartActivity::ProcessFailedResponse. CSRF Failed. Status: " + status +
	    		             "; extra_params: " + extra_params);
	      return;
	    }
    	SharedPreferences user_info = getSharedPreferences(
            getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
        String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
        num_server_tasks_++;
	    if (!ShoppingUtils.GetCsrfToken(this, cookies, task)) {
	      num_server_tasks_--;
	    }
	  } else {
	    Log.w("PHB TEMP", "ShoppingCartActivity::ProcessFailedResponse. Status: " + status +
		                   "; extra_params: " + extra_params + ". Trying parent resolution.");
	    // ProcessFailedCartResponse will decrement num_server_tasks_, so re-increment it here so the net is no change.
	    num_server_tasks_++;
	    ProcessFailedCartResponse(status, extra_params);
	  }
	}
}