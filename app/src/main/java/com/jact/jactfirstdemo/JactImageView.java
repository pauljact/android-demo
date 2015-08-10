package com.jact.jactfirstdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

public class JactImageView extends ImageView {
  private int top_;
  private int bottom_;
  private int left_;
  private int right_;

  public enum UseCase {
	UNKNOWN,
	EARN_IMAGE_THUMBNAIL,
    FEATURED_EARN,
  }
  private UseCase use_case_;
  
  public JactImageView(Context context) {
	super(context);
	use_case_ = UseCase.UNKNOWN;
    top_ = -1;
    bottom_ = -1;
    left_ = -1;
    right_ = -1;
  }

  public JactImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
	use_case_ = UseCase.UNKNOWN;
    top_ = -1;
    bottom_ = -1;
    left_ = -1;
    right_ = -1;
  }
  
  public JactImageView(Context context, AttributeSet attrs, int defStyleAttr) {
	super(context, attrs, defStyleAttr);
	use_case_ = UseCase.UNKNOWN;
    top_ = -1;
    bottom_ = -1;
    left_ = -1;
    right_ = -1;
  }
  
  // TODO(PHB): The below constructor requires API 21. Wrap it with TargetApi.
  //public JactImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
  //	super(context, attrs, defStyleAttr, defStyleRes);
  //}

  @Override
  protected void onDraw(Canvas canvas) {
    if (use_case_ == UseCase.EARN_IMAGE_THUMBNAIL) {
        try {
            LayerDrawable layers = (LayerDrawable) this.getDrawable();
            if (layers.getNumberOfLayers() != 2) {
                if (!JactActionBarActivity.IS_PRODUCTION) Log.w("JactImageView::onDraw",
                        "For UseCase 'EARN_IMAGE_THUMBNAIL', expected image to have two layers, but found: " +
                                layers.getNumberOfLayers());
                return;
            }
            Drawable play_button_icon = layers.getDrawable(1);
            int top = top_ < 0 ? 1 * play_button_icon.getIntrinsicHeight() / 3 : top_;
            int bottom = bottom_ < 0 ? 2 * play_button_icon.getIntrinsicHeight() / 3 : bottom_;
            int left = left_ < 0 ? 1 * play_button_icon.getIntrinsicWidth() / 3 : left_;
            int right = right_ < 0 ? 2 * play_button_icon.getIntrinsicWidth() / 3 : right_;
            play_button_icon.setBounds(left, top, right, bottom);
            this.invalidateDrawable(play_button_icon);
        } catch (ClassCastException e) {
            if (!JactActionBarActivity.IS_PRODUCTION) Log.w("JactImageView::onDraw", "For UseCase 'EARN_IMAGE_THUMBNAIL', expected " +
                    "drawable to be layered, but unable to cast as such.");
        }
    } else if (use_case_ == UseCase.FEATURED_EARN) {
        ViewGroup.LayoutParams img_params = this.getLayoutParams();
        img_params.height = 250;
        img_params.width = 250;
        try {
            LayerDrawable layers = (LayerDrawable) this.getDrawable();
            if (layers.getNumberOfLayers() != 2) {
                if (!JactActionBarActivity.IS_PRODUCTION) Log.w("JactImageView::onDraw",
                        "For UseCase 'EARN_IMAGE_THUMBNAIL', expected image to have two layers, but found: " +
                                layers.getNumberOfLayers());
                return;
            }
            Drawable play_button_icon = layers.getDrawable(1);
            int top = top_ < 0 ? 1 * play_button_icon.getIntrinsicHeight() / 3 : top_;
            int bottom = bottom_ < 0 ? 2 * play_button_icon.getIntrinsicHeight() / 3 : bottom_;
            int left = left_ < 0 ? 1 * play_button_icon.getIntrinsicWidth() / 3 : left_;
            int right = right_ < 0 ? 2 * play_button_icon.getIntrinsicWidth() / 3 : right_;
            play_button_icon.setBounds(left, top, right, bottom);
            this.invalidateDrawable(play_button_icon);
        } catch (ClassCastException e) {
            if (!JactActionBarActivity.IS_PRODUCTION) Log.w("JactImageView::onDraw", "For UseCase 'EARN_IMAGE_THUMBNAIL', expected " +
                    "drawable to be layered, but unable to cast as such.");
        }
    }
    super.onDraw(canvas);
  }
  
  public void SetUseCase(UseCase use_case) {
	use_case_ = use_case;
  }

  public void SetBounds(int top, int bottom, int left, int right) {
    top_ = top;
    bottom_ = bottom;
    left_ = left;
    right_ = right;
  }
}
