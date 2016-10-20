package com.jact.jactapp;

/**
 * Created by Paul on 10/19/2015.
 */

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

public class JactGcmListenerService extends GcmListenerService {

    private static final String TAG = "JactGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String message = data.getString("message");
        if (!JactActionBarActivity.IS_PRODUCTION) {
            Log.e("PHB TEMP", TAG + "::onMessageReceived. From: " + from);
            Log.e("PHB TEMP", TAG + "::onMessageReceived. Message: " + message);
            Log.e("PHB TEMP", TAG + "::onMessageReceived. Data: " + data.toString());
        }

        if (from.startsWith("/topics/")) {
            // message received from some topic.
        } else {
            // normal downstream message.
        }

        // [START_EXCLUDE]
        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */
        sendNotification(message);
        // [END_EXCLUDE]
    }
    // [END receive_message]

    @TargetApi(16)
    private PendingIntent GetPendingIntentNew() {
        if (!JactActionBarActivity.IS_PRODUCTION) {
            Log.e("PHB TEMP", "JactGcmListenerService::GetPendingIntentNew.");
        }
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
        if (!JactActionBarActivity.IS_PRODUCTION) {
            Log.e("PHB TEMP", "JactGcmListenerService::GetPendingIntentOld.");
        }
        Intent result_intent = new Intent(this, JactLoggedInHomeActivity.class);
        result_intent.putExtra(getString(R.string.was_logged_off_key), "true");
        return PendingIntent.getActivity(this, 0, result_intent, 0);
    }

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message) {
        if (!JactActionBarActivity.IS_PRODUCTION) {
            Log.e("PHB TEMP", TAG + "::sendNotification. Message: " + message);
        }

        // Check if user disabled GCM.
        SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
        String user_disabled_gcm = user_info.getString(getString(R.string.gcm_disabled_key), "false");
        boolean gcm_disabled =
                (user_disabled_gcm != null) && user_disabled_gcm.equalsIgnoreCase("true");
        if (gcm_disabled) {
            if (!JactActionBarActivity.IS_PRODUCTION) {
                Log.e("PHB TEMP", TAG + "::sendNotification. Not accepting notification (disabled).");
            }
            return;
        }


            PendingIntent pendingIntent = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            pendingIntent = GetPendingIntentNew();
        } else {
            pendingIntent = GetPendingIntentOld();
        }

        if (pendingIntent == null) {
            if (!JactActionBarActivity.IS_PRODUCTION) {
                Log.e("JactGcmListenerService::sendNotification",
                        "Null Pending Intent. No Notification will be sent");
            }
            return;
        }

        //PHB ORIGINAL Intent:
        // Intent intent = new Intent(this, JactLoggedInHomeActivity.class);
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
        //        PendingIntent.FLAG_ONE_SHOT);

        //PHB Original Sound:
        //Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
              .setSmallIcon(R.drawable.ic_launcher_transparent)
              .setContentTitle("Notification from Jact")
              .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
              .setContentText(message)
              .setAutoCancel(true)
              .setDefaults(Notification.DEFAULT_VIBRATE)
                      //PHB OriginalSound:.setSound(defaultSoundUri)
              .setSound(Uri.parse(PreferenceManager.getDefaultSharedPreferences(this).
                      getString("pref_tone", "content://settings/system/notification_sound")))
              .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}