package com.example.jactfirstdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OnBootBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
	  Log.e("PHB TEMP", "OnBootBroadcastReceiver::onRecieve.");
	  Intent intent_to_start = new Intent(context, GcmIntentService.class);
	  context.startService(intent_to_start);
	}

}
