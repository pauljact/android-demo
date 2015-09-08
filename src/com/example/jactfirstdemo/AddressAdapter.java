package com.jact.jactapp;

import java.util.ArrayList;
 
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
 
public class AddressAdapter extends ArrayAdapter<ShoppingCartActivity.JactAddress> {

	private JactActionBarActivity parent_activity_;
	private ArrayList<ShoppingCartActivity.JactAddress> addresses_;
	private static LayoutInflater inflater_;
	
	public static class AddressViewHolder {
	  public CheckBox checkbox_;
	  public TextView name_;
	  public TextView street_;
	  public TextView city_;
	  public TextView delete_;
	  public int position_;
	}
	
	public static class CheckBoxPosition {
	  public int position_;
	}
	
	public AddressAdapter(
			JactActionBarActivity a, int resource, ArrayList<ShoppingCartActivity.JactAddress> list) {
		super(a, resource, list);
		parent_activity_ = a;
		addresses_ = list;
		inflater_ = (LayoutInflater) parent_activity_.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public int getCount() {
		return addresses_.size();
	}

	@Override
	public ShoppingCartActivity.JactAddress getItem(int position) {
		return addresses_.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ShoppingCartActivity.JactAddress address = addresses_.get(position);
		View vi;
        if (convertView == null) {
            vi = inflater_.inflate(R.layout.address_item, parent, false);
            // Could look these up below this if/else block, but more efficient
            // to use ProductViewHolder (with get/setTag) to perform the lookup once.
            AddressViewHolder viewHolder = new AddressViewHolder();
            viewHolder.checkbox_ = (CheckBox) vi.findViewById(R.id.addr_item_cb);
            viewHolder.name_ = (TextView) vi.findViewById(R.id.addr_item_name);
            viewHolder.street_ = (TextView) vi.findViewById(R.id.addr_item_street);
            viewHolder.city_ = (TextView) vi.findViewById(R.id.addr_item_city);
            viewHolder.delete_ = (TextView) vi.findViewById(R.id.addr_item_delete_item);
            CheckBoxPosition foo = new CheckBoxPosition();
            foo.position_ = position;
            viewHolder.checkbox_.setTag(foo);
            vi.setTag(viewHolder);
        } else {
        	vi = convertView;
        }
 
        // Set Product Info in the view.
        AddressViewHolder holder = (AddressViewHolder) vi.getTag();
        
        boolean has_credit_card = false;
        // Set Credit Card.
        if (address.cc_number_ != null && address.cc_number_.length() > 4 &&
        	address.cc_type_ != ShoppingCartActivity.CardType.NO_TYPE) {
          String reddit_number = address.cc_type_.toString() + " Ending in " + 
        	address.cc_number_.substring(address.cc_number_.length() - 4);
          has_credit_card = true;
          holder.name_.setText(reddit_number);
          holder.delete_.setText("Delete Credit Card");
          Log.e("PHB", "BHP address w/ CC: " + reddit_number + ", and address:\n" + address.Print());
        } else {
          holder.delete_.setText("Delete Address");
          Log.e("PHB", "BHP address w/o CC:\n" + address.Print());
        }
        
        // Set Name.
        String name = "";
        if (address.first_name_ != null) {
          name = address.first_name_;
        }
        if (address.last_name_ != null && !address.last_name_.isEmpty()) {
          if (name.isEmpty()) {
            name = address.last_name_;
          } else {
            name += " " + address.last_name_;
          }
        }
        if (has_credit_card) {
          holder.street_.setText(name);
        } else {
          holder.name_.setText(name);
        }
        
        // Set Street Address.
        String street = "";
        if (address.street_addr_ != null && !address.street_addr_.isEmpty()) {
          street = address.street_addr_;
        }
        if (address.street_addr_extra_ != null && !address.street_addr_extra_.isEmpty()) {
          if (street.isEmpty()) {
            street = address.street_addr_extra_;
          } else {
            street += " " + address.street_addr_extra_;
          }
        }
        // Only display street address on Shipping screen (for these, address.cc_* will be empty).
        if (!has_credit_card) {
          holder.street_.setText(street);
        }
        
        // Set City, State, and Zip.
        String city = "";
        if (address.city_ != null && !address.city_.isEmpty()) {
          city = address.city_;
        }
        if (address.state_ != null && !address.state_.isEmpty()) {
          if (street.isEmpty()) {
        	city = address.state_;
          } else {
        	city += " " + address.state_;
          }
        }
        if (address.zip_ != null && !address.zip_.isEmpty()) {
          if (street.isEmpty()) {
            city = address.zip_;
          } else {
        	city += " " + address.zip_;
          }
        }
        holder.city_.setText(city);
        
        return vi;
    }
}