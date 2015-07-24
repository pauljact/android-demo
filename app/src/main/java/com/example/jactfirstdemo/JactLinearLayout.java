package com.example.jactfirstdemo;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;

public class JactLinearLayout extends LinearLayout {
    private JactTextView tv_to_match_;
	
	public JactLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public JactLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public JactLinearLayout(Context context) {
        super(context);
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int old_w, int old_h) {
      if (w <= 0 || tv_to_match_ == null || w < tv_to_match_.getWidth()) return;
      tv_to_match_.setTextScaleX((float) ((float) this.getWidth() / tv_to_match_.getWidth()));
    }
    
    public void SetTextViewToMatch(JactTextView to_match) {
      tv_to_match_ = to_match;
    }

}
