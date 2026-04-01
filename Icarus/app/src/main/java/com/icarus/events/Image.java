package com.icarus.events;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.cloudinary.android.MediaManager;
import com.cloudinary.utils.ObjectUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.Map;

/**
 * Represents an image in the application.
 * <p>
 * Stores image information retrieved from Firebase Firestore, including
 * the imag's URL and Cloudinary publicId.
 *
 * @author Benjamin Hall
 */
public class Image {
    private String URL;
    private String publicId;

    /**
     * Creates new Image object with the provided parameters.
     *
     * @param URL       URL of the Image
     * @param publicId  Cloudinary publicId of the image (for displaying with Picasso)
     */
    public Image (String URL, String publicId) {
        this.URL = URL;
        this.publicId = publicId;
    }

    /**
     * Gets URL of the image
     *
     * @return  URL of the image object.
     */
    public String getURL() {
        return URL;
    }

    /**
     * Gets publicId of the image
     *
     * @return  PublicId of the image object.
     */
    public String getPublicId() {
        return publicId;
    }

    /**
     * Deletes the document corresponding to the image object from Firestore and Cloudinary
     *
     * @param context   Context of the activity calling the delete method
     * @param db        Firestore database reference
     */
    public void delete(Context context, FirebaseFirestore db) {
        // Taken from Claude AI March 21st, 2026
        // "How to delete image from cloudinary database"
        new Thread(() -> {
            try {
                Map result = MediaManager.get().getCloudinary()
                        .uploader()
                        .destroy(this.publicId, ObjectUtils.emptyMap());

                if ("ok".equals(result.get("result"))) {
                    // Cloudinary delete succeeded, now delete from Firestore
                    new Handler(Looper.getMainLooper()).post(() -> {
                        db.collection(FirestoreCollections.IMAGES_COLLECTION)
                                .document(this.publicId)
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    // Remove image URl from all events and users that use it
                                    db.collection(FirestoreCollections.EVENTS_COLLECTION)
                                            .whereEqualTo("image", this.URL)
                                            .get()
                                            .addOnSuccessListener(querySnapshot -> {
                                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                                    doc.getReference().update("image", "No Image");
                                                }
                                            });
                                    db.collection(FirestoreCollections.USERS_COLLECTION)
                                            .whereEqualTo("image", this.URL)
                                            .get()
                                            .addOnSuccessListener(querySnapshot -> {
                                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                                    doc.getReference().update("image", "No Image");
                                                }
                                            });
                                })
                                .addOnFailureListener(e ->
                                        Log.e("IMAGE_DELETE_ERROR", e.getMessage())
                                );
                    });
                } else {
                    Log.e("IMAGE_DELETE_ERROR", "Cloudinary delete failed for publicId: " + this.publicId);
                }
            } catch (IOException e) {
                Log.e("IMAGE_DELETE_ERROR", e.getMessage());
            }
        }).start();
    }
}
