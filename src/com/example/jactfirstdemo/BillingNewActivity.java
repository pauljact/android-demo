package com.example.jactfirstdemo;


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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class BillingNewActivity extends JactActionBarActivity implements OnItemSelectedListener, ProcessUrlResponseCallback {
  private JactNavigationDrawer navigation_drawer_;
  private JactDialogFragment dialog_;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState);
    num_server_tasks_ = 0;
    setContentView(R.layout.credit_card_new_layout);
    Toolbar toolbar = (Toolbar) findViewById(R.id.jact_toolbar);
    TextView ab_title = (TextView) findViewById(R.id.toolbar_title_tv);
    ab_title.setText(R.string.billing_label);
    setSupportActionBar(toolbar);
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    
    // Setup Spinner (Dropdown for Credit Card Type).
    Spinner type = (Spinner) findViewById(R.id.credit_card_type_spinner);
    ArrayAdapter<CharSequence> type_adapter = ArrayAdapter.createFromResource(
    		this, R.array.cc_types_array, R.layout.jact_state_spinner_item);
    type_adapter.setDropDownViewResource(R.layout.jact_state_spinner_dropdown);
    type.setAdapter(type_adapter);
    type.setOnItemSelectedListener((OnItemSelectedListener) this);
    type.setSelection(0);
    
    // Setup Spinner (Dropdown for Expiration Month).
    Spinner month = (Spinner) findViewById(R.id.credit_card_month_spinner);
    ArrayAdapter<CharSequence> month_adapter = ArrayAdapter.createFromResource(
    		this, R.array.months_array, R.layout.jact_state_spinner_item);
    month_adapter.setDropDownViewResource(R.layout.jact_state_spinner_dropdown);
    month.setAdapter(month_adapter);
    month.setOnItemSelectedListener((OnItemSelectedListener) this);
    month.setSelection(0);
    
    // Setup Spinner (Dropdown for Expiration Year).
    Spinner year = (Spinner) findViewById(R.id.credit_card_year_spinner);
    ArrayAdapter<CharSequence> year_adapter = ArrayAdapter.createFromResource(
    		this, R.array.years_array, R.layout.jact_state_spinner_item);
    year_adapter.setDropDownViewResource(R.layout.jact_state_spinner_dropdown);
    year.setAdapter(year_adapter);
    year.setOnItemSelectedListener((OnItemSelectedListener) this);
    year.setSelection(0);

 // Setup Spinner (Dropdown for State).
    Spinner state = (Spinner) findViewById(R.id.credit_card_new_state_spinner);
    ArrayAdapter<CharSequence> state_adapter = ArrayAdapter.createFromResource(
    		this, R.array.states, R.layout.jact_state_spinner_item);
    state_adapter.setDropDownViewResource(R.layout.jact_state_spinner_dropdown);
    state.setAdapter(state_adapter);
    state.setOnItemSelectedListener((OnItemSelectedListener) this);
    state.setSelection(0);
    
    // Set Navigation Drawer.
    navigation_drawer_ =
        new JactNavigationDrawer(this,
        		                 findViewById(R.id.credit_card_new_drawer_layout),
        		                 findViewById(R.id.credit_card_new_left_drawer),
        		                 JactNavigationDrawer.ActivityIndex.BILLING_NEW);
  }
    
  @Override
  protected void onResume() {
	SetCartIcon(this);
	fadeAllViews(num_server_tasks_ > 0);
	super.onResume();
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
    boolean set_cart_icon = false;
    if (menu_bar_ == null) set_cart_icon = true;
    menu_bar_ = menu;
    if (set_cart_icon) {
      SetCartIcon(this);
    }
	ShoppingCartActivity.SetCartIcon(menu);
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
    ProgressBar spinner = (ProgressBar) findViewById(R.id.credit_card_new_progress_bar);
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
    RelativeLayout layout = (RelativeLayout) findViewById(R.id.credit_card_new_content_frame);
    layout.startAnimation(alpha); // Add animation to the layout.
  }
  
  public void doDialogOkClick(View view) {
	// Close Dialog window.
	dialog_.dismiss();
  }
  
  public void doBillingNewPrevButtonClick(View view) {
      fadeAllViews(true);
	  startActivity(new Intent(this, ShippingActivity.class));
  }
  
  public void onSameAsShippingAddrCheckboxClicked(View view) {
	  SetEditViewsFromAddress(ShoppingCartActivity.GetShippingAddress());
  }
  
  private int GetStatePosFromString(String[] states_array, String state) {
	  if (states_array == null || state == null || state.isEmpty()) return -1;
	  for (int i = 0; i < states_array.length; ++i) {
        if (state.equalsIgnoreCase(states_array[i])) {
        	return i;
        }
	  }
	  return -1;
  }
  
  private void SetEditViewsFromAddress(ShoppingCartActivity.JactAddress address) {
	  if (address == null) return;
	  // TODO(PHB): Add try/catch and other error catching below.
	  if (address.first_name_ != null && !address.first_name_.isEmpty()) {
	    EditText first_name = (EditText) findViewById(R.id.credit_card_new_first_name_et);
	    first_name.setText(address.first_name_);
	  }
	  if (address.last_name_ != null && !address.last_name_.isEmpty()) {
	    EditText last_name = (EditText) findViewById(R.id.credit_card_new_last_name_et);
	    last_name.setText(address.last_name_);
	  }
	  if (address.cc_type_ != null) {
	    Spinner type = (Spinner) findViewById(R.id.credit_card_type_spinner);
	    if (address.cc_type_ == ShoppingCartActivity.CardType.VISA) {
	      type.setSelection(1);
	    } else if (address.cc_type_ == ShoppingCartActivity.CardType.MC) {
		  type.setSelection(2);
	    } else if (address.cc_type_ == ShoppingCartActivity.CardType.AMEX) {
		  type.setSelection(3);
	    } else if (address.cc_type_ == ShoppingCartActivity.CardType.NO_TYPE) {
		  // TODO(PHB): Handle this.
	    }
	  }
	  if (address.cc_number_ != null && !address.cc_number_.isEmpty()) {
	    EditText cc_number = (EditText) findViewById(R.id.credit_card_card_number_et);
	    cc_number.setText(address.cc_number_);
	  }
	  if (address.cc_crv_ > 0) {
	    EditText crv = (EditText) findViewById(R.id.credit_card_crv_et);
	    crv.setText("" + address.cc_crv_);
	  }
	  if (address.cc_exp_date_month_ > 0) {
	    Spinner month = (Spinner) findViewById(R.id.credit_card_month_spinner);
	    if (address.cc_exp_date_month_ > 0 && address.cc_exp_date_month_ < 13) {
	      month.setSelection(address.cc_exp_date_month_);
	    }
	  }
	  if (address.cc_exp_date_year_ > 0) {
	    Spinner year = (Spinner) findViewById(R.id.credit_card_year_spinner);
	    if (address.cc_exp_date_year_ > 2014 && address.cc_exp_date_year_ < 2026) {
	      year.setSelection(address.cc_exp_date_year_ - 2014);
	    }
	  }
	  String street_addr = "";
	  if (address.street_addr_ != null && !address.street_addr_.isEmpty()) {
	    street_addr = address.street_addr_;
	  }
	  if (address.street_addr_extra_ != null && !address.street_addr_extra_.isEmpty()) {
	    if (street_addr.isEmpty()) {
	      street_addr = address.street_addr_extra_;
	    } else {
		  street_addr += " " + address.street_addr_extra_;
	    }
	  }
	  if (!street_addr.isEmpty()) {
		EditText street = (EditText) findViewById(R.id.credit_card_new_street_et); 
		street.setText(street_addr);
	  }
	  if (address.city_ != null && !address.city_.isEmpty()) {
	    EditText city = (EditText) findViewById(R.id.credit_card_new_city_et);
	    city.setText(address.city_);
	  }
	  int pos = GetStatePosFromString(getResources().getStringArray(R.array.states),
			                          address.state_);
	  if (pos > 0) {
	    Spinner state = (Spinner) findViewById(R.id.credit_card_new_state_spinner);
	    state.setSelection(pos);
	  } else {
		  Log.e("PHB", "BHP. State: " + address.state_ + ", pos: " + pos);
	  }
	  if (address.zip_ != null && !address.zip_.isEmpty()) {
	    EditText zip = (EditText) findViewById(R.id.credit_card_new_zip_et);
	    zip.setText(address.zip_);
	  }
  }
  
  private ShoppingCartActivity.JactAddress GetAddressFromEditViews() {
	  // TODO(PHB): Add try/catch and other error catching below.
	  ShoppingCartActivity.JactAddress address = new ShoppingCartActivity.JactAddress();
	  EditText first_name = (EditText) findViewById(R.id.credit_card_new_first_name_et);
	  address.first_name_ = first_name.getText().toString();
	  EditText last_name = (EditText) findViewById(R.id.credit_card_new_last_name_et);
	  address.last_name_ = last_name.getText().toString();
	  Spinner type = (Spinner) findViewById(R.id.credit_card_type_spinner);
	  String cc_type = (String) type.getSelectedItem();
	  if (!cc_type.equalsIgnoreCase(getResources().getString(R.string.cc_type_str))) {
	    if (cc_type.equalsIgnoreCase("Visa")) {
	      address.cc_type_ = ShoppingCartActivity.CardType.VISA;
	    } else if (cc_type.equalsIgnoreCase("MasterCard")) {
	      address.cc_type_ = ShoppingCartActivity.CardType.MC;
	    } else if (cc_type.equalsIgnoreCase("AmEx")) {
	      address.cc_type_ = ShoppingCartActivity.CardType.AMEX;
	    }
	  }
	  EditText cc_number = (EditText) findViewById(R.id.credit_card_card_number_et);
	  address.cc_number_ = cc_number.getText().toString();
	  EditText crv = (EditText) findViewById(R.id.credit_card_crv_et);
	  try {
	    address.cc_crv_ = Integer.parseInt(crv.getText().toString());
	  } catch (NumberFormatException e) {
		  address.cc_crv_ = -1;
	  }
	  Spinner month = (Spinner) findViewById(R.id.credit_card_month_spinner);
	  String cc_month = (String) month.getSelectedItem();
	  if (!cc_month.equalsIgnoreCase("Month")) {
	    address.cc_exp_date_month_ = Integer.parseInt(cc_month);
	  }
	  Spinner year = (Spinner) findViewById(R.id.credit_card_year_spinner);
	  String cc_year = (String) year.getSelectedItem();
	  if (!cc_year.equalsIgnoreCase("Year")) {
	    address.cc_exp_date_year_ = Integer.parseInt(cc_year);
	  }
	  EditText street = (EditText) findViewById(R.id.credit_card_new_street_et);
	  address.street_addr_ = street.getText().toString(); 
	  EditText city = (EditText) findViewById(R.id.credit_card_new_city_et);
	  address.city_ = city.getText().toString();
	  Spinner state = (Spinner) findViewById(R.id.credit_card_new_state_spinner);
	  String cc_state = (String) state.getSelectedItem();
	  if (!cc_state.equalsIgnoreCase("State")) {
	    address.state_ = cc_state;
	  }
	  EditText zip = (EditText) findViewById(R.id.credit_card_new_zip_et);
	  address.zip_ = zip.getText().toString();
	  return address;
  }
  
  public void doBillingNewNextButtonClick(View view) {
    // Parse Page, make sure all information is correct. If so, proceed to review.
	// Otherwise, popup dialog warning about missing/incorrect information.
	ShoppingCartActivity.JactAddress address = GetAddressFromEditViews();
	if (address == null) {
	  // TODO(PHB): Handle this.
	  return;
	}
	if (address.first_name_ == null || address.first_name_.isEmpty()) {
	  dialog_ = new JactDialogFragment("Must Enter First Name");
	  dialog_.show(getSupportFragmentManager(), "No_first_name_billing");
	  return;
	}
	if (address.last_name_ == null || address.last_name_.isEmpty()) {
	  dialog_ = new JactDialogFragment("Must Enter Last Name");
	  dialog_.show(getSupportFragmentManager(), "No_last_name_billing");
	  return;
	}
	if (address.cc_type_ == ShoppingCartActivity.CardType.NO_TYPE) {
	  dialog_ = new JactDialogFragment("Must Enter Credit Card Type");
	  dialog_.show(getSupportFragmentManager(), "No_type");
	  return;
	}
	if (address.cc_number_ == null || address.cc_number_.isEmpty()) {
	  dialog_ = new JactDialogFragment("Must Enter Credit Card Number");
	  dialog_.show(getSupportFragmentManager(), "No_cc_number");
	  return;
	}
	if (address.cc_exp_date_month_ == 0) {
	  dialog_ = new JactDialogFragment("Must Enter Expiration Date (Month)");
	  dialog_.show(getSupportFragmentManager(), "No_cc_month");
	  return;
	}
	if (address.cc_exp_date_year_ == 0) {
	  dialog_ = new JactDialogFragment("Must Enter Expiration Date (Year)");
	  dialog_.show(getSupportFragmentManager(), "No_cc_year");
	  return;
	}
	if (address.cc_crv_ < 0) {
	  dialog_ = new JactDialogFragment("Must Enter Credit Card CRV");
	  dialog_.show(getSupportFragmentManager(), "No_crv");
	  return;
	}
	if (address.street_addr_ == null || address.street_addr_.isEmpty()) {
	  dialog_ = new JactDialogFragment("Must Enter Street Address");
	  dialog_.show(getSupportFragmentManager(), "No_street_billing");
	  return;
	}
	if (address.city_ == null || address.city_.isEmpty()) {
	  dialog_ = new JactDialogFragment("Must Enter City");
	  dialog_.show(getSupportFragmentManager(), "No_city_billing");
	  return;
	}
	if (address.state_ == null || address.state_.isEmpty()) {
	  dialog_ = new JactDialogFragment("Must Enter State");
	  dialog_.show(getSupportFragmentManager(), "No_state_billing");
	  return;
	}
	String zip_str = "";
    if (address.zip_ != null) {
      zip_str = address.zip_; 
    }
	boolean valid_zip = false;
	if (!zip_str.isEmpty() && zip_str.length() == 5) {
	  try {
	    Integer.parseInt(zip_str);
	    valid_zip = true;
	  } catch (NumberFormatException e) {
	    // Nothing to do here: will skip setting valid_zip to true in the 'try' statement above.
	  }
	}
    if (!valid_zip) {
	  dialog_ = new JactDialogFragment("Must Enter Valid Zip Code");
	  dialog_.show(getSupportFragmentManager(), "No_zip_billing");
	  return;
	}
	fadeAllViews(true);
    ShoppingCartActivity.SetBillingAddress(address);
	startActivity(new Intent(this, ReviewCartActivity.class));
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
	// TODO(PHB): Implement response to Spinners, if necessary.
	if (position != 0) {
	  TextView tv = (TextView) view;
	  tv.setTextColor(getResources().getColor(R.color.black));
	}
	if (view.getId() == R.id.credit_card_new_state_spinner) {
	} else if (view.getId() == R.id.credit_card_year_spinner) {
	} else if (view.getId() == R.id.credit_card_month_spinner) {
	} else if (view.getId() == R.id.credit_card_type_spinner) {
	} else {
	}   
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
  }

  @Override
  public void ProcessUrlResponse(String webpage, String cookies, String extra_params) {
	ProcessCartResponse(webpage, cookies, extra_params);
  }

  @Override
  public void ProcessUrlResponse(Bitmap pic, String cookies, String extra_params) {
	// TODO Auto-generated method stub
  }

  @Override
  public void ProcessFailedResponse(FetchStatus status, String extra_params) {
	ProcessFailedCartResponse(status, extra_params);
  }
}