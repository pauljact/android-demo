package com.example.jactfirstdemo;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
 
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
 
public class ProductsAdapter extends ArrayAdapter<ProductsPageParser.ProductItem> {

	private ProductsActivity parent_activity_;
	private ArrayList<ProductsPageParser.ProductItem> products_;
	private boolean[] unfinished_list_elements_;
	private static LayoutInflater inflater_;
	public ProductsImageLoader image_loader_;
	
	public static class ProductViewHolder {
	  public RelativeLayout layout_;
	  public TextView drawing_;
	  public TextView title_;
	  public TextView date_;
	  public TextView bux_;
	  public TextView price_and_;
	  public TextView points_;
	  public TextView pid_;
	  public ImageView img_;
	}
	
	public ProductsAdapter(
		    ProductsActivity a, int resource, ArrayList<ProductsPageParser.ProductItem> list,
		    String activity_name) {
		super(a, resource, list);
		parent_activity_ = a;
		products_ = list;
		unfinished_list_elements_ = new boolean[100];
		inflater_ = (LayoutInflater) parent_activity_.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        image_loader_ = new ProductsImageLoader(
        	parent_activity_.getApplicationContext(), this, activity_name);
	}
	
	public void alertPositionsReady (HashSet<Integer> positions) {
		if (positions == null) {
			Log.e("PHB ERROR", "ProductsAdapter::alertPositionsReady. Null positions");
			return;
		}
		if (positions.isEmpty()) {
			Log.e("PHB ERROR", "ProductsAdapter::alertPositionsReady. Empty positions");
			return;
		}
		boolean should_alert_state_change = false;
		for (Integer i : positions) {
			int position = i.intValue();
			if (unfinished_list_elements_[position]) {
				unfinished_list_elements_[position] = false;
				should_alert_state_change = true;
			}
		}
		if (should_alert_state_change) {
			super.notifyDataSetChanged();
		}
	}

	// Reset array of unfinished elements.
	public void ClearUnfinishedElementsTracker() {
		unfinished_list_elements_ = new boolean[100];
	}
	
	@Override
	public int getCount() {
		return products_.size();
	}

	@Override
	public ProductsPageParser.ProductItem getItem(int position) {
		return products_.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public Filter getFilter() {
		return parent_activity_.getFilter();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ProductsPageParser.ProductItem product = products_.get(position);
		View vi;
        if (convertView == null) {
            vi = inflater_.inflate(R.layout.products_item, parent, false);
            // Could look these up below this if/else block, but more efficient
            // to use ProductViewHolder (with get/setTag) to perform the lookup once.
            ProductViewHolder viewHolder = new ProductViewHolder();
            viewHolder.layout_ = (RelativeLayout) vi.findViewById(R.id.products_item_ll);
            viewHolder.drawing_ = (TextView) vi.findViewById(R.id.product_drawing_title);
            viewHolder.title_ = (TextView) vi.findViewById(R.id.product_title);
            viewHolder.date_ = (TextView) vi.findViewById(R.id.product_date);
            viewHolder.bux_ = (TextView) vi.findViewById(R.id.product_bux);
            viewHolder.price_and_ = (TextView) vi.findViewById(R.id.product_price_seperator);
            viewHolder.points_ = (TextView) vi.findViewById(R.id.product_points);
            viewHolder.pid_ = (TextView) vi.findViewById(R.id.product_pid);
            viewHolder.img_ = (ImageView) vi.findViewById(R.id.product_image);
            vi.setTag(viewHolder);
        } else {
        	vi = convertView;
        }
 
        // Set Product Info in the view.
        ProductViewHolder holder = (ProductViewHolder) vi.getTag();
        // Set Title.
        if (product.title_ != null) {
          holder.title_.setText(product.title_);
        }
        // Set Date.
        if (product.date_ == null || product.date_.isEmpty()) {
        	holder.date_.setText("N/A");
        } else {
        	holder.date_.setText(product.date_);
        }
        // Set PID.
        if (product.pid_ != null) {
          holder.pid_.setText(product.pid_);
        }
        
        // Set Price.
        String bux = "";
        if (product.bux_ != null) bux = product.bux_;
        String usd = "";
        if (product.usd_ != null) usd = product.usd_;
        String points = "";
        if (product.points_ != null) points = product.points_;
        if (!usd.isEmpty()) {
        	// Only products that sell for USD should be Jact BUX, and these should
        	// NOT have a combined price in BUX nor Points.
        	if (!bux.isEmpty()) {
        		Log.e("PHB ERROR", "ProductsAdapter::getView. Product has both USD and BUX:\n" + product.toString());
        	} else if (!points.isEmpty()) {
        		// Product has a combination of Points and USD.
        		holder.bux_.setText("$" + NumberFormat.getNumberInstance(Locale.US).format(Double.parseDouble(usd)) + " USD");
        		holder.points_.setText(NumberFormat.getNumberInstance(Locale.US).format(Integer.parseInt(points)) + " Points");
        		holder.points_.setVisibility(View.VISIBLE);
        		holder.price_and_.setVisibility(View.VISIBLE);
        	} else {
        		holder.bux_.setText("$" + NumberFormat.getNumberInstance(Locale.US).format(Double.parseDouble(usd)) + " USD");
        		holder.points_.setVisibility(View.INVISIBLE);
        		holder.price_and_.setVisibility(View.INVISIBLE);
        	}
        } else if (!bux.isEmpty()) {
        	if (!points.isEmpty()) {
        		// Product has a combination of Points and BUX.
        		holder.bux_.setText("J" + NumberFormat.getNumberInstance(Locale.US).format(Integer.parseInt(bux)) + " BUX");
        		holder.points_.setText(NumberFormat.getNumberInstance(Locale.US).format(Integer.parseInt(points)) + " Points");
        		holder.points_.setVisibility(View.VISIBLE);
        		holder.price_and_.setVisibility(View.VISIBLE);
        	} else {
        		// BUX only Product.
        		holder.bux_.setText("J" + NumberFormat.getNumberInstance(Locale.US).format(Integer.parseInt(bux)) + " BUX");
        		holder.points_.setVisibility(View.INVISIBLE);
        		holder.price_and_.setVisibility(View.INVISIBLE);
        	}
        } else if (!points.isEmpty()) {
        	// Points Only Product.
        	holder.bux_.setText(NumberFormat.getNumberInstance(Locale.US).format(Integer.parseInt(points)) + " Points");
    		holder.points_.setVisibility(View.INVISIBLE);
    		holder.price_and_.setVisibility(View.INVISIBLE);
        } else {
        	Log.e("PHB ERROR", "ProductsAdapter::getView. Product has no price:\n" + product.toString());
        }
        
        // Set Image.
        if (product.img_url_ != null &&
            !image_loader_.DisplayImage(product.img_url_, holder.img_, position)) {
          unfinished_list_elements_[position] = true;
        }
        
        // Set Different color background for prize drawings.
        String node_type = "";
        if (product.node_type_ != null) node_type = product.node_type_;
        if (node_type.equalsIgnoreCase("jact_prize_drawing")) {
        	holder.layout_.setBackgroundResource(R.drawable.products_drawing_bg_selector);
        	holder.drawing_.setVisibility(View.VISIBLE);
        } else {
        	holder.layout_.setBackgroundResource(R.drawable.products_bg_selector);
        	holder.drawing_.setVisibility(View.GONE);
        }
        
        return vi;
    }
}