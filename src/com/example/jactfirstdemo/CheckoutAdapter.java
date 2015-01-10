package com.example.jactfirstdemo;

import java.util.ArrayList;

import com.example.jactfirstdemo.ShoppingCartActivity.CartItem;

import android.app.Activity;
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

public class CheckoutAdapter extends ArrayAdapter<CartItem> implements OnItemSelectedListener {
	private Activity parent_activity_;
	private ArrayList<CartItem> items_;
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
	
	public CheckoutAdapter(Activity a, int resource, ArrayList<CartItem> items) {
		super(a, resource, items);
		parent_activity_ = a;
		items_ = items;
		inflater_ = (LayoutInflater) parent_activity_.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public int getCount() {
		return items_.size();
	}

	@Override
	public CartItem getItem(int position) {
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
            // Setup Quanity Spinner DropDown.
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
        CartItem item = items_.get(position);
        CheckoutViewHolder holder = (CheckoutViewHolder) vi.getTag();
        
        // Sets Product Title.
        holder.title_.setText(item.product_title_);
        
        // Sets Product Price.
        if (!item.usd_.isEmpty()) {
        	holder.bux_.setText("$" + item.usd_ + " USD");
        	holder.price_and_.setVisibility(View.GONE);
        	holder.points_.setVisibility(View.GONE);
        } else if (!item.bux_.isEmpty()) {
        	if (!item.points_.isEmpty()) {
            	holder.bux_.setText("J" + item.bux_ + " BUX");
            	holder.price_and_.setVisibility(View.VISIBLE);
            	holder.points_.setVisibility(View.VISIBLE);
            	holder.points_.setText(item.points_ + " Points");
        	} else {
            	holder.bux_.setText("J" + item.bux_ + " BUX");
            	holder.price_and_.setVisibility(View.GONE);
            	holder.points_.setVisibility(View.GONE);
        	}
        } else if (!item.points_.isEmpty()) {
        	holder.bux_.setText(item.points_ + " Points");
        	holder.price_and_.setVisibility(View.GONE);
        	holder.points_.setVisibility(View.GONE);
        } else {
        	Log.e("PHB ERROR", "CheckoutAdapter::getView. Unable to parse price for item:\n" + item.toString());
        }
        
        // Sets PID (invisible as a view, used to find product in the Cart).
        holder.pid_.setText(item.product_id_);
        
        // Sets Product Image.
        holder.img_.setImageBitmap(item.product_icon_);
        
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
		  CartItem item = ShoppingCartActivity.GetCartItem(pid_tv.getText().toString());
		  item.quantity_ = position;
		  
		  // If quantity is zero, tell Shopping Cart to Remove item once it is no longer top activity.
		  if (position == 0) {
			  ShoppingCartActivity.AddItemToRemove(pid_tv.getText().toString());
		  }
		  
		  // Set product fading, depending on whether quantity is zero or not.
		  AlphaAnimation alpha = position == 0 ?
		      new AlphaAnimation(0.5F, 0.5F) : new AlphaAnimation(1.0F, 1.0F);
		  // The AlphaAnimation will make the whole content frame transparent
		  // (so that none of the views show).
		  alpha.setDuration(0); // Make animation instant.
		  alpha.setFillAfter(true); // Tell it to persist after the animation ends.
		  RelativeLayout layout = (RelativeLayout) holder_ll.getParent();
		  layout.startAnimation(alpha); // Add animation to the layout.
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub
	}
}
