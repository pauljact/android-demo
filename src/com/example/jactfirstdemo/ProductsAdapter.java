package com.example.jactfirstdemo;

import java.util.ArrayList;
import java.util.HashMap;
 
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
 
public class ProductsAdapter extends BaseAdapter {

	private Activity parent_activity_;
	private ArrayList<HashMap<String, String>> products_;
	private static LayoutInflater inflater_;
	public ProductsImageLoader image_loader_;
	
	public ProductsAdapter(Activity a, ArrayList<HashMap<String, String>> list) {
		parent_activity_ = a;
		products_ = list;
		inflater_ = (LayoutInflater) parent_activity_.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        image_loader_ = new ProductsImageLoader(parent_activity_.getApplicationContext());
	}
	
	@Override
	public int getCount() {
		return products_.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View vi;
        if (convertView == null) {
            vi = inflater_.inflate(R.layout.products_item, null);
        } else {
        	vi = convertView;
        }
 
        TextView title = (TextView) vi.findViewById(R.id.product_title);
        TextView artist = (TextView) vi.findViewById(R.id.product_date);
        TextView duration = (TextView) vi.findViewById(R.id.product_bux);
        ImageView thumb_image= (ImageView) vi.findViewById(R.id.product_image);
 
        HashMap<String, String> product = new HashMap<String, String>();
        product = products_.get(position);
 
        // Setting all values in listview
        title.setText(product.get(ProductsActivity.PRODUCT_TITLE_KEY));
        artist.setText(product.get(ProductsActivity.PRODUCT_DATE_KEY));
        duration.setText(product.get(ProductsActivity.PRODUCT_BUX_KEY));
        Log.e("PHB", "About to fetch image at: " + product.get(ProductsActivity.PRODUCT_IMAGE_KEY));
        image_loader_.DisplayImage(product.get(ProductsActivity.PRODUCT_IMAGE_KEY), thumb_image);
        return vi;
    }
}
