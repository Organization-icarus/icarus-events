package com.icarus.events;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Instrumented integration tests for entrant waitlist actions.
 * <p>
 * User Stories Tested:
 * US 01.01.01 As an entrant, I want to join the waiting list for a specific event.
 * US 01.01.02 As an entrant, I want to leave the waiting list for a specific event.
 * <p>
 * Tests utilize {@link Tasks#await} to handle asynchronous Firestore operations and use
 * a temporary Firestore environment ({@code events_test}, {@code users_test}) to ensure
 * data isolation.
 *
 * @author Kito Lee Son
 */
@RunWith(AndroidJUnit4.class)
public class EntrantWaitlistTest {

    private FirebaseFirestore db;
    private final String testEventId = "test_event_123";
    private final String testUserId = "test_user_456";

    /**
     * Sets up Firestore test data before each test.
     * <p>
     * Switches to test collections, initializes the database instance, and
     * creates a dummy event and user document to serve as the baseline state.
     *
     * @throws InterruptedException if the Firestore operations are interrupted
     */
    @Before
    public void setUp() throws InterruptedException {
        // Switch to test collections (events_test, users_test, etc.)
        FirestoreCollections.startTest();
        db = FirebaseFirestore.getInstance();

        // Ensure environment is clean before starting
        cleanUpData();

        // Create a dummy event and user for testing
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("name", "JUnit Test Event");
        eventData.put("capacity", 10.0);

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", "Test Entrant");
        userData.put("events", new ArrayList<String>());

        try {
            Tasks.await(db.collection(FirestoreCollections.EVENTS_COLLECTION).document(testEventId).set(eventData), 5, TimeUnit.SECONDS);
            Tasks.await(db.collection(FirestoreCollections.USERS_COLLECTION).document(testUserId).set(userData), 5, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cleans up Firestore test data and reverts collection names after each test.
     *
     * @throws InterruptedException if the cleanup wait is interrupted
     */
    @After
    public void tearDown() throws InterruptedException {
        // Wipe the test collections and revert names to production defaults
        FirestoreCollections.endTest();
    }

    /**
     * Deletes specific test documents to ensure a clean state.
     *
     * @throws InterruptedException if the deletion task is interrupted
     */
    private void cleanUpData() throws InterruptedException {
        // Helper to ensure specific test docs are gone
        db.collection(FirestoreCollections.EVENTS_COLLECTION).document(testEventId).delete();
        db.collection(FirestoreCollections.USERS_COLLECTION).document(testUserId).delete();
    }

    /**
     * Verifies that an entrant can successfully join a specific event's waiting list.
     * <p>
     * This test ensures that a document is created in the event's "entrants" subcollection
     * and that the event ID is added to the user's "events" array.
     * <p>
     * User Story Tested:
     * US 01.01.01 As an entrant, I want to join the waiting list for a specific event
     *
     * @throws ExecutionException if the Firestore task fails
     * @throws InterruptedException if the wait operation is interrupted
     * @throws TimeoutException if the database operation times out
     */
    @Test
    public void testJoinWaitingList() throws ExecutionException, InterruptedException, TimeoutException {
        // 1. Simulate the Action (Add to event's entrants and user's event list)
        Map<String, Object> entrantData = new HashMap<>();
        entrantData.put("status", "waitlist");
        entrantData.put("geopoint", null);

        Tasks.await(db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(testEventId)
                .collection("entrants")
                .document(testUserId)
                .set(entrantData), 5, TimeUnit.SECONDS);

        Tasks.await(db.collection(FirestoreCollections.USERS_COLLECTION)
                .document(testUserId)
                .update("events", FieldValue.arrayUnion(testEventId)), 5, TimeUnit.SECONDS);

        // 2. Verify: Check if user exists in the event's subcollection
        DocumentSnapshot entrantDoc = Tasks.await(db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(testEventId)
                .collection("entrants")
                .document(testUserId).get(), 5, TimeUnit.SECONDS);

        assertTrue("User should be in the event's entrants subcollection", entrantDoc.exists());

        // 3. Verify: Check if event ID is in user's events array
        DocumentSnapshot userDoc = Tasks.await(db.collection(FirestoreCollections.USERS_COLLECTION)
                .document(testUserId).get(), 5, TimeUnit.SECONDS);

        List<String> userEvents = (List<String>) userDoc.get("events");
        assertTrue("Event ID should be in user's event list", userEvents != null && userEvents.contains(testEventId));
    }

    /**
     * Verifies that an entrant can successfully leave a specific event's waiting list.
     * <p>
     * This test first sets up a "joined" state, then performs the removal actions and
     * verifies that the entrant document is deleted and the event ID is removed from
     * the user's data.
     * <p>
     * User Story Tested:
     * US 01.01.02 As an entrant, I want to leave the waiting list for a specific event
     *
     * @throws ExecutionException if the Firestore task fails
     * @throws InterruptedException if the wait operation is interrupted
     * @throws TimeoutException if the database operation times out
     */
    @Test
    public void testLeaveWaitingList() throws ExecutionException, InterruptedException, TimeoutException {
        // 1. Setup: First join the list
        Map<String, Object> entrantData = new HashMap<>();
        entrantData.put("status", "waitlist");

        // Ensure User doc exists first
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", "Test Entrant");
        userData.put("events", new ArrayList<String>());
        Tasks.await(db.collection(FirestoreCollections.USERS_COLLECTION).document(testUserId).set(userData));

        // Add to waitlist
        Tasks.await(db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(testEventId)
                .collection("entrants")
                .document(testUserId)
                .set(entrantData));

        // Link to user
        Tasks.await(db.collection(FirestoreCollections.USERS_COLLECTION)
                .document(testUserId)
                .update("events", FieldValue.arrayUnion(testEventId)));

        // 2. Simulate the Action (Leave)
        Tasks.await(db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(testEventId)
                .collection("entrants")
                .document(testUserId)
                .delete(), 5, TimeUnit.SECONDS);

        Tasks.await(db.collection(FirestoreCollections.USERS_COLLECTION)
                .document(testUserId)
                .update("events", FieldValue.arrayRemove(testEventId)), 5, TimeUnit.SECONDS);

        // 3. Verify: Check if user is gone from event subcollection
        DocumentSnapshot entrantDoc = Tasks.await(db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(testEventId)
                .collection("entrants")
                .document(testUserId).get(), 5, TimeUnit.SECONDS);

        assertFalse("User should be removed from event's entrants", entrantDoc.exists());

        // 4. Verify: Check if event is gone from user's list
        DocumentSnapshot userDoc = Tasks.await(db.collection(FirestoreCollections.USERS_COLLECTION)
                .document(testUserId).get(), 5, TimeUnit.SECONDS);

        List<String> userEvents = (List<String>) userDoc.get("events");
        assertFalse("Event ID should be removed from user's event list", userEvents != null && userEvents.contains(testEventId));
    }
}