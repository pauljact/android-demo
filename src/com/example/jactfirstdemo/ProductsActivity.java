package com.example.jactfirstdemo;

import android.annotation.TargetApi;
import android.app.ActionBar.LayoutParams;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;

import com.example.jactfirstdemo.ShoppingCartActivity.ItemAddedStatus;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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

public class ProductsActivity extends JactActionBarActivity implements OnItemSelectedListener {
	private enum SortState {
	  NONE,
	  ASC,
	  DES
	}
	
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
	
	private PopupWindow product_popup_;
	private OnProductClickListener product_listener_;
	
    private ListView list_;
    private ProductsAdapter adapter_;
    private static final String activity_name_ = "ProductsActivity";
    
    private ArrayList<ProductsPageParser.ProductItem> products_list_;
    private ArrayList<ProductsPageParser.ProductItem> displayed_products_list_;
    private ArrayList<String> product_categories_;
    private ArrayList<String> product_types_;
    
    private JactNavigationDrawer navigation_drawer_;
    private Menu menu_bar_;
    private JactDialogFragment dialog_;
 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.products_main);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SetProductsList();
        SetHeaderBar();
        SetFilterBar();
        SetFilters();

        // Initialize Navigation Drawer.
        navigation_drawer_ =
            new JactNavigationDrawer(this,
            		                 findViewById(R.id.products_drawer_layout),
            		                 findViewById(R.id.products_left_drawer),
            		                 JactNavigationDrawer.ActivityIndex.REWARDS);
    }

    @Override
    protected void onResume() {
      ClearSortStates();
      filter_category_ = "";
      filter_type_ = "";
      adapter_.getFilter().filter("");
      //PHBadapter_.notifyDataSetChanged();
  	  // Re-enable parent activity before transitioning to the next activity.
  	  // This ensures e.g. that when user hits 'back' button, the screen
  	  // is 'active' (not faded) when the user returns.
  	  fadeAllViews(false);
      // Set Cart Icon.
  	  ShoppingCartActivity.SetCartIcon(menu_bar_);
 	  super.onResume();
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
  	  ShoppingCartActivity.SetCartIcon(menu_bar_);
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
    
    private void SetProductsList() {
      // Initialize Product Popup Window.
      product_popup_ =
          new PopupWindow(((LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).
          	inflate(R.layout.product_popup, null),
          	LayoutParams.MATCH_PARENT,
          	LayoutParams.WRAP_CONTENT);

      // Get products from URL, store all product info in a hash map.
      products_list_ = new ArrayList<ProductsPageParser.ProductItem>();
      ProductsPageParser.ParseRewardsPage(
          getIntent().getStringExtra("server_response"), products_list_);
      //PHBdisplayed_products_list_ = new ArrayList<ProductsPageParser.ProductItem>();
      displayed_products_list_ = (ArrayList<ProductsPageParser.ProductItem>) products_list_.clone();

      list_ = (ListView) findViewById(R.id.products_list);

      // Getting adapter by passing xml data ArrayList
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

    public void doSortByTitleClick(View view) {
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
      // Update sort states of the other sort verticals.
      SortState current = bux_sort_;
      ClearSortStates();
      if (current == SortState.NONE || current == SortState.DES) {
        // Sort by buxs (Ascending).
    	adapter_.sort(JactComparators.BuxComparatorAscending());
        bux_sort_ = SortState.ASC;
        bux_arrow_.setImageResource(R.drawable.up_arrow);
      } else if (current == SortState.ASC) {
        // Toggle to sort by buxs (Descending).
      	adapter_.sort(JactComparators.BuxComparatorDescending());
        bux_sort_ = SortState.DES;
        bux_arrow_.setImageResource(R.drawable.down_arrow);
      }
      bux_arrow_.setVisibility(View.VISIBLE);
      adapter_.ClearUnfinishedElementsTracker();
      adapter_.notifyDataSetChanged();
    }
    
    public void doPopupDismissClick(View view) {
    	product_listener_.DismissClick();
    }
    
    public void doDialogOkClick(View view) {
  	  // Close Dialog window.
  	  dialog_.dismiss();
    }
    
    public void doAddToCartClick(View view) {
    	// Add item to cart.
    	ShoppingCartActivity.CartItem details = new ShoppingCartActivity.CartItem();
    	ShoppingCartActivity.ItemStatus item_status = product_listener_.GetProductDetails(details);
    	if (item_status != ShoppingCartActivity.ItemStatus.VALID) {
    	  String message = "";
    	  if (item_status == ShoppingCartActivity.ItemStatus.INVALID_PRICE) {
    		  message = "Invalid Price";
    	  } else if (item_status == ShoppingCartActivity.ItemStatus.NO_PID) {
    		  message = "No Product Id";
    	  } else if (item_status == ShoppingCartActivity.ItemStatus.NO_PRICE) {
    		  message = "Invalid Price";
    	  } else if (item_status == ShoppingCartActivity.ItemStatus.NO_TITLE) {
    		  message = "No Product Title";
    	  }
	      product_listener_.DismissClick();
	      dialog_ = new JactDialogFragment("Unable to Add Item: " + message);
	      dialog_.show(getSupportFragmentManager(), "Bad_Add_Product_Dialog");
	      return;
    	}
    	
    	// Set quantity to '1'. Below, AddCartItem(details) will adjust this appropriately
    	// if item is already in cart (i.e., it will increment quantity by one).
    	details.quantity_ = 1;
    	ItemAddedStatus add_status = ShoppingCartActivity.AddCartItem(details);
    	String title = "";
    	String message = "";
    	if (add_status == ItemAddedStatus.CART_FULL) {
    		title = "Unable to Add Item: Cart Full";
    		message = "Only 10 distinct products allowed at a time; Checkout with existing items or remove items";
    	} else if (add_status == ItemAddedStatus.ITEM_MAX) {
    		title = "Unable to Add Item";
    		message = "You've reached the maximum quantity (10) for this product";
    	} else if (add_status == ItemAddedStatus.NO_PID) {
    		title = "Unable to Add Item";
    		message = "Product Id not recognized; Try again later";
    	} else if (add_status == ItemAddedStatus.INCREMENTED) {
    		title = "Item Added to Cart";
    		message = "Item already existed in cart; increased quantity for this product by one";
    	} else if (add_status == ItemAddedStatus.NEW) {
    		title = "Item Added to Cart";
    	}
    	
    	// Dismiss Product popup.
    	product_listener_.DismissClick();    	
    	
    	// Set Cart Icon.
    	ShoppingCartActivity.SetCartIcon(menu_bar_);
    	
    	// Display popup alerting user of the results of the attempt to add item to cart.
    	if (!title.isEmpty()) {
    	      dialog_ = new JactDialogFragment(title, message);
    	      dialog_.show(getSupportFragmentManager(), "Bad_Add_Product_Dialog");
      	}
    }

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
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
}