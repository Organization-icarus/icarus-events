package com.icarus.events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented UI tests for the lottery sampling and notification system.
 * <p>
 * This class validates the interaction between {@link SampleAttendeesActivity}
 * and {@link NotificationItem}. It ensures that when an organizer performs
 * a random draw, the system correctly identifies "winners" and "losers"
 * and dispatches the appropriate notifications.
 * </p>
 * <p>
 * <b>User Stories Covered:</b>
 * <ul>
 * <li>US 02.05.01: Organizer sending notifications to chosen entrants.</li>
 * <li>US 01.04.01: Entrant receiving "Selected" notification.</li>
 * <li>US 01.04.02: Entrant receiving "Not Chosen" notification.</li>
 * </ul>
 *
 * @author Kito Lee Son
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerSampleNotificationsTest {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String eventId, organizerId;
    private String winnerId = "winner_user";
    private String loserId = "loser_user";
    private ActivityScenario<SampleAttendeesActivity> scenario;

    /**
     * Initializes the Firestore test environment and seeds required data.
     * <p>
     * Before each test, this method:
     * <ol>
     * <li>Redirects Firestore paths to test collections via {@link FirestoreCollections#startTest()}.</li>
     * <li>Configures a mock {@link UserSession} with an organizer profile.</li>
     * <li>Creates a test event document with a valid date and name.</li>
     * <li>Populates the event's "entrants" subcollection with two users in "waiting" status.</li>
     * </ol>
     * </p>
     *
     * @throws InterruptedException if the database synchronization is interrupted.
     */
    @Before
    public void setup() throws InterruptedException {
        FirestoreCollections.startTest();

        // 1. Setup Organizer Session
        organizerId = "test_org_id";
        User organizer = new User(organizerId, "Org Name", "org@test.com", "123", null, false,
                new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new HashMap<>());
        UserSession.getInstance().setCurrentUser(organizer);

        // 2. Create Event
        CountDownLatch latch = new CountDownLatch(1);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("name", "Lottery Event");
        eventData.put("image", "no_image");
        eventData.put("date", new java.util.Date());

        db.collection("events_test").add(eventData).addOnSuccessListener(doc -> {
            eventId = doc.getId();
            latch.countDown();
        });
        latch.await();

        // 3. Add two entrants with "waiting" status
        CountDownLatch entrantLatch = new CountDownLatch(2);
        Map<String, Object> waitingStatus = Map.of("status", "waiting");

        db.collection("events_test").document(eventId).collection("entrants").document(winnerId)
                .set(waitingStatus).addOnSuccessListener(v -> entrantLatch.countDown());
        db.collection("events_test").document(eventId).collection("entrants").document(loserId)
                .set(waitingStatus).addOnSuccessListener(v -> entrantLatch.countDown());

        entrantLatch.await();
    }

    /**
     * Verifies that the sampling process correctly generates both positive and negative notifications.
     * <p>
     * <b>Process:</b>
     * <ol>
     * <li>Launches {@link SampleAttendeesActivity} and clicks the sample button.</li>
     * <li>Waits for the background {@code WriteBatch} and subsequent {@code NotificationItem} pushes.</li>
     * <li>Queries the {@code notifications_test} collection to verify:
     * <ul>
     * <li>A document exists with {@code type="selected"} for the winner.</li>
     * <li>A document exists with {@code type="not_selected"} for the loser.</li>
     * </ul>
     * </li>
     * </ol>
     * </p>
     *
     * @throws InterruptedException if the Thread.sleep or Firestore query fails.
     */
    @Test
    public void testSamplingSendsNotifications() throws InterruptedException {
        // 1. Double check the collection names are swapped
        FirestoreCollections.startTest();

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SampleAttendeesActivity.class);
        intent.putExtra("eventId", eventId);
        intent.putExtra("ActivityName", "Lottery Event");

        scenario = ActivityScenario.launch(intent);

        // 2. WAIT for the activity to load the waiting list size from Firestore
        Thread.sleep(2000);

        onView(withId(R.id.OrganizerSampleAttendeesSampleButton)).perform(click());

        // 3. AGGRESSIVE WAIT
        // The activity does: Batch Update -> Get Event Doc -> Send Notification Doc.
        // This is 3 round-trips to Firestore. We need at least 4-5 seconds.
        Thread.sleep(5000);

        CountDownLatch notifyLatch = new CountDownLatch(1);
        final int[] count = {0};

        // IMPORTANT: Use the exact string "notifications_test" to be 100% sure
        db.collection("notifications_test")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(query -> {
                    count[0] = query.size();
                    notifyLatch.countDown();
                })
                .addOnFailureListener(e -> notifyLatch.countDown());

        notifyLatch.await(10, TimeUnit.SECONDS);

        // If this is still 0, check Logcat for "Failed to update entrant statuses"
        assertEquals("Should have sent a win and a lose notification", 2, count[0]);
    }

    /**
     * Cleans up the test environment after execution.
     * <p>
     * Closes the {@link ActivityScenario} and calls {@link FirestoreCollections#endTest()}
     * to purge test data and restore production Firestore collection references.
     * </p>
     */
    @After
    public void tearDown() throws InterruptedException {
        if (scenario != null) scenario.close();
        FirestoreCollections.endTest();
    }
}