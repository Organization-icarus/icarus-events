package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class UserSettingsActivity extends NavigationBarActivity{
    private Button deleteProfileButton;
    private Switch adminNotificationsSwitch;
    private Switch organizerNotificationsSwitch;
    private User user;
    FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_settings);
        setupNavBar();

        // Initialize database reference and collection references
        db = FirebaseFirestore.getInstance();

        // Initialize buttons
        deleteProfileButton = findViewById(R.id.user_profile_delete_button);

        // Initialize switches
        adminNotificationsSwitch = findViewById(R.id.user_settings_admin_notifications_switch);
        organizerNotificationsSwitch = findViewById(R.id.user_settings_org_notifications_switch);

        // Retrieve device Id/User object
        user = UserSession.getInstance().getCurrentUser();
        String deviceId = user.getId();

        // Set buttons on click listeners
        // Taken from Claude March 11th 2026,
        // "I need to modify my query to also delete the user from the event collection entrant subcollection"
        deleteProfileButton.setOnClickListener(v -> {
            // First remove user from all event entrant subcollections
            db.collection("events").get()
                    .addOnSuccessListener(eventSnapshots -> {
                        for (QueryDocumentSnapshot eventSnapshot : eventSnapshots) {
                            eventSnapshot.getReference()
                                    .collection("entrants")
                                    .document(deviceId)
                                    .delete();
                        }
                        // Then delete the user document itself
                        db.collection("users").document(deviceId).delete()
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();
                                    UserSession.getInstance().clear();
                                    startActivity(new Intent(this, MainActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show());
        });


        // Taken from Claude March 11th 2026, "What query can I use to load in current settings"
        db.collection("users").document(deviceId).get()
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
                        db.collection("users").document(deviceId)
                                .update("settings.adminNotifications", isChecked)
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to save setting", Toast.LENGTH_SHORT).show());
                    });
                    organizerNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        db.collection("users").document(deviceId)
                                .update("settings.organizerNotifications", isChecked)
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to save setting", Toast.LENGTH_SHORT).show());
                    });
                })
                .addOnFailureListener(e ->
                        Log.e("UserSettings", "Failed to load settings: " + e.getMessage()));

    }
}