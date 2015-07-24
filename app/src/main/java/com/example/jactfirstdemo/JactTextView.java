package com.example.jactfirstdemo;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

public class JactTextView extends TextView {
    private JactLinearLayout ll_to_match_;
	
	public JactTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public JactTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public JactTextView(Context context) {
        super(context);
    }
    @Override
    protected void onSizeChanged(int w, int h, int old_w, int old_h) {
      if (w <= 0 || ll_to_match_ == null || w >= ll_to_match_.getWidth()) return;
      // We set the width of the text box to be just bigger than the linearlayout
      // it is to match, so that a small padding applied to the text box won't
      // make it so some of the text does not have room to be displayed.
      // Update: When text overflows it's linear layout, the last word "Purpose" gets cut off.
      // While this wasn't happening all the time, it would occassionally happen when the login
      // activity got reloaded, when clicking on a button (perhaps later I don't have to worry
      // about this, as any button click leaves the login screen anyway; but i was temporarily
      // in a state where Facebook and Google login buttons could be pressed but weren't full
      // implemented yet).
      // float new_width = (float) ll_to_match_.getWidth() * (float) 1.05;

      // PHB Now matching width via Relative Layout
      //float new_width = (float) ll_to_match_.getWidth() * (float) 0.95;
      //this.setWidth((int) new_width);
        Log.e("PHB TEMP", "JactTextView::onSizeChanged. LL width: " + ll_to_match_.getWidth() +
                          ", TV width: " + this.getWidth());
      this.setTextScaleX((float) ((float) 0.95 * this.getWidth()) / w);
      //this.setTextScaleX((float) ((float) ll_to_match_.getWidth() / w));
    }

    public void SetLinearLayoutToMatch(JactLinearLayout to_match) {
      ll_to_match_ = to_match;
    }
}
