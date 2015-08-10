package com.jact.jactfirstdemo;

import com.jact.jactfirstdemo.GetUrlTask.FetchStatus;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ReviewCartActivity extends JactActionBarActivity implements ProcessUrlResponseCallback {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState, R.string.review_label,
		       R.layout.review_order_layout,
		       JactNavigationDrawer.ActivityIndex.REVIEW_ORDER);
  }
    
  @Override
  protected void onResume() {
	super.onResume();
	ShoppingCartActivity.JactAddress shipping_address = ShoppingCartActivity.GetShippingAddress();
	if (shipping_address != null) {
	  String name = "";
	  if (shipping_address.first_name_ != null) name = shipping_address.first_name_;
	  if (shipping_address.last_name_ != null) {
	    if (name.isEmpty()) {
	      name = shipping_address.last_name_; 
	    } else {
	      name += " " + shipping_address.last_name_;
	    }
	  }
	  if (name.isEmpty()) {
		  // TODO(PHB): Handle this.
	  } else {
	    TextView shipping_name = (TextView) findViewById(R.id.review_order_ship_to_addr_name);
  	    shipping_name.setText(name);
	  }
	  String street = "";
	  if (shipping_address.street_addr_ != null) {
	    street = shipping_address.street_addr_;
	  }
	  if (shipping_address.street_addr_extra_ != null && !shipping_address.street_addr_extra_.isEmpty()) {
		if (street.isEmpty()) {
		  street = shipping_address.street_addr_extra_;
		} else {
		  street += " " + shipping_address.street_addr_extra_;
		}
	  }
	  if (!street.isEmpty()) {
	    TextView shipping_street = (TextView) findViewById(R.id.review_order_ship_to_addr_street);
  	    shipping_street.setText(street);
	  } else {
		// TODO(PHB): Handle this
	  }
	  String city = "";
	  if (shipping_address.city_ != null && !shipping_address.city_.isEmpty()) {
		city = shipping_address.city_;
	  }
	  if (shipping_address.state_ != null && !shipping_address.state_.isEmpty()) {
		if (city.isEmpty()) {
		  city = shipping_address.state_;
		} else {
		  city += " " + shipping_address.state_;
		}
	  }
	  if (shipping_address.zip_ != null && !shipping_address.zip_.isEmpty()) {
		if (city.isEmpty()) {
		  city = shipping_address.zip_;
		} else {
		  city += " " + shipping_address.zip_;
		}
	  }
	  if (!city.isEmpty()) {
	    TextView shipping_addr = (TextView) findViewById(R.id.review_order_ship_to_addr_city);
  	    shipping_addr.setText(city);
	  } else {
		// TODO(PHB): Handle this
	  }
	} else {
		// TODO(PHB): Handle this
	}
	ShoppingCartActivity.JactAddress billing_address = ShoppingCartActivity.GetBillingAddress();
    if (billing_address != null) {
      String name = "";
  	  if (billing_address.first_name_ != null) name = billing_address.first_name_;
  	  if (billing_address.last_name_ != null) {
  	    if (name.isEmpty()) {
  	      name = billing_address.last_name_; 
  	    } else {
  	      name += " " + billing_address.last_name_;
  	    }
  	  }
  	  if (name.isEmpty()) {
  		  // TODO(PHB): Handle this.
  	  } else {
  	    TextView billing_name = (TextView) findViewById(R.id.review_order_bill_to_addr_name);
  	    billing_name.setText(name);
  	  }
	  String street = "";
	  if (billing_address.street_addr_ != null) {
	    street = billing_address.street_addr_;
	  }
	  if (billing_address.street_addr_extra_ != null && !billing_address.street_addr_extra_.isEmpty()) {
		if (street.isEmpty()) {
		  street = billing_address.street_addr_extra_;
		} else {
		  street += " " + billing_address.street_addr_extra_;
		}
	  }
	  if (!street.isEmpty()) {
	    TextView billing_street = (TextView) findViewById(R.id.review_order_bill_to_addr_street);
  	    billing_street.setText(street);
	  } else {
		// TODO(PHB): Handle this
	  }
	  String city = "";
	  if (billing_address.city_ != null && !billing_address.city_.isEmpty()) {
		city = billing_address.city_;
	  }
	  if (billing_address.state_ != null && !billing_address.state_.isEmpty()) {
		if (city.isEmpty()) {
		  city = billing_address.state_;
		} else {
		  city += " " + billing_address.state_;
		}
	  }
	  if (billing_address.zip_ != null && !billing_address.zip_.isEmpty()) {
		if (city.isEmpty()) {
		  city = billing_address.zip_;
		} else {
		  city += " " + billing_address.zip_;
		}
	  }
	  if (!city.isEmpty()) {
	    TextView billing_addr = (TextView) findViewById(R.id.review_order_bill_to_addr_city);
  	    billing_addr.setText(city);
	  } else {
		// TODO(PHB): Handle this
	  }
      if (billing_address.cc_number_ != null && billing_address.cc_number_.length() > 4 &&
      	  billing_address.cc_type_ != ShoppingCartActivity.CardType.NO_TYPE) {
        String reddit_number = billing_address.cc_type_.toString() + " Ending in " + 
        	billing_address.cc_number_.substring(billing_address.cc_number_.length() - 4);
	    TextView billing_cc = (TextView) findViewById(R.id.review_order_bill_to_addr_cc);
        billing_cc.setText(reddit_number); 
      }
	} else {
		// TODO(PHB): Handle this
	}
    SetCartIcon(this);
    fadeAllViews(num_server_tasks_ > 0);
  }

  @Override
  public void fadeAllViews(boolean should_fade) {
    ProgressBar spinner = (ProgressBar) findViewById(R.id.review_order_progress_bar);
    AlphaAnimation alpha;
    if (should_fade) {
      spinner.setVisibility(View.VISIBLE);
      alpha = new AlphaAnimation(0.5F, 0.5F);
    } else {
      spinner.setVisibility(View.GONE);
      alpha = new AlphaAnimation(1.0F, 1.0F);
    }
    // The AlphaAnimation will make the whole content frame transparent
    // (so that none of the views show).
    alpha.setDuration(0); // Make animation instant
    alpha.setFillAfter(true); // Tell it to persist after the animation ends
    RelativeLayout layout = (RelativeLayout) findViewById(R.id.review_order_content_frame);
    layout.startAnimation(alpha); // Add animation to the layout.
  }
  
  public void doReviewOrderPrevButtonClick(View view) {
	  fadeAllViews(true);
	  startActivity(new Intent(this, BillingActivity.class));
  }
  
  public void doReviewOrderPlaceOrderButtonClick(View view) {
	  // TODO(PHB): Implement this.
	  DisplayPopupFragment("Place Order Not Yet Implemented", "place_order_not_implemented");
  }
  
  @Override
  public void ProcessUrlResponse(String webpage, String cookies, String extra_params) {
	ProcessCartResponse(this, webpage, cookies, extra_params);
  }

  @Override
  public void ProcessUrlResponse(Bitmap pic, String cookies, String extra_params) {
	// TODO Auto-generated method stub
  }

  @Override
  public void ProcessFailedResponse(FetchStatus status, String extra_params) {
	ProcessFailedCartResponse(this, status, extra_params);
  }
}