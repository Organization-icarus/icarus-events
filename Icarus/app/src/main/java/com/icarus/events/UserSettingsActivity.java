package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Picasso;

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
}