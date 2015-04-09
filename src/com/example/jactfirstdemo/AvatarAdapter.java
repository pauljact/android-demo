package com.example.jactfirstdemo;

import java.util.ArrayList;
import java.util.HashSet;

import android.content.Context;
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
	
	public AvatarAdapter(NewUserActivity a, int resource, ArrayList<NewUserActivity.AvatarItem> items) {
		super(a, resource, items);
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
		Log.e("AvatarAdapter::alertPositionsReady", "Null positions");
		return;
	  }
	  if (positions.isEmpty()) {
		Log.e("AvatarAdapter::alertPositionsReady", "Empty positions");
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
        } else if (item.img_url_ != null &&
                   !image_loader_.DisplayImage(item.img_url_, holder.img_, position)) {
          unfinished_list_elements_[position] = true;
        }
        return vi;
    }
}