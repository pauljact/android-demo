package com.jact.jactapp;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
 
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
 
public class ProductsAdapter extends ArrayAdapter<ProductsPageParser.ProductItem> implements AdapterWithImages {

	private ProductsActivity parent_activity_;
	private ArrayList<ProductsPageParser.ProductItem> products_;
	private boolean[] unfinished_list_elements_;
	private static LayoutInflater inflater_;
	public ProductsImageLoader image_loader_;
	
	public static class ProductViewHolder {
	  public RelativeLayout layout_;
	  public TextView drawing_;
	  public TextView title_;
	  public TextView summary_;
	  public TextView max_quantity_;
	  public TextView date_;
	  public TextView orig_price_usd_;
	  public ImageView orig_price_point_icon_;
	  public TextView orig_price_points_;
	  public TextView jact_price_usd_;
	  public ImageView jact_price_point_icon_;
	  public TextView jact_price_points_;
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
	
	@Override
	public void AlertPositionsReady (HashSet<Integer> positions) {
		if (positions == null) {
			Log.e("ProductsAdapter::alertPositionsReady", "Null positions");
			return;
		}
		if (positions.isEmpty()) {
			Log.e("ProductsAdapter::alertPositionsReady", "Empty positions");
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
            viewHolder.summary_ = (TextView) vi.findViewById(R.id.product_summary);
            viewHolder.max_quantity_ = (TextView) vi.findViewById(R.id.product_max_quantity);
            viewHolder.date_ = (TextView) vi.findViewById(R.id.product_date);
            viewHolder.orig_price_usd_ = (TextView) vi.findViewById(R.id.product_orig_price_usd);
            viewHolder.orig_price_point_icon_ = (ImageView) vi.findViewById(R.id.product_orig_price_points_icon);
            viewHolder.orig_price_points_ = (TextView) vi.findViewById(R.id.product_orig_price_points);
            viewHolder.jact_price_usd_ = (TextView) vi.findViewById(R.id.product_jact_price_usd);
            viewHolder.jact_price_point_icon_ = (ImageView) vi.findViewById(R.id.product_jact_price_points_icon);
            viewHolder.jact_price_points_ = (TextView) vi.findViewById(R.id.product_jact_price_points);
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
        
        // Set Summary.
        if (product.summary_ != null) {
          holder.summary_.setText(product.summary_);
        }
        
        // Set Max Quantity.
        if (product.max_quantity_ != null) {
          holder.max_quantity_.setText(product.max_quantity_);
        } else {
          holder.max_quantity_.setText("");
        }
        
        // Set Drawing Date.
        if (product.date_ != null && !product.date_.isEmpty()) {
          holder.date_.setVisibility(View.VISIBLE);
          holder.date_.setText(product.date_);
      	  holder.date_.setGravity(Gravity.CENTER_HORIZONTAL);
        } else {
          holder.date_.setVisibility(View.GONE);
        }
        
        // Set PID.
        if (product.pid_ != null) {
          holder.pid_.setText(product.pid_);
        }
        
        // Set Original Price.
        SetPrice(product, holder, true);
        
        // Set Price.
        SetPrice(product, holder, false);
        
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
        	holder.drawing_.setGravity(Gravity.CENTER_HORIZONTAL);
        } else {
        	holder.layout_.setBackgroundResource(R.drawable.products_bg_selector);
        	holder.drawing_.setVisibility(View.GONE);
        }
        
        return vi;
    }
	
	private void SetPrice(ProductsPageParser.ProductItem product,
			              ProductViewHolder holder,
			              boolean isOriginalPrice) {
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
          Log.e("ProductsAdapter::SetPrice", "Product has both USD and BUX:\n" + product.toString());
        } else if (!points.isEmpty()) {
          // Product has a combination of Points and USD.
          if (isOriginalPrice) {
            holder.orig_price_usd_.setText(parent_activity_.getString(R.string.orig_price_str) +
            	" $" + NumberFormat.getNumberInstance(Locale.US).format(Double.parseDouble(usd)) +
        		" + ");
            holder.orig_price_point_icon_.setVisibility(View.VISIBLE);
            holder.orig_price_points_.setText(
            	NumberFormat.getNumberInstance(Locale.US).format(Integer.parseInt(points)));
          } else {
            holder.jact_price_usd_.setText(parent_activity_.getString(R.string.jact_price_str) +
            	" $" + NumberFormat.getNumberInstance(Locale.US).format(Double.parseDouble(usd)) +
		        " + ");
            holder.jact_price_point_icon_.setVisibility(View.VISIBLE);
            holder.jact_price_points_.setText(NumberFormat.getNumberInstance(Locale.US).format(Integer.parseInt(points)));
          }
        } else {
          if (isOriginalPrice) {
            holder.orig_price_usd_.setText(parent_activity_.getString(R.string.orig_price_str) +
        	    " $" + NumberFormat.getNumberInstance(Locale.US).format(Double.parseDouble(usd)));
            holder.orig_price_point_icon_.setVisibility(View.GONE);
          } else {
            holder.jact_price_usd_.setText(parent_activity_.getString(R.string.jact_price_str) +
                " $" + NumberFormat.getNumberInstance(Locale.US).format(Double.parseDouble(usd)));
            holder.jact_price_point_icon_.setVisibility(View.VISIBLE);
          }
        }
      } else if (!bux.isEmpty()) {
       	if (!points.isEmpty()) {
          // Product has a combination of Points and BUX.
       	  if (isOriginalPrice) {
       		holder.orig_price_usd_.setText(parent_activity_.getString(R.string.orig_price_str) +
       			" J" + NumberFormat.getNumberInstance(Locale.US).format(Integer.parseInt(bux)) + " + ");
            holder.orig_price_point_icon_.setVisibility(View.VISIBLE);
       		holder.orig_price_points_.setText(NumberFormat.getNumberInstance(Locale.US).format(Integer.parseInt(points)));
       	  } else {
         	holder.jact_price_usd_.setText(parent_activity_.getString(R.string.jact_price_str) +
            	" J" + NumberFormat.getNumberInstance(Locale.US).format(Integer.parseInt(bux)) + " + ");
            holder.jact_price_point_icon_.setVisibility(View.VISIBLE);
         	holder.jact_price_points_.setText(NumberFormat.getNumberInstance(Locale.US).format(Integer.parseInt(points)));
       	  }
        } else {
          // BUX only Product.
          if (isOriginalPrice) {
        	holder.orig_price_usd_.setText(parent_activity_.getString(R.string.orig_price_str) +
        	    " J" + NumberFormat.getNumberInstance(Locale.US).format(Integer.parseInt(bux)));
            holder.orig_price_point_icon_.setVisibility(View.GONE);
          } else {
          	holder.jact_price_usd_.setText(parent_activity_.getString(R.string.jact_price_str) +
            	" J" + NumberFormat.getNumberInstance(Locale.US).format(Integer.parseInt(bux)));
            holder.jact_price_point_icon_.setVisibility(View.GONE);
          }
        }
      } else if (!points.isEmpty()) {
       	// Points Only Product.
    	if (isOriginalPrice) {
      	  holder.orig_price_usd_.setText(parent_activity_.getString(R.string.orig_price_str));
          holder.orig_price_point_icon_.setVisibility(View.VISIBLE);
    	  holder.orig_price_points_.setText(NumberFormat.getNumberInstance(Locale.US).format(Integer.parseInt(points)));
    	} else {
    	  holder.jact_price_usd_.setText(parent_activity_.getString(R.string.jact_price_str));
          holder.jact_price_point_icon_.setVisibility(View.VISIBLE);
      	  holder.jact_price_points_.setText(NumberFormat.getNumberInstance(Locale.US).format(Integer.parseInt(points)));
    	}
      } else {
       	Log.e("ProductsAdapter::SetPrice", "Product has no price:\n" + product.toString());
      }
	}

	@Override
	public Drawable GetDrawable(int resource_id) {
		return parent_activity_.getResources().getDrawable(resource_id);
	}

	@Override
	public Drawable GetDrawable(Bitmap bitmap) {
	  return new BitmapDrawable(parent_activity_.getResources(), bitmap);
	}
}