package com.icarus.events;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EventNotificationsActivity extends NavigationBarActivity {

    private FirebaseFirestore db;
    private String eventId;
    private TextView titleText;
    private ListView notificationsList;

    private final ArrayList<NotificationItem> notifications = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_notifications);

        db = FirebaseFirestore.getInstance();

        titleText = findViewById(R.id.notifications_page_title);
        notificationsList = findViewById(R.id.notifications_list_view);

        eventId = getIntent().getStringExtra("eventId");

        titleText.setText("Notifications");

        if (eventId != null && !eventId.isEmpty()) {
            loadNotifications();
        }
    }

    private void loadNotifications() {
        String currentUserId = UserSession.getInstance().getCurrentUser().getId();
        notifications.clear();

        db.collection(FirestoreCollections.NOTIFICATIONS_COLLECTION)
                .whereEqualTo("eventId", eventId)
                .whereArrayContains("recipients", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        NotificationItem notification = new NotificationItem(
                                eventId,
                                doc.getString("sender"),
                                doc.getBoolean("isEvent"),
                                (ArrayList<String>) doc.get("recipients"),
                                doc.getString("message"),
                                doc.getString("type")
                        );
                        notifications.add(notification);
                    }

                    NotificationListAdapter adapter = new NotificationListAdapter(this, notifications);
                    notificationsList.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }
}