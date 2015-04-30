package com.example.jactfirstdemo;

import android.graphics.Bitmap;

public interface ProcessUrlResponseCallback {
	void ProcessUrlResponse(String webpage, String cookies, String extra_params);
	void ProcessUrlResponse(Bitmap pic, String cookies, String extra_params);
	void ProcessFailedResponse(GetUrlTask.FetchStatus status, String extra_params);
	void IncrementNumRequestsCounter();
	void DecrementNumRequestsCounter();
	int GetNumRequestsCounter();
	void DisplayPopup(String message);
}