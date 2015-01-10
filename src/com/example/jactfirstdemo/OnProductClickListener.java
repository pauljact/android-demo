package com.example.jactfirstdemo;

import com.example.jactfirstdemo.ProductsAdapter.ProductViewHolder;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.PopupWindow;

public class OnProductClickListener implements OnItemClickListener {
	private PopupWindow product_popup_;
	private int popup_height_;
	
	static class PopupWindowViewHolder {
	  public TextView drawing_;
	  public TextView title_;
	  public TextView date_;
	  public TextView bux_;
	  public TextView points_;
	  public TextView pid_;
	  public ImageView img_;	  	
	}
	
	private PopupWindowViewHolder view_holder_;
	
	public OnProductClickListener(PopupWindow p) {
		product_popup_ = p;
		popup_height_ = 0;
		
		View vi = product_popup_.getContentView();
		view_holder_ = new PopupWindowViewHolder();
        view_holder_.drawing_ = (TextView) vi.findViewById(R.id.product_popup_text_drawing);
        view_holder_.title_ = (TextView) vi.findViewById(R.id.product_popup_text_title);
        view_holder_.date_ = (TextView) vi.findViewById(R.id.product_popup_text_date);
        view_holder_.bux_ = (TextView) vi.findViewById(R.id.product_popup_text_bux);
        view_holder_.points_ = (TextView) vi.findViewById(R.id.product_popup_text_points);
        view_holder_.pid_ = (TextView) vi.findViewById(R.id.product_popup_text_pid);
        view_holder_.img_ = (ImageView) vi.findViewById(R.id.product_popup_img);
        vi.setTag(view_holder_);
        if (view_holder_.title_ == null || view_holder_.date_ == null ||
            view_holder_.bux_ == null || view_holder_.img_ == null || view_holder_.pid_ == null) {
        	// TODO(PHB): Gracefully handle this.
        	Log.e("PHB ERROR", "OnProductClickListener::Constructor.\n" +
                               "Null PopupWindowViewHolder. App will crash.");
        }
	}

	@Override
	public void onItemClick(
			AdapterView<?> parent, View view, int position, long id) {
		// TODO(PHB): The below isn't working right. It is supposed
		// to toggle state (selected or not), but it is only working
		// to go to selected state (e.g. probably view.isSelected() is
		// always false).
		view.setSelected(!view.isSelected());
		SetPopupHeight(parent, view);
		if (product_popup_.isShowing()) {
			product_popup_.dismiss();
		} else {
			DisplayProductPopup(parent, view);
		}
	}
	
	private void SetPopupHeight(AdapterView<?> parent, View view) {
		// No need to do anything if height alread had been set.
		if (popup_height_ != 0) return;
		int parent_height = parent.getHeight();
		int list_item_height = view.getHeight();
		if (parent_height >= 4 * list_item_height) {
			popup_height_ = 3 * list_item_height;
		} else if (parent_height >= 2 * list_item_height) {
			popup_height_ = 2 * list_item_height;
		} else {
			// Screen is so small that having a popup can't display any extra useful
			// information. Skip popup and go directly to purchase screen.
			// Hopefully, this will never happen in practice.
			popup_height_ = -1;
			return;
		}
		product_popup_.setHeight(popup_height_);
	}
	
	private int GetYOffset(AdapterView<?> parent, View view) {
		if (popup_height_ <= 0 || popup_height_ < 2 * view.getHeight()) {
			// TODO(PHB): This will happen if popup_height_ never got 
			// updated from its default value of zero (which is an error),
			// or if it was set to -1 due to not enough space on the screen,
			// or if somehow SetPopupHeight failed to work right (also an error).
			// In the middle case, need to implement going straight to
			// "Product Purchase" page.
			return 1;
		}
		int y_offset = 1;
		int extra_margins_needed = (popup_height_ - view.getHeight()) / 2;
		if ((view.getTop() - extra_margins_needed) >= 0 &&
			(view.getBottom() + extra_margins_needed) <= parent.getHeight()) {
			// There is enough space to display the popup so it is centered
			// over the selected item.
			y_offset = view.getTop() - extra_margins_needed - parent.getHeight();
		} else if ((view.getTop() - extra_margins_needed) < 0) {
			// Not enough space on top to center over the selected item, so
			// just display it centered on top of window.
			y_offset = -1 * parent.getHeight();
		} else {
			// Not enough space on bottom to center over the selected item, so
			// just display it centered on bottom of window.
			y_offset = -1 * popup_height_;
		}
		return y_offset;
	}
	
	private void SetPopupView(View parent_view) {
		View popup_view = product_popup_.getContentView();
		PopupWindowViewHolder popup_holder =
			(PopupWindowViewHolder) popup_view.getTag();
		ProductViewHolder orig_holder = (ProductViewHolder) parent_view.getTag();
		if (orig_holder.drawing_.isShown()) {
			popup_holder.drawing_.setVisibility(View.VISIBLE);
		} else {
			popup_holder.drawing_.setVisibility(View.GONE);
		}
		popup_holder.title_.setText(orig_holder.title_.getText());
        popup_holder.date_.setText("Drawing Date: " + orig_holder.date_.getText());
        popup_holder.bux_.setText(orig_holder.bux_.getText());
        if (orig_holder.points_.isShown()) {
        	popup_holder.points_.setText("+ " + orig_holder.points_.getText());
        	popup_holder.points_.setVisibility(View.VISIBLE);
        } else {
        	popup_holder.points_.setVisibility(View.INVISIBLE);
        }
        popup_holder.pid_.setText(orig_holder.pid_.getText());
        popup_holder.img_.setImageDrawable(orig_holder.img_.getDrawable());
	}
	
	private void DisplayProductPopup(AdapterView<?> parent, View view) {
		int y_offset = GetYOffset(parent, view);
		if (y_offset > 0) {
			// GetYOffset returns positive values when something went wrong.
			Log.e("PHB ERROR", "OnProductClickListener::DisplayProductPopup.\n" +
	                   "Invalid popup_height_: " + popup_height_);
			return;
		}
        SetPopupView(view);
		product_popup_.showAsDropDown(parent, 0, y_offset);
	}
	
	public ShoppingCartActivity.ItemStatus GetProductDetails(ShoppingCartActivity.CartItem details) {
		// Fetch Image from the Popup.
		view_holder_.img_.setDrawingCacheEnabled(true);
		details.product_icon_ = Bitmap.createBitmap(view_holder_.img_.getDrawingCache());
		view_holder_.img_.setDrawingCacheEnabled(false);
		
		// Fetch PID from the Popup.
		details.product_id_ = view_holder_.pid_.getText().toString();
		if (details.product_id_.isEmpty()) {
			return ShoppingCartActivity.ItemStatus.NO_PID;
		}
		
		// Fetch from the Popup whether this product is a drawing or not.
		details.is_drawing_ = view_holder_.drawing_.isShown();
		
		// Fetch Product Title from the Popup.
		details.product_title_ = view_holder_.title_.getText().toString();
		if (details.product_title_.isEmpty()) {
			return ShoppingCartActivity.ItemStatus.NO_TITLE;
		}
		
		// Price is either in BUX, POINTS, or USD; determine which it is by trying to parse
		// into one of the three acceptable formats: "JXXX BUX", "XXX Points", or "$XXX USD".
		String price = view_holder_.bux_.getText().toString();
		if (price.isEmpty()) {
			Log.e("PHB ERROR", "OnProductClickListener::GetProductDetails. Unable to parse price: " + price);
			return ShoppingCartActivity.ItemStatus.NO_PRICE;
		}
		if (price.substring(0, 1).equalsIgnoreCase("J")) {
		  int bux_index = price.indexOf(" BUX");
		  if (bux_index < 0) {
			Log.e("PHB ERROR", "OnProductClickListener::GetProductDetails. Unable to parse price: " + price);
			return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
		  }
		  details.bux_ = price.substring(1, bux_index);
		  details.usd_ = "";
		  details.points_ = "";
		} else if (price.substring(0, 1).equalsIgnoreCase("$")) {
		  int usd_index = price.indexOf(" USD");
		  if (usd_index < 0) {
			Log.e("PHB ERROR", "OnProductClickListener::GetProductDetails. Unable to parse price: " + price);
			return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
		  }
		  details.usd_ = price.substring(1, usd_index);
		  details.bux_ = "";
		  details.points_ = "";
		} else if (price.substring(price.length() - 7).equalsIgnoreCase(" Points")) {
		  details.points_ = price.substring(0, price.length() - 7);
		  details.bux_ = "";
		  details.usd_ = "";
		} else {
			Log.e("PHB ERROR", "OnProductClickListener::GetProductDetails. Unable to parse price: " + price);
			return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
		}
		
		// There can be a second price line, for products that have combined price in
		// Points and Bux. Check visibility of 2nd price line in Popup, and if present,
		// populate details.points_ with it (when present, 2nd line always represents Points).
		if (view_holder_.points_.isShown()) {
			String points_and_plus = view_holder_.points_.getText().toString();
			if (!(points_and_plus.substring(0, 2).equalsIgnoreCase("+ "))) {
				Log.e("PHB ERROR", "OnProductClickListener::GetProductDetails. Unable to parse price: " +
	                       price + " with points: " + points_and_plus);
				return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
			}
			String points = points_and_plus.substring(2);
			if (details.points_.isEmpty() && points.substring(points.length() - 7).equalsIgnoreCase(" Points")) {
			  details.points_ = points.substring(0, points.length() - 7);
			} else {
				Log.e("PHB ERROR", "OnProductClickListener::GetProductDetails. Unable to parse price: " +
			                       price + " with points: " + points);
				return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
			}
		} 
		
		return ShoppingCartActivity.ItemStatus.VALID;
	}
	
	public void DismissClick() {
		product_popup_.dismiss();
	}
}