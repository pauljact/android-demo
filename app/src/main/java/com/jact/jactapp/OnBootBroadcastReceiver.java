package com.jact.jactapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OnBootBroadcastReceiver extends BroadcastReceiver {

	private static final Boolean use_old_gcm_ = false;
	private static final Boolean use_new_gcm_ = true;

	@Override
	public void onReceive(Context context, Intent intent) {
	  if (!JactActionBarActivity.IS_PRODUCTION) {
		  Log.e("PHB TEMP", "OnBootBroadcastReceiver::onRecieve.");
	  }

	  if (use_new_gcm_) {
		  Intent new_intent_to_start = new Intent(context, JactGcmListenerService.class);
		  context.startService(new_intent_to_start);
	  }

	  if (use_old_gcm_) {
		Intent intent_to_start = new Intent(context, GcmIntentService.class);
		context.startService(intent_to_start);
	  }
	}
}
