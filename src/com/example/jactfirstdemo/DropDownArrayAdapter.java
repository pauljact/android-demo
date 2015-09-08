package com.jact.jactapp;

import java.util.List;

import com.jact.jactapp.ProductsAdapter.ProductViewHolder;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class DropDownArrayAdapter extends ArrayAdapter<String> implements OnItemSelectedListener {
	private static LayoutInflater inflater_;
	private Activity parent_activity_;
	private List<String> items_;
	
	private class TextViewHolder {
		private TextView drop_down_text_view_;
	}
	
	public DropDownArrayAdapter(Activity c, int resource, int textView, List<String> items) {
		super((Context) c, resource, textView, items);
		parent_activity_ = c;
		inflater_ = (LayoutInflater) parent_activity_.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		items_ = items;
	}
	
	@Override
	public int getCount() {
		return items_.size();
	}

	@Override
	public String getItem(int position) {
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
            vi = inflater_.inflate(R.layout.drop_down_layout, parent, false);
            // Could look this up below this if/else block, but more efficient
            // to use TextViewHolder (with get/setTag) to perform the lookup once.
            TextViewHolder viewHolder = new TextViewHolder();
            viewHolder.drop_down_text_view_ = (TextView) vi.findViewById(R.id.dropdown_textview);
            vi.setTag(viewHolder);
        } else {
        	vi = convertView;
        }
 
        // Set text in TextView.
        TextViewHolder holder = (TextViewHolder) vi.getTag();
        holder.drop_down_text_view_.setText(items_.get(position));
		return vi;
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}
}
