package com.jact.jactfirstdemo;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;


public class GcmIntentService extends IntentService {
  public static final int NOTIFICATION_ID = 1;
  private NotificationManager notification_manager_;
    
  public GcmIntentService() {
	super("JactGcmIntentService");
  }
  
  @Override
  protected void onHandleIntent(Intent intent) {
      Bundle extras = intent.getExtras();
      GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
      // The getMessageType() intent parameter must be the intent you received
      // in your BroadcastReceiver.
      String message_type = gcm.getMessageType(intent);
      String intent_action = intent.getAction();
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB Temp", "GcmIntentService::onHandleIntent. message_type: " +
                        message_type + ", intent_action: " + intent_action);
      if (intent_action == null) {
    	// TODO(PHB): Handle this case.
        if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB Temp", "GcmIntentService::onHandleIntent. Null intent_action.");
      } else if (intent_action.equals("com.google.android.c2dm.intent.REGISTRATION")) {
    	// Callback from registering this user-specific Jact app with Jact's GCM Service.
    	HandleRegistration(intent);
      } else if (intent_action.equals("com.google.android.c2dm.intent.RECEIVE")) {
    	// Handle an alert from Jact GCM.
    	// TODO(PHB): Implement this.
      }
      if (extras != null && !extras.isEmpty()) {  // has effect of unparcelling Bundle
          if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB TEMP", "GcmIntentService::onHandleEvent. Non-null extras: " +
                            extras.toString() + ". GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE: " +
                            GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE);
    	  /*
           * Filter messages based on message type. Since it is likely that GCM
           * will be extended in the future with new message types, just ignore
           * any message types you're not interested in, or that you don't
           * recognize.
           */
          if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(message_type)) {
              sendNotification("Send error: " + extras.toString());
              if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB TEMP", "GcmIntentService::onHandleEvent. Send error: " + extras.toString());
          } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(message_type)) {
              sendNotification("Deleted messages on server: " + extras.toString());
              if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB TEMP", "GcmIntentService::onHandleEvent. Deleted messages on server: " + extras.toString());
          } else if (GoogleCloudMessaging.ERROR_MAIN_THREAD.equals(message_type)) {
              sendNotification("Main Thread Error: " + extras.toString());
              if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB TEMP", "GcmIntentService::onHandleEvent. Main thread error: " + extras.toString());
          } else if (GoogleCloudMessaging.ERROR_SERVICE_NOT_AVAILABLE.equals(message_type)) {
              sendNotification("Service Not Available: " + extras.toString());
              if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB TEMP", "GcmIntentService::onHandleEvent. Service Not Available: " + extras.toString());
          //PHB this constant is new (May 10, 2015)? It's not compiling as is, and even google search can't find it anywhere...
          //} else if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_EVENT.equals(message_type)) {
          //    sendNotification("Service Not Available: " + extras.toString());
          //    if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB TEMP", "GcmIntentService::onHandleEvent. Service Not Available: " + extras.toString());
          } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(message_type)) {
              if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB TEMP", "GcmIntentService::onHandleEvent. Received: " + extras.toString());
              // If it's a regular GCM message, do some work.
              // This loop represents the service doing some work.
              for (int i=0; i<5; i++) {
                  if (!JactActionBarActivity.IS_PRODUCTION) Log.i("GcmIntentService::onHandleEvent", "Working... " + (i+1)
                          + "/5 @ " + SystemClock.elapsedRealtime());
                  try {
                      Thread.sleep(5000);
                  } catch (InterruptedException e) {
                  }
              }
              if (!JactActionBarActivity.IS_PRODUCTION) Log.i("GcmIntentService::onHandleEvent", "Completed work @ " + SystemClock.elapsedRealtime());
              // Post notification of received message.
              sendNotification("Received: " + extras.toString());
              if (!JactActionBarActivity.IS_PRODUCTION) Log.i("GcmIntentService::onHandleEvent", "Received: " + extras.toString());
          } else {
        	sendNotification("else: " + extras.toString());
            if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB TEMP", "GcmIntentService::onHandleEvent. else: " + extras.toString());
          }
      }
      // Release the wake lock provided by the WakefulBroadcastReceiver.
      GcmBroadcastReceiver.completeWakefulIntent(intent);
  }
  
  @TargetApi(16)
  private PendingIntent GetPendingIntentNew() {
    if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB TEMP", "GcmIntentService::GetPendingIntentNew.");
	Intent result_intent = new Intent(this, JactLoggedInHomeActivity.class);

    result_intent.putExtra(getString(R.string.was_logged_off_key), "true");
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    // Adds the back stack
    stackBuilder.addParentStack(JactLoggedInHomeActivity.class);
    // Adds the Intent to the top of the stack
    stackBuilder.addNextIntent(result_intent);
    // Gets a PendingIntent containing the entire back stack
    return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
  }
  
  private PendingIntent GetPendingIntentOld() {
	if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB TEMP", "GcmIntentService::GetPendingIntentOld.");
	Intent result_intent = new Intent(this, JactLoggedInHomeActivity.class);
    result_intent.putExtra(getString(R.string.was_logged_off_key), "true");
	return PendingIntent.getActivity(this, 0, result_intent, 0);
  }

  // Put the message into a notification and post it.
  // This is just one simple example of what you might choose to do with
  // a GCM message.
  private void sendNotification(String msg) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB TEMP", "GcmIntentService::sendNotification. msg: " + msg);
      
      PendingIntent contentIntent = null;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        contentIntent = GetPendingIntentNew();
      } else {
    	contentIntent = GetPendingIntentOld();
      }
      
      if (contentIntent == null) {
    	if (!JactActionBarActivity.IS_PRODUCTION) Log.e("GcmIntentService::sendNotification", "Null Pending Intent. No Notification will be sent");
    	return;
      }
      
      notification_manager_ =
    	  (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

      NotificationCompat.Builder notification_builder =
              new NotificationCompat.Builder(this)
      .setSmallIcon(R.drawable.ic_launcher_transparent)
      .setAutoCancel(true)   // So that when user clicks notification, it disappears
      .setDefaults(Notification.DEFAULT_VIBRATE)
      // Works:
      //.setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
      .setSound(Uri.parse(PreferenceManager.getDefaultSharedPreferences(this).
                    getString("pref_tone", "content://settings/system/notification_sound")))
      // Works:
      //.setDefaults(Notification.DEFAULT_SOUND)
      // Works:
      //.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
      //Doesn't Work (can't find R.raw)
      //.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.notification_sound))
      .setContentTitle("Jact GCM Notification")
      .setStyle(new NotificationCompat.BigTextStyle()
      .bigText(msg))
      .setContentText(msg);

      notification_builder.setContentIntent(contentIntent);
      notification_manager_.notify(NOTIFICATION_ID, notification_builder.build());
  }
  
  private void HandleRegistration(Intent intent) {
	// TODO(PHB): Implement this.
	String registration = intent.getStringExtra("registration_id");
	if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB TEMP", "GcmIntentService::HandleRegistration. non-null registration: " + registration);
	if (intent.getStringExtra("error") != null) {
	  // Registration failed, should try again later.
	  if (!JactActionBarActivity.IS_PRODUCTION) Log.d("c2dm", "registration failed");
	  String error = intent.getStringExtra("error");
	  if (error == "SERVICE_NOT_AVAILABLE") {
		if (!JactActionBarActivity.IS_PRODUCTION) Log.d("c2dm", "SERVICE_NOT_AVAILABLE");
	  } else if (error == "ACCOUNT_MISSING"){
		if (!JactActionBarActivity.IS_PRODUCTION) Log.d("c2dm", "ACCOUNT_MISSING");
	  } else if (error == "AUTHENTICATION_FAILED") {
		if (!JactActionBarActivity.IS_PRODUCTION) Log.d("c2dm", "AUTHENTICATION_FAILED");
	  } else if (error == "TOO_MANY_REGISTRATIONS") {
		if (!JactActionBarActivity.IS_PRODUCTION) Log.d("c2dm", "TOO_MANY_REGISTRATIONS");
	  } else if (error == "INVALID_SENDER"){
		if (!JactActionBarActivity.IS_PRODUCTION) Log.d("c2dm", "INVALID_SENDER");
	  } else if (error == "PHONE_REGISTRATION_ERROR") {
		if (!JactActionBarActivity.IS_PRODUCTION) Log.d("c2dm", "PHONE_REGISTRATION_ERROR");
	  }
	} else if (intent.getStringExtra("unregistered") != null) {
	  // unregistration done, new messages from the authorized sender will be rejected
	  if (!JactActionBarActivity.IS_PRODUCTION) Log.d("c2dm", "unregistered");
	} else if (registration != null) {
	  if (!JactActionBarActivity.IS_PRODUCTION) Log.d("c2dm", registration);
	  if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB TEMP", "GcmIntentService::HandleRegistration. non-null registration: " + registration);
	  //Editor editor =
      //    context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
      //editor.putString(REGISTRATION_KEY, registration);
      //editor.commit();
	  // Send the registration ID to the 3rd party site that is sending the messages.
	  // This should be done in a separate thread.
	  // When done, remember that all registration is done.
	}
  }
}