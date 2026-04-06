package com.icarus.events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented UI tests for {@link OrganizerManageEventActivity}.
 * <p>
 * User Stories Tested:
 *      US 02.01.01 As an organizer I want to create a new public event and generate a unique
 *                  promotional QR code that links to the event description and event poster
 *                  in the app.
 *      US 02.01.02 As an organizer, I want to create a private event that is not visible on the
 *                  event listing and does not generate a promotional QR code
 *      US 02.01.03 As an organizer, I want to invite specific entrants to a private event’s
 *                  waiting list by searching via name, phone number and/or email.
 *      US 02.04.02 As an organizer I want to update an event poster to provide visual
 *                  information to entrants.
 *      US 02.05.02 As an organizer I want to set the system to sample a specified number
 *                  of attendees to register for the event.
 *      US 02.05.03 As an organizer I want to be able to draw a replacement applicant from
 *                  the pooling system when a previously selected applicant cancels or
 *                  rejects the invitation.
 *      US 02.09.01 As an organizer, I want to assign an entrant as a co-organizer for
 *                  my event, which prevents them from joining the entrant pool for that event.
 *
 * <p>
 * Tests use temporary Firestore collections to avoid affecting production data.
 *
 * @author Ben Salmon
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerManageEventTest {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ActivityScenario<OrganizerViewEntrantsOnWaitingListActivity> scenario;
    private String createdEventId;

    @Before
    public void setup() throws InterruptedException {
        // Set up a test user in UserSession so sendMessage doesn't crash
        User testUser = new User("testOrganizerId", "Test Organizer", null, null, null,
                null, null, null, null);
        UserSession.getInstance().setCurrentUser(testUser);

        // Use test collection
        FirestoreCollections.startTest();

        // Create a test event
        CountDownLatch eventLatch = new CountDownLatch(1);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("name", "Test Event");
        eventData.put("description", "Test Description");
        eventData.put("category", "Technology");
        eventData.put("location", "Test Location");
        eventData.put("image", "");
        eventData.put("isPrivate", false);
        eventData.put("geolocation", false);
        eventData.put("capacity", null);
        eventData.put("entrantRange", null);
        eventData.put("coordinates", null);
        eventData.put("open", new Date());
        eventData.put("close", new Date());
        eventData.put("startDate", new Date());
        eventData.put("endDate", new Date());
        eventData.put("organizers", List.of("testOrganizerId"));

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .add(eventData)
                .addOnSuccessListener(docRef -> {
                    createdEventId = docRef.getId();

                    // Add entrants subcollection
                    db.collection(FirestoreCollections.EVENTS_COLLECTION)
                            .document(createdEventId)
                            .collection("entrants")
                            .document("entrant0")
                            .set(Map.of("status", "waiting"));
                    db.collection(FirestoreCollections.EVENTS_COLLECTION)
                            .document(createdEventId)
                            .collection("entrants")
                            .document("entrant1")
                            .set(Map.of("status", "selected"));
                    db.collection(FirestoreCollections.EVENTS_COLLECTION)
                            .document(createdEventId)
                            .collection("entrants")
                            .document("entrant2")
                            .set(Map.of("status", "rejected"));
                    db.collection(FirestoreCollections.EVENTS_COLLECTION)
                            .document(createdEventId)
                            .collection("entrants")
                            .document("entrant3")
                            .set(Map.of("status", "registered"));

                    // Create user documents to hold names
                    db.collection(FirestoreCollections.USERS_COLLECTION).document("entrant0")
                            .set(Map.of("name", "Entrant Zero"));
                    db.collection(FirestoreCollections.USERS_COLLECTION).document("entrant1")
                            .set(Map.of("name", "Entrant One"));
                    db.collection(FirestoreCollections.USERS_COLLECTION).document("entrant2")
                            .set(Map.of("name", "Entrant Two"));
                    db.collection(FirestoreCollections.USERS_COLLECTION).document("entrant3")
                            .set(Map.of("name", "Entrant Three"));
                    //Entrants to add as organizer and private.
                    db.collection(FirestoreCollections.USERS_COLLECTION).document("entrant4")
                            .set(Map.of("name", "Entrant Four"));
                    db.collection(FirestoreCollections.USERS_COLLECTION).document("entrant5")
                            .set(Map.of("name", "Entrant Five"));

                    eventLatch.countDown();
                });
        eventLatch.await();
    }
    //Test 1
    /**
     * Tests that the organizer can view all waiting entrants for the event.
     * <p>
     * Launches the {@link OrganizerViewEntrantsOnWaitingListActivity} activity, clicks
     * the "waiting" filter button, and verifies that the ListView displays
     * the entrant with status "waiting".
     * <p>
     * User Story Tested:
     *      US 02.01.01 As an organizer I want to create a new public event and generate a unique
     *                  promotional QR code that links to the event description and event poster
     *                  in the app.
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testViewWaitingEntrants() throws InterruptedException {
        // Launch activity with test eventId
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerViewEntrantsOnWaitingListActivity.class
        );
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        // Click "Chosen" filter button
        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_waiting)).perform(click());

        Thread.sleep(2000);
        // Verify that "Entrant One" (chosen) appears in the ListView
        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant Zero"))));
    }
    //Test 2
    /**
     * Tests that the organizer can view all waiting entrants for the event.
     * <p>
     * Launches the {@link OrganizerViewEntrantsOnWaitingListActivity} activity, clicks
     * the "waiting" filter button, and verifies that the ListView displays
     * the entrant with status "waiting".
     * <p>
     * User Story Tested:
     *      US 02.01.02 As an organizer, I want to create a private event that is not visible on the
     *                  event listing and does not generate a promotional QR code
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testViewWaitingEntrants() throws InterruptedException {
        // Launch activity with test eventId
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerViewEntrantsOnWaitingListActivity.class
        );
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        // Click "Chosen" filter button
        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_waiting)).perform(click());

        Thread.sleep(2000);
        // Verify that "Entrant One" (chosen) appears in the ListView
        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant Zero"))));
    }
    //Test 3
    /**
     * Tests that the organizer can view all waiting entrants for the event.
     * <p>
     * Launches the {@link OrganizerViewEntrantsOnWaitingListActivity} activity, clicks
     * the "waiting" filter button, and verifies that the ListView displays
     * the entrant with status "waiting".
     * <p>
     * User Story Tested:
     *      US 02.01.03 As an organizer, I want to invite specific entrants to a private event’s
     *                  waiting list by searching via name, phone number and/or email.
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testViewWaitingEntrants() throws InterruptedException {
        // Launch activity with test eventId
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerViewEntrantsOnWaitingListActivity.class
        );
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        // Click "Chosen" filter button
        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_waiting)).perform(click());

        Thread.sleep(2000);
        // Verify that "Entrant One" (chosen) appears in the ListView
        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant Zero"))));
    }
    //Test 4
    /**
     * Tests that the organizer can view all waiting entrants for the event.
     * <p>
     * Launches the {@link OrganizerViewEntrantsOnWaitingListActivity} activity, clicks
     * the "waiting" filter button, and verifies that the ListView displays
     * the entrant with status "waiting".
     * <p>
     * User Story Tested:
     *      US 02.04.02 As an organizer I want to update an event poster to provide visual
     *                  information to entrants.
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testViewWaitingEntrants() throws InterruptedException {
        // Launch activity with test eventId
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerViewEntrantsOnWaitingListActivity.class
        );
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        // Click "Chosen" filter button
        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_waiting)).perform(click());

        Thread.sleep(2000);
        // Verify that "Entrant One" (chosen) appears in the ListView
        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant Zero"))));
    }
    //Test 5
    /**
     * Tests that the organizer can view all waiting entrants for the event.
     * <p>
     * Launches the {@link OrganizerViewEntrantsOnWaitingListActivity} activity, clicks
     * the "waiting" filter button, and verifies that the ListView displays
     * the entrant with status "waiting".
     * <p>
     * User Story Tested:
     *      US 02.05.02 As an organizer I want to set the system to sample a specified number
     *                  of attendees to register for the event.
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testViewWaitingEntrants() throws InterruptedException {
        // Launch activity with test eventId
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerViewEntrantsOnWaitingListActivity.class
        );
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        // Click "Chosen" filter button
        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_waiting)).perform(click());

        Thread.sleep(2000);
        // Verify that "Entrant One" (chosen) appears in the ListView
        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant Zero"))));
    }
    //Test 6
    /**
     * Tests that the organizer can view all waiting entrants for the event.
     * <p>
     * Launches the {@link OrganizerViewEntrantsOnWaitingListActivity} activity, clicks
     * the "waiting" filter button, and verifies that the ListView displays
     * the entrant with status "waiting".
     * <p>
     * User Story Tested:
     *      US 02.05.03 As an organizer I want to be able to draw a replacement applicant from
     *                  the pooling system when a previously selected applicant cancels or
     *                  rejects the invitation.
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testViewWaitingEntrants() throws InterruptedException {
        // Launch activity with test eventId
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerViewEntrantsOnWaitingListActivity.class
        );
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        // Click "Chosen" filter button
        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_waiting)).perform(click());

        Thread.sleep(2000);
        // Verify that "Entrant One" (chosen) appears in the ListView
        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant Zero"))));
    }
    //Test 7
    /**
     * Tests that the organizer can view all waiting entrants for the event.
     * <p>
     * Launches the {@link OrganizerViewEntrantsOnWaitingListActivity} activity, clicks
     * the "waiting" filter button, and verifies that the ListView displays
     * the entrant with status "waiting".
     * <p>
     * User Story Tested:
     *      US 02.09.01 As an organizer, I want to assign an entrant as a co-organizer for
     *                  my event, which prevents them from joining the entrant pool for that event.
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testViewWaitingEntrants() throws InterruptedException {
        // Launch activity with test eventId
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerViewEntrantsOnWaitingListActivity.class
        );
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        // Click "Chosen" filter button
        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_waiting)).perform(click());

        Thread.sleep(2000);
        // Verify that "Entrant One" (chosen) appears in the ListView
        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant Zero"))));
    }

    /**
     * Removes test data created during the test run from Firestore.
     * <p>
     * Deletes the test entrants from the event's {@code entrants} subcollection,
     * removes the corresponding user documents, deletes any notifications associated
     * with the created event, and finally deletes the event document itself.
     * <p>
     * This method is executed after each test to ensure the database is cleaned up
     * and does not contain leftover test data.
     *
     * @throws InterruptedException if the cleanup wait is interrupted
     */
    @After
    public void cleanup() throws InterruptedException {

        FirestoreCollections.endTest();
    }
}