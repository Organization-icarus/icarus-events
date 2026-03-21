package com.icarus.events;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.cloudinary.android.MediaManager;
import com.cloudinary.utils.ObjectUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.Map;

public class Image {
    private String URL;
    private String publicId;

    public Image (String URL, String publicId) {
        this.URL = URL;
        this.publicId = publicId;
    }

    public String getURL() {
        return URL;
    }

    public String getPublicId() {
        return publicId;
    }

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
                                    // Remove image URl from all events that use it
                                    db.collection(FirestoreCollections.EVENTS_COLLECTION)
                                            .whereEqualTo("image", this.URL)
                                            .get()
                                            .addOnSuccessListener(querySnapshot -> {
                                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                                    doc.getReference().update("image", "No Image");
                                                }
                                            });
                                    Toast.makeText(context, "Image deleted", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Failed to delete from database", Toast.LENGTH_SHORT).show()
                                );
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, "Failed to delete image", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (IOException e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "Failed to delete image", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
}
