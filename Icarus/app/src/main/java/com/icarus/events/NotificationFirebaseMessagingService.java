package com.icarus.events;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Service that handles incoming Firebase Cloud Messaging (FCM) messages.
 * <p>
 * This service intercepts push notifications sent from the FCM server. It is responsible
 * for catching messages while the app is in the foreground and manually displaying
 * them in the Android system tray using notification channels.
 *
 * @author Kito Lee Son
 */
public class NotificationFirebaseMessagingService extends FirebaseMessagingService {
    /**
     * Called when a message is received from FCM while the app is in the foreground.
     * <p>
     * Overrides the default behavior to ensure that notifications are still displayed
     * to the user even if the application is currently active and on-screen. Extracts
     * the title and body from the remote message and triggers the local notification builder.
     *
     * @param remoteMessage the object representing the message received from Firebase
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // This code runs when a push arrives
        if (remoteMessage.getNotification() != null) {
            showNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody()
            );
        }
    }

    /**
     * Configures and displays a system notification in the Android notification drawer.
     * <p>
     * Handles the creation of notification channels for Android 8.0 (Oreo) and above,
     * sets high priority for "Heads-up" displays, and binds the message content to
     * the system's notification UI.
     *
     * @param title the string to be used as the notification headline
     * @param message the string to be used as the main notification body text
     */
    private void showNotification(String title, String message) {
        String channelId = "event_notifications";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Android 8.0+ requires a Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Events", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notification) // Ensure this icon exists!
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        notificationManager.notify(1, builder.build());
    }
}