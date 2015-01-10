package com.example.jactfirstdemo;

import java.util.Comparator;
import java.util.HashMap;

import android.util.Log;

public class JactComparators {
  static public Comparator<ProductsPageParser.ProductItem> TitleComparatorAscending() {
    return new Comparator<ProductsPageParser.ProductItem>() {
	    public int compare(ProductsPageParser.ProductItem arg0,
	    		           ProductsPageParser.ProductItem arg1) {
	    	if (arg0.title_ == null) return 1;
	    	if (arg1.title_ == null) return -1;
	    	return arg0.title_.compareTo(arg1.title_);
	    }
	};
  }

  static public Comparator<ProductsPageParser.ProductItem> TitleComparatorDescending() {
    return new Comparator<ProductsPageParser.ProductItem>() {
	    public int compare(ProductsPageParser.ProductItem arg0,
	    		           ProductsPageParser.ProductItem arg1) {
	    	if (arg0.title_ == null) return -1;
	    	if (arg1.title_ == null) return 1;
	    	return arg1.title_.compareTo(arg0.title_);
	    }
	};
  }
  
  // Expected format:
  //   DD/MM/YYYY - HH:MMxm
  // where x = 'a' or 'p'
  static public boolean ParseDate(String date, JactDateHolder holder) {
	  int length = date.length();
	  if (length < 10) return false;
	  String day_str = date.substring(0, 2);
	  String month_str = date.substring(3, 5);
	  String year_str = date.substring(6, 10);
	  String hour_str = "";
	  String min_str = "";
	  String am_pm = "";
	  if (length > 10) {
		  am_pm = date.substring(length - 2, length);
		  if (am_pm != "am" && am_pm != "pm") {
			  Log.e("PHB", "JactComparators::ParseDate. Unknown trailing two characters in :" + date);
			  return false;
		  }
		  int hyphen = date.indexOf("- ");
		  int colon = date.indexOf(":");
		  if (hyphen >= 0 && colon > hyphen + 2 && colon + 2 < length) {
			  hour_str = date.substring(hyphen + 2, hyphen + 4);
			  min_str = date.substring(colon + 1, colon + 3);
		  }
	  }
	  try {
		  holder.day_ = Integer.parseInt(day_str);
		  holder.month_ = Integer.parseInt(month_str);
		  holder.year_ = Integer.parseInt(year_str);
		  if (!am_pm.isEmpty()) {
			  holder.am_ = am_pm == "am";
			  holder.hour_ = Integer.parseInt(hour_str);
			  holder.min_ = Integer.parseInt(min_str);
		  }
	  } catch (NumberFormatException e) {
		  Log.e("PHB ERROR", "JactComparators::ParseDate. Unable to parse: " + date);
		  return false;
	  }
	  return true;
  }
  
  static private int CompareDateHolders(JactDateHolder holder_one,
		                                JactDateHolder holder_two) {
	  if (holder_one.year_ != holder_two.year_) {
		  return holder_one.year_ < holder_two.year_ ? -1 : 1;
	  }
	  if (holder_one.month_ != holder_two.month_) {
		  return holder_one.month_ < holder_two.month_ ? -1 : 1;
	  }
	  if (holder_one.day_ != holder_two.day_) {
		  return holder_one.day_ < holder_two.day_ ? -1 : 1;
	  }
	  if (holder_one.am_ != holder_two.am_) {
		  return holder_one.am_ ? -1 : 1;
	  }
	  if (holder_one.hour_ != holder_two.hour_) {
		  return holder_one.hour_ < holder_two.hour_ ? -1 : 1;
	  }
	  if (holder_one.min_ != holder_two.min_) {
		  return holder_one.min_ < holder_two.min_ ? -1 : 1;
	  }
	  return 0;
  }
  
  static public int CompareDates(String date_one, String date_two) {
	  JactDateHolder holder_one = new JactDateHolder();
	  JactDateHolder holder_two = new JactDateHolder();
	  if (!ParseDate(date_one, holder_one) || !ParseDate(date_two, holder_two)) {
		  return date_one.compareTo(date_two);
	  }
	  return CompareDateHolders(holder_one, holder_two);
  }

  static public Comparator<ProductsPageParser.ProductItem> DateComparatorAscending() {
    return new Comparator<ProductsPageParser.ProductItem>() {
	    public int compare(ProductsPageParser.ProductItem arg0,
	    		           ProductsPageParser.ProductItem arg1) {
	    	if (arg0.date_ == null) return 1;
	    	if (arg1.date_ == null) return -1;
	    	return arg0.date_.compareTo(arg1.date_);
	    }
	};
  }

  static public Comparator<ProductsPageParser.ProductItem> DateComparatorDescending() {
    return new Comparator<ProductsPageParser.ProductItem>() {
	    public int compare(ProductsPageParser.ProductItem arg0,
	    		           ProductsPageParser.ProductItem arg1) {
	    	if (arg0.date_ == null) return -1;
	    	if (arg1.date_ == null) return 1;
	    	return arg1.date_.compareTo(arg0.date_);
	    }
	};
  }
  
  static public Comparator<ProductsPageParser.ProductItem> BuxComparatorAscending() {
    return new Comparator<ProductsPageParser.ProductItem>() {
	    public int compare(ProductsPageParser.ProductItem arg0,
	    		           ProductsPageParser.ProductItem arg1) {
	    	if ((arg1.usd_ != null && arg1.usd_.isEmpty()) !=
	    		(arg0.usd_ != null && arg0.usd_.isEmpty())) {
	    		return (arg0.usd_ != null && arg0.usd_.isEmpty()) ? -1 : 1;
	    	} else if (arg0.usd_ != null && !arg0.usd_.isEmpty() && !arg0.usd_.equalsIgnoreCase(arg1.usd_)) {
	    		return arg0.usd_.compareTo(arg1.usd_);
	    	} else if ((arg1.points_ != null && arg1.points_.isEmpty()) != 
	    			   (arg0.points_ != null && arg0.points_.isEmpty())) {
	    		return (arg0.points_ != null && arg0.points_.isEmpty()) ? -1 : 1;
	    	} else if (arg0.points_ != null && !arg0.points_.isEmpty() && !arg0.points_.equalsIgnoreCase(arg1.points_)) {
	    		return arg0.points_.compareTo(arg1.points_);
	    	} else if ((arg1.bux_ != null && arg1.bux_.isEmpty()) !=
		    		   (arg0.bux_ != null && arg0.bux_.isEmpty())) {
		    	return (arg0.bux_ != null && arg0.bux_.isEmpty()) ? -1 : 1;
	    	} else if (arg0.bux_ != null && arg0.bux_.isEmpty()) {
	    		return arg0.bux_.compareTo(arg1.bux_);
	    	}
	    	// Only reach here if all three prices of both items are null and/or empty.
	    	return 0;
	    }
	};
  }

  static public Comparator<ProductsPageParser.ProductItem> BuxComparatorDescending() {
    return new Comparator<ProductsPageParser.ProductItem>() {
	    public int compare(ProductsPageParser.ProductItem arg0,
	    		           ProductsPageParser.ProductItem arg1) {
	    	return arg1.bux_.compareTo(arg0.bux_);
	    }
	};
  }
}
