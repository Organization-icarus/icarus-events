package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

/**
 * Screen for users to manage personal settings and account deletion.
 * <p>
 * Loads the current user's profile image and notification preferences from
 * Firestore, allows the user to update notification settings, and provides
 * functionality to delete the user's account. During account deletion, the
 * user is removed from all event entrant subcollections, any stored profile
 * image record is deleted, and the app returns to {@link MainActivity} with
 * the session cleared.
 *
 * @author Alex Alves
 */
public class UserSettingsActivity extends HeaderNavBarActivity {
    private Button deleteProfileButton;
    private ImageView profileImage;
    private Switch adminNotificationsSwitch;
    private Switch organizerNotificationsSwitch;
    private User user;
    FirebaseFirestore db;

    /**
     * Initializes the user settings screen.
     * <p>
     * Loads the current user from {@link UserSession}, retrieves and displays the
     * user's profile image, loads notification preferences from Firestore into the
     * corresponding switches, and registers listeners to persist any switch changes.
     * Also configures the delete profile button to remove the user from all event
     * entrant subcollections, delete the user's stored profile image, remove the
     * user document, and return to the main screen.
     *
     * @param savedInstanceState the previously saved activity state,
     *                           or null if the activity is newly created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_settings);
        setupNavBar();

        // Initialize database reference and collection references
        db = FirebaseFirestore.getInstance();

        // Retrieve device Id/User object
        user = UserSession.getInstance().getCurrentUser();
        String deviceId = user.getId();

        // Initialize user profile image
        profileImage = findViewById(R.id.user_settings_profile_image);
        db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId).get()
                .addOnSuccessListener(snapshot -> {
                    String imageURL = snapshot.getString("image");
                    if (imageURL != null && !imageURL.isEmpty()) {
                        Picasso.get()
                                .load(imageURL)
                                .placeholder(R.drawable.poster)
                                .error(R.drawable.poster)           // Optional: shows if link fails
                                .into(profileImage);
                    } else {
                        profileImage.setImageResource(R.drawable.poster);
                    }
                });

        // Initialize buttons
        deleteProfileButton = findViewById(R.id.user_profile_delete_button);

        // Initialize switches
        adminNotificationsSwitch = findViewById(R.id.user_settings_admin_notifications_switch);
        organizerNotificationsSwitch = findViewById(R.id.user_settings_org_notifications_switch);

        // Set buttons on click listeners
        // Taken from Claude March 11th 2026,
        // "I need to modify my query to also delete the user from the event collection entrant subcollection"
        android.content.Context appContext = getApplicationContext();
        deleteProfileButton.setOnClickListener(v -> {
            db.collection(FirestoreCollections.EVENTS_COLLECTION).get()
                    .addOnSuccessListener(eventSnapshots -> {
                        for (QueryDocumentSnapshot eventSnapshot : eventSnapshots) {
                            eventSnapshot.getReference()
                                    .collection("entrants")
                                    .document(deviceId)
                                    .delete();
                        }

                        // Remove all events organized by ONLY this user
                        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                                .whereArrayContains("organizers", user.getId())
                                .get()
                                .addOnSuccessListener(snapshot -> {
                                    for (QueryDocumentSnapshot eventSnapshot : snapshot) {
                                        java.util.List<String> organizers = (java.util.List<String>) eventSnapshot.get("organizers");
                                        if (organizers != null && organizers.size() == 1) {
                                            removeEvent(eventSnapshot.getId(), eventSnapshot.getString("image"), appContext);
                                        }
                                    }
                                });

                        // Delete profile image
                        db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId).get()
                                .addOnSuccessListener(userSnapshot -> {
                                    String imageURL = userSnapshot.getString("image");
                                    deleteOldProfileImage(imageURL);

                                    // Delete the user document after
                                    db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId).delete()
                                            .addOnSuccessListener(unused -> {
                                                Toast.makeText(appContext, "Profile deleted", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(UserSettingsActivity.this, MainActivity.class);
                                                intent.putExtra("clearSession", true);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                                finish();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(appContext, "Failed to delete profile", Toast.LENGTH_SHORT).show());
                                });
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(appContext, "Failed to delete profile", Toast.LENGTH_SHORT).show());
        });

        // Taken from Claude March 11th 2026, "What query can I use to load in current settings"
        db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Map<String, Object> settings = (Map<String, Object>) snapshot.get("settings");
                        if (settings != null) {
                            Boolean adminNotif = (Boolean) settings.get("adminNotifications");
                            Boolean organizerNotif = (Boolean) settings.get("organizerNotifications");
                            if (adminNotif != null) adminNotificationsSwitch.setChecked(adminNotif);
                            if (organizerNotif != null) organizerNotificationsSwitch.setChecked(organizerNotif);
                        }
                    }
                    // Set listeners AFTER loading so setChecked doesn't trigger writes
                    // Taken from Claude March 11th 2026, "What queries can I use to update currently selected settings"
                    adminNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId)
                                .update("settings.adminNotifications", isChecked)
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to save setting", Toast.LENGTH_SHORT).show());
                    });
                    organizerNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId)
                                .update("settings.organizerNotifications", isChecked)
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to save setting", Toast.LENGTH_SHORT).show());
                    });
                })
                .addOnFailureListener(e ->
                        Log.e("UserSettings", "Failed to load settings: " + e.getMessage()));

    }

    /**
     * Deletes the previously stored profile image associated with the given URL.
     * <p>
     * Looks up matching image records in the Firestore images collection and
     * removes them using the {@link Image} helper.
     *
     * @param URL the URL of the profile image to delete
     */
    private void deleteOldProfileImage(String URL) {
        db.collection(FirestoreCollections.IMAGES_COLLECTION)
                .whereEqualTo("URL", URL)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Image oldImage = new Image(URL, doc.getId());
                        oldImage.delete(this, db);
                    }
                });
    }

    /**
     * Performs logic to remove event from the Firestore Database, including any references to that event
     *
     * @param eventId   Firestore ID of event
     * @param eventImage    Image URL of event poster
     */
    private void removeEvent(String eventId, String eventImage, android.content.Context context) {
        // Code generated by Claude AI March 11, 2026
        // "How to remove event ID from a users list of events for each user document in the
        // events 'entrants' subcollection."
        CollectionReference eventEntrantsRef = db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId).collection("entrants");
        CollectionReference eventCommentsRef = db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId).collection("comments");
        CollectionReference eventNotificationsRef = db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId).collection("notifications");

        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> entrantsTask = eventEntrantsRef.get();
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> commentsTask = eventCommentsRef.get();
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> notificationsTask = eventNotificationsRef.get();

        com.google.android.gms.tasks.Tasks.whenAllSuccess(entrantsTask, commentsTask, notificationsTask)
                .addOnSuccessListener(results -> {
                    WriteBatch batch = db.batch();

                    com.google.firebase.firestore.QuerySnapshot userSnapshots = (com.google.firebase.firestore.QuerySnapshot) results.get(0);
                    com.google.firebase.firestore.QuerySnapshot commentSnapshots = (com.google.firebase.firestore.QuerySnapshot) results.get(1);
                    com.google.firebase.firestore.QuerySnapshot notificationSnapshots = (com.google.firebase.firestore.QuerySnapshot) results.get(2);

                    // Remove event from each user's events array and delete entrant docs
                    for (DocumentSnapshot userDoc : userSnapshots.getDocuments()) {
                        DocumentReference userRef = db.collection(FirestoreCollections.USERS_COLLECTION).document(userDoc.getId());
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("events", FieldValue.arrayRemove(eventId));
                        batch.set(userRef, updateData, SetOptions.merge());
                        batch.delete(userDoc.getReference());
                    }

                    // Delete all comment documents
                    for (DocumentSnapshot commentDoc : commentSnapshots.getDocuments()) {
                        batch.delete(commentDoc.getReference());
                    }

                    // Delete all notification documents
                    for (DocumentSnapshot notificationDoc : notificationSnapshots.getDocuments()) {
                        batch.delete(notificationDoc.getReference());
                    }

                    // Remove the event's poster from the database
                    db.collection(FirestoreCollections.IMAGES_COLLECTION)
                            .whereEqualTo("URL", eventImage)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    new Image(eventImage, doc.getId()).delete(context, db);
                                }
                            });

                    // Delete event document and commit
                    batch.delete(db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId));

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Firestore", "Event deleted successfully");
                                Toast.makeText(context, "Event deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Firestore", "Error deleting event", e);
                                Toast.makeText(context, "Failed to delete event", Toast.LENGTH_SHORT).show();
                            });

                }).addOnFailureListener(e -> Log.e("Firestore", "Error fetching subcollections", e));
    };
}