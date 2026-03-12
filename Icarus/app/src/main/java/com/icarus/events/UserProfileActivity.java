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

/**
 * Activity that allows a user to view and update their profile information.
 * <p>
 * The screen displays the user's current name, email, and phone number.
 * Users can toggle between view mode and edit mode to modify their profile
 * details. When changes are confirmed, the updated information is written
 * to the Firestore "users" collection and the local {@link UserSession}
 * is updated accordingly.
 *
 * @author Alex Alves
 */
public class UserProfileActivity extends NavigationBarActivity {

    private EditText nameEditText;
    private EditText phoneEditText;
    private EditText emailEditText;
    private Button editProfileButton;
    private ImageButton userSettingsButton;
    private User user;
    private boolean editState = false;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Initializes the user profile screen and populates the fields with
     * the current user's information.
     *
     * @param savedInstanceState previously saved activity state, or null
     *                           if the activity is newly created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        setupNavBar();

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

        //Pre fill text fields if user is logged in and information exists
        nameEditText.setText(user.getName());
        if (user.getEmail() != null) emailEditText.setText(user.getEmail());
        if (user.getPhone() != null)phoneEditText.setText(user.getPhone());

        // Set buttons on click listeners
        userSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserSettingsActivity.class);
            startActivity(intent);
        });

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

                db.collection("users").document(deviceId).update(userData)
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
    }

    /**
     * Toggles the profile editing state.
     * <p>
     * When editing is enabled, text fields become editable and the
     * button changes to "Confirm Changes". When editing is disabled,
     * the fields become read-only and the button returns to "Edit Profile".
     */
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
