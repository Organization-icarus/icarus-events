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

public class NotificationHelper {
    public static void getCurrentToken(OnTokenReceivedListener listener) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) listener.onTokenReceived(task.getResult());
            else Log.e("FCM_HELPER", "Failed to get token", task.getException());
        });
    }

    public interface OnTokenReceivedListener {
        void onTokenReceived(String token);
    }

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
