package com.jact.jactapp;

import java.util.HashSet;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public interface AdapterWithImages {
	void AlertPositionsReady(HashSet<Integer> positions);
	Drawable GetDrawable(int resource_id);
	Drawable GetDrawable(Bitmap bitmap);
}
