package com.example.jactfirstdemo;

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
    String foo = "";
    if (extras != null) foo = extras.toString();
    String intent_action = intent.getAction();
	Log.e("PHB TEMP", "GcmBroadcastReceiver::onReceive. extras: " + foo + ", intent_action: " + intent_action);
    // END PHB TEMP
	
	// Explicitly specify that GcmIntentService will handle the intent.
    ComponentName comp =
    	new ComponentName(context.getPackageName(), GcmIntentService.class.getName());
    // Start the service, keeping the device awake while it is launching.
    startWakefulService(context, (intent.setComponent(comp)));
    setResultCode(Activity.RESULT_OK);
  }
}
