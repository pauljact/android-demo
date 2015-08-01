package com.example.jactfirstdemo;

import java.util.ArrayList;
import java.util.Iterator;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ShippingActivity extends JactActionBarActivity implements ProcessUrlResponseCallback {
  private ListView list_;
  private ArrayList<ShoppingCartActivity.JactAddress> addresses_;
  private AddressAdapter adapter_;
  private int num_checked_addresses_;
  private int selected_address_position_;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState, R.string.shipping_label,
		       R.layout.shipping_layout,
		       JactNavigationDrawer.ActivityIndex.SHIPPING);
    addresses_ = new ArrayList<ShoppingCartActivity.JactAddress>();
    list_ = (ListView) findViewById(R.id.shipping_list);
  }
    
  @Override
  protected void onResume() {
	super.onResume();
	num_checked_addresses_ = 0;
	selected_address_position_ = -1;
    addresses_ = GetAddresses();
	adapter_ = new AddressAdapter(this, R.layout.address_item, addresses_);
	list_.setAdapter(adapter_);
	SetCartIcon(this);
	fadeAllViews(num_server_tasks_ > 0);
  }

  @Override
  public void fadeAllViews(boolean should_fade) {
    ProgressBar spinner = (ProgressBar) findViewById(R.id.shipping_progress_bar);
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
    RelativeLayout layout = (RelativeLayout) findViewById(R.id.shipping_content_frame);
    layout.startAnimation(alpha); // Add animation to the layout.
  }
    
  public void doShippingAddNewAddressButtonClick(View view) {
    fadeAllViews(true);
	startActivity(new Intent(this, ShippingNewActivity.class));
  }
  
  public void doShippingPrevButtonClick(View view) {
    fadeAllViews(true);
	startActivity(new Intent(this, ShoppingCartActivity.class));
  }
  
  public void doShippingNextButtonClick(View view) {
    if (num_checked_addresses_ == 1 && selected_address_position_ >= 0 &&
    	addresses_.size() > selected_address_position_) {
      fadeAllViews(true);
      ShoppingCartActivity.SetShippingAddress(addresses_.get(selected_address_position_));
	  if (BillingActivity.GetNumBillingAddresses() > 0) {
	    startActivity(new Intent(this, BillingActivity.class));
	  } else {
	    startActivity(new Intent(this, BillingNewActivity.class));
	  }
    } else if (num_checked_addresses_ == 0 ){
      DisplayPopupFragment("Missing Shipping Address",
    		               "Select an Address From the List, or Add a New One",
    		               "no_shipping_addr_selected");
    } else if (num_checked_addresses_ > 1) {
      DisplayPopupFragment("Too Many Shipping Addresses",
    		               "Select At Most One From the List, or Add a New One",
    		               "too_many_shipping_addr_selected");
    } else {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "ShippingActivity::doShippingNextButtonClick. Unexpected value for num_checked_addresses_: " +
                         num_checked_addresses_ + ", selected_address_position_: " + selected_address_position_);
    }
  }
  
  public void onAddressItemCheckboxClicked(View view) {
    if (view.getTag() != null) {
      try {
        CheckBox cb = (CheckBox) view;
        AddressAdapter.CheckBoxPosition pos = (AddressAdapter.CheckBoxPosition) cb.getTag();
        selected_address_position_ = pos.position_;
        if (cb.isChecked()) {
          num_checked_addresses_++;
        } else {
          num_checked_addresses_--;
        }
      } catch (ClassCastException e) {
        if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "ShippingActivity::onAddressItemCheckboxClicked. Unexpected click on view:\n" + view.toString());
      }
    }
  }
  
  public void doAddressItemDeleteAddressClick(View view) {
    // TODO(PHB): Implement this.
	DisplayPopupFragment("Remove Address Not Implemented Yet", "remove_addr_not_yet_implemented");
  }
  
  public static void WriteAddresses(ArrayList<ShoppingCartActivity.JactAddress> addresses) {
    if (addresses == null) return;
	Iterator<ShoppingCartActivity.JactAddress> itr = addresses.iterator();
    while (itr.hasNext()) {
      WriteAddress(itr.next());
    }  
  }
  
  public static void WriteAddress(ShoppingCartActivity.JactAddress address) {
  }
  
  public static ArrayList<ShoppingCartActivity.JactAddress> GetAddresses() {
	  // TODO(PHB): Get addresses from website, then write them to file and return them here.
	  ArrayList<ShoppingCartActivity.JactAddress> addresses = new ArrayList<ShoppingCartActivity.JactAddress>();
	  ShoppingCartActivity.JactAddress address = new ShoppingCartActivity.JactAddress();
	  address.city_ = "los angeles";
	  address.state_ = "CA";
	  address.first_name_ = "Paul";
	  address.last_name_ = "Bunn";
	  address.street_addr_ = "1234 Main Street";
	  address.zip_ = "90025";
	  addresses.add(address);
	  
	  ShoppingCartActivity.JactAddress address2 = new ShoppingCartActivity.JactAddress();
	  address2.first_name_ = "Paul";
	  address2.last_name_ = "Bunn";
	  address2.street_addr_ = "007 Baker Street";
	  address2.city_ = "Washington";
	  address2.state_ = "DC";
	  address2.zip_ = "00001";
	  addresses.add(address2);
	  return addresses;
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