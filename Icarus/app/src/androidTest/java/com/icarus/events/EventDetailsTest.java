package com.icarus.events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;

import android.content.Intent;
import android.widget.ListView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Instrumented UI tests for {@link EventDetailsActivity}.
 * <p>
 * User Stories Tested:
 *      US 01.05.02 As an entrant I want to accept an invitation to register.
 *      US 01.05.03 As an entrant I want to decline an invitation.
 *      US 01.05.04 As an entrant I want to know how many users are on the waiting list.
 * <p>
 * Tests use temporary Firestore collections to avoid interfering with production data.
 *
 * @author Kito Lee Son
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventDetailsTest {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String organizerId;
    private String entrantId;
    private String event1Id;
    private String event2Id;

    private ActivityScenario<EventDetailsActivity> scenario;

    /**
     * Prepares test data in Firestore before each test runs.
     * <p>
     * Creates a test organizer, test entrant, and two events.
     * The entrant is added to both events with status "selected".
     * One additional user is placed on the waiting list to simulate
     * a waiting list size of 1.
     *
     * @throws InterruptedException if the Firestore insertion wait is interrupted
     */
    @Before
    public void setupTestData() throws InterruptedException {

        FirestoreCollections.EVENTS_COLLECTION = "events_test";
        FirestoreCollections.USERS_COLLECTION = "users_test";

        // create organizer
        CountDownLatch organizerLatch = new CountDownLatch(1);

        db.collection("users_test")
                .add(Map.of(
                        "name", "Test Organizer",
                        "role", "organizer"))
                .addOnSuccessListener(doc -> {
                    organizerId = doc.getId();
                    organizerLatch.countDown();
                });

        organizerLatch.await();

        // create entrant
        CountDownLatch entrantLatch = new CountDownLatch(1);

        db.collection("users_test")
                .add(Map.of(
                        "name", "Test Entrant",
                        "role", "entrant"))
                .addOnSuccessListener(doc -> {
                    entrantId = doc.getId();

                    User testUser = new User(
                            entrantId,
                            "Test Entrant",
                            "test@email.com",
                            "1234567890",
                            "entrant",
                            new java.util.ArrayList<>(),
                            new HashMap<>());

                    UserSession.getInstance().setCurrentUser(testUser);

                    entrantLatch.countDown();
                });

        entrantLatch.await();

        // create events
        CountDownLatch eventLatch = new CountDownLatch(2);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("name", "Test Event 1");
        eventData.put("capacity", 20);
        eventData.put("organizer", organizerId);
        eventData.put("category", "Music");

        db.collection("events_test")
                .add(eventData)
                .addOnSuccessListener(doc -> {
                    event1Id = doc.getId();
                    eventLatch.countDown();
                });

        eventData.put("name", "Test Event 2");

        db.collection("events_test")
                .add(eventData)
                .addOnSuccessListener(doc -> {
                    event2Id = doc.getId();
                    eventLatch.countDown();
                });

        eventLatch.await();


        // add entrants
        CountDownLatch entrantSetupLatch = new CountDownLatch(4);

        Map<String, Object> selected = Map.of("status", "selected");
        Map<String, Object> waiting = Map.of("status", "waiting");

        // Selected entrant
        db.collection("events_test").document(event1Id)
                .collection("entrants").document(entrantId)
                .set(selected)
                .addOnSuccessListener(v -> entrantSetupLatch.countDown());

        db.collection("events_test").document(event2Id)
                .collection("entrants").document(entrantId)
                .set(selected)
                .addOnSuccessListener(v -> entrantSetupLatch.countDown());

        // Waiting list users
        db.collection("events_test").document(event1Id)
                .collection("entrants").document("waitingUser1")
                .set(waiting)
                .addOnSuccessListener(v -> entrantSetupLatch.countDown());

        db.collection("events_test").document(event2Id)
                .collection("entrants").document("waitingUser2")
                .set(waiting)
                .addOnSuccessListener(v -> entrantSetupLatch.countDown());

        entrantSetupLatch.await();
    }

    /**
     * Tests that an entrant can see the number of people on the waiting list for an event.
     * <p>
     * The test enters an event details page, and verifies that the correct waitlist
     * number is displayed.
     * <p>
     * User Story Tested:
     *     US 01.05.04 As an entrant I want to know how many users are on the waiting list.
     */
    @Test
    public void testWaitingListDisplayed() {

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EventDetailsActivity.class);
        intent.putExtra("eventId", event1Id);

        scenario = ActivityScenario.launch(intent);

        scenario.onActivity(activity -> {
            ListView listView = activity.findViewById(R.id.event_details_event_list);

            EventField waitingField =
                    (EventField) listView.getAdapter().getItem(10);

            assertEquals("1", waitingField.getValue());
        });
    }

    /**
     * Tests that an entrant can accept the invitation to an event.
     * <p>
     * The test enters an event details page, clicks the register button, and
     * verifies that the status is updated to "registered" in the {@code entrants}
     * subdirectory in the Firestore events database.
     * <p>
     * User Story Tested:
     *     US 01.05.02 As an entrant I want to accept an invitation to register.
     */
    @Test
    public void testAcceptInvitation() throws InterruptedException {

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EventDetailsActivity.class);
        intent.putExtra("eventId", event1Id);

        scenario = ActivityScenario.launch(intent);

        onView(withId(R.id.register_button)).perform(click());

        CountDownLatch latch = new CountDownLatch(1);
        final String[] status = {""};

        db.collection("events_test")
                .document(event1Id)
                .collection("entrants")
                .document(entrantId)
                .get()
                .addOnSuccessListener(doc -> {
                    status[0] = doc.getString("status");
                    latch.countDown();
                });

        latch.await();

        assertEquals("registered", status[0]);
    }

    /**
     * Tests that an entrant can reject the invitation to an event.
     * <p>
     * The test enters an event details page, clicks the decline button, and
     * verifies that the status is updated to "rejected" in the {@code entrants}
     * subdirectory in the Firestore events database.
     * <p>
     * User Story Tested:
     *     US 01.05.03 As an entrant I want to decline an invitation.
     */
    @Test
    public void testDeclineInvitation() throws InterruptedException {

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EventDetailsActivity.class);
        intent.putExtra("eventId", event2Id);

        scenario = ActivityScenario.launch(intent);

        onView(withId(R.id.decline_button)).perform(click());

        CountDownLatch latch = new CountDownLatch(1);
        final String[] status = {""};

        db.collection("events_test")
                .document(event2Id)
                .collection("entrants")
                .document(entrantId)
                .get()
                .addOnSuccessListener(doc -> {
                    status[0] = doc.getString("status");
                    latch.countDown();
                });

        latch.await();

        assertEquals("rejected", status[0]);
    }

    /**
     * Removes all test data from Firestore after each test.
     * <p>
     * This method deletes documents from {@code events_test} and
     * {@code users_test} collections to ensure tests do not affect each other
     * or pollute the production database.
     *
     * @throws InterruptedException if the cleanup wait operation is interrupted
     */
    @After
    public void cleanup() throws InterruptedException {

        FirestoreCollections.EVENTS_COLLECTION = "events";
        FirestoreCollections.USERS_COLLECTION = "users";

        if (scenario != null) scenario.close();

        CountDownLatch latch = new CountDownLatch(4);

        db.collection("events_test").document(event1Id).delete()
                .addOnSuccessListener(v -> latch.countDown());

        db.collection("events_test").document(event2Id).delete()
                .addOnSuccessListener(v -> latch.countDown());

        db.collection("users_test").document(entrantId).delete()
                .addOnSuccessListener(v -> latch.countDown());

        db.collection("users_test").document(organizerId).delete()
                .addOnSuccessListener(v -> latch.countDown());

        latch.await();
    }
}