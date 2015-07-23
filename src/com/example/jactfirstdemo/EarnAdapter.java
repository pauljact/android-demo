package com.example.jactfirstdemo;

import java.util.ArrayList;
import java.util.HashSet;

import com.example.jactfirstdemo.JactImageView.UseCase;
 
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
 
public class EarnAdapter extends ArrayAdapter<EarnPageParser.EarnItem> implements AdapterWithImages {

	private EarnActivity parent_activity_;
	private ArrayList<EarnPageParser.EarnItem> earn_items_;
	private boolean[] unfinished_list_elements_;
	private static LayoutInflater inflater_;
	public ProductsImageLoader image_loader_;
	
	public static class EarnViewHolder {
	  public LinearLayout layout_;
	  public TextView title_;
	  public TextView points_;
	  public TextView nid_;
	  public Button earn_now_;
	  public JactImageView img_;
	}
	
	public EarnAdapter(
		    EarnActivity a, int resource, ArrayList<EarnPageParser.EarnItem> list,
		    String activity_name) {
		super(a, resource, list);
		parent_activity_ = a;
		earn_items_ = list;
		unfinished_list_elements_ = new boolean[100];
		inflater_ = (LayoutInflater) parent_activity_.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        image_loader_ = new ProductsImageLoader(
        	parent_activity_.getApplicationContext(), this, activity_name);
	}
	
	@Override
	public void AlertPositionsReady (HashSet<Integer> positions) {
		if (positions == null) {
			Log.e("PHB ERROR", "EarnAdapter::alertPositionsReady. Null positions");
			return;
		}
		if (positions.isEmpty()) {
			Log.e("PHB ERROR", "EarnAdapter::alertPositionsReady. Empty positions");
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
		return earn_items_.size();
	}

	@Override
	public EarnPageParser.EarnItem getItem(int position) {
		return earn_items_.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		EarnPageParser.EarnItem earn_item = earn_items_.get(position);
		View vi;
        if (convertView == null) {
            vi = inflater_.inflate(R.layout.earn_item, parent, false);
            // Could look these up below this if/else block, but more efficient
            // to use EarnViewHolder (with get/setTag) to perform the lookup once.
            EarnViewHolder viewHolder = new EarnViewHolder();
            viewHolder.layout_ = (LinearLayout) vi.findViewById(R.id.earn_item_ll);
            viewHolder.title_ = (TextView) vi.findViewById(R.id.earn_title);
            viewHolder.earn_now_ = (Button) vi.findViewById(R.id.earn_now_button);
            viewHolder.points_ = (TextView) vi.findViewById(R.id.earn_points);
            viewHolder.nid_ = (TextView) vi.findViewById(R.id.earn_nid);
            viewHolder.img_ = (JactImageView) vi.findViewById(R.id.earn_image);
            vi.setTag(viewHolder);
        } else {
        	vi = convertView;
        }
 
        // Set Product Info in the view.
        EarnViewHolder holder = (EarnViewHolder) vi.getTag();
        
        // Set Title.
        if (earn_item.title_ != null) {
          holder.title_.setText(earn_item.title_);
        }
        
        // Set Points.
        holder.points_.setText(Integer.toString(earn_item.earn_points_) + " Points");

        // Set Earn ID.
        holder.nid_.setText(Integer.toString(earn_item.nid_));
        
        // Set Image.
        if (earn_item.img_url_ != null &&
            !image_loader_.DisplayImage(earn_item.img_url_, holder.img_, position, true)) {
          unfinished_list_elements_[position] = true;
        }
        holder.img_.SetUseCase(UseCase.EARN_IMAGE_THUMBNAIL);
        
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