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
 * Activity that allows users to view a personalized list of notifications.
 * <p>
 * This activity retrieves notification documents from Firestore where the current
 * user's ID is present in the recipients array. It manages the user session
 * verification and populates a ListView using the {@link NotificationListAdapter}.
 *
 * @author Yifan Jiao
 */
public class UserNotificationsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ListView notificationsList;
    private TextView titleText;

    private final ArrayList<NotificationItem> notifications = new ArrayList<>();
    private NotificationListAdapter adapter;
    private String currentUserId;

    /**
     * Initializes the activity, sets up the UI components, and verifies the user session.
     * <p>
     * If no valid user session is found, the activity displays a toast and finishes
     * immediately to prevent unauthorized access to the notification data.
     *
     * @param savedInstanceState if the activity is being re-initialized after
     * previously being shut down, this contains the most
     * recent data; otherwise it is null.
     */
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
        backButton.setOnClickListener(v -> {
            Toast.makeText(this, "back clicked", Toast.LENGTH_SHORT).show();
            finish();
        });

        adapter = new NotificationListAdapter(this, notifications);
        notificationsList.setAdapter(adapter);

        loadStoredNotifications();
    }

    /**
     * Queries the Firestore database for notifications targeted at the current user.
     * <p>
     * This method searches the notifications collection for documents where the
     * "recipients" array contains the current user's ID. Each document is parsed into
     * a {@link NotificationItem} and added to the local list. Upon completion,
     * the adapter is notified to refresh the UI.
     */
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