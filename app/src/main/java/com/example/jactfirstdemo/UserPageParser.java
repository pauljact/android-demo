package com.example.jactfirstdemo;

import java.util.ArrayList;

import com.example.jactfirstdemo.ShoppingCartActivity.JactUserCompleteInfo;

public class UserPageParser {
	static public void ParseUserInfoFromWebpage(
		String webpage, ShoppingCartActivity.JactUserCompleteInfo user_info) {
	  // TODO(PHB): Implement this once server is ready with a url target.
	  user_info.bux_ = 4100;
	  user_info.points_ = 1000;
	  if (user_info.shipping_addresses_ == null) {
	    user_info.shipping_addresses_ = new ArrayList<ShoppingCartActivity.JactAddress>();
	  }
	  ShoppingCartActivity.JactAddress address = new ShoppingCartActivity.JactAddress();
	  address.city_ = "los angeles";
	  address.state_ = "CA";
	  address.first_name_ = "Paul";
	  address.last_name_ = "Bunn";
	  address.street_addr_ = "1234 Main Street";
	  address.zip_ = "90025";
	  user_info.shipping_addresses_.add(address);
	}
}