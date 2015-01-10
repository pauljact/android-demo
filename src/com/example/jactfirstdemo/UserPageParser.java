package com.example.jactfirstdemo;

import com.example.jactfirstdemo.ShoppingCartActivity.JactUserCompleteInfo;

public class UserPageParser {
	static public void ParseUserInfoFromWebpage(
		String webpage, ShoppingCartActivity.JactUserCompleteInfo user_info) {
	  // TODO(PHB): Implement this once server is ready with a url target.
	  user_info.bux_ = 4100;
	  user_info.points_ = 1000;
	}
}