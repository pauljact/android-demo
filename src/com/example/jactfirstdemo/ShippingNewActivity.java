package com.example.jactfirstdemo;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView;

public class ShippingNewActivity extends JactActionBarActivity
                                 implements OnItemSelectedListener, ProcessUrlResponseCallback {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState, R.string.shipping_label,
		       R.layout.shipping_new_layout,
		       JactNavigationDrawer.ActivityIndex.SHIPPING_NEW);
    // Setup Spinner (Dropdown for State).
    Spinner state = (Spinner) findViewById(R.id.shipping_new_state_spinner);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
    		this, R.array.states, R.layout.jact_state_spinner_item);
    adapter.setDropDownViewResource(R.layout.jact_state_spinner_dropdown);
    state.setAdapter(adapter);
    state.setOnItemSelectedListener((OnItemSelectedListener) this);
    state.setSelection(0);
  }
    
  @Override
  protected void onResume() {
	super.onResume();
	SetCartIcon(this);
	fadeAllViews(num_server_tasks_ > 0);
  }

  @Override
  public void fadeAllViews(boolean should_fade) {
    ProgressBar spinner = (ProgressBar) findViewById(R.id.shipping_new_progress_bar);
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
    RelativeLayout layout = (RelativeLayout) findViewById(R.id.shipping_new_content_frame);
    layout.startAnimation(alpha); // Add animation to the layout.
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
	if (view.getId() == R.id.state_spinner_item_tv) {
	  TextView tv = (TextView) view;
	  if (!tv.getText().equals("State")) {
	    tv.setTextColor(getResources().getColor(R.color.black));
	  }
	}
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
	// TODO Auto-generated method stub
  }
  
  public void doShippingNewPrevButtonClick(View view) {
      fadeAllViews(true);
	  startActivity(new Intent(this, ShoppingCartActivity.class));
  }
  
  private ShoppingCartActivity.JactAddress GetAddressFromEditTexts() {
	ShoppingCartActivity.JactAddress address = new ShoppingCartActivity.JactAddress();
	EditText first_name = (EditText) findViewById(R.id.shipping_new_first_name_et);
	address.first_name_ = first_name.getText().toString().trim();
	EditText last_name = (EditText) findViewById(R.id.shipping_new_last_name_et);
	address.last_name_ = last_name.getText().toString().trim();
	EditText street = (EditText) findViewById(R.id.shipping_new_street_et);
	address.street_addr_ = street.getText().toString().trim();
	EditText city = (EditText) findViewById(R.id.shipping_new_city_et);
	address.city_ = city.getText().toString().trim();
	Spinner state = (Spinner) findViewById(R.id.shipping_new_state_spinner);
	if (!state.getSelectedItem().equals("State")) {
	  address.state_ = (String) state.getSelectedItem();
	}
	EditText zip = (EditText) findViewById(R.id.shipping_new_zip_et);
	String zip_str = zip.getText().toString().trim(); 
	int zip_code = -1;
	if (!zip_str.isEmpty() && zip_str.length() == 5) {
	  try {
	    zip_code = Integer.parseInt(zip_str);
	  } catch (NumberFormatException e) {
		zip_code = -1;
	  }
	}
    if (zip_code != -1) {
      address.zip_ = zip_str;
	}
	return address;
  }
  
  public void doShippingNewNextButtonClick(View view) {
    // Parse Page, make sure all information is correct. If so, proceed to billing.
	// Otherwise, popup dialog warning about missing/incorrect information.
	ShoppingCartActivity.JactAddress address = GetAddressFromEditTexts();
	if (address.first_name_ == null || address.first_name_.isEmpty()) {
	  DisplayPopupFragment("Must Enter First Name", "No_first_name");
	  return;
	}
	if (address.last_name_ == null || address.last_name_.isEmpty()) {
	  DisplayPopupFragment("Must Enter Last Name", "No_last_name");
	  return;
	}
	if (address.street_addr_ == null || address.street_addr_.isEmpty()) {
	  DisplayPopupFragment("Must Enter Street Address", "No_street");
	  return;
	}
	if (address.city_ == null || address.city_.isEmpty()) {
	  DisplayPopupFragment("Must Enter City", "No_city");
	  return;
	}
	if (address.state_ == null || address.state_.isEmpty()) {
	  DisplayPopupFragment("Must Enter State", "No_state");
	  return;
	}
    if (address.zip_ == null || address.zip_.isEmpty()) {
      DisplayPopupFragment("Must Enter Valid Zip Code", "No_zip");
	  return;
	}
    ShoppingCartActivity.SetShippingAddress(address);
	fadeAllViews(true);
	if (BillingActivity.GetNumBillingAddresses() > 0) {
	  startActivity(new Intent(this, BillingActivity.class));
	} else {
	  startActivity(new Intent(this, BillingNewActivity.class));
	}
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