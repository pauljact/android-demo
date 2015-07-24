package com.example.jactfirstdemo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActionBar.LayoutParams;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executors;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;
import com.example.jactfirstdemo.ShoppingCartActivity.CartAccessResponse;
import com.example.jactfirstdemo.ShoppingCartActivity.ItemToAddStatus;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ListView;
import android.widget.Spinner;

public class ProductsActivity extends JactActionBarActivity
                              implements OnItemSelectedListener, ProcessUrlResponseCallback {	
	// For Sorting.
	private SortState title_sort_;
	private SortState date_sort_;
	private SortState bux_sort_;
	private ImageView title_arrow_;
	private ImageView date_arrow_;
	private ImageView bux_arrow_;
	
	// For Filtering.
	private String filter_category_;
	private String filter_type_;
	private final String ANY_STR = " - Any - "; 
	private Filter empty_filter_;
	private Filter category_filter_;
	private Filter type_filter_;
	private Filter category_and_type_filter_;
	private boolean start_activity_w_drawings_only_;
	
	private PopupWindow product_popup_;
	private OnProductClickListener product_listener_;
	
    private ListView list_;
    private ProductsAdapter adapter_;
    private static final String activity_name_ = "ProductsActivity";
    
    private static ArrayList<ProductsPageParser.ProductItem> products_list_;
    private static ArrayList<ProductsPageParser.ProductItem> displayed_products_list_;
    private ArrayList<String> product_categories_;
    private ArrayList<String> product_types_;
    private int num_failed_requests_;
	private static final int MAX_FAILED_REQUESTS = 5;
    
    private enum SortState {
		  NONE,
		  ASC,
		  DES
	}

	public static class MessageHolder {
		String title_;
		String message_;
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState, R.string.rewards_label,
				R.layout.products_main, JactNavigationDrawer.ActivityIndex.REWARDS);
    }

    @Override
    protected void onResume() {
   	  super.onResume();

	  // Determine if we should display on Prize Drawings (calling task will have populated
	  // extra params in the Intent if so).
	  String drawings_only = getIntent().getStringExtra("to_display");
	  if (drawings_only == null || drawings_only.isEmpty() ||
		  !drawings_only.equals("prize_drawings")) {
		start_activity_w_drawings_only_ = false;
	  } else {
		start_activity_w_drawings_only_ = true;
	  }
	  SetProductsList();
	  SetHeaderBar();
	  SetFilterBar();
	  SetFilters();
   	  num_failed_requests_ = 0;
      if (ShoppingCartActivity.AccessCart(ShoppingCartActivity.CartAccessType.INITIALIZE_CART)) {
	    GetInitialShoppingCart();
	  }
      ClearSortStates();
	  filter_category_ = "";
	  filter_type_ = "";
	  boolean found_prize_drawings = false;
	  if (start_activity_w_drawings_only_) {
		// Display Prize Drawings only.
		Spinner category_dropdown = (Spinner) findViewById(R.id.products_filter_bar_category_spinner);
		for (int i = 0; i < category_dropdown.getAdapter().getCount(); ++i) {
		  String curr_item = (String) category_dropdown.getItemAtPosition(i);
		  if (curr_item.equals("Prize Drawings")) {
			category_dropdown.setSelection(i);
			found_prize_drawings = true;
			break;
		  }
		}
		// Sort Prize Drawings by date.
		if (found_prize_drawings) {
		  // We'll go ahead and try to sort by date here; but this seems to get undone by other
		  // things that onResume is doing. We have a hack below to re-do this.
		  doSortByDateClick(null);
		} else {
		  start_activity_w_drawings_only_ = false;
		}
	  } else {
		filter_category_ = "";
		filter_type_ = "";
		adapter_.getFilter().filter("");
	  }
      ShoppingCartActivity.ResetNumCsrfRequests();
      // Set Cart Icon.
  	  GetCart(this);
	  fadeAllViews(GetNumRequestsCounter() > 0);
    }
    
    @Override
    protected void onDestroy() {
    	// If necessary, free space in CacheDir by removing files (representing product images).
    	Executors.newFixedThreadPool(1).submit(new Runnable() {
			@Override
			public void run() {
				JactFileUtils.TrimDirToMaxSize(
						new File(getApplicationContext().getCacheDir(), activity_name_),
						4 * 1024 * 1024 /* 4Mb */);
			}
		});
    	super.onDestroy();
    }

    @Override
    public void fadeAllViews(boolean should_fade) {
      ProgressBar spinner = (ProgressBar) findViewById(R.id.products_progress_bar);
      AlphaAnimation alpha;
      if (should_fade) {
        spinner.setVisibility(View.VISIBLE);
        alpha = new AlphaAnimation(0.5F, 0.5F);
      } else {
        spinner.setVisibility(View.INVISIBLE);
        alpha = new AlphaAnimation(1.0F, 1.0F);
      }
      alpha.setDuration(0); // Make animation instant
      alpha.setFillAfter(true); // Tell it to persist after the animation ends
      RelativeLayout layout = (RelativeLayout) findViewById(R.id.products_content_frame);
      layout.startAnimation(alpha); // Add animation to the layout.
    }
    
    public static boolean IsProductsListInitialized() {
      return (products_list_ != null && products_list_.size() > 0);
    }
    
    public Filter getFilter() {
    	if (filter_category_.isEmpty() && filter_type_.isEmpty()) return empty_filter_;
    	else if (filter_category_.isEmpty()) return type_filter_;
    	else if (filter_type_.isEmpty()) return category_filter_;
    	else return category_and_type_filter_;
    }
    
    private void SetFilters() {
    	empty_filter_ = new Filter() {
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults filter_results = new FilterResults();
		        filter_results.values = products_list_;
		        filter_results.count = products_list_.size();
		        return filter_results;
			}

			@Override
			protected void publishResults(CharSequence constraint,
					                      FilterResults results) {
				displayed_products_list_ = (ArrayList<ProductsPageParser.ProductItem>) results.values;
				PublishResults(results.count);
			}
    	};
    	
    	category_filter_ = new Filter() {
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults filter_results = new FilterResults();   
		        ArrayList<ProductsPageParser.ProductItem> temp_list =
		            new ArrayList<ProductsPageParser.ProductItem>();
		        boolean set_results = false;
		        if (constraint != null && constraint.length() > 0) {
		          String full_constraint = constraint.toString();
		          int separator = full_constraint.indexOf(",");
		          if (separator >= 0) {
		            set_results = true;
		            String category_constraint = full_constraint.substring(0, separator);
		            int length = products_list_.size();
		            int i = 0;
		            while(i < length) {
		        	  ProductsPageParser.ProductItem item = products_list_.get(i);
		        	  if (item != null && item.node_type_ != null &&
		        		  item.node_type_.equalsIgnoreCase(category_constraint)) {
                        temp_list.add(item);
		        	  }
                      ++i;
	                }
	                filter_results.values = temp_list;
	                filter_results.count = temp_list.size();
		          }
		        }
		        if (!set_results) {
		        	filter_results.values = products_list_;
		            filter_results.count = products_list_.size();
		        }
		        return filter_results;
			}

			@Override
			protected void publishResults(CharSequence constraint,
					                      FilterResults results) {
				displayed_products_list_ = (ArrayList<ProductsPageParser.ProductItem>) results.values;
				PublishResults(results.count);
			}
    	};
    	
    	type_filter_ = new Filter() {
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults filter_results = new FilterResults();   
		        ArrayList<ProductsPageParser.ProductItem> temp_list =
		            new ArrayList<ProductsPageParser.ProductItem>();
		        boolean set_results = false;
		        if (constraint != null && constraint.length() > 0) {
		          String full_constraint = constraint.toString();
		          int separator = full_constraint.indexOf(",");
		          if (separator >= 0) {
		            set_results = true;
		            String type_constraint = full_constraint.substring(separator + 1);
			        int length = products_list_.size();
		            int i = 0;
		            while(i < length) {
		        	  ProductsPageParser.ProductItem item = products_list_.get(i);
		        	  if (item != null && item.types_ != null) {
		        	    int types_length = item.types_.size();
		        	    int j = 0;
		        	    while (j < types_length) {
		        	      if (item.types_.get(j).equalsIgnoreCase(type_constraint)) {
		        	        temp_list.add(item);
			        	    j = types_length;  // break.
		        	      }
			        	  ++j;
		        	    }
		        	  }
                      ++i;
	                }
	                filter_results.values = temp_list;
	                filter_results.count = temp_list.size();
		          }
		        }
		        if (!set_results) {
		        	filter_results.values = products_list_;
		            filter_results.count = products_list_.size();
		        }
		        return filter_results;
			}

			@Override
			protected void publishResults(CharSequence constraint,
					                      FilterResults results) {
				displayed_products_list_ = (ArrayList<ProductsPageParser.ProductItem>) results.values;
				PublishResults(results.count);
			}
    	};
    	
    	category_and_type_filter_ = new Filter() {
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults filter_results = new FilterResults();   
		        ArrayList<ProductsPageParser.ProductItem> temp_list =
		            new ArrayList<ProductsPageParser.ProductItem>();
		        boolean set_results = false;
		        if (constraint != null && constraint.length() > 0) {
		          String full_constraint = constraint.toString();
		          int separator = full_constraint.indexOf(",");
		          if (separator >= 0) {
		            set_results = true;
		            String category_constraint = full_constraint.substring(0, separator);
		            String type_constraint = full_constraint.substring(separator + 1);
			        int length = products_list_.size();
		            int i = 0;
		            while (i < length) {
		        	  ProductsPageParser.ProductItem item = products_list_.get(i);
		        	  if (item == null || item.node_type_ == null || item.types_ == null ||
			        	  !item.node_type_.equalsIgnoreCase(category_constraint)) {
		        		++i;
		        		continue;
			          }
		        	  int types_length = item.types_.size();
		        	  int j = 0;
		        	  while (j < types_length) {
		        	    if (item.types_.get(j).equalsIgnoreCase(type_constraint)) {
		        	      temp_list.add(item);
		        	      j = types_length;  // break.
		        	    }
		        	    ++j;
		        	  }
                      ++i;
	                }
	                filter_results.values = temp_list;
	                filter_results.count = temp_list.size();
		          }
		        }
		        if (!set_results) {
		        	filter_results.values = products_list_;
		            filter_results.count = products_list_.size();
		        }
		        return filter_results;
			}

			@Override
			protected void publishResults(CharSequence constraint,
					                      FilterResults results) {
				displayed_products_list_ = (ArrayList<ProductsPageParser.ProductItem>) results.values;
				PublishResults(results.count);
			}
    	};
    }
    
    private void PublishResults(int count) {
  	  adapter_.clear();
  	  adapter_.addAll(displayed_products_list_);
      if (count > 0) {
        adapter_.notifyDataSetChanged();
      } else {
    	adapter_.notifyDataSetInvalidated();
      }
    }
    
    private void SetHeaderBar() {
      // Set default sort states to 'NONE' (display is per order retrieved from url).
      title_arrow_ = (ImageView) findViewById(R.id.products_header_bar_title_arrow);
      date_arrow_ = (ImageView) findViewById(R.id.products_header_bar_date_arrow);
      bux_arrow_ = (ImageView) findViewById(R.id.products_header_bar_bux_arrow);
      ClearSortStates();
    }
    
    private void SetFilterBar() {
      Spinner category_dropdown = (Spinner) findViewById(R.id.products_filter_bar_category_spinner);
      product_categories_ = new ArrayList<String>();
      product_categories_.add(ANY_STR);
      product_types_ = new ArrayList<String>();
      product_types_.add(ANY_STR);
      int length = products_list_.size();
      int i = 0;
      while (i < length) {
	    ProductsPageParser.ProductItem item = products_list_.get(i);
	    if (item == null) {
		  ++i;
		  continue;
        }
	    if (item.node_type_ != null && !item.node_type_.isEmpty() &&
	    	!product_categories_.contains(item.node_type_)) {
	      product_categories_.add(item.node_type_);
	    }
	    if (item.types_ == null) {
	      ++i;
	      continue;
	    }
	    int types_length = item.types_.size();
	    int j = 0;
	    while (j < types_length) {
	      String type = item.types_.get(j);
	      if (!product_types_.contains(type)) {
	        product_types_.add(type);
	      }
	      ++j;
	    }
        ++i;
      }
      DropDownArrayAdapter category_adapter =
    	  new DropDownArrayAdapter(this, R.layout.drop_down_layout, R.id.dropdown_textview, product_categories_);
      category_adapter.setDropDownViewResource(R.layout.drop_down_layout_item);
      category_dropdown.setAdapter(category_adapter);
      category_dropdown.setOnItemSelectedListener((OnItemSelectedListener) this);
      // PHB NOTE: Uncomment following line to switch to a DropDown layout with a black surrounding box
      // PHBcategory_dropdown.setBackgroundResource(R.drawable.spinner_border);
      
      Spinner type_dropdown = (Spinner) findViewById(R.id.products_filter_bar_type_spinner);
      // Create an ArrayAdapter using the string array and a default spinner layout
      DropDownArrayAdapter type_adapter =
          new DropDownArrayAdapter(this, R.layout.drop_down_layout, R.id.dropdown_textview, product_types_);
      // Specify the layout to use when the list of choices appears
      type_adapter.setDropDownViewResource(R.layout.drop_down_layout_item);
      // Apply the adapter to the spinner
      type_dropdown.setAdapter(type_adapter);
      type_dropdown.setOnItemSelectedListener((OnItemSelectedListener) this);
      // PHB NOTE: Uncomment following line to switch to a DropDown layout with a black surrounding box
      // PHBtype_dropdown.setBackgroundResource(R.drawable.spinner_border);
      // TODO(PHB): Determine how we want Spinner UI to look.
      // Options:
      //   1a) Present UI for Category (Above): Pros: Has box, Cons: No 'Down' arrow
      //   1b) Same as (1a), but update R.drawable.spinner_border to include a layer-list
      //       (which implements my own Down Arrow drawable); see R.drawable.gradient_spinner for example code.
      //   2) Present UI for Type. Pros: Has Down arrow; Cons: No box
      //   3) Can control the Spinner's Popup Dropdown explicitly, by uncommenting out code below
      //      (and updating R.drawable.empty_shape to the desired drawable)
      //type_dropdown.setBackgroundResource(R.drawable.spinner_border);
      //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
    	//  SetDropdownPopupBackground(type_dropdown);
      //}
    }
    
    @TargetApi(16)
    private void SetDropdownPopupBackground(Spinner dropdown) {
      dropdown.setPopupBackgroundResource(R.drawable.empty_shape);
    }
    
    public static synchronized void SetProductsList(String webpage) {
        products_list_ = new ArrayList<ProductsPageParser.ProductItem>();
        Log.d("ProductsActivity::SetProductsList", "Rewards page response:\n" + webpage);
        ProductsPageParser.ParseRewardsPage(webpage, products_list_);
        for (ProductsPageParser.ProductItem item : products_list_) {
          Log.d("ProductsActivity::SetProductsList", "Parsed Rewards item: " + item.toString());
        }
    }
    
    private synchronized void SetProductsList() {
      // Initialize Product Popup Window.
      product_popup_ =
          new PopupWindow(((LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).
          	inflate(R.layout.product_popup, null),
          	LayoutParams.MATCH_PARENT,
          	LayoutParams.WRAP_CONTENT);

      // Get products from URL, store all product info in a hash map.
      SetProductsList(getIntent().getStringExtra("server_response"));
      displayed_products_list_ = (ArrayList<ProductsPageParser.ProductItem>) products_list_.clone();

      list_ = (ListView) findViewById(R.id.products_list);

      // Getting adapter by passing xml data ArrayList.
      adapter_ = new ProductsAdapter(
      	  this, R.layout.products_item, displayed_products_list_, activity_name_);
      list_.setAdapter(adapter_);

      // Click event for single list row
      product_listener_ = new OnProductClickListener(product_popup_);
      list_.setOnItemClickListener(product_listener_);
    }
    
    private void ClearSortStates() {
      title_sort_ = SortState.NONE;
      date_sort_ = SortState.NONE;
      bux_sort_ = SortState.NONE;
      title_arrow_.setVisibility(View.INVISIBLE);
      date_arrow_.setVisibility(View.INVISIBLE);
      bux_arrow_.setVisibility(View.INVISIBLE);
    }

    public static synchronized boolean FillItemDetails(ShoppingUtils.LineItem line_item) {
	  Log.e("PHB TEMP", "ProductsActivity::FillItemDetails. Attempting to find an item in cart matching " +
				        ShoppingUtils.PrintLineItemHumanReadable(line_item));
      if (line_item == null) {
		Log.e("ProductsActivity::FillItemDetails", "Null line_item");
		return false;
	  }
	  if (line_item.pid_ <= 0) {
		Log.e("ProductsActivity::FillItemDetails", "Bad pid: " + line_item.pid_);
		return false;
	  }
	  if (products_list_ == null) {
		Log.e("ProductsActivity::FillItemDetails", "Null products_list_");
		return false;
	  }
      for (ProductsPageParser.ProductItem item : products_list_) {
        if (item.pid_.equals(Integer.toString(line_item.pid_))) {
			Log.e("PHB TEMP", "ProductsActivity::FillItemDetails. Found matching line item: " +
			                  ProductsPageParser.PrintProductItem(item));
          line_item.type_ = item.node_type_;
          line_item.img_url_ = item.img_url_;
          try {
            line_item.entity_id_ = Integer.parseInt(item.nid_);
            line_item.max_quantity_ = Integer.parseInt(item.max_quantity_);
          } catch (NumberFormatException e) {
        	Log.e("ProductsActivity::FillItemDetails",
				  "Unable to parse a String field as an int. " + "nid: " + item.nid_ +
				  ", max quantity: " + item.max_quantity_);
        	return false;
          }
          return true;
        }
      }
	  Log.e("ProductsActivity::FillItemDetails",
			"Unable to fill item details: Couldn't find a pid (among the " + products_list_.size() +
	        " rewards fetched from server) that matches: '" + line_item.pid_ + "'");
      return false;
    }
    
    public static synchronized int GetMaxQuantity(int pid) {
      if (products_list_ == null) return -2;
      for (ProductsPageParser.ProductItem item : products_list_) {
        if (item.pid_.equals(Integer.toString(pid))) {
    	  try {
    	    return Integer.parseInt(item.max_quantity_);
    	  } catch (NumberFormatException e) {
    	    Log.e("PHB ERROR", "ProductsActivity::GetMaxQuantity. Unable to parse " +
    	                       "max quantity: " + item.max_quantity_ + " as an int.");
    	    return -2;
    	  }
        }
      }
      return -2;
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
        
	  	if (GetNumRequestsCounter() >= ShoppingUtils.MAX_OUTSTANDING_SHOPPING_REQUESTS) {
		  DisplayPopup("Unable to Reach Jact Server. Please Try Again.");
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
    
    // Implement this same way e.g. ShoppingCartActivity.CreateEmptyCart(): Do checks on cookies and
    // csrf_token. Then implement all the successful/failed responses.
    private void AddLineItem(ShoppingUtils.LineItem line_item) {
    	if (line_item == null) return;
    	Log.i("PHB TEMP", "ProductsActivity::AddLineItem. Adding line item:\n" +
    	                  ShoppingUtils.PrintLineItemHumanReadable(line_item));
    	
        // Need cookies and csrf_token to create server's cart.
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

    	// Make sure shopping cart has been initialized. If not, fetch it from server; then
    	// add the line item in a subsequent call to server.
        // UPDATE: We call InitializeOnce in onCreate(), and there is no way to reach here
        // without going through there, so no need to call InitializeOnce below.
    	/*if (ShoppingCartActivity.InitializeOnce()) {
    	  // PHB Temp: remove below log line.
    	  num_server_tasks_++;
    	  Log.e("PHB TEMP", "ProductsActivity::AddLineItem. Initializing shopping_cart_");
          GetUrlTask task = new GetUrlTask(this, GetUrlTask.TargetType.JSON);
    	  GetUrlTask.UrlParams params = new GetUrlTask.UrlParams();
    	  params.url_ = jact_shopping_cart_url_;
    	  params.connection_type_ = "GET";
    	  params.extra_info_ =
    		  ShoppingUtils.GET_CART_TASK + ShoppingUtils.TASK_CART_SEPARATOR +
  	    	  ShoppingUtils.PrintLineItem(line_item);
    	  params.cookies_ = cookies;
    	  task.execute(params);
	      return;
	    }*/
    	
    	// Shopping Cart should be initialized if we reach this point. Set order id from it.
        CartAccessResponse response = new CartAccessResponse();
		if (!ShoppingCartActivity.AccessCart(
		  ShoppingCartActivity.CartAccessType.GET_ORDER_ID, response)) {
		  Log.e("PHB ERROR", "ProductsActivity::AddLineItem. Failed to get order id.");
		  return;
		}
        line_item.order_id_ = response.order_id_;
    	Log.i("PHB TEMP", "ProductsActivity::AddLineItem. Just fetched order_id: " + line_item.order_id_);
    	
        String csrf_token = user_info.getString(getString(R.string.ui_csrf_token), "");
        if (csrf_token.isEmpty()) {
          if (!ShoppingUtils.GetCsrfToken(
              this, cookies, ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK +
              ShoppingUtils.TASK_CART_SEPARATOR + ShoppingUtils.PrintLineItem(line_item))) {
        	// Nothing to do.
          }
          return;
        }
        
        if (line_item.order_id_ > 0) {
          // Update line-item in server's cart.
     	  if (!ShoppingUtils.UpdateLineItem(this, cookies, csrf_token, line_item)) {
     		// Nothing to do.
     		//PHB_OLDDisplayPopupFragment("Unable to update cart on server. Check connection and try again.",
     		//PHB_OLD                     "Unable_to_update_cart");
     	  }
        } else {
          // Create new cart, and add line-item to it.
     	  if (!ShoppingUtils.CreateServerCart(this, cookies, csrf_token, line_item)) {
     		// Nothing to do.
     		//PHB_OLDDisplayPopupFragment("Unable to update cart on server. Check connection and try again.",
     		//PHB_OLD                     "Unable_to_update_cart_two");
     	  }
        }
    }

    public void doSortByTitleClick(View view) {
		Log.e("PHB TEMP", "ProductsActivity::SortByTitle");
	  Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	  vibe.vibrate(JactConstants.VIBRATION_DURATION);
      // Update sort states of the other sort verticals.
      SortState current = title_sort_;
      ClearSortStates();
      if (current == SortState.NONE || current == SortState.DES) {
        // Sort by titles (Ascending).
      	adapter_.sort(JactComparators.TitleComparatorAscending());
        title_sort_ = SortState.ASC;
        title_arrow_.setImageResource(R.drawable.up_arrow);
      } else if (current == SortState.ASC) {
        // Toggle to sort by titles (Descending).
      	adapter_.sort(JactComparators.TitleComparatorDescending());
        title_sort_ = SortState.DES;
        title_arrow_.setImageResource(R.drawable.down_arrow);
      }
      title_arrow_.setVisibility(View.VISIBLE);
      adapter_.ClearUnfinishedElementsTracker();
      adapter_.notifyDataSetChanged();
    }
    
    public void doSortByDateClick(View view) {
		Log.e("PHB TEMP", "ProductsActivity::SortByDate");
	  // Vibrate, if this came from a user-action (as determined by whether View is null or not).
	  if (view != null) {
		Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vibe.vibrate(JactConstants.VIBRATION_DURATION);
	  }
      // Update sort states of the other sort verticals.
      SortState current = date_sort_;
      ClearSortStates();
      if (current == SortState.NONE || current == SortState.DES) {
        // Sort by dates (Ascending).
      	adapter_.sort(JactComparators.DateComparatorAscending());
        date_sort_ = SortState.ASC;
        date_arrow_.setImageResource(R.drawable.up_arrow);
      } else if (current == SortState.ASC) {
        // Toggle to sort by dates (Descending).
      	adapter_.sort(JactComparators.DateComparatorDescending());
        date_sort_ = SortState.DES;
        date_arrow_.setImageResource(R.drawable.down_arrow);
      }
      date_arrow_.setVisibility(View.VISIBLE);
      adapter_.ClearUnfinishedElementsTracker();
      adapter_.notifyDataSetChanged();
    }
    
    public void doSortByBuxClick(View view) {
		Log.e("PHB TEMP", "ProductsActivity::SortByBux");
	  Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	  vibe.vibrate(JactConstants.VIBRATION_DURATION);
      // Update sort states of the other sort verticals.
      SortState current = bux_sort_;
      ClearSortStates();
      if (current == SortState.NONE || current == SortState.DES) {
        // Sort by buxs (Ascending).
    	adapter_.sort(JactComparators.PriceComparatorAscending());
        bux_sort_ = SortState.ASC;
        bux_arrow_.setImageResource(R.drawable.up_arrow);
      } else if (current == SortState.ASC) {
        // Toggle to sort by buxs (Descending).
      	adapter_.sort(JactComparators.PriceComparatorDescending());
        bux_sort_ = SortState.DES;
        bux_arrow_.setImageResource(R.drawable.down_arrow);
      }
      bux_arrow_.setVisibility(View.VISIBLE);
      adapter_.ClearUnfinishedElementsTracker();
      adapter_.notifyDataSetChanged();
    }
    
    public void doPopupDismissClick(View view) {
		Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vibe.vibrate(JactConstants.VIBRATION_DURATION);
		product_listener_.DismissClick();
    }

	public void doAddToCartClick(View view) {
		Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vibe.vibrate(JactConstants.VIBRATION_DURATION);
		ShoppingUtils.LineItem item = new ShoppingUtils.LineItem();
		MessageHolder holder = new MessageHolder();
		if (!doAddToCartClick(product_listener_, item, holder)) {
			if (holder.title_ != null && !holder.title_.isEmpty()) {
				if (holder.message_ != null && !holder.message_.isEmpty()) {
					DisplayPopupFragment(holder.title_, holder.message_, "Bad_Add_Product_Dialog");
				} else {
					DisplayPopupFragment(holder.title_, "Bad_Add_Product_Dialog");
				}
			} else if (holder.message_ != null && !holder.message_.isEmpty()) {
				DisplayPopupFragment(holder.message_, "Bad_Add_Product_Dialog");
			} else {
				Log.e("ProductsActivity::doAddToCartClick", "Unrecognized failed return.");
			}
		} else {
			fadeAllViews(true);
			Log.e("PHB TEMP", "ProductsActivity::doAddToCartClick. Adding item: " +
					ShoppingUtils.PrintLineItemHumanReadable(item));
			AddLineItem(item);
		}
	}

	static public boolean doAddToCartClick(
			OnProductClickListener listener, ShoppingUtils.LineItem item, MessageHolder holder) {
    	// Add item to cart.
    	// Get the product details that are available from the Product Popup View (pid, title, etc.).
    	ShoppingCartActivity.ItemStatus item_status = listener.GetProductDetails(item);
    	Log.i("PHB TEMP", "ProductsActivity::doAddToCartClick. Item: " +
    	                  ShoppingUtils.PrintLineItemHumanReadable(item));
    	if (item_status != ShoppingCartActivity.ItemStatus.VALID) {
    	  String message = "";
    	  if (item_status == ShoppingCartActivity.ItemStatus.INVALID_PRICE) {
    		  message = "Invalid Price";
    	  } else if (item_status == ShoppingCartActivity.ItemStatus.NO_PID) {
    		  message = "No Product Id";
    	  } else if (item_status == ShoppingCartActivity.ItemStatus.NO_PRICE) {
    		  message = "No Price";
    	  } else if (item_status == ShoppingCartActivity.ItemStatus.NO_TITLE) {
    		  message = "No Product Title";
    	  }
	      listener.DismissClick();
  		  Log.e("PHB ERROR", "ProductsActivity::doAddToCartClick. Unable to add item to cart: " +
				  "Message: " + message + ", Item Status: " + item_status +
				  "\nLine Item: " + ShoppingUtils.PrintLineItem(item));
		  holder.message_ = message;
	      return false;
    	}
    	
    	// Set quantity to '1'. Below, GetCartItem(item.pid_) will adjust this appropriately
    	// if item is already in cart (i.e., it will increment quantity by one).
    	ShoppingCartActivity.CartAccessResponse quantity_response =
    		new ShoppingCartActivity.CartAccessResponse();
    	if (!ShoppingCartActivity.AccessCart(
    		    ShoppingCartActivity.CartAccessType.GET_PRODUCT_QUANTITY, item.pid_, quantity_response)) {
    	  Log.e("PHB ERROR", "ProductsActivity::doAddToCartClick. Failed to get quantity.");
    	  return false;
    	}
    	if (quantity_response.num_cart_items_ == -1) {
    	  item.quantity_ = 1;
    	} else {
    	  item.quantity_ = quantity_response.num_cart_items_ + 1;
    	}
    	// Fill in Item details that are not available from the Product Popup View
    	// (e.g. max_quantity, node_id and product_type).
    	if (!FillItemDetails(item)) {
    		// TODO(PHB): Handle this.
    		Log.e("ProductsActivity::doAddToCartClick",
				  "Unable to FillItemDetails for line item: " +
				  ShoppingUtils.PrintLineItemHumanReadable(item));
    		return false;
    	}
    	
    	// Get Item_to_add_status.
    	ShoppingCartActivity.CartAccessResponse response = new ShoppingCartActivity.CartAccessResponse();
    	if (!ShoppingCartActivity.AccessCart(
    		    ShoppingCartActivity.CartAccessType.ITEM_TO_ADD_STATUS, item, response)) {
    	  Log.e("PHB ERROR", "ProductsActivity::doAddToCartClick. Failed Cart Access.");
    	  return false;
    	}
    	ItemToAddStatus add_status = response.to_add_status_;
    	Log.e("PHB TEMP", "ProductsActivity::OnClick. add_status: " + add_status);
    	String title = "";
    	String message = "";
    	if (add_status == ItemToAddStatus.CART_FULL) {
    		title = "Unable to Add Item: Cart Full";
    		message = "Only 10 distinct products allowed at a time; Checkout with existing items or remove items";
    	} else if (add_status == ItemToAddStatus.ITEM_MAX) {
    		title = "Unable to Add Item";
    		message = "You've reached the maximum quantity (10) for this product";
    	} else if (add_status == ItemToAddStatus.NO_PID) {
    		title = "Unable to Add Item";
    		message = "Product Id not recognized; Try again later";
    	} else if (add_status == ItemToAddStatus.INCREMENTED) {
  	      ShoppingCartActivity.CartAccessResponse response_two = new ShoppingCartActivity.CartAccessResponse();
  	      response_two.line_item_ = new ShoppingUtils.LineItem();
  	      if (!ShoppingCartActivity.AccessCart(
  	    	      ShoppingCartActivity.CartAccessType.GET_LINE_ITEM, item.pid_, -1, "", response_two)) {
  	    	Log.e("PHB ERROR", "ProductsActivity::onItemSelected. Failed Cart Access.");
  	    	return false;
  	      }
		  ShoppingUtils.CopyLineItem(response_two.line_item_, item);
		  item.quantity_++;
		  Log.i("PHB TEMP", "ProductsActivity::doAddToCartButton. Incrementing to: " + item.quantity_);
		  listener.DismissClick();
    	  holder.title_ = "Item Added to Cart";
    	  holder.message_ = "Item already existed in cart; increased quantity for this product by one";
		  return true;
    	} else if (add_status == ItemToAddStatus.CART_NOT_READY) {
    	  title = "Unable to Add Item";
    	  message = "Still communicating with Jact for Cart.";
    	} else if (add_status == ItemToAddStatus.MAX_QUANTITY_EXCEEDED) {
      	  title = "Unable to Add Item";
      	  message = "Max quantity for this item is: " + GetMaxQuantity(item.pid_);
    	} else if (add_status == ItemToAddStatus.INCOMPATIBLE_TYPE) {
      	  title = "Unable to Add Item";
      	  message = "Cart already contains items of different type. " +
      	            "Clear cart or complete checkout with existing items.";
    	} else if (add_status == ItemToAddStatus.REWARDS_NOT_FETCHED) {
      	  title = "Unable to Add Item";
      	  message = "Still communicating with Jact to get product details.";
    	} else if (add_status == ItemToAddStatus.EXPIRED_DATE) {
      	  title = "Unable to Add Item";
          message = "Drawing Date has passed.";
    	} else if (add_status == ItemToAddStatus.NO_DATE) {
    	  title = "Unable to Add Item";
      	  message = "Unable to find a Drawing Date for this product.";
    	} else if (add_status == ItemToAddStatus.OK) {
    		//PHBShoppingUtils.LineItem line_item = ShoppingCartActivity.GetCartItem(item.pid_);
    		//PHBAddLineItem(line_item);
    		Log.i("PHB TEMP", "ProductsActivity::doAddToCartButton. Adding new item.");
			listener.DismissClick();
    		holder.title_ = "Item Added to Cart";
			return true;
    	} else {
    	  Log.e("PHB ERROR", "ProductsActivity::doAddToCartClick. Unrecognized ItemToAddStatus: " + add_status);
    	}
    	
    	// Dismiss Product popup.
    	listener.DismissClick();
    	
    	// Set Cart Icon.
    	//PHB TEMP: This isn't reaady to be set yet;still need server's response.ShoppingCartActivity.SetCartIcon(menu_bar_);
    	
    	// Display popup alerting user of the results of the attempt to add item to cart.
    	Log.w("PHB ERROR", "ProductsActivity::doAddToCartClick. Unable to add item to cart. " +
				           "Title of Error: " + title + ", message: " + message +
				           "\nLine Item: " + ShoppingUtils.PrintLineItem(item));
		holder.title_ = title;
		holder.message_ = message;
		return false;
    }

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		Log.e("PHB TEMP", "ProductsActivity::onItemSelected");
		if (parent.getId() == R.id.products_filter_bar_category_spinner) {
		  String selected_text = (String) parent.getItemAtPosition(position);
		  if (selected_text.equalsIgnoreCase(ANY_STR)) {
			  filter_category_ = "";
		  } else {
			  filter_category_ = selected_text;
		  }
		} else if (parent.getId() == R.id.products_filter_bar_type_spinner) {
			  String selected_text = (String) parent.getItemAtPosition(position);
			  if (selected_text.equalsIgnoreCase(ANY_STR)) {
				  filter_type_ = "";
			  } else {
				  filter_type_ = selected_text;
			  }
		} else {
		  Log.e("PHB ERROR", "ProductsActivity::onItemSelected. Unknown Spinner click: Position: " + position +
				     ", id: " + id + ", item: " + parent.getItemAtPosition(position) +
				     ", item id: " + parent.getItemIdAtPosition(position) + ", parent_id:\n" + parent.getId()
				     + "\nCategory Spinner id: " + R.id.products_filter_bar_category_spinner +
				     ", Type Spinner id: " + R.id.products_filter_bar_type_spinner);
		  return;
		}
	    adapter_.getFilter().filter(filter_category_ + "," + filter_type_);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}

	@Override
	public void ProcessUrlResponse(String webpage, String cookies, String extra_params) {
	  DecrementNumRequestsCounter();
	  Log.i("ProductsActivity::ProcessUrlResponse", "Completed task: " + extra_params);
	  if (extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_GET_CART_TASK) == 0) {
		SaveCookies(cookies);
		GetInitialShoppingCart();
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
		  if (new_line_items.size() != 1 ||
			  !ShoppingCartActivity.AccessCart(ShoppingCartActivity.CartAccessType.UPDATE_LINE_ITEM,
					                           new_line_items.get(0))) {
			// TODO(PHB): Handle this error (e.g. popup warning to user).
			Log.e("PHB ERROR", "ProductsActivity::ProcessUrlResponse. Unable to parse cart response. " +
			                   "Num new line items: " + new_line_items.size() + "; Webpage response:\n" + webpage);
			return;
		  }
		  // PHB Temp.
		  //Log.w("PHB TEMP", "ProductsActivity::ProcessUrlResponse. Updated line item. Now shopping cart is: " +
		  //                  ShoppingCartActivity.PrintCart());
		  DisplayPopupFragment("Added Item to Cart", "Finally_added_item");
    	  SetCartIcon(this);
    	  fadeAllViews(false);
		} catch (JSONException e) {
		  // TODO(PHB): Handle this error (e.g. popup warning to user).
		  Log.e("PHB ERROR", "ProductsActivity::ProcessUrlResponse. Unable to parse add line-item response " +
		                     "from server. Exception: " + e.getMessage() + "; webpage response:\n" + webpage);
		  return;
		}
	  } else if (extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) > 0 &&
			     (extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_UPDATE_LINE_ITEM_TASK) == 0 ||
			      extra_params.indexOf(ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK) == 0 ||
			      extra_params.indexOf(ShoppingUtils.CREATE_CART_TASK) == 0 ||
			      extra_params.indexOf(ShoppingUtils.GET_CART_TASK) == 0)) {
		String parsed_line_item = extra_params.substring(
		    extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) +
		    ShoppingUtils.TASK_CART_SEPARATOR.length());
		if (extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_UPDATE_LINE_ITEM_TASK) == 0) {
		  SaveCookies(cookies);
		} else if (extra_params.indexOf(ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK) == 0) {
		  SaveCsrfToken(webpage);
		} else if (extra_params.indexOf(ShoppingUtils.GET_CART_TASK) == 0) {
		  if (!ShoppingCartActivity.AccessCart(ShoppingCartActivity.CartAccessType.SET_CART_FROM_WEBPAGE, webpage)) {
			// TODO(PHB): Handle this gracefully (pop-up a dialog).
			Log.e("PHB ERROR", "ProductsActivity::ProcessUrlResponse. Unable to parse cart response:\n" + webpage);
			return;
		  }
		} else if (extra_params.indexOf(ShoppingUtils.CREATE_CART_TASK) == 0) {
		  if (!ShoppingCartActivity.AccessCart(ShoppingCartActivity.CartAccessType.SET_CART_FROM_WEBPAGE, webpage)) {
		    // TODO(PHB): Handle this gracefully (pop-up a dialog).
	        Log.e("PHB ERROR", "ProductsActivity::ProcessUrlResponse. Create_cart: Unable to parse cart response:\n" + webpage);
		    return;
		  }
		}
		AddLineItem(ShoppingUtils.ParseLineItem(parsed_line_item));
	  } else if (extra_params.indexOf(ShoppingUtils.GET_CART_TASK) == 0) {
		  // Get Cart Task will be handled by super-class (JactActionBarActivity).
		  // As a hack, we also Sort here by Prize Drawing date (if necessary): we do this here
		  // because:
		  //  1) Doing it onResume (where it naturally belongs) doesn't work, as asynchronous nature
		  //     of some of the other stuff that is happening will undo the desired sorting
		  //  2) Waiting for a callback (e.g. this one) should give all the other things
		  //     (those that undo the sorting) a chance to finish; and this callback will only be
		  //     hit once (which makes it a good candidate for the hack, since we only want to
		  //     do the special sorting once).
		  if (start_activity_w_drawings_only_) {
			start_activity_w_drawings_only_ = false;
			// We want to sort via Ascending state; it's possible that SortState indicates Ascending
			// state, even though it's not really. If so, trigger a sort twice (first will send it
			// to descending, then to ascending.
			if (date_sort_ == SortState.ASC) {
			  doSortByDateClick(null);
			  doSortByDateClick(null);
			} else {
			  doSortByDateClick(null);
			}
		  }
		  IncrementNumRequestsCounter();
		  ProcessCartResponse(this, webpage, cookies, extra_params);
		  return;
	  } else {
	    Log.w("ProductsActivity::ProcessUrlResponse",
			  "Returning from unrecognized task; trying parent processing...: " +
			  GetUrlTask.PrintExtraParams(extra_params));
	    // ProcessCartResponse below is going to decrement num_server_tasks_. Artificially
	    // increase it here, so that there is a zero net effect.
	    IncrementNumRequestsCounter();
	    ProcessCartResponse(this, webpage, cookies, extra_params);
	    return;
	  }
	  if (GetNumRequestsCounter() == 0) {
		SetCartIcon(this);
		if (GetNumRequestsCounter() == 0) {
		  fadeAllViews(false);
		}
	  }
	}

	@Override
	public void ProcessUrlResponse(Bitmap pic, String cookies, String extra_params) {
		// TODO Auto-generated method stub.
	}

	@Override
	public void ProcessFailedResponse(FetchStatus status, String extra_params) {
	  num_failed_requests_++;
	  DecrementNumRequestsCounter();
	  if (status == GetUrlTask.FetchStatus.ERROR_CSRF_FAILED) {
		if (extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) > 0 &&
			(extra_params.indexOf(ShoppingUtils.CREATE_CART_TASK) == 0 ||
			 extra_params.indexOf(ShoppingUtils.ADD_LINE_ITEM_TASK) == 0 ||
			 extra_params.indexOf(ShoppingUtils.UPDATE_LINE_ITEM_TASK) == 0)) {
		  String parsed_line_item = extra_params.substring(
		      extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) +
		      ShoppingUtils.TASK_CART_SEPARATOR.length());
	      String task = ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK +
	    	  ShoppingUtils.TASK_CART_SEPARATOR + parsed_line_item;
    	  SharedPreferences user_info = getSharedPreferences(
              getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
          String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
	      if (!ShoppingUtils.GetCsrfToken(this, cookies, task)) {
	        // Nothing to do.
	      }
		} else {
		  Log.e("PHB TEMP", "ProductsActivity::ProcessFailedResponse. Status: " + status +
                            "; extra_params: " + GetUrlTask.PrintExtraParams(extra_params));
		}
	  } else if (status == GetUrlTask.FetchStatus.ERROR_RESPONSE_CODE ||
			     status == GetUrlTask.FetchStatus.ERROR_OUTPUT_STREAM_ERROR ||
			     status == GetUrlTask.FetchStatus.ERROR_UNABLE_TO_CONNECT) {
		// Failed to Connect with Jact Server. Retry, if we haven't had too many consecutive failures.
		if (num_failed_requests_ >= MAX_FAILED_REQUESTS) {
		  DisplayPopupFragment("Unable to Reach Jact",
			  	               "Your last action may not have been processed.",
					           "Failed_server_connection");
		}
		if (extra_params.indexOf(ShoppingUtils.GET_CART_TASK) >= 0 ||
				   extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_GET_CART_TASK) >= 0) {
		  GetInitialShoppingCart();
		} else if (extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) > 0 &&
	    		   (extra_params.indexOf(ShoppingUtils.ADD_LINE_ITEM_TASK) == 0 ||
	    		    extra_params.indexOf(ShoppingUtils.UPDATE_LINE_ITEM_TASK) == 0 ||
	    		    extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_UPDATE_LINE_ITEM_TASK) >= 0)) {
		  String parsed_line_item = extra_params.substring(
					    extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) +
					    ShoppingUtils.TASK_CART_SEPARATOR.length());
		  AddLineItem(ShoppingUtils.ParseLineItem(parsed_line_item));
		} else if (extra_params.indexOf(ShoppingUtils.GET_CSRF_THEN_CREATE_CART_TASK) >= 0 ||
				   extra_params.indexOf(ShoppingUtils.GET_CSRF_THEN_CLEAR_CART_TASK) >= 0 ||
				   extra_params.indexOf(ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK) >= 0) {
	      SharedPreferences user_info = getSharedPreferences(
	          getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
	      String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
	      ShoppingUtils.GetCsrfToken(this, cookies, extra_params);
		} else {
		  Log.e("ShoppingCartActivity::ProcessFailedResponse",
				"Unrecognized task: " + GetUrlTask.PrintExtraParams(extra_params));
		}
	  } else if (extra_params.indexOf(ShoppingUtils.GET_CART_TASK) == 0) {
		  // Get Cart Task will be handled by super-class (JactActionBarActivity).
		  // As a hack, we also Sort here by Prize Drawing date (if necessary): we do this here
		  // because:
		  //  1) Doing it onResume (where it naturally belongs) doesn't work, as asynchronous nature
		  //     of some of the other stuff that is happening will undo the desired sorting
		  //  2) Waiting for a callback (e.g. this one) should give all the other things
		  //     (those that undo the sorting) a chance to finish; and this callback will only be
		  //     hit once (which makes it a good candidate for the hack, since we only want to
		  //     do the special sorting once).
		  if (start_activity_w_drawings_only_) {
			  start_activity_w_drawings_only_ = false;
			  // We want to sort via Ascending state; it's possible that SortState indicates Ascending
			  // state, even though it's not really. If so, trigger a sort twice (first will send it
			  // to descending, then to ascending.
			  if (date_sort_ == SortState.ASC) {
				  doSortByDateClick(null);
				  doSortByDateClick(null);
			  } else {
				  doSortByDateClick(null);
			  }
		  }
		  IncrementNumRequestsCounter();
		  ProcessFailedCartResponse(this, status, extra_params);
		  return;
	  } else {
	    Log.w("PHB TEMP", "ProductsActivity::ProcessFailedResponse. Status: " + status +
		                   "; extra_params: " + GetUrlTask.PrintExtraParams(extra_params) +
				           ". Attempting parent resolution.");
	    // ProcessFailedCartResponse will decrement num_server_tasks_, so re-increment it here so the net is no change.
	    IncrementNumRequestsCounter();
	    ProcessFailedCartResponse(this, status, extra_params);
		return;
	  }
	  if (GetNumRequestsCounter() == 0) {
		SetCartIcon(this);
		if (GetNumRequestsCounter() == 0) {
		  fadeAllViews(false);
		}
	  }
	}
	
	@Override
	public void DisplayPopup(String message) {
	  DisplayPopupFragment(message, "too_many_server_requests");
	}
	
}