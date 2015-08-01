package com.example.jactfirstdemo;

import java.util.ArrayList;
import java.util.HashSet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AvatarAdapter extends ArrayAdapter<NewUserActivity.AvatarItem>
                           implements AdapterWithImages {
	private NewUserActivity parent_activity_;
	private ArrayList<NewUserActivity.AvatarItem> items_;
	private boolean[] unfinished_list_elements_;
	public ProductsImageLoader image_loader_;
	private static LayoutInflater inflater_;
	
	public static class AvatarViewHolder {
	  public TextView id_;
	  public ImageView img_;
    }
	//public AvatarAdapter(NewUserActivity a, int layout_id, int tv_resource_id, ArrayList<NewUserActivity.AvatarItem> items) {
	public AvatarAdapter(NewUserActivity a, int layout_id, ArrayList<NewUserActivity.AvatarItem> items) {
		//super(a, layout_id, tv_resource_id, items);
		// Must override (implement) both getView and getDropDownView (see below), or else you'll get
		// a NullPointerException when app tries to run the default functionality for getDropDownView
		// (it will try to convert the layout_id as a TextView).
		super(a, layout_id, items);
		parent_activity_ = a;
		items_ = items;
		inflater_ = (LayoutInflater) parent_activity_.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		unfinished_list_elements_ = new boolean[100];
        image_loader_ = new ProductsImageLoader(
        	parent_activity_.getApplicationContext(), this, "NewUserActivity");
	}
	
	@Override
	public void AlertPositionsReady (HashSet<Integer> positions) {
	  if (positions == null) {
		if (!JactActionBarActivity.IS_PRODUCTION) Log.e("AvatarAdapter::alertPositionsReady", "Null positions");
		return;
	  }
	  if (positions.isEmpty()) {
		if (!JactActionBarActivity.IS_PRODUCTION) Log.e("AvatarAdapter::alertPositionsReady", "Empty positions");
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
	
	@Override
	public int getCount() {
		return items_.size();
	}

	@Override
	public NewUserActivity.AvatarItem getItem(int position) {
		return items_.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
		View vi;
        if (convertView == null) {
            vi = inflater_.inflate(R.layout.avatar_item_layout, parent, false);
            // Could look these up below this if/else block, but more efficient
            // to use AvatarViewHolder (with get/setTag) to perform the lookup once.
            AvatarViewHolder viewHolder = new AvatarViewHolder();
            viewHolder.id_ = (TextView) vi.findViewById(R.id.avatar_id);
            viewHolder.img_ = (ImageView) vi.findViewById(R.id.avatar_image);
            vi.setTag(viewHolder);
        } else {
        	vi = convertView;
        }
 
        // Sets Text and image fields of this product.
        AvatarViewHolder holder = (AvatarViewHolder) vi.getTag();
        NewUserActivity.AvatarItem item = items_.get(position);
        
        // Sets  Avatar ID (invisible as a view, used to send id to server).
        holder.id_.setText(item.id_);
        
        // Sets Product Image.
        if (item.icon_ != null) {
          holder.img_.setImageBitmap(item.icon_);
        } else if (item.img_url_ != null && !item.img_url_.isEmpty() &&
                   !image_loader_.DisplayImage(item.img_url_, holder.img_, position)) {
          unfinished_list_elements_[position] = true;
        } else if (item.img_url_ == null || item.img_url_.isEmpty()) {
          // This is the top (empty). Don't show this in the dropdown.
          vi.setVisibility(View.GONE);
        }
        return vi;
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View vi;
        if (convertView == null) {
            vi = inflater_.inflate(R.layout.avatar_item_layout, parent, false);
            // Could look these up below this if/else block, but more efficient
            // to use AvatarViewHolder (with get/setTag) to perform the lookup once.
            AvatarViewHolder viewHolder = new AvatarViewHolder();
            viewHolder.id_ = (TextView) vi.findViewById(R.id.avatar_id);
            viewHolder.img_ = (ImageView) vi.findViewById(R.id.avatar_image);
            vi.setTag(viewHolder);
        } else {
        	vi = convertView;
        }
        
        // Sets Text and image fields of this product.
        AvatarViewHolder holder = (AvatarViewHolder) vi.getTag();
        NewUserActivity.AvatarItem item = items_.get(position);
        
        // Sets  Avatar ID (invisible as a view, used to send id to server).
        holder.id_.setText(item.id_);
        
        // Sets Product Image.
        if (item.icon_ != null) {
          holder.img_.setImageBitmap(item.icon_);
        } else if (item.img_url_ != null && !item.img_url_.isEmpty() &&
                   !image_loader_.DisplayImage(item.img_url_, holder.img_, position)) {
          unfinished_list_elements_[position] = true;
        } else if (item.img_url_ == null || item.img_url_.isEmpty()) {
          holder.img_.setVisibility(View.INVISIBLE);
        }
        return vi;
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