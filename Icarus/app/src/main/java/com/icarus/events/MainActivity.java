package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.provider.Settings;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Map;

/**
 * Entry activity for the application.
 * <p>
 * Determines whether the current device is associated with an existing user
 * in Firebase Firestore. If a user is found, the user is loaded into the
 * global session and the event list activity is launched. Otherwise, the
 * user is redirected to the registration activity.
 *
 * @author Alex Alves
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Initializes the main activity and determines the appropriate screen
     * to display based on whether the device is associated with an existing
     * user in Firestore.
     *
     * @param savedInstanceState the previously saved activity state, or null if
     *                           the activity is being created for the first time
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView deviceIdText = findViewById(R.id.main_device_id_text);

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        deviceIdText.setText("Device ID: " + deviceId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        // if user exists, load that user into global session and go to event list
                        User user = new User(
                                snapshot.getId(),
                                snapshot.getString("name"),
                                snapshot.getString("email"),
                                snapshot.getString("phone"),
                                snapshot.getString("role"),
                                (ArrayList<String>) snapshot.get("events"),
                                (Map<String, Object>) snapshot.get("settings")
                        );
                        UserSession.getInstance().setCurrentUser(user);
                        startActivity(new Intent(this, EntrantEventListActivity.class));
                    } else {
                        // if user doesn't exist, go to sign up page
                        Intent intent = new Intent(this, UserRegistrationActivity.class);
                        intent.putExtra("deviceId", deviceId);
                        startActivity(intent);
                    }
                    finish();
                });

    }
}