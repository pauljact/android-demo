package com.example.jactfirstdemo;

import android.graphics.Bitmap;

public interface ProcessUrlResponseCallback {
	void ProcessUrlResponse(String webpage, String cookies);
	void ProcessUrlResponse(Bitmap pic, String cookies);
	void ProcessFailedResponse(GetUrlTask.FetchStatus status);
}