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
    private Boolean locationEnabled;
    private Boolean isPrivate;

    private FirebaseFirestore db;

    /**
     * Initializes the activity, loads the selected event from Firestore, populates
     * the UI with event information, configures poster loading, and sets up button
     * actions for managing entrants, organizers, QR code sharing, and poster updates.
     *
     * @param savedInstanceState the saved activity state, or {@code null} if none exists
     */
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
                    locationEnabled = document.getBoolean("geolocation");
                    if(isPrivate == null){isPrivate = false;}
                    if(locationEnabled == null){locationEnabled = false;}
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
                                    .error(R.drawable.poster)
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
            if (locationEnabled) {
                // View Entrant Map
                Intent intent = new Intent(this, EntrantMapActivity.class);
                intent.putExtra("eventId", eventId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Geolocation is not enabled for this event.", Toast.LENGTH_SHORT).show();
            }
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
                intent.putExtra("ActivityName", eventName);
                startActivity(intent);
            }
        });

        shareQRCode.setOnClickListener(v -> {
            // Open Share menu to share a QR code containing the event ID.
            if(!isPrivate){
                try {
                    Bitmap qrBitmap = generateQRCodeBitmap(eventId);
                    shareQRCodeBitmap(qrBitmap);
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
                    Log.e("QR_CODE_ERROR", "Failed to generate/share QR code", e);
                }
            }else{
                Toast.makeText(this, "This event is Private. No QR code will be created", Toast.LENGTH_SHORT).show();
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

    /**
     * Generates a QR code bitmap that encodes the provided content string.
     * The generated bitmap includes white padding around the QR code.
     *
     * @param content the string to encode into the QR code
     * @return a bitmap containing the generated QR code
     * @throws Exception if QR code generation fails
     */
    // Taken from ChatGPT March 29th 2026,
    // "How do I generate a QR code bitmap from a string using ZXing"
    private Bitmap generateQRCodeBitmap(String content) throws Exception {
        int qrSize = 800;
        int padding = 80;
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, qrSize, qrSize);

        int outputSize = qrSize + (padding * 2);
        Bitmap bitmap = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.RGB_565);

        for (int x = 0; x < outputSize; x++) {
            for (int y = 0; y < outputSize; y++) {
                bitmap.setPixel(x, y, Color.WHITE);
            }
        }

        for (int x = 0; x < qrSize; x++) {
            for (int y = 0; y < qrSize; y++) {
                bitmap.setPixel(x + padding, y + padding, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }

        return bitmap;
    }

    /**
     * Shares the given QR code bitmap by writing it to cache storage and launching
     * Android's share sheet with a content URI.
     *
     * @param bitmap the bitmap image to share
     * @throws IOException if the bitmap cannot be written to cache storage
     */
    // Taken from ChatGPT March 29th 2026,
    // "How do I implement sharing a generated bitmap image"
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

    /**
     * Deletes the previously stored poster image record associated with the given URL
     * from the images collection.
     *
     * @param URL the URL of the image to delete
     */
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

    /**
     * Creates and returns an activity result launcher that opens the system image picker.
     * When the user selects an image, the image is uploaded to Cloudinary, stored in
     * Firestore, the old poster is deleted, and the UI is updated with the new poster.
     *
     * @return an {@link ActivityResultLauncher} used to select an image from device storage
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
                                                    Toast.makeText(OrganizerManageEventActivity.this,
                                                            "Image uploaded", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(OrganizerManageEventActivity.this,
                                                            "Image upload failed", Toast.LENGTH_SHORT).show();
                                                });
                                    }

                                    @Override
                                    public void onError(String requestId, ErrorInfo error) {
                                        Toast.makeText(OrganizerManageEventActivity.this,
                                                "Image upload failed.", Toast.LENGTH_SHORT).show();
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