package com.icarus.events;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for managing Firebase Cloud Messaging (FCM) operations.
 * <p>
 * Provides static utility methods to retrieve the current device's FCM registration token
 * and to send push notifications directly from the application using a Google Service Account
 * for OAuth2 authentication.
 *
 * @author Kito Lee Son
 */
public class NotificationHelper {
    /**
     * Retrieves the current FCM registration token for the device asynchronously.
     * <p>
     * Uses the FirebaseMessaging service to fetch the unique token that identifies this
     * app instance on the Google servers. Results are returned via the provided listener.
     *
     * @param listener the callback listener to handle the retrieved token or errors
     */
    public static void getCurrentToken(OnTokenReceivedListener listener) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) listener.onTokenReceived(task.getResult());
            else Log.e("FCM_HELPER", "Failed to get token", task.getException());
        });
    }

    /**
     * Interface definition for a callback to be invoked when an FCM token is received.
     */
    public interface OnTokenReceivedListener {
        void onTokenReceived(String token);
    }

    /**
     * Sends a push notification to a specific target device using the FCM HTTP v1 API.
     * <p>
     * This method runs on a background thread to perform network operations. It loads
     * service account credentials from the local assets, generates an OAuth2 access token,
     * and dispatches a POST request to the Firebase messaging endpoint.
     *
     * @param context the context used to access assets and initialize the Volley request queue
     * @param targetToken the FCM registration token of the recipient device
     * @param eventName the title of the notification (typically the name of the associated event)
     * @param body the message content to be displayed in the notification
     */
    public static void sendPush(Context context, String targetToken, String eventName, String body) {
        new Thread(() -> {
            try {
                InputStream is = context.getAssets().open("service_account.json");
                GoogleCredentials credentials = GoogleCredentials.fromStream(is)
                        .createScoped(Collections.singleton("https://www.googleapis.com/auth/firebase.messaging"));
                credentials.refreshIfExpired();
                String accessToken = credentials.getAccessToken().getTokenValue();
                String url = "https://fcm.googleapis.com/v1/projects/icarus-events/messages:send";

                JSONObject message = new JSONObject();
                try {
                    JSONObject notification = new JSONObject();
                    notification.put("title", eventName);
                    notification.put("body", body);

                    JSONObject messageContent = new JSONObject();
                    messageContent.put("token", targetToken);
                    messageContent.put("notification", notification);

                    message.put("message", messageContent);
                } catch (JSONException e) { e.printStackTrace(); }

                JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, message,
                        response -> Log.d("FCM", "Sent!"),
                        error -> Log.e("FCM", "Error code: " + error.networkResponse.statusCode)) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Authorization", "Bearer " + accessToken);
                        headers.put("Content-Type", "application/json");
                        return headers;
                    }
                };
                Volley.newRequestQueue(context).add(request);
            } catch (Exception e) { Log.e("FCM_SEND", "Error", e); }
        }).start();
    }
}
