package com.jact.jactapp;

import android.content.Context;
import android.util.AttributeSet;
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
      //PHB_OLDif (w <= 0 || tv_to_match_ == null || w < tv_to_match_.getWidth()) return;
      //PHB_OLDtv_to_match_.setTextScaleX((float) ((float) this.getWidth() / tv_to_match_.getWidth()));
      super.onSizeChanged(w, h, old_w, old_h);
    }
    
    public void SetTextViewToMatch(JactTextView to_match) {
      tv_to_match_ = to_match;
    }

}
