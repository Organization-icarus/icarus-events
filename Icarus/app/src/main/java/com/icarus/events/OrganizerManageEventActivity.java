package com.icarus.events;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Activity that allows organizers to manage a event.
 * <p>
 * This activity lets an organizer manage an event by loading its data from Firestore.
 * It displays the event title and poster, using a default image if none exists.
 * Buttons allow viewing entrants, sampling attendees, or navigating to other screens.
 * The poster can be updated: chosen images upload to Cloudinary and update Firestore.
 * Old images are deleted, and the UI refreshes to show the new poster.
 * <p>
 * This activity extends {@link HeaderNavBarActivity} to include
 * the application's reusable navigation bar.
 *
 * @author Ben Salmon
 */

public class OrganizerManageEventActivity extends HeaderNavBarActivity {

    private ImageView eventPoster;
    private Button UpdatePoster;
    private Button ViewEntrantMap;
    private Button ViewEntrantList;
    private Button inviteEntrant;
    private Button addOrganizers;
    private Button ReplaceDeclined;
    private Button shareQRCode;
    private TextView eventTitle;
    private String eventId;
    private String eventName;
    private String posterURL;
    private Boolean isPrivate;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_manage_event);
        setupNavBar();

        //Create Image
        eventPoster = findViewById(R.id.OrganizerManageEventViewImage);
        //Create Buttons
        UpdatePoster = findViewById(R.id.OrganizerManageEventUpdatePoster);
        ViewEntrantMap = findViewById(R.id.OrganizerManageEventViewEntrantMap);
        ViewEntrantList = findViewById(R.id.OrganizerManageEventViewEntrantList);
        addOrganizers = findViewById(R.id.OrganizerManageEventAddOrganizer);
        ReplaceDeclined = findViewById(R.id.OrganizerManageEventReplaceDeclined);
        shareQRCode = findViewById(R.id.OrganizerManageEventShareQRCode);
        inviteEntrant = findViewById(R.id.OrganizerManageEventInviteEntrant);

        //Create textView
        eventTitle = findViewById(R.id.OrganizerManageEventTitle);

        eventId = getIntent().getStringExtra("eventId");

        db = FirebaseFirestore.getInstance();

        db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    eventName = document.getString("name");
                    eventTitle.setText(eventName);
                    isPrivate = document.getBoolean("isPrivate");
                    if(isPrivate == null){isPrivate = false;}
                    if(isPrivate){
                        inviteEntrant.setText("Invite Specific Entrant");
                    } else {
                        inviteEntrant.setText("Sample Attendees");
                    }
                });


        // Initialize imagePickerLauncher
        ActivityResultLauncher<String> imagePickerLauncher = createImagePicker();

        //Upload image into activity
        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(eventId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String imageURL = snapshot.getString("image");
                        if (imageURL != null && !imageURL.isEmpty()) {
                            Picasso.get()
                                    .load(snapshot.getString("image"))
                                    .error(R.drawable.poster)           // Optional: shows if link fails
                                    .into(eventPoster);
                        } else {
                            eventPoster.setImageResource(R.drawable.poster);
                        }
                    }
                })
                .addOnFailureListener( e -> {
                    Toast.makeText(this, "Failed to load poster", Toast.LENGTH_SHORT).show();
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
        ViewEntrantMap.setOnClickListener(v -> {
            // View Entrant Map
            Intent intent = new Intent(this, EntrantMapActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });
        ViewEntrantList.setOnClickListener(v -> {
            // View Entrant List
            Intent intent = new Intent(this, OrganizerViewEntrantsOnWaitingListActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });
        inviteEntrant.setOnClickListener(v -> {
            // Invite Entrants to a private event
            if(isPrivate){
                Intent intent = new Intent(this, OrganizerEntrantSearchActivity.class);
                intent.putExtra("eventId", eventId);
                intent.putExtra("ActivityName", "Entrant Search");
                startActivity(intent);
            }else{
                Intent intent = new Intent(this, SampleAttendeesActivity.class);
                intent.putExtra("eventId", eventId);
                startActivity(intent);
            }


        });
        shareQRCode.setOnClickListener(v -> {
            // Open Share menu to share a QR code containing the event ID.
            try {
                Bitmap qrBitmap = generateQRCodeBitmap(eventId);
                shareQRCodeBitmap(qrBitmap);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
                Log.e("QR_CODE_ERROR", "Failed to generate/share QR code", e);
            }
        });
        addOrganizers.setOnClickListener(v -> {
            // add Organizers as Co-Organzers
            Intent intent = new Intent(this, OrganizerEntrantSearchActivity.class);
            intent.putExtra("eventId", eventId);
            intent.putExtra("ActivityName", "Find Co-Organizers");
            startActivity(intent);
        });
        ReplaceDeclined.setOnClickListener(v -> {
            // Replaced Declined
            Intent intent = new Intent(this, OrganizerEntrantSearchActivity.class);
            intent.putExtra("eventId", eventId);
            intent.putExtra("ActivityName", "Replace Declined");
            startActivity(intent);

        });
    }

    // Taken from ChatGPT March 29th 2026,
    // "How do I generate a QR code bitmap from a string using ZXing"
    /**
     * Generates a QR code bitmap that encodes the provided event ID.
     *
     * @param content The event ID to encode into the QR code.
     * @return A bitmap representation of the QR code.
     * @throws Exception if QR generation fails.
     */
    private Bitmap generateQRCodeBitmap(String content) throws Exception {
        int size = 800;
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size);

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }

    // Taken from ChatGPT March 29th 2026,
    // "How do I implement sharing a generated bitmap image"
    /**
     * Shares the generated QR code bitmap using Android's share sheet.
     *
     * @param bitmap The QR code bitmap to share.
     * @throws IOException if the bitmap cannot be written to cache.
     */
    private void shareQRCodeBitmap(Bitmap bitmap) throws IOException {
        File cachePath = new File(getCacheDir(), "images");
        if (!cachePath.exists()) {
            cachePath.mkdirs();
        }

        File file = new File(cachePath, eventName + "_qr_code.png");
        FileOutputStream stream = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        stream.flush();
        stream.close();

        Uri contentUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                file
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.putExtra(Intent.EXTRA_TEXT, eventName + " QR Code");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share Event QR Code"));
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
                                                    eventPoster.setImageURI(uri);
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
