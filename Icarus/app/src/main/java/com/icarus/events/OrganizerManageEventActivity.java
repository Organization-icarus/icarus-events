package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class OrganizerManageEventActivity extends NavigationBarActivity{
    private Button ViewEntrantMap;
    private Button ViewEntrantList;
    private Button UpdatePoster;
    private Button SampleAttendees;
    private Button ReplaceDeclined;
    private TextView eventTitle;
    private String eventId;
    private String posterURL;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_manage_event);
        setupNavBar();

        //Create Buttons
        ViewEntrantMap = findViewById(R.id.OrganizerManageEventViewEntrantMap);
        ViewEntrantList = findViewById(R.id.OrganizerManageEventViewEntrantList);
        UpdatePoster = findViewById(R.id.OrganizerManageEventUpdatePoster);
        SampleAttendees = findViewById(R.id.OrganizerManageEventSampleAttendees);
        ReplaceDeclined = findViewById(R.id.OrganizerManageEventReplaceDeclined);

        //Create textView
        eventTitle = findViewById(R.id.OrganizerManageEventTitle);

        eventId = getIntent().getStringExtra("eventId");

        db = FirebaseFirestore.getInstance();

        db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    String eventName = document.getString("name");
                    eventTitle.setText(eventName);
                });

        // Initialize imagePickerLauncher
        ActivityResultLauncher<String> imagePickerLauncher = createImagePicker();

        ViewEntrantMap.setOnClickListener(v -> {
            // View Entrant Map
//            Intent intent = new Intent(this, UserRegistrationActivity.class);
//            intent.putExtra("deviceId", deviceId);
//            startActivity(intent);
        });
        ViewEntrantList.setOnClickListener(v -> {
            // View Entrant List
            Intent intent = new Intent(this, OrganizerViewEntrantsOnWaitingListActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });
        UpdatePoster.setOnClickListener(v -> {
            // Get old poster url
            db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId).get()
                    .addOnSuccessListener(document -> {
                        posterURL = document.getString("image");
                    });
            // Update Poster
            imagePickerLauncher.launch("image/*");
        });
        SampleAttendees.setOnClickListener(v -> {
            // Sample Attendees
            Intent intent = new Intent(this, SampleAttendeesActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);

        });
        ReplaceDeclined.setOnClickListener(v -> {
            // Replaced Declined
//            Intent intent = new Intent(this, UserRegistrationActivity.class);
//            intent.putExtra("deviceId", deviceId);
//            startActivity(intent);

        });
    }

    private void deleteOldPoster(String URL) {
        db.collection(FirestoreCollections.IMAGES_COLLECTION)
                .whereEqualTo("URL", URL)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Image oldPoster = new Image(URL, doc.getId());
                        oldPoster.delete(this, db);
                    }
                });
    }

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
                                        String newPosterURL = (String) resultData.get("secure_url");
                                        String newPublicId = (String) resultData.get("public_id");
                                        // Create new firebase document for the image
                                        Map<String, Object> imageData = new HashMap<>();
                                        imageData.put("URL", newPosterURL);
                                        db.collection(FirestoreCollections.IMAGES_COLLECTION)
                                                .document(newPublicId)
                                                .set(imageData)
                                                .addOnSuccessListener(unused -> {
                                                    deleteOldPoster(posterURL);
                                                    db.collection(FirestoreCollections.EVENTS_COLLECTION)
                                                            .document(eventId)
                                                            .update("image", newPosterURL);
                                                    posterURL = newPosterURL;
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(OrganizerManageEventActivity.this,
                                                            "Failed to add image to firestore", Toast.LENGTH_SHORT).show();
                                                });
                                    }

                                    @Override
                                    public void onError(String requestId, ErrorInfo error) {
                                        Toast.makeText(OrganizerManageEventActivity.this,
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
