package com.icarus.events;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.CountDownLatch;

/**
 * Utility class that manages the names of Firestore collections used throughout the application.
 * <p>
 * This class provides a centralized location for collection identifiers, ensuring consistency
 * across the app. It also includes utility methods to toggle between production and
 * test environments, allowing for automated testing without polluting real user data.
 *
 * @author Kito Lee Son
 */
public class FirestoreCollections {
    public static String EVENTS_COLLECTION = "events";
    public static String USERS_COLLECTION = "users";
    public static String IMAGES_COLLECTION = "images";
    public static String EVENT_CATEGORIES_COLLECTION = "event-categories";
    public static String NOTIFICATIONS_COLLECTION = "notifications";

    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Swaps the active collection names to their "test" counterparts.
     * <p>
     * This method should be called before running integration tests to ensure
     * that data is written to isolated test collections rather than production ones.
     */
    public static void startTest(){
        EVENTS_COLLECTION = "events_test";
        USERS_COLLECTION = "users_test";
        IMAGES_COLLECTION = "images_test";
        NOTIFICATIONS_COLLECTION = "notifications_test";
    }

    /**
     * Reverts collection names to production defaults and wipes the test data.
     * <p>
     * This method utilizes a {@link CountDownLatch} to block the calling thread
     * until all asynchronous deletion tasks across the four test collections
     * have been completed.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     * for the cleanup tasks to finish.
     */
    public static void endTest() throws InterruptedException {
        EVENTS_COLLECTION = "events";
        USERS_COLLECTION = "users";
        IMAGES_COLLECTION = "images";
        NOTIFICATIONS_COLLECTION = "notifications";

        CountDownLatch latch = new CountDownLatch(4);
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
        db.collection("notifications_test")
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
