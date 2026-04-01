package com.icarus.events;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Picasso;

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
public class UserProfileActivity extends HeaderNavBarActivity {

    private ImageView profileImage;
    private EditText nameEditText;
    private EditText phoneEditText;
    private EditText emailEditText;
    private Button editProfileButton;
    private Button adminDeleteButton;
    private ImageButton userSettingsButton;
    private String profileImageURL;
    private ActivityResultLauncher<String> imagePickerLauncher;
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
        adminDeleteButton = findViewById(R.id.user_profile_admin_delete_button);

        // Initialize User Image View
        profileImage = findViewById(R.id.user_profile_image);
        // Initialize imagePickerLauncher
        ActivityResultLauncher<String> imagePickerLauncher = createImagePicker();


        // Initialize text fields
        nameEditText = findViewById(R.id.user_profile_name_edit);
        emailEditText = findViewById(R.id.user_profile_email_edit);
        phoneEditText = findViewById(R.id.user_profile_phone_edit);

        // Checking if screen was entered from the admin dashboard or regular profile
        String deviceId;
        String passedId = getIntent().getStringExtra("deviceId");

        if (passedId != null) {
            deviceId = passedId;
            // Disable settings menu access, and edit profile, enable delete button for admins
            userSettingsButton.setVisibility(View.GONE);
            editProfileButton.setVisibility(View.GONE);
            adminDeleteButton.setVisibility(View.VISIBLE);

            nameEditText.setHint("Not provided");
            emailEditText.setHint("Not provided");
            phoneEditText.setHint("Not provided");

            // Bring context user information into the menu
            db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId).get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            if (snapshot.getString("name") != null) nameEditText.setText(snapshot.getString("name"));
                            if (snapshot.getString("email") != null) emailEditText.setText(snapshot.getString("email"));
                            if (snapshot.getString("phone") != null) phoneEditText.setText(snapshot.getString("phone"));
                            if (snapshot.getString("image") != null) {
                                profileImageURL = snapshot.getString("image");
                                Picasso.get()
                                        .load(profileImageURL)
                                        .placeholder(R.drawable.poster)
                                        .error(R.drawable.poster)           // Optional: shows if link fails
                                        .into(profileImage);
                            } else {
                                profileImageURL = "";
                                profileImage.setImageResource(R.drawable.poster);
                            }
                        }
                    })
                    .addOnFailureListener( e -> {
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Retrieve device Id/User object
            user = UserSession.getInstance().getCurrentUser();
            deviceId = user.getId();

            //Pre-fill text fields if user is logged in and information exists
            nameEditText.setText(user.getName());
            if (user.getEmail() != null) emailEditText.setText(user.getEmail());
            if (user.getPhone() != null) phoneEditText.setText(user.getPhone());

            // Display profile image
            db.collection(FirestoreCollections.USERS_COLLECTION)
                    .document(deviceId).get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            String imageURL = snapshot.getString("image");
                            profileImageURL = imageURL;
                            if (imageURL != null && !imageURL.isEmpty()) {
                                Picasso.get()
                                        .load(profileImageURL)
                                        .placeholder(R.drawable.poster)
                                        .error(R.drawable.poster)           // Optional: shows if link fails
                                        .into(profileImage);
                            } else {
                                profileImage.setImageResource(R.drawable.poster);
                            }
                        }
                    })
                    .addOnFailureListener( e -> {
                        Toast.makeText(this, "Failed to load profile image", Toast.LENGTH_SHORT).show();
                    });
        }

        // Set buttons on click listeners
        userSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserSettingsActivity.class);
            startActivity(intent);
        });

        adminDeleteButton.setOnClickListener(v -> {
            // First remove user from all event entrant subcollections
            db.collection(FirestoreCollections.USERS_COLLECTION).get()
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

                                    // Then delete the user document itself
                                    db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId).delete()
                                            .addOnSuccessListener(unused -> {
                                                Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();
                                                finish();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "Failed to delete profile", Toast.LENGTH_SHORT).show());
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to delete profile", Toast.LENGTH_SHORT).show());
                    });
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
                // Check if user entered email in text fields
                if (email.isEmpty()) {
                    Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Send user data to database
                Map<String, Object> userData = new HashMap<>();
                userData.put("name", name);
                if (!email.isEmpty()) userData.put("email", email);
                if (!phone.isEmpty()) userData.put("phone", phone);

                db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId).update(userData)
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

        profileImage.setOnClickListener(v -> {
            if(editState) {
                // Get old profile image URL
                db.collection(FirestoreCollections.USERS_COLLECTION).document(user.getId()).get()
                        .addOnSuccessListener(document -> {
                            profileImageURL = document.getString("image");
                        });
                // Update Profile Image
                imagePickerLauncher.launch("image/*");
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
                            R.color.white
                    )
            );
            editProfileButton.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            this,
                            R.color.primary
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
                            R.color.accent_first
                    )
            );
            editProfileButton.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            this,
                            R.color.primary
                    )
            );
            editProfileButton.setText("Confirm Changes");
            editState = true;
        }
    }

    /**
     * Delete image from firestore database
     *
     * @param URL   URL of image to delete
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
     * Create image picker activity for selecting a new profile image.
     *
     * @return  Result of activity
     */
    private ActivityResultLauncher<String> createImagePicker() {
        return registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        // Add the new poster
                        MediaManager.get().upload(uri)
                                .option("upload_preset", "ml_default")
                                .callback(new UploadCallback() {
                                    @Override
                                    public void onSuccess(String requestId, Map resultData) {
                                        String newProfileImageURL = (String) resultData.get("secure_url");
                                        String newPublicId = (String) resultData.get("public_id");
                                        // Create new firebase document for the image
                                        Map<String, Object> imageData = new HashMap<>();
                                        imageData.put("URL", newProfileImageURL);
                                        db.collection(FirestoreCollections.IMAGES_COLLECTION)
                                                .document(newPublicId)
                                                .set(imageData)
                                                .addOnSuccessListener(unused -> {
                                                    deleteOldProfileImage(profileImageURL);
                                                    db.collection(FirestoreCollections.USERS_COLLECTION)
                                                            .document(user.getId())
                                                            .update("image", newProfileImageURL);
                                                    profileImageURL = newProfileImageURL;
                                                    profileImage.setImageURI(uri);
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(UserProfileActivity.this,
                                                            "Failed to add image to firestore", Toast.LENGTH_SHORT).show();
                                                });
                                    }

                                    @Override
                                    public void onError(String requestId, ErrorInfo error) {
                                        Toast.makeText(UserProfileActivity.this,
                                                "Failed to Upload Image.", Toast.LENGTH_SHORT).show();
                                        Log.e("UPLOAD_ERROR", error.getDescription());
                                    }

                                    @Override public void onStart(String requestId) {}
                                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                                })
                                .dispatch();
                    }
                }
        );
    }
}
