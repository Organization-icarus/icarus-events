package com.icarus.events;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.CountDownLatch;

public class FirestoreCollections {
    public static String EVENTS_COLLECTION = "events";
    public static String USERS_COLLECTION = "users";
    public static String IMAGES_COLLECTION = "images";
    public static String NOTIFICATIONS_COLLECTION = "notifications";
    public static String EVENT_CATEGORIES_COLLECTION = "event-categories";

    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static void startTest(){
        EVENTS_COLLECTION = "events-test";
        USERS_COLLECTION = "users-test";
        IMAGES_COLLECTION = "images-test";
        NOTIFICATIONS_COLLECTION = "notifications-test";

    }

    public static void endTest() throws InterruptedException {
        EVENTS_COLLECTION = "events";
        USERS_COLLECTION = "users";
        IMAGES_COLLECTION = "images";

        CountDownLatch latch = new CountDownLatch(2);
        db.collection("events-test")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot) {
                        // Delete entrants subcollection
                        doc.getReference().collection("entrants").get()
                                .addOnSuccessListener(entrants -> {
                                    for (DocumentSnapshot entrant : entrants) {
                                        entrant.getReference().delete();
                                    }
                                });
                        // Delete comments subcollection
                        doc.getReference().collection("comments").get()
                                .addOnSuccessListener(comments -> {
                                    for (DocumentSnapshot comment : comments) {
                                        comment.getReference().delete();
                                    }
                                });
                        doc.getReference().delete();
                    }
                    latch.countDown();
                });
        db.collection("users-test")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot) {
                        doc.getReference().delete();
                    }
                    latch.countDown();
                });
        db.collection("images-test")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot) {
                        doc.getReference().delete();
                    }
                    latch.countDown();
                });
        db.collection("notifications-test")
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
