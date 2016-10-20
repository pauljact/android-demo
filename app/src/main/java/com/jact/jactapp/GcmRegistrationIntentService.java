package com.jact.jactapp;

/**
 * Created by Paul on 10/19/2015.
 */

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

public class GcmRegistrationIntentService extends IntentService {

    private static final String TAG = "GcmRegIntentService";
    private static final String[] TOPICS = {"global"};

    public GcmRegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!JactActionBarActivity.IS_PRODUCTION) {
            Log.i(TAG + "::onHandleIntent", "Top");
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            // [START get_token]
            InstanceID instanceID = InstanceID.getInstance(this);
            // R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
            // See https://developers.google.com/cloud-messaging/android/start for details on this file.
            //PHBString token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
            String token = instanceID.getToken("404003292102",
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // [END get_token]
            if (!JactActionBarActivity.IS_PRODUCTION) {
                Log.i(TAG + "::onHandleIntent", "GCM Registration Token: " + token);
            }

            // TODO: Implement this method to send any registration to your app's servers.
            //PHB: Not doing this here; instead, write token to SharedPreferences, and then
            // have a broadcast receiver detect when the
            // registration is done, and then read the written token there, and register the
            // app with jact server then (we don't just register it here, because we don't
            // have the CSRF and cookies here).
            // sendRegistrationToServer(token);
            sharedPreferences.edit().putString("new_gcm_token", token).apply();

            // Subscribe to topic channels
            subscribeTopics(token);

            // You should store a boolean that indicates whether the generated token has been
            // sent to your server. If the boolean is false, send the token to your server,
            // otherwise your server should have already received the token.
            sharedPreferences.edit().putBoolean("sentTokenToServer", true).apply();
            // [END register_for_gcm]
        } catch (Exception e) {
            if (!JactActionBarActivity.IS_PRODUCTION) {
                Log.e("PHB TEMP", TAG + "::onHandleIntent. Failed to complete token refresh", e);
            }
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            sharedPreferences.edit().putBoolean("sentTokenToServer", false).apply();
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent("registrationComplete");
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    /**
     * Persist registration to third-party servers.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        // Add custom implementation, as needed.
        if (!JactActionBarActivity.IS_PRODUCTION) {
            Log.e("PHB TEMP", TAG + "::sendRegistrationToServer. token: " + token);
        }
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
    // [END subscribe_topics]
}