package com.icarus.events;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for viewing all notifications sent for a specific event.
 * <p>
 * Organizers and admins can use this page to review notification history
 * related to an event.
 *
 * @author Ben Salmon
 */
public class EventNotificationsActivity extends HeaderNavBarActivity {

    private FirebaseFirestore db;
    private String eventId;
    private TextView titleText;
    private ListView notificationsList;

    private final ArrayList<NotificationItem> notifications = new ArrayList<>();
    private NotificationListAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_notifications);
        setupNavBar();

        db = FirebaseFirestore.getInstance();

        titleText = findViewById(R.id.notifications_page_title);
        notificationsList = findViewById(R.id.notifications_list_view);

        adapter = new NotificationListAdapter(this, notifications);
        notificationsList.setAdapter(adapter);

        eventId = getIntent().getStringExtra("eventId");

        titleText.setText("Event Notifications");

        if (eventId != null && !eventId.isEmpty()) {
            loadNotifications();
        } else {
            Toast.makeText(this, "Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Loads all notifications sent for this event.
     */
    private void loadNotifications() {
        notifications.clear();

        db.collection(FirestoreCollections.NOTIFICATIONS_COLLECTION)
                .whereEqualTo("eventId", eventId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        List<String> rawRecipients = (List<String>) doc.get("recipients");
                        ArrayList<String> recipients = rawRecipients == null
                                ? new ArrayList<>()
                                : new ArrayList<>(rawRecipients);

                        Boolean isEvent = doc.getBoolean("isEvent");
                        if (isEvent == null) {
                            isEvent = true;
                        }

                        NotificationItem notification = new NotificationItem(
                                doc.getString("eventId"),
                                doc.getString("eventName"),
                                doc.getString("eventImage"),
                                doc.getString("sender"),
                                isEvent,
                                recipients,
                                doc.getString("message") != null ? doc.getString("message") : "",
                                doc.getString("type") != null ? doc.getString("type") : "general"
                        );

                        notifications.add(notification);
                    }

                    adapter.notifyDataSetChanged();

                    if (notifications.isEmpty()) {
                        Toast.makeText(this, "No notifications found for this event", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }
}