package com.example.jactfirstdemo;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.apache.http.ParseException;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;

public class ShoppingCartActivity extends JactActionBarActivity implements ProcessUrlResponseCallback {
	
	public static class CartItem {
		public String product_id_;
		public String product_title_;
		public Bitmap product_icon_;
		public String bux_;
		public String points_;
		public String usd_;
		public int quantity_;
		public boolean is_drawing_;
	}
	
	public static class JactAddress {
		public String first_name_;
		public String last_name_;
		public String street_addr_;
		public String street_addr_extra_;
		public String city_;
		public String state_;
		public String zip_;
		public String credit_card_;
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
		INVALID_PRICE,
	}
	
	public enum ItemAddedStatus {
		NEW,
		INCREMENTED,
		CART_FULL,
		ITEM_MAX,
		NO_PID
	}
	
	private static final int MAX_CART_ITEMS = 10;
	// TODO(PHB): Update string below with actual target.
	private static final String jact_billing_info_url_ = "https://us7.jact.com:3081/rest/rewards.json";
	private static final String GET_BILLING_INFO_TASK = "get_billing_info_task";
	
	private static ArrayList<CartItem> cart_items_;
	private static ArrayList<String> items_to_remove_;
	private ListView items_listview_;
	private CheckoutAdapter adapter_;
	private JactNavigationDrawer navigation_drawer_;
	private Menu menu_bar_;
	private JactDialogFragment dialog_;
	
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
	
