package com.jact.jactfirstdemo;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

/**
 * Created by Paul on 6/16/2015.
 */
public class JactFacebookButton extends com.facebook.login.widget.LoginButton {
    public JactFacebookButton(Context context) {
        super(context);
    }
    public JactFacebookButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public JactFacebookButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resize();
    }

    private void Resize() {
        float fbIconScale = 1.45F;
        Drawable drawable = getResources().getDrawable(
                com.facebook.R.drawable.com_facebook_button_icon);
        drawable.setBounds(0, 0, (int) (drawable.getIntrinsicWidth() * fbIconScale),
                (int) (drawable.getIntrinsicHeight() * fbIconScale));
        this.setCompoundDrawables(drawable, null, null, null);
        this.setCompoundDrawablePadding(17);
        this.setPadding(10, 13, 0, 13);
        this.setHeight(40);
        this.invalidate();
    }
}
