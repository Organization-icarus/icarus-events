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
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.provider.Settings;

import com.cloudinary.android.MediaManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
    private static boolean isCloudinaryInitialized = false;
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

        if (getIntent().getBooleanExtra("clearSession", false)) {
            UserSession.getInstance().clear();
        }

        // Setup MediaManager for Cloudinary image storage (ONLY DO ONCE)
        if (!isCloudinaryInitialized) {
            Map config = new HashMap();
            config.put("cloud_name", "icarus-images");
            config.put("api_key", "291231889216385");
            config.put("api_secret", "ToWWi626oI0M7Ou1pmPQx_vd5x8");
            MediaManager.init(this, config);
            isCloudinaryInitialized = true;
        }

        //Create Background Worker thread that checks Event date (DO THIS ONCE)
        //See EventStatusBackgroundWorker for task
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                EventStatusBackgroundWorker.class,
                15, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "EventStatusCheck",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );

        //Proceed as normal
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String rawDeviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceId = hashDeviceId(rawDeviceId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        // if user exists, load that user into global session and go to event list
                        Boolean isAdmin = snapshot.getBoolean("isAdmin");
                        User user = new User(
                                snapshot.getId(),
                                snapshot.getString("name"),
                                snapshot.getString("email"),
                                snapshot.getString("phone"),
                                snapshot.getString("image"),
                                isAdmin != null ? isAdmin : false,
                                (ArrayList<String>) snapshot.get("events"),
                                (ArrayList<String>) snapshot.get("organizedEvents"),
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
    // Taken from ChatGPT March 29th 2026,
    //"Create a method to hash our device ids"
    /**
     * Hashes the provided device ID using SHA-256 so a deterministic but non-raw
     * identifier can be stored and used by the app.
     *
     * @param rawDeviceId the raw Android device ID
     * @return the SHA-256 hash of the device ID as a lowercase hex string
     */
    private String hashDeviceId(String rawDeviceId) {
        if (rawDeviceId == null) {
            return "";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawDeviceId.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}