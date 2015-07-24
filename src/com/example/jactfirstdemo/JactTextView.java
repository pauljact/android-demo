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
      float new_width = (float) ll_to_match_.getWidth() * (float) 1.05; 
      this.setWidth((int) new_width);
      this.setTextScaleX((float) ((float) ll_to_match_.getWidth() / w));
    }
    
    public void SetLinearLayoutToMatch(JactLinearLayout to_match) {
      ll_to_match_ = to_match;
    }
}