	public ShoppingCartActivity() {
		if (cart_items_ == null) {
			cart_items_ = new ArrayList<CartItem>();
		}
		if (items_to_remove_ == null) {
			items_to_remove_ = new ArrayList<String>();
		}
	}

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState);
    setContentView(R.layout.checkout_layout);
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    // Set Navigation Drawer.
    navigation_drawer_ =
        new JactNavigationDrawer(this,
        		                 findViewById(R.id.checkout_drawer_layout),
        		                 findViewById(R.id.checkout_left_drawer),
        		                 JactNavigationDrawer.ActivityIndex.CHECKOUT_MAIN);
    SetCartItemsUI();
    fadeAllViews(false);
  }
  

  @Override
  protected void onResume() {
	  // Re-enable parent activity before transitioning to the next activity.
	  // This ensures e.g. that when user hits 'back' button, the screen
	  // is 'active' (not faded) when the user returns.
	  fadeAllViews(false);
	  if (menu_bar_ != null) {
	    SetCartIcon(menu_bar_);
	  }
	  CheckForEmptyCart();
      adapter_.notifyDataSetChanged();
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
	    RemoveCartItem(itr.next());
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
      spinner.setVisibility(View.INVISIBLE);
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
	
  public static synchronized int GetNumberCartItems() {
	if (cart_items_ == null) return -1;
	return cart_items_.size();
  }
  
  private static synchronized void GetTotalCartPrice(TotalCartPrice total) {
	if (cart_items_ == null) return;
	int bux = 0;
	int points = 0;
	int usd = 0;
	int bux_purchased = 0;
  	Iterator<CartItem> itr = cart_items_.iterator();
  	try {
  	  while (itr.hasNext()) {
  		CartItem item = itr.next();
  		if (item.quantity_ == 0) continue;
  		if (item.bux_ != null && !item.bux_.isEmpty()) {
  		  bux += item.quantity_ * NumberFormat.getNumberInstance(java.util.Locale.US).parse(item.bux_).intValue();
  		}
  		if (item.points_ != null && !item.points_.isEmpty()) {
  		  points += item.quantity_ * NumberFormat.getNumberInstance(java.util.Locale.US).parse(item.points_).intValue();
  		}
  		if (item.usd_ != null && !item.usd_.isEmpty()) {
  		  usd += item.quantity_ * NumberFormat.getNumberInstance(java.util.Locale.US).parse(item.usd_).intValue();
  		}
  		// TODO(PHB): Determine how to check if current item is a BUX bundle,
  		// and if so how much. Then += this amount to bux_purchased.
  	  }
	  total.bux_ = bux;
	  total.bux_purchased_ = bux_purchased;
	  total.points_ = points;
	  total.usd_ = usd;
  	} catch (java.text.ParseException e) {
  	  Log.e("PHB ERROR", "ShoppingCartActivity::GetTotalCartPrice. Unable to parse items:\n" + cart_items_.toString()
  			             + "\nError: " + e.getMessage());
	}
  }

  private static synchronized int GetTotalCartQuantity() {
    if (cart_items_ == null) return 0;
    int count = 0;
    Iterator<CartItem> itr = cart_items_.iterator();
    while (itr.hasNext()) {
	  CartItem item = itr.next();
	  count += item.quantity_;
    }
    return count;
  }
  
  private void SetCartItemsUI() {
	  items_listview_ = (ListView) findViewById(R.id.checkout_list);

      // Getting adapter by passing xml data ArrayList
      adapter_ = new CheckoutAdapter(this, R.layout.checkout_item, cart_items_);
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
  
  public static void SetCartIcon(Menu menu_bar) {
    if (menu_bar == null) return;
    int num_items = GetNumberCartItems();
    if (num_items >= 0) {
      menu_bar.findItem(R.id.menu_shopping_cart).setIcon(
          GetCartIconResource(num_items));
    }
  }
  
    public static synchronized CartItem GetCartItem(String product_id) {
    	if (cart_items_ == null) return null;
    	Iterator<CartItem> itr = cart_items_.iterator();
    	while (itr.hasNext()) {
    		CartItem item = itr.next(); 
    		if (item.product_id_ == product_id) {
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
    
    public static synchronized boolean RemoveCartItem(String product_id) {
    	if (cart_items_ == null) return false;
    	Iterator<CartItem> itr = cart_items_.iterator();
    	while (itr.hasNext()) {
    		CartItem item = itr.next(); 
    		if (item.product_id_ == product_id) {
    			itr.remove();
    			return true;
    		}
    	}
    	return false;
    }
    
    public static synchronized void ClearCart() {
    	if (cart_items_ == null) return;
    	cart_items_.clear();
    }
    
    public static synchronized ItemAddedStatus AddCartItem(
    		String product_id, String title, Bitmap bitmap, int bux, int points, int usd) {
    	return AddCartItem(product_id, title, bitmap, bux, points, usd, 1);
    }
    
	public static synchronized ItemAddedStatus AddCartItem(
			String product_id, String title, Bitmap bitmap, int bux, int points, int usd, int quantity) {
		CartItem item = new CartItem();
		item.product_id_ = product_id;
		item.product_title_ = title;
		item.product_icon_ = bitmap;
		item.bux_ = NumberFormat.getNumberInstance(Locale.US).format(bux);
		item.points_ = NumberFormat.getNumberInstance(Locale.US).format(points);
		item.usd_ = NumberFormat.getNumberInstance(Locale.US).format(usd);
		item.quantity_ = quantity;
		return AddCartItem(item);
	}
	
	public static synchronized ItemAddedStatus AddCartItem(CartItem item) {
		if (item == null) {
			return ItemAddedStatus.NO_PID;
		}
		if (cart_items_ == null) {
			cart_items_ = new ArrayList<CartItem>();
		}
		CartItem temp_item = GetCartItem(item.product_id_);
		if (temp_item != null) {
			if (temp_item.quantity_ > 10) {
				return ItemAddedStatus.ITEM_MAX;
			}
			(temp_item.quantity_)++;
			return ItemAddedStatus.INCREMENTED;
		}
		if (GetNumberCartItems() >= MAX_CART_ITEMS) {
			return ItemAddedStatus.CART_FULL;
		}
		cart_items_.add(item);
		if (item.product_id_.isEmpty()) {
			// TODO(PHB): Determine if items with missing PID should be allowed to be added
			// to the cart (i.e. may want to move this check above the 'cart_items_.add()'.
			return ItemAddedStatus.NO_PID;
		}
		return ItemAddedStatus.NEW;
	}
	
	public void doProceedToShippingButtonClick(View view) {
		if (GetTotalCartQuantity() == 0) {
			dialog_ = new JactDialogFragment("No Items in Cart.");
			dialog_.show(getSupportFragmentManager(), "No_items_in_cart");
		} else {
			fadeAllViews(true);
			new GetUrlTask(this, GetUrlTask.TargetType.JSON).execute(
					jact_billing_info_url_, "GET", "", "", GET_BILLING_INFO_TASK);
		}
	}

	public void doDialogOkClick(View view) {
		  // Close Dialog window.
		  dialog_.dismiss();
	}
	
	private void ProcessBillingInfoResponse(String webpage, String cookies) {
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

	@Override
	public void ProcessUrlResponse(String webpage, String cookies, String extra_params) {
		if (extra_params.equalsIgnoreCase(GET_BILLING_INFO_TASK)) {
			ProcessBillingInfoResponse(webpage, cookies);
		}
		// TODO Auto-generated method stub
	}

	@Override
	public void ProcessUrlResponse(Bitmap pic, String cookies,
			String extra_params) {
		// TODO Auto-generated method stub
	}

	@Override
	public void ProcessFailedResponse(FetchStatus status, String extra_params) {
		// TODO Auto-generated method stub
	}
}
