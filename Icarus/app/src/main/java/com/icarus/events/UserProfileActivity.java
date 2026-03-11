package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private EditText nameEditText;
    private EditText phoneEditText;
    private EditText emailEditText;
    private Button editProfileButton;
    private ImageButton userSettingsButton;
    private User user;
    private boolean editState = false;
    FirebaseFirestore db = FirebaseFirestore.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Initialize database reference and collection references
        db = FirebaseFirestore.getInstance();

        // Initialize buttons
        editProfileButton = findViewById(R.id.user_profile_edit_confirm_button);
        userSettingsButton = findViewById(R.id.user_profile_settings_button);

        // Initialize text fields
        nameEditText = findViewById(R.id.user_profile_name_edit);
        emailEditText = findViewById(R.id.user_profile_email_edit);
        phoneEditText = findViewById(R.id.user_profile_phone_edit);

        // Retrieve device Id/User object
        user = UserSession.getInstance().getCurrentUser();
        String deviceId = user.getId();

        // Set buttons on click listeners
        editProfileButton.setOnClickListener(v -> {
            if (editState) {
                String name = nameEditText.getText().toString().trim();
                String email = emailEditText.getText().toString().trim();
                String phone = phoneEditText.getText().toString().trim();

                // Check if user entered name in text fields
                if (name.isEmpty()) {
                    Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                    return;
                }
                /**@ TODO
                 * Check if user entered email in text field IF THAT IS A REQUIREMENT
                 */
                // Send user data to database
                Map<String, Object> userData = new HashMap<>();
                userData.put("name", name);
                if (!email.isEmpty()) userData.put("email", email);
                if (!phone.isEmpty()) userData.put("phone", phone);

                db.collection("users").document(deviceId).set(userData)
                        .addOnSuccessListener(unused -> {
                            // Add information into global session
                            user.setName(name);
                            if (!email.isEmpty()) user.setEmail(email);
                            if (!phone.isEmpty()) user.setPhone(phone);
                            changeEditState();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                        });
            } else {
                changeEditState();
            }
        });

        userSettingsButton.setOnClickListener(v -> {
            //Intent intent = new Intent(this, UserSettingsActivity.class);
            //startActivity(intent);
        });
    }

    public void changeEditState() {
        // User confirmed changes and finished editing profile
        if (editState) {
            // Disable text edits
            nameEditText.setEnabled(false);
            emailEditText.setEnabled(false);
            phoneEditText.setEnabled(false);
            //Set button to be normal colour
            editProfileButton.setBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(
                            this,
                            R.color.primary_container
                    )
            );
            editProfileButton.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            this,
                            R.color.primary_container_highlighted
                    )
            );
            editProfileButton.setText("Edit Profile");
            editState = false;
        } else {
            //User clicked edit profile

            // Enable text edits
            nameEditText.setEnabled(true);
            emailEditText.setEnabled(true);
            phoneEditText.setEnabled(true);
            // Set button to be normal colour
            editProfileButton.setBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(
                            this,
                            R.color.primary_container_highlighted
                    )
            );
            editProfileButton.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            this,
                            R.color.primary_container
                    )
            );
            editProfileButton.setText("Confirm Changes");
            editState = true;
        }
    }
}
