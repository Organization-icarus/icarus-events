package com.icarus.events;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

/**
 * Activity for users to view their own notifications.
 */
public class UserNotificationsActivity extends NavigationBarActivity {

    private FirebaseFirestore db;
    private ListView notificationsList;
    private TextView titleText;

    private final ArrayList<NotificationItem> notifications = new ArrayList<>();
    private NotificationListAdapter adapter;
    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_notifications);


        db = FirebaseFirestore.getInstance();
        currentUserId = UserSession.getInstance().getCurrentUser().getId();

        titleText = findViewById(R.id.notifications_page_title);
        notificationsList = findViewById(R.id.notifications_list_view);

        titleText.setText("My Notifications");

        adapter = new NotificationListAdapter(this, notifications);
        notificationsList.setAdapter(adapter);

        loadAllNotifications();
    }

    private void loadAllNotifications() {
        notifications.clear();
        loadStoredNotifications();
        loadPrivateInviteNotifications();
        loadCoOrganizerNotifications();
    }

    private void loadStoredNotifications() {
        db.collection(FirestoreCollections.NOTIFICATIONS_COLLECTION)
                .whereArrayContains("recipients", currentUserId)
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        Boolean isEvent = doc.getBoolean("isEvent");
                        if (isEvent == null) isEvent = true;

                        ArrayList<String> recipients = (ArrayList<String>) doc.get("recipients");
                        if (recipients == null) recipients = new ArrayList<>();

                        NotificationItem item = new NotificationItem(
                                doc.getString("eventId"),
                                doc.getString("sender"),
                                isEvent,
                                recipients,
                                doc.getString("message") != null ? doc.getString("message") : "",
                                doc.getString("type") != null ? doc.getString("type") : "general"
                        );

                        if (!containsDuplicate(item)) {
                            notifications.add(item);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show());
    }

    private void loadPrivateInviteNotifications() {
        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .whereEqualTo("isPrivate", true)
                .get()
                .addOnSuccessListener(events -> {
                    for (QueryDocumentSnapshot eventDoc : events) {
                        String eventId = eventDoc.getId();
                        String eventName = eventDoc.getString("name");

                        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                                .document(eventId)
                                .collection("entrants")
                                .document(currentUserId)
                                .get()
                                .addOnSuccessListener(entrantDoc -> {
                                    if (!entrantDoc.exists()) return;

                                    String status = entrantDoc.getString("status");
                                    if (status == null) return;

                                    ArrayList<String> recipients = new ArrayList<>();
                                    recipients.add(currentUserId);

                                    NotificationItem item = new NotificationItem(
                                            eventId,
                                            "system",
                                            true,
                                            recipients,
                                            "You have been invited to join the waiting list for the private event: " + eventName,
                                            "private_waitlist_invite"
                                    );

                                    if (!containsDuplicate(item)) {
                                        notifications.add(item);
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                    }
                });
    }

    private void loadCoOrganizerNotifications() {
        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .whereArrayContains("organizers", currentUserId)
                .get()
                .addOnSuccessListener(events -> {
                    for (QueryDocumentSnapshot eventDoc : events) {
                        ArrayList<String> organizers = (ArrayList<String>) eventDoc.get("organizers");
                        String eventName = eventDoc.getString("name");
                        String eventId = eventDoc.getId();

                        if (organizers == null || organizers.isEmpty()) continue;

                        if (!currentUserId.equals(organizers.get(0))) {
                            ArrayList<String> recipients = new ArrayList<>();
                            recipients.add(currentUserId);

                            NotificationItem item = new NotificationItem(
                                    eventId,
                                    "system",
                                    true,
                                    recipients,
                                    "You have been invited to be a co-organizer for the event: " + eventName,
                                    "co_organizer_invite"
                            );

                            if (!containsDuplicate(item)) {
                                notifications.add(item);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
    }

    private boolean containsDuplicate(NotificationItem newItem) {
        for (NotificationItem item : notifications) {
            boolean sameEvent = safeEquals(item.getEventId(), newItem.getEventId());
            boolean sameType = safeEquals(item.getType(), newItem.getType());
            boolean sameMessage = safeEquals(item.getMessage(), newItem.getMessage());

            if (sameEvent && sameType && sameMessage) {
                return true;
            }
        }
        return false;
    }

    private boolean safeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}