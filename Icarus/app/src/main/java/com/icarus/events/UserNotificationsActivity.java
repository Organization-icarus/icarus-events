package com.icarus.events;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for users to view notifications.
 * If eventId is provided, show notifications only for that event.
 * Otherwise, show all notifications for the current user.
 *
 * @author Yifan Jiao
 */
public class UserNotificationsActivity extends AppCompatActivity {

    private static final String TAG = "UserNotifications";

    private FirebaseFirestore db;
    private ListView notificationsList;
    private TextView titleText;

    private final ArrayList<NotificationItem> notifications = new ArrayList<>();
    private NotificationListAdapter adapter;

    private String currentUserId;
    private String currentEventId;
    private String currentEventName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_notifications);

        db = FirebaseFirestore.getInstance();

        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            Toast.makeText(this, "User session not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = currentUser.getId();

        currentEventId = getIntent().getStringExtra("eventId");
        currentEventName = getIntent().getStringExtra("eventName");

        titleText = findViewById(R.id.notifications_page_title);
        notificationsList = findViewById(R.id.notifications_list_view);
        ImageButton backButton = findViewById(R.id.notifications_back_button);

        if (titleText == null || notificationsList == null || backButton == null) {
            Toast.makeText(this, "Notification layout failed to load", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (currentEventId != null && !currentEventId.isEmpty()) {
            if (currentEventName != null && !currentEventName.isEmpty()) {
                titleText.setText(currentEventName + " Notifications");
            } else {
                titleText.setText("Event Notifications");
            }
        } else {
            titleText.setText("My Notifications");
        }

        backButton.setOnClickListener(v -> finish());

        adapter = new NotificationListAdapter(this, notifications);
        notificationsList.setAdapter(adapter);

        loadStoredNotifications();
    }

    private void loadStoredNotifications() {
        notifications.clear();

        Query query = db.collection(FirestoreCollections.NOTIFICATIONS_COLLECTION)
                .whereArrayContains("recipients", currentUserId);

        if (currentEventId != null && !currentEventId.isEmpty()) {
            query = query.whereEqualTo("eventId", currentEventId);
        }

        query = query.orderBy("date", Query.Direction.DESCENDING);

        query.get()
                .addOnSuccessListener(result -> {
                    for (QueryDocumentSnapshot doc : result) {
                        Boolean isEvent = doc.getBoolean("isEvent");
                        if (isEvent == null) {
                            isEvent = true;
                        }

                        List<String> rawRecipients = (List<String>) doc.get("recipients");
                        ArrayList<String> recipients = rawRecipients == null
                                ? new ArrayList<>()
                                : new ArrayList<>(rawRecipients);

                        String eventId = doc.getString("eventId");

                        String eventNameFromDoc = doc.getString("eventName");
                        String finalEventName;
                        if (eventNameFromDoc != null && !eventNameFromDoc.isEmpty()) {
                            finalEventName = eventNameFromDoc;
                        } else if (currentEventName != null) {
                            finalEventName = currentEventName;
                        } else {
                            finalEventName = "";
                        }

                        String message = doc.getString("message") != null ? doc.getString("message") : "";
                        String type = doc.getString("type") != null ? doc.getString("type") : "general";
                        String sender = doc.getString("sender");
                        String eventImage = doc.getString("eventImage");

                        NotificationItem item = new NotificationItem(
                                eventId,
                                finalEventName,
                                eventImage,
                                sender,
                                isEvent,
                                recipients,
                                message,
                                type
                        );

                        notifications.add(item);
                    }

                    adapter.notifyDataSetChanged();

                    if (notifications.isEmpty()) {
                        Toast.makeText(this, "No notifications found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load notifications", e);
                    Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                });
    }
}