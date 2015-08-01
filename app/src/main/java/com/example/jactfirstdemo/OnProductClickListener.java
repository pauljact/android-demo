package com.example.jactfirstdemo;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import com.example.jactfirstdemo.ProductsAdapter.ProductViewHolder;
import com.example.jactfirstdemo.ShoppingUtils.Amount;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
	  public TextView max_quantity_;
	  public TextView orig_price_usd_;
	  public TextView jact_price_usd_;
	  public ImageView jact_price_point_icon_;
	  public TextView jact_price_points_;
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
        view_holder_.summary_.setMovementMethod(ScrollingMovementMethod.getInstance());
        view_holder_.date_ = (TextView) vi.findViewById(R.id.product_popup_text_date);
        view_holder_.max_quantity_ = (TextView) vi.findViewById(R.id.product_popup_max_quantity);
        view_holder_.orig_price_usd_ = (TextView) vi.findViewById(R.id.product_popup_orig_price_usd);
        view_holder_.jact_price_usd_ = (TextView) vi.findViewById(R.id.product_popup_jact_price_usd);
        view_holder_.jact_price_point_icon_ = (ImageView) vi.findViewById(R.id.product_popup_jact_price_points_icon);
        view_holder_.jact_price_points_ = (TextView) vi.findViewById(R.id.product_popup_jact_price_points);
        view_holder_.pid_ = (TextView) vi.findViewById(R.id.product_popup_text_pid);
        view_holder_.img_ = (ImageView) vi.findViewById(R.id.product_popup_img);
        vi.setTag(view_holder_);
        if (view_holder_.title_ == null || view_holder_.summary_ == null || view_holder_.date_ == null ||
            view_holder_.orig_price_usd_ == null || view_holder_.jact_price_usd_ == null ||
			view_holder_.jact_price_points_ == null || view_holder_.jact_price_point_icon_ == null ||
            view_holder_.img_ == null || view_holder_.pid_ == null || view_holder_.max_quantity_ == null) {
        	// TODO(PHB): Gracefully handle this.
        	if (!JactActionBarActivity.IS_PRODUCTION) Log.e("OnProductClickListener::Constructor", "Null PopupWindowViewHolder. App will crash.");
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
		SetPopupHeight(view, ((View) parent.getParent()).getHeight());
		if (product_popup_.isShowing()) {
			product_popup_.dismiss();
		} else {
			DisplayProductPopup(parent, view, false);
		}
	}

	public void onFeaturedItemClick(View view) {
		SetPopupHeight(view, ((View) view.getParent().getParent().getParent().getParent()).getHeight());
		if (product_popup_.isShowing()) {
			product_popup_.dismiss();
		} else {
			DisplayProductPopup((View) view.getParent().getParent().getParent().getParent(), view, true);
		}
	}
	
	private void SetPopupHeight(View view, int parent_height) {
		/* PHB_OLD
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
		}*/
		popup_height_ = parent_height;
		product_popup_.setHeight(popup_height_);
	}
	
	private int GetYOffset(View view, int parent_height) {
		return -1 * popup_height_;
		/* PHB_OLD
		if (popup_height_ <= 0 || popup_height_ < view.getHeight()) {
			// TODO(PHB): This will happen if popup_height_ never got 
			// updated from its default value of zero (which is an error),
			// or if it was set to -1 due to not enough space on the screen,
			// or if somehow SetPopupHeight failed to work right (also an error).
			// In the middle case, need to implement going straight to
			// "Product Purchase" page.
			if (!JactActionBarActivity.IS_PRODUCTION) Log.e("OnProductClickListener::GetYOffset", "popup_height_: " +
					popup_height_ + ", view.getHeight(): " + view.getHeight());
			return 1;
		}
		int y_offset = 1;
		int extra_margins_needed = (popup_height_ - view.getHeight()) / 2;
		if ((view.getTop() - extra_margins_needed) >= 0 &&
			(view.getBottom() + extra_margins_needed) <= parent_height) {
			// There is enough space to display the popup so it is centered
			// over the selected item.
			y_offset = view.getTop() - extra_margins_needed - parent_height;
		} else if ((view.getTop() - extra_margins_needed) < 0) {
			// Not enough space on top to center over the selected item, so
			// just display it centered on top of window.
			y_offset = -1 * parent_height;
		} else {
			// Not enough space on bottom to center over the selected item, so
			// just display it centered on bottom of window.
			y_offset = -1 * popup_height_;
		}
		return y_offset;
		*/
	}

	private void SetPopupView(View parent_view) {
		View popup_view = product_popup_.getContentView();
		PopupWindowViewHolder popup_holder =
				(PopupWindowViewHolder) popup_view.getTag();
		ProductViewHolder orig_holder = (ProductViewHolder) parent_view.getTag();
		if (orig_holder.drawing_.isShown()) {
			popup_holder.drawing_.setVisibility(View.GONE);
			//PHB_OLDpopup_holder.drawing_.setVisibility(View.VISIBLE);
		} else {
			popup_holder.drawing_.setVisibility(View.GONE);
		}
		popup_holder.title_.setText(orig_holder.title_.getText());
		popup_holder.summary_.setText("Description: " + orig_holder.summary_.getText());
		String max_quantity = orig_holder.max_quantity_.getText().toString();
		if (max_quantity.isEmpty()) {
			popup_holder.max_quantity_.setVisibility(View.GONE);
		} else {
			popup_holder.max_quantity_.setVisibility(View.VISIBLE);
			popup_holder.max_quantity_.setText("Max Quantity: " + max_quantity);
		}
		if (orig_holder.drawing_.isShown()) {
			popup_holder.date_.setVisibility(View.VISIBLE);
			popup_holder.date_.setText("Drawing Date: " + orig_holder.date_.getText());
		} else {
			popup_holder.date_.setVisibility(View.GONE);
		}
		SetOriginalPrice(orig_holder, popup_holder);
		SetJactPrice(orig_holder, popup_holder);
		popup_holder.pid_.setText(orig_holder.pid_.getText());
		popup_holder.img_.setImageDrawable(orig_holder.img_.getDrawable());
	}
/*
	private void SetPopupViewFromFeatured(LinearLayout parent_view) {
		View popup_view = product_popup_.getContentView();
		PopupWindowViewHolder popup_holder =
			(PopupWindowViewHolder) popup_view.getTag();
		popup_holder.img_.setImageDrawable(((ImageView) parent_view.getChildAt(0)).getDrawable());
		if (parent_view.getChildAt(1).isShown()) {
			popup_holder.drawing_.setVisibility(View.VISIBLE);
		} else {
			popup_holder.drawing_.setVisibility(View.GONE);
		}
		if (parent_view.getChildAt(2).isShown()) {
			popup_holder.date_.setVisibility(View.VISIBLE);
			popup_holder.date_.setText(
					"Drawing Date: " + ((TextView) parent_view.getChildAt(2)).getText().toString());
		} else {
			popup_holder.date_.setVisibility(View.GONE);
		}
		popup_holder.title_.setText(((TextView) parent_view.getChildAt(3)).getText().toString());
		popup_holder.summary_.setText(
			"Description: " + ((TextView) parent_view.getChildAt(4)).getText().toString());
		String max_quantity = ((TextView) parent_view.getChildAt(5)).getText().toString();
		if (max_quantity.isEmpty()) {
		  popup_holder.max_quantity_.setVisibility(View.GONE);
		} else {
		  popup_holder.max_quantity_.setVisibility(View.VISIBLE);
		  popup_holder.max_quantity_.setText("Max Quantity: " + max_quantity);
		}
		popup_holder.pid_.setText(((TextView) parent_view.getChildAt(6)).getText().toString());
		if (parent_view.getChildAt(7).isShown()) {
			popup_holder.orig_price_usd_.setText(((TextView) parent_view.getChildAt(7)).getText());
			popup_holder.orig_price_usd_.setPaintFlags(
					popup_holder.orig_price_usd_.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
			popup_holder.orig_price_usd_.setVisibility(View.VISIBLE);
		} else {
			popup_holder.orig_price_usd_.setVisibility(View.GONE);
		}
		LinearLayout price_ll = (LinearLayout) parent_view.getChildAt(8);
		popup_holder.jact_price_usd_.setText(((TextView) price_ll.getChildAt(0)).getText());
		if (price_ll.getChildCount() == 3) {
			popup_holder.jact_price_point_icon_.setVisibility(View.VISIBLE);
			popup_holder.jact_price_points_.setText(((TextView) price_ll.getChildAt(2)).getText());
		} else if (price_ll.getChildCount() == 1) {
			popup_holder.jact_price_point_icon_.setVisibility(View.GONE);
		} else {
			if (!JactActionBarActivity.IS_PRODUCTION) Log.e("onProductClickListener::SetPopupViewFromFeatured",
				  "Unexpected length of Price LL: " + price_ll.getChildCount());
		}
	}
	*/
	private void SetOriginalPrice(ProductViewHolder orig_holder, PopupWindowViewHolder popup_holder) {
	  if (orig_holder.orig_price_usd_.isShown()) {
		popup_holder.orig_price_usd_.setText(orig_holder.orig_price_usd_.getText());
		popup_holder.orig_price_usd_.setPaintFlags(
			popup_holder.orig_price_usd_.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
		popup_holder.orig_price_usd_.setVisibility(View.VISIBLE);
	  } else {
		popup_holder.orig_price_usd_.setVisibility(View.GONE);
	  }
	}
	
	private void SetJactPrice(ProductViewHolder orig_holder, PopupWindowViewHolder popup_holder) {
	  popup_holder.jact_price_usd_.setText(orig_holder.jact_price_usd_.getText());
	  if (orig_holder.jact_price_point_icon_.isShown()) {
		popup_holder.jact_price_point_icon_.setVisibility(View.VISIBLE);  
	  } else {
		popup_holder.jact_price_point_icon_.setVisibility(View.GONE);
	  }
	  popup_holder.jact_price_points_.setText(orig_holder.jact_price_points_.getText());
	}
	
	private void DisplayProductPopup(View parent, View view, boolean from_featured_click) {
		int y_offset = GetYOffset(view, parent.getHeight());
		if (y_offset > 0) {
			// GetYOffset returns positive values when something went wrong.
			if (!JactActionBarActivity.IS_PRODUCTION) Log.e("OnProductClickListener::DisplayProductPopup", "Invalid popup_height_: " + popup_height_);
			return;
		}
		//if (from_featured_click) {
		//	SetPopupViewFromFeatured((LinearLayout) view);
		//} else {
			SetPopupView(view);
		//}
		product_popup_.showAsDropDown(parent, 0, y_offset);
	}
	
	public ShoppingCartActivity.ItemStatus GetProductDetails(ShoppingUtils.LineItem item) {
		// Fetch Image from the Popup.
		view_holder_.img_.setDrawingCacheEnabled(true);
		item.product_icon_ = Bitmap.createBitmap(view_holder_.img_.getDrawingCache());
		view_holder_.img_.setDrawingCacheEnabled(false);
		
		// Fetch from the Popup whether this product is a drawing or not; and if so, grab date.
		item.is_drawing_ = view_holder_.date_.isShown();
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
		
		  ShoppingCartActivity.ItemStatus price_status = GetPrice(item);
		  if (price_status != ShoppingCartActivity.ItemStatus.VALID) {
			return price_status;
		  }
		} catch (NumberFormatException e) {
			if (!JactActionBarActivity.IS_PRODUCTION) Log.e("OnProductClickListener::GetProductDetails", "NumberFormatException. " +
		                       "Error Msg: " + err_msg + ", Item:\n" + ShoppingUtils.PrintLineItem(item));
			return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
		}
		return ShoppingCartActivity.ItemStatus.VALID;
	}

	private ShoppingCartActivity.ItemStatus GetPrice(ShoppingUtils.LineItem item) {
	  // Price is either in BUX, POINTS, or USD; determine which it is by trying to parse
	  // into one of the three acceptable formats: "JXXX BUX", "XXX Points", or "$XXX USD".
	  String usd_price = view_holder_.jact_price_usd_.getText().toString();
	  String usd_prefix = "JACT Price:";
	  if (usd_price.isEmpty() || usd_price.length() < usd_prefix.length() ||
		  !usd_price.substring(0, usd_prefix.length()).equals(usd_prefix)) {
		if (!JactActionBarActivity.IS_PRODUCTION) Log.e("OnProductClickListener::GetPrice", "Unable to parse USD price: |" + usd_price +
			  "|, comparing to: |" + usd_prefix + "|");
		return ShoppingCartActivity.ItemStatus.NO_PRICE;
	  }
	  String price = usd_price.substring(usd_prefix.length());
	  // If there is both USD (or BUX) and Points, then Price string will have suffix " + ". Remove it.
	  if (price.length() > 3 && price.substring(price.length() - 3).equals(" + ")) {
		price = price.substring(0, price.length() - 3);
	  }
	  item.cost_ = new ArrayList<Amount>();
	  if (price.length() < 2) {
		// No USD or Bux price (Points only). Nothing to do.
	  } else if (price.substring(0, 2).equalsIgnoreCase(" J")) {
	    Amount amount = new Amount();
	    try {
	      amount.price_ = NumberFormat.getNumberInstance(Locale.US).parse(price.substring(2)).doubleValue();
		  amount.type_ = ShoppingUtils.CurrencyCode.BUX;
		  item.cost_.add(amount);
		} catch (java.text.ParseException e) {
		  if (!JactActionBarActivity.IS_PRODUCTION) Log.e("OnProductClickListener::GetPrice", "ParseException. Bux Price: " + price + ", Item:\n" +
	            ShoppingUtils.PrintLineItem(item));
	      return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
		}
	  } else if (price.substring(0, 2).equalsIgnoreCase(" $")) {
	    Amount amount = new Amount();
	    try {
	      amount.price_ = NumberFormat.getNumberInstance(Locale.US).parse(price.substring(2)).doubleValue();
		  amount.type_ = ShoppingUtils.CurrencyCode.USD;
		  item.cost_.add(amount);
		} catch (java.text.ParseException e) {
			if (!JactActionBarActivity.IS_PRODUCTION) Log.e("OnProductClickListener::GetPrice", "ParseException. USD Price: " + price +", Item:\n" +
	              ShoppingUtils.PrintLineItem(item));
	       return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
		}
	  } else {
		if (!JactActionBarActivity.IS_PRODUCTION) Log.e("OnProductClickListener::GetPrice", "Unable to parse price: |" + price + "|");
		return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
	  }
      
	  if (!view_holder_.jact_price_point_icon_.isShown()) return ShoppingCartActivity.ItemStatus.VALID;
	  String points_price = view_holder_.jact_price_points_.getText().toString();
	  Amount amount = new Amount();
	  try {
		amount.price_ = NumberFormat.getNumberInstance(Locale.US).parse(points_price).doubleValue();
		amount.type_ = ShoppingUtils.CurrencyCode.POINTS;
	    item.cost_.add(amount);
	  } catch (java.text.ParseException e) {
		if (!JactActionBarActivity.IS_PRODUCTION) Log.e("OnProductClickListener::GetPrice",
			  "ParseException. PointsPrice: |" + points_price +
			  "|, Item:\n" + ShoppingUtils.PrintLineItem(item));
	    return ShoppingCartActivity.ItemStatus.INVALID_PRICE;
	  }
	  return ShoppingCartActivity.ItemStatus.VALID;
	}

  	public void DismissClick() {
      product_popup_.dismiss();
  	}
}
