package com.icarus.events;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EventNotificationsActivity extends HeaderNavBarActivity {

    private FirebaseFirestore db;
    private String eventId;
    private TextView titleText;
    private ListView notificationsList;

    private final List<String> messages = new ArrayList<>();

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
        db.collection("events")
                .document(eventId)
                .collection("notifications")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    messages.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String message = doc.getString("message");
                        if (message != null) {
                            messages.add(message);
                        }
                    }

                    NotificationListAdapter adapter =
                            new NotificationListAdapter(this, messages);
                    notificationsList.setAdapter(adapter);
                });
    }
}