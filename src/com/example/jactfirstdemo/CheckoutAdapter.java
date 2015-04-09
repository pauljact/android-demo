package com.example.jactfirstdemo;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

import com.example.jactfirstdemo.ShoppingCartActivity.CartAccessResponse;
import com.example.jactfirstdemo.ShoppingUtils.Amount;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class CheckoutAdapter extends ArrayAdapter<ShoppingUtils.LineItem>
                             implements OnItemSelectedListener, AdapterWithImages {
	private ShoppingCartActivity parent_activity_;
	private ArrayList<ShoppingUtils.LineItem> items_;
	private boolean[] unfinished_list_elements_;
	public ProductsImageLoader image_loader_;
	private static LayoutInflater inflater_;
	// The relative position (child num) of the PID TextView within its parent LinearLayout.
	private static final int REL_POS_OF_PID = 4;
	
	public static class CheckoutViewHolder {
		  public TextView drawing_;
		  public TextView title_;
		  public TextView bux_;
		  public TextView price_and_;
		  public TextView points_;
		  public TextView pid_;
		  public Spinner quantity_;
		  public ImageView img_;
	}
	
	public CheckoutAdapter(ShoppingCartActivity a, int resource, ArrayList<ShoppingUtils.LineItem> items) {
		super(a, resource, items);
		parent_activity_ = a;
		items_ = items;
		inflater_ = (LayoutInflater) parent_activity_.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		unfinished_list_elements_ = new boolean[100];
        image_loader_ = new ProductsImageLoader(
        	parent_activity_.getApplicationContext(), this, "ShoppingCartActivity");
	}
	
	@Override
	public void AlertPositionsReady (HashSet<Integer> positions) {
	  if (positions == null) {
		Log.e("PHB ERROR", "CheckoutAdapter::alertPositionsReady. Null positions");
		return;
	  }
	  if (positions.isEmpty()) {
		Log.e("PHB ERROR", "CheckoutAdapter::alertPositionsReady. Empty positions");
		return;
	  }
	  boolean should_alert_state_change = false;
	  for (Integer i : positions) {
		int position = i.intValue();
		if (unfinished_list_elements_[position]) {
		  unfinished_list_elements_[position] = false;
		  should_alert_state_change = true;
		}
	  }
	  if (should_alert_state_change) {
		super.notifyDataSetChanged();
	  }
	}
	
	@Override
	public int getCount() {
		return items_.size();
	}

	@Override
	public ShoppingUtils.LineItem getItem(int position) {
		return items_.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View vi;
        if (convertView == null) {
            vi = inflater_.inflate(R.layout.checkout_item, parent, false);
            // Could look these up below this if/else block, but more efficient
            // to use CheckoutViewHolder (with get/setTag) to perform the lookup once.
            CheckoutViewHolder viewHolder = new CheckoutViewHolder();
            viewHolder.drawing_ = (TextView) vi.findViewById(R.id.checkout_drawing_title);
            viewHolder.title_ = (TextView) vi.findViewById(R.id.checkout_item_title);
            viewHolder.bux_ = (TextView) vi.findViewById(R.id.checkout_item_price);
            viewHolder.price_and_ = (TextView) vi.findViewById(R.id.checkout_price_seperator);
            viewHolder.points_ = (TextView) vi.findViewById(R.id.checkout_item_price_two);
            viewHolder.pid_ = (TextView) vi.findViewById(R.id.checkout_item_pid);
            viewHolder.img_ = (ImageView) vi.findViewById(R.id.checkout_image);
            // Setup Quantity Spinner DropDown.
            viewHolder.quantity_ = (Spinner) vi.findViewById(R.id.cart_quantity_spinner);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            		parent_activity_, R.array.quantities, R.layout.jact_spinner_item);
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(R.layout.jact_spinner_dropdown);
            // Apply the adapter to the spinner
            viewHolder.quantity_.setAdapter(adapter);
            viewHolder.quantity_.setOnItemSelectedListener((OnItemSelectedListener) this);
            vi.setTag(viewHolder);
        } else {
        	vi = convertView;
        }
 
        // Sets Text and image fields of this product.
        CheckoutViewHolder holder = (CheckoutViewHolder) vi.getTag();
        ShoppingUtils.LineItem item = items_.get(position);
        
        // Sets Product Title.
        holder.title_.setText(item.title_);
        
        // Sets Product Price.
        if (item.cost_ == null || item.cost_.isEmpty() || item.cost_.size() > 2) {
          int amount;
          if (item.cost_ == null || item.cost_.isEmpty()) {
            amount = 0;
          } else {
            amount = item.cost_.size();
          }
          Log.e("PHB ERROR", "CheckoutAdapter::getView. Product " + item.title_ +
        		             " has " + amount + " prices associated it it.");
          return null;
        }
        Iterator<Amount> cost_itr = item.cost_.iterator();
  	    double bux = -1;
  	    double points = -1;
  	    double usd = -1;
  	    while (cost_itr.hasNext()) {
  	      Amount amount = cost_itr.next();
  	      if (amount.type_ == ShoppingUtils.CurrencyCode.BUX) {
  	        bux = amount.price_;
  	      } else if (amount.type_ == ShoppingUtils.CurrencyCode.POINTS) {
    	    points = amount.price_;
  	      } else if (amount.type_ == ShoppingUtils.CurrencyCode.USD) {
    	    usd = amount.price_ / 100.0;
  	      }
  	    }
  	    // There are two currency types associated to this view; one of them
  	    // should be POINTS, the other may be BUX or USD.
  	    if (item.cost_.size() == 2) {
          holder.price_and_.setVisibility(View.VISIBLE);
          holder.points_.setVisibility(View.VISIBLE);
          holder.points_.setText(NumberFormat.getNumberInstance(Locale.US).format(points) + " Points");
      	  if (bux >= 0) {
          	holder.bux_.setText("J" + NumberFormat.getNumberInstance(Locale.US).format(bux) + " BUX");
    	  } else {
          	holder.bux_.setText("$" + NumberFormat.getNumberInstance(Locale.US).format(usd) + " USD");
    	  }
  	    } else if (usd >= 0) {
          holder.bux_.setText("$" + NumberFormat.getNumberInstance(Locale.US).format(usd) + " USD");
          holder.price_and_.setVisibility(View.GONE);
          holder.points_.setVisibility(View.GONE);
        } else if (bux >= 0) {
          holder.bux_.setText("J" + NumberFormat.getNumberInstance(Locale.US).format(bux) + " BUX");
          holder.price_and_.setVisibility(View.GONE);
          holder.points_.setVisibility(View.GONE);
        } else if (points >= 0) {
          holder.bux_.setText(NumberFormat.getNumberInstance(Locale.US).format(points) + " Points");
          holder.price_and_.setVisibility(View.GONE);
          holder.points_.setVisibility(View.GONE);
        } else {
          Log.e("PHB ERROR", "CheckoutAdapter::getView. Unable to parse price for item:\n" + item.toString());
        }
        
        // Sets PID (invisible as a view, used to find product in the Cart).
        holder.pid_.setText(Integer.toString(item.pid_));
        
        // Sets Product Image.
        if (item.product_icon_ != null) {
          holder.img_.setImageBitmap(item.product_icon_);
        } else if (item.img_url_ != null &&
                   !image_loader_.DisplayImage(item.img_url_, holder.img_, position)) {
          unfinished_list_elements_[position] = true;
        }
        
        // Sets Product Quantity.
        holder.quantity_.setSelection(item.quantity_);
        
        // Sets Drawing Date (if applicable).
        if (item.is_drawing_) {
        	holder.drawing_.setVisibility(View.VISIBLE);
        } else {
        	holder.drawing_.setVisibility(View.GONE);
        }
        
        return vi;
    }

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		// Check that the item selection action was on the quantity spinner.
		if (parent.getId() == R.id.cart_quantity_spinner) {
		  LinearLayout holder_ll = (LinearLayout) parent.getParent().getParent();
		  TextView pid_tv = (TextView) holder_ll.getChildAt(REL_POS_OF_PID);
		  int pid = 0;
		  try {
			pid = Integer.parseInt(pid_tv.getText().toString());
		  } catch (NumberFormatException e) {
		    Log.e("PHB ERROR", "CheckoutAdapter::onItemSelectd. Unable to parse PID|" +
		                       pid_tv.getText().toString() + "|as int.");
		    return;
		  }
	      ShoppingCartActivity.CartAccessResponse response = new ShoppingCartActivity.CartAccessResponse();
	      response.line_item_ = new ShoppingUtils.LineItem();
	      if (!ShoppingCartActivity.AccessCart(
	    	      ShoppingCartActivity.CartAccessType.GET_LINE_ITEM, pid, -1, "", response)) {
	    	Log.e("PHB ERROR", "CheckoutAdapter::onItemSelected. Failed Cart Access.");
	    	return;
	      }
		  ShoppingUtils.LineItem item = response.line_item_;
		  
		  // Check if the selected position matches the current quantity for this item.
		  if (item.quantity_ == position) {
		    Log.i("PHB", "CheckoutAdapter::onItemSelected. Nothing to do, as position selected (" +
		                 position + ") matches the current quantity for this item: " +
		    		     ShoppingUtils.PrintLineItemHumanReadable(item));
		    return;
		  }

		  CartAccessResponse response_two = new CartAccessResponse();
		  if (!ShoppingCartActivity.AccessCart(
			  ShoppingCartActivity.CartAccessType.ENFORCE_CART_RULES, item.pid_, position, item.type_, response_two)) {
			Log.e("PHB ERROR", "CheckoutAdapter::onItemSelected. Failed CartAccessResponse.");
			return;
		  }
		  ShoppingCartActivity.ItemToAddStatus item_status = response_two.to_add_status_;
		  if (item_status != ShoppingCartActivity.ItemToAddStatus.OK) {
			if (item_status == ShoppingCartActivity.ItemToAddStatus.INCOMPATIBLE_TYPE) {
			  parent_activity_.DisplayPopup("Unable to add item: Cart already contains items of different type.",
					                        "Clear cart or complete checkout with existing items, then try again.");
			} else if (item_status == ShoppingCartActivity.ItemToAddStatus.CART_NOT_READY) {
			  parent_activity_.DisplayPopup("Unable to adjust quantity: still fetching Cart Information from Jact.");
			} else if (item_status == ShoppingCartActivity.ItemToAddStatus.REWARDS_NOT_FETCHED) {
			  parent_activity_.DisplayPopup("Unable to adjust quantity; still fetching Product Information from Jact.");
			} else if (item_status == ShoppingCartActivity.ItemToAddStatus.MAX_QUANTITY_EXCEEDED) {
			  parent_activity_.DisplayPopup("Unable to adjust quantity",
					                        "Max quantity for this item is: " + ProductsActivity.GetMaxQuantity(item.pid_));
			} else {
			  Log.e("PHB ERROR", "CheckoutAdapter::onItemSelected. Unrecognized ItemToAddStatus: " + item_status);
			}
			return;
		  }
		  
		  if (item.quantity_ == 0) {
		    // This item had been set to quantity zero, which set it for removal from cart,
		    // but it hadn't be officially removed yet (so that item quantity could still be
		    // adjusted on Cart page). Now, we need to indicate that it shouldn't be removed,
		    // since user has now adjusted quantity to non-zero.
		    ShoppingCartActivity.RemoveItemFromToRemoveList(pid_tv.getText().toString());
		    // Manually update cart icon, since technically the item has not yet been removed from
		    // the cart yet.
		    int current_icon_index = parent_activity_.GetCartIconPos();
		    if (current_icon_index >= 0) {
		      ShoppingCartActivity.SetCartIcon(parent_activity_.menu_bar_, current_icon_index + 1);
		    }
		    parent_activity_.EnableProceedToCheckoutButton();
		  }
		  
		  item.quantity_ = position;
		  
		  // If quantity is zero, tell Shopping Cart to Remove item once it is no longer top activity.
		  if (position == 0) {
			ShoppingCartActivity.AddItemToRemove(pid_tv.getText().toString());
			// Manually update cart icon, since technically the item has not yet been re-added to
			// the cart yet.
			int current_icon_index = parent_activity_.GetCartIconPos();
		    Log.e("PHB TEMP", "CheckoutAdapter::onItemSelected. current_icon_index: " + current_icon_index);
			if (current_icon_index > 0) {
			  ShoppingCartActivity.SetCartIcon(parent_activity_.menu_bar_, current_icon_index - 1);
			}
			// Disable the "Proceed to checkout button" if setting this quantity to zero resulted in
			// an empty cart.
			if (current_icon_index == 1) {
			  parent_activity_.DisableProceedToCheckoutButton();
			}
		  }
		  
		  // Set product fading, depending on whether quantity is zero or not.
		  AlphaAnimation alpha = position == 0 ?
		      new AlphaAnimation(0.5F, 0.5F) : new AlphaAnimation(1.0F, 1.0F);
		  // The AlphaAnimation will make the line item transparent/faded.
		  alpha.setDuration(0); // Make animation instant.
		  alpha.setFillAfter(true); // Tell it to persist after the animation ends.
		  RelativeLayout layout = (RelativeLayout) holder_ll.getParent();
		  layout.startAnimation(alpha); // Add animation to the layout.
		  
		  parent_activity_.UpdateLineItem(item);
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub
	}
}
