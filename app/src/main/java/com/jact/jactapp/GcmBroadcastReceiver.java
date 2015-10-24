package com.jact.jactapp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
	// PHB TEMP
    Bundle extras = intent.getExtras();
    String extras_str = "";
    if (extras != null) extras_str = extras.toString();
    String intent_action = intent.getAction();
	if (!JactActionBarActivity.IS_PRODUCTION) {
      Log.i("GcmBroadcastReceiver::onReceive",
            "Extras: " + extras_str + ", intent_action: " + intent_action);
    }
    // END PHB TEMP
	
	// Explicitly specify that GcmIntentService will handle the intent.
    ComponentName comp =
    	new ComponentName(context.getPackageName(), GcmIntentService.class.getName());
    // Start the service, keeping the device awake while it is launching.
    startWakefulService(context, (intent.setComponent(comp)));
    setResultCode(Activity.RESULT_OK);
  }
}
