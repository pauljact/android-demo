package com.jact.jactapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

public class JactImageView extends ImageView {
  public enum UseCase {
	UNKNOWN,
	EARN_IMAGE_THUMBNAIL,
  }
  
  private UseCase use_case_;
  
  public JactImageView(Context context) {
	super(context);
	use_case_ = UseCase.UNKNOWN;
  }
  
  public JactImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
	use_case_ = UseCase.UNKNOWN;
  }
  
  public JactImageView(Context context, AttributeSet attrs, int defStyleAttr) {
	super(context, attrs, defStyleAttr);
	use_case_ = UseCase.UNKNOWN;
  }
  
  // TODO(PHB): The below constructor requires API 21. Wrap it with TargetApi.
  //public JactImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
  //	super(context, attrs, defStyleAttr, defStyleRes);
  //}
  
  @Override
  public void setImageDrawable(Drawable background) {
	super.setImageDrawable(background);
	if (use_case_ == UseCase.EARN_IMAGE_THUMBNAIL) {
	  try {
        LayerDrawable layers = (LayerDrawable) this.getDrawable();
        if (layers.getNumberOfLayers() != 2) {
          Log.w("JactImageView::setBackgroundDrawable",
        		"For UseCase 'EARN_IMAGE_THUMBNAIL', expected image to have two layers, but found: " +
                layers.getNumberOfLayers());
          return;
        }
        Drawable play_button_icon = layers.getDrawable(1);
        int top = 3 * this.getHeight() / 5;
        int bottom = 11 * this.getHeight() / 10;
        int left = 3 * this.getWidth() / 5;
        int right = 11 * this.getWidth() / 10;
        play_button_icon.setBounds(left, top, right, bottom);
	  } catch (ClassCastException e) {
		Log.w("JactImageView::setImageDrawable", "For UseCase 'EARN_IMAGE_THUMBNAIL', expected " +
	                                             "drawable to be layered, but unable to cast as such.");
	  }
	}
  }
  
  public void SetUseCase(UseCase use_case) {
	use_case_ = use_case;
  }
  /*
  @Override
  protected void onDraw(Canvas canvas) {
	super.onDraw(canvas);
	if (use_case_ == UseCase.EARN_IMAGE_THUMBNAIL) {
	  try {
        LayerDrawable layers = (LayerDrawable) this.getDrawable();
        if (layers.getNumberOfLayers() != 2) {
          Log.w("JactImageView::onDraw", "For UseCase 'EARN_IMAGE_THUMBNAIL', expected " +
                                         "image to have two layers, but found: " +
                                         layers.getNumberOfLayers());
          return;
        }
        Drawable play_button_icon = layers.getDrawable(1);
        Rect r = play_button_icon.getBounds();
        int top = this.getHeight() / 4;
        int bottom = 3 * this.getHeight() / 4;
        int left = this.getWidth() / 4;
        int right = 3 * this.getWidth() / 4;
        play_button_icon.setBounds(left, top, right, bottom);
	  } catch (ClassCastException e) {
		Log.w("JactImageView::onDraw", "For UseCase 'EARN_IMAGE_THUMBNAIL', expected " +
	                                   "drawable to be layered, but unable to cast as such.");
	  }
	}
  }*/
}
