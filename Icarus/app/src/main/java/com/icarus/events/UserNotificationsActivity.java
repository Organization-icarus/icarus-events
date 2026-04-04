package com.icarus.events;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import android.widget.ImageButton;

/**
 * Activity for users to view their own notifications.
 */
public class UserNotificationsActivity extends AppCompatActivity {

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

        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            Toast.makeText(this, "User session not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = currentUser.getId();

        titleText = findViewById(R.id.notifications_page_title);
        notificationsList = findViewById(R.id.notifications_list_view);
        ImageButton backButton = findViewById(R.id.notifications_back_button);

        if (titleText == null || notificationsList == null) {
            Toast.makeText(this, "Notification layout failed to load", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        titleText.setText("My Notifications");
        backButton.setOnClickListener(v -> finish());

        adapter = new NotificationListAdapter(this, notifications);
        notificationsList.setAdapter(adapter);

        loadStoredNotifications();
    }

    private void loadStoredNotifications() {
        notifications.clear();

        db.collection(FirestoreCollections.NOTIFICATIONS_COLLECTION)
                .whereArrayContains("recipients", currentUserId)
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        Boolean isEvent = doc.getBoolean("isEvent");
                        if (isEvent == null) {
                            isEvent = true;
                        }

                        List<String> rawRecipients = (List<String>) doc.get("recipients");
                        ArrayList<String> recipients = rawRecipients == null
                                ? new ArrayList<>()
                                : new ArrayList<>(rawRecipients);

                        NotificationItem item = new NotificationItem(
                                doc.getString("eventId"),
                                doc.getString("eventName"),
                                doc.getString("eventImage"),
                                doc.getString("sender"),
                                isEvent,
                                recipients,
                                doc.getString("message") != null ? doc.getString("message") : "",
                                doc.getString("type") != null ? doc.getString("type") : "general"
                        );

                        notifications.add(item);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show());
    }
}