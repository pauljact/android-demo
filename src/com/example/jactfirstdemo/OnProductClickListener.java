package com.example.jactfirstdemo;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import com.example.jactfirstdemo.ProductsAdapter.ProductViewHolder;
import com.example.jactfirstdemo.ShoppingUtils.Amount;

import android.graphics.Bitmap;
import android.net.ParseException;
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
	  public TextView summary_;
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
        view_holder_.summary_ = (TextView) vi.findViewById(R.id.product_popup_text_summary);
        view_holder_.date_ = (TextView) vi.findViewById(R.id.product_popup_text_date);
        view_holder_.bux_ = (TextView) vi.findViewById(R.id.product_popup_text_bux);
        //PHB_OLDview_holder_.points_ = (TextView) vi.findViewById(R.id.product_popup_text_points);
        view_holder_.pid_ = (TextView) vi.findViewById(R.id.product_popup_text_pid);
        view_holder_.img_ = (ImageView) vi.findViewById(R.id.product_popup_img);
        vi.setTag(view_holder_);
        if (view_holder_.title_ == null || view_holder_.summary_ == null || view_holder_.date_ == null ||
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
		// PHB_OLD (Want to set popup height after every click, since some products have
		// bigger height (e.g. from long product description)
		// PHB_OLD No need to do anything if height already had been set.
		//PHB_OLDif (popup_height_ != 0) return;
		int parent_height = parent.getHeight();
		int list_item_height = view.getHeight();
		if (parent_height >= 3 * list_item_height) {
			popup_height_ = 3 * list_item_height;
		} else if (parent_height >= 2 * list_item_height) {
			popup_height_ = 2 * list_item_height;
		} else if (parent_height >= list_item_height + (list_item_height / 2)) {
			popup_height_ = list_item_height + (list_item_height / 2);
		} else if (parent_height >= list_item_height + (list_item_height / 4)) {
			popup_height_ = list_item_height + (list_item_height / 4);
		} else {
			// Screen is so small that having a popup can't display any extra useful
			// information. Skip popup and go directly to purchase screen.
			// Hopefully, this will never happen in practice.
			// UPDATE: It breaks flow not to have a popup, so just have one that is the same
			// height as the list item itself.
			popup_height_ = list_item_height;
		}
		product_popup_.setHeight(popup_height_);
	}
	
	private int GetYOffset(AdapterView<?> parent, View view) {
		if (popup_height_ <= 0 || popup_height_ < view.getHeight()) {
			// TODO(PHB): This will happen if popup_height_ never got 
			// updated from its default value of zero (which is an error),
			// or if it was set to -1 due to not enough space on the screen,
			// or if somehow SetPopupHeight failed to work right (also an error).
			// In the middle case, need to implement going straight to
			// "Product Purchase" page.
			Log.e("PHB ERROR", "OnProductClickListener::GetYOffset. popup_height_: " +
				  popup_height_ + ", view.getHeight(): " + view.getHeight());
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
		popup_holder.summary_.setText(orig_holder.summary_.getText());
		if (orig_holder.drawing_.isShown()) {
		  popup_holder.date_.setVisibility(View.VISIBLE);
		  popup_holder.date_.setText("Drawing Date: " + orig_holder.date_.getText());
		} else {
		  popup_holder.date_.setVisibility(View.GONE);
		}
        popup_holder.bux_.setText(orig_holder.bux_.getText());
        //PHB_OLDif (orig_holder.points_.isShown()) {
        //PHB_OLD	popup_holder.points_.setText("+ " + orig_holder.points_.getText());
        //PHB_OLD	popup_holder.points_.setVisibility(View.VISIBLE);
        //PHB_OLD} else {
        //PHB_OLD	popup_holder.points_.setVisibility(View.INVISIBLE);
        //PHB_OLD}
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
	
	public ShoppingCartActivity.ItemStatus GetProductDetails(ShoppingUtils.LineItem item) {
		// Fetch Image from the Popup.
		view_holder_.img_.setDrawingCacheEnabled(true);
		item.product_icon_ = Bitmap.createBitmap(view_holder_.img_.getDrawingCache());
		view_holder_.img_.setDrawingCacheEnabled(false);
		
		// Fetch from the Popup whether this product is a drawing or not; and if so, grab date.
		item.is_drawing_ = view_holder_.drawing_.isShown();
		if (item.is_drawing_ && view_holder_.date_ != null) {
		  item.drawing_date_ = view_holder_.date_.getText().toString();
		}
		
		// Fetch Product Title from the Popup.
		item.title_ = view_holder_.title_.getText().toString();
		if (item.title_.isEmpty()) {
			return ShoppingCartActivity.ItemStatus.NO_TITLE;
		}

		String err_msg = "";
		try {
  		  // Fetch PID from the Popup.
		  item.pid_ = Integer.parseInt(view_holder_.pid_.getText().toString());
		  if (item.pid_ == 0) {
			return ShoppingCartActivity.ItemStatus.NO_PID;
		  }
		
	  	  // Price is either in BUX, POINTS, or USD; determine which it is by trying to parse
		  // into one of the three acceptable formats: "JXXX BUX", "XXX Points", or "$XXX USD".
		  String price = view_holder_.bux_.getText().toString();
		  if (price.isEmpty()) {
			Log.e("PHB ERROR", "OnProductClickListener::GetProductDetails. Unable to parse price: " + price);
			return ShoppingCartActivity.ItemStatus.NO_PRICE;
		  }
		  item.cost_ = new ArrayList<Amount>();
		  if (price.substring(0, 1).equalsIgnoreCase("J")) {
		    int bux_index = price.indexOf(" BUX");
		    if (bux_index < 0) {
			  Log.e("PHB ERROR", "OnProductClickListener::GetProductDetails. Unable to parse price: " + price);
			  return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
		    }
		    Amount amount = new Amount();
		    err_msg = "bux";
		    try {
		      amount.price_ = NumberFormat.getNumberInstance(Locale.US).parse(price.substring(1, bux_index)).doubleValue();
			} catch (java.text.ParseException e) {
				Log.e("PHB ERROR", "OnProductClickListener::GetProductDetails. ParseException. Item:\n" +
	                    ShoppingUtils.PrintLineItem(item));
	 	        return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
			}
		    amount.type_ = ShoppingUtils.CurrencyCode.BUX;
		    item.cost_.add(amount);
		  } else if (price.substring(0, 1).equalsIgnoreCase("$")) {
		    int usd_index = price.indexOf(" USD");
		    if (usd_index < 0) {
			  Log.e("PHB ERROR", "OnProductClickListener::GetProductDetails. Unable to parse price: " + price);
			  return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
		    }
		    Amount amount = new Amount();
		    try {
		      amount.price_ = NumberFormat.getNumberInstance(Locale.US).parse(price.substring(1, usd_index)).doubleValue();
			} catch (java.text.ParseException e) {
				Log.e("PHB ERROR", "OnProductClickListener::GetProductDetails. ParseException. Item:\n" +
	                    ShoppingUtils.PrintLineItem(item));
	 	        return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
			}
		    amount.type_ = ShoppingUtils.CurrencyCode.USD;
		    item.cost_.add(amount);
		  } else if (price.substring(price.length() - 7).equalsIgnoreCase(" Points")) {
		    Amount amount = new Amount();
		    err_msg = "points";
		    try {
				amount.price_ = NumberFormat.getNumberInstance(Locale.US).parse(
						price.substring(0, price.length() - 7)).doubleValue();
			} catch (java.text.ParseException e) {
				Log.e("PHB ERROR", "OnProductClickListener::GetProductDetails. ParseException. Item:\n" +
	                    ShoppingUtils.PrintLineItem(item));
	 	        return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
			}
		    amount.type_ = ShoppingUtils.CurrencyCode.POINTS;
		    item.cost_.add(amount);
		  } else {
			Log.e("PHB ERROR", "OnProductClickListener::GetProductDetails. Unable to parse price: " + price);
			return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
		  }
		
		  // There can be a second price line, for products that have combined price in
		  // Points and USD or Points and BUX. Check visibility of 2nd price line in Popup, and if present,
		  // populate details.points_ with it (when present, 2nd line always represents Points).
		  if (view_holder_.points_.isShown()) {
			String points_and_plus = view_holder_.points_.getText().toString();
			if (!(points_and_plus.substring(0, 2).equalsIgnoreCase("+ "))) {
				Log.e("PHB ERROR", "OnProductClickListener::GetProductDetails. Unable to parse price: " +
	                       price + " with points: " + points_and_plus);
				return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
			}
			String points = points_and_plus.substring(2);
			if (points.substring(points.length() - 7).equalsIgnoreCase(" Points")) {
			    Amount amount = new Amount();
			    err_msg = "|" + points.substring(0, points.length() - 7) + "|" + points.substring(0, points.length() - 7).trim() + "|";
			    try {
					amount.price_ = NumberFormat.getNumberInstance(Locale.US).parse(
							points.substring(0, points.length() - 7)).doubleValue();
				} catch (java.text.ParseException e) {
					Log.e("PHB ERROR", "OnProductClickListener::GetProductDetails. ParseException. Item:\n" +
		                    ShoppingUtils.PrintLineItem(item));
		 	        return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
				}
			    amount.type_ = ShoppingUtils.CurrencyCode.POINTS;
			    item.cost_.add(amount);
			} else {
				Log.e("PHB ERROR", "OnProductClickListener::GetProductDetails. Unable to parse price: " +
			                       price + " with points: " + points);
				return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
			}
		  } 
		} catch (NumberFormatException e) {
			Log.e("PHB ERROR", "OnProductClickListener::GetProductDetails. NumberFormatException. " +
		                       "Error Msg: " + err_msg + ", Item:\n" + ShoppingUtils.PrintLineItem(item));
			return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
		}
		return ShoppingCartActivity.ItemStatus.VALID;
	}
	
	public void DismissClick() {
		product_popup_.dismiss();
	}
}