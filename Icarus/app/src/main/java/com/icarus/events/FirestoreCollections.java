package com.icarus.events;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.CountDownLatch;

public class FirestoreCollections {
    public static String EVENTS_COLLECTION = "events";
    public static String USERS_COLLECTION = "users";
    public static String IMAGES_COLLECTION = "images";
    public static String EVENT_CATEGORIES_COLLECTION = "event-categories";

    private static FirebaseFirestore db = FirebaseFirestore.getInstance();


    public static void startTest(){
        EVENTS_COLLECTION = "events_test";
        USERS_COLLECTION = "users_test";
        IMAGES_COLLECTION = "images_test";
    }

    public static void endTest() throws InterruptedException {
        EVENTS_COLLECTION = "events";
        USERS_COLLECTION = "users";
        IMAGES_COLLECTION = "images";

        CountDownLatch latch = new CountDownLatch(2);
        db.collection("events_test")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot) {
                        doc.getReference().delete();
                    }
                    latch.countDown();
                });
        db.collection("users_test")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot) {
                        doc.getReference().delete();
                    }
                    latch.countDown();
                });
        db.collection("images_test")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot) {
                        doc.getReference().delete();
                    }
                    latch.countDown();
                });
        latch.await();
    }
}
