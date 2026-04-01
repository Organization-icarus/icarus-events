package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Activity that allows a new user to register in the application.
 * <p>
 * Users enter their name and select a role before their information
 * is stored in the Firestore "users" collection. Once registered,
 * the user is stored in the {@link UserSession} and redirected to
 * the event list screen.
 *
 * @author Alex Alves
 */
public class UserRegistrationActivity extends AppCompatActivity {
    private EditText nameEditText;
    private EditText emailEditText;
    private EditText phoneEditText;
    private Button registerButton;
    private FirebaseFirestore db;
    private String deviceId;

    /**
     * Initializes the user registration interface.
     * <p>
     * This method sets up the input fields and handles registration
     * when the user submits their name and role. The user information
     * is validated and then saved to Firestore.
     *
     * @param savedInstanceState the previously saved activity state,
     *                           or null if the activity is newly created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_registration);

        // Initialize database reference and collection references
        db = FirebaseFirestore.getInstance();

        // Initialize buttons
        registerButton = findViewById(R.id.user_register_button);

        // Initialize text field
        nameEditText = findViewById(R.id.user_register_name_field);
        emailEditText = findViewById(R.id.user_register_email_field);
        phoneEditText = findViewById(R.id.user_register_phone_field);


        // Retrieve device Id
        deviceId = getIntent().getStringExtra("deviceId");

        // Set buttons on click listeners
        registerButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String phone = phoneEditText.getText().toString().trim();

            // Check if user entered name in text field
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if user entered email in text field
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }
            String userPhone = phone.isEmpty() ? null : phone;

            Boolean isAdmin = false;

            // Initialize default settings map
            Map<String, Object> settings = new HashMap<>();
            settings.put("adminNotifications", true);
            settings.put("organizerNotifications", true);

            // Send user data to database
            Map<String, Object> userData = new HashMap<>();
            userData.put("name", name);
            userData.put("email", email);
            if (userPhone != null) userData.put("phone", phone);
            userData.put("isAdmin", isAdmin);
            userData.put("settings", settings);
            userData.put("image", "");

            db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId).set(userData)
                    .addOnSuccessListener(unused -> {

                        // Add information into global session and return user to event list
                        User user = new User(deviceId, name, email, userPhone, "", isAdmin, null, null, null);
                        UserSession.getInstance().setCurrentUser(user);
                        startActivity(new Intent(this, EntrantEventListActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to register", Toast.LENGTH_SHORT).show();
                    });
        });
    }
}
