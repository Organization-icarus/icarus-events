package com.icarus.events;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.action.ViewActions.click;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Instrumented UI tests for {@link OrganizerViewEntrantsOnWaitingListActivity}.
 * <p>
 * User Stories Tested:
 *      US 02.02.01 As an organizer I want to view the list of entrants who joined my event
 *                  waiting list
 *      US 02.06.01 As an organizer I want to view a list of all chosen entrants.
 *      US 02.06.02 As an organizer I want to see a list of all cancelled entrants.
 *      US 02.06.03 As an organizer I want to see a final list of enrolled entrants.
 *      US 02.06.04 As an organizer I want to cancel entrants that did not sign up for the event
 *      US 02.06.05 As an organizer I want to export a final list of entrants who enrolled for
 *                  the event in CSV format.
 *      US 02.07.01 As an organizer I want to send notifications to all entrants on
 *                  the waiting list
 *      US 02.07.02 As an organizer I want to send notifications to all selected entrants
 *      US 02.07.03 As an organizer I want to send a notification to all cancelled entrants
 * <p>
 * Tests use temporary Firestore collections to avoid affecting production data.
 *
 * @author Ben Salmon
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerViewEntrantsTest {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ActivityScenario<OrganizerViewEntrantsOnWaitingListActivity> scenario;
    private String createdEventId;

    /**
     * Sets up Firestore test data before each test.
     * <p>
     * Overrides Firestore collections to test versions, adds
     * a sample event with ID {@code createdEventId}, adds entrants
     * with different statuses ("selected", "rejected", "registered"),
     * creates user documents to hold entrant names.
     *
     * @throws InterruptedException if Firestore operations are interrupted
     */
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

                    eventLatch.countDown();
                });
        eventLatch.await();
    }

    /**
     * Tests that the organizer can view all waiting entrants for the event.
     * <p>
     * Launches the {@link OrganizerViewEntrantsOnWaitingListActivity} activity, clicks
     * the "waiting" filter button, and verifies that the ListView displays
     * the entrant with status "waiting".
     * <p>
     * User Story Tested:
     *      US 02.02.01 As an organizer I want to view the list of entrants who joined
     *      my event waiting list
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
     * Tests that the organizer can view all chosen entrants for the event.
     * <p>
     * Launches the {@link OrganizerViewEntrantsOnWaitingListActivity} activity, clicks
     * the "Chosen" filter button, and verifies that the ListView displays
     * the entrant with status "selected".
     * <p>
     * User Story Tested:
     *      US 02.06.01 As an organizer I want to view a list of all chosen
     *      entrants who are invited to apply.
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testViewChosenEntrants() throws InterruptedException {
        // Launch activity with test eventId
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerViewEntrantsOnWaitingListActivity.class
        );
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        // Click "Chosen" filter button
        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_chosen)).perform(click());

        Thread.sleep(2000);
        // Verify that "Entrant One" (chosen) appears in the ListView
        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant One"))));
    }

    /**
     * Tests that the organizer can view all cancelled entrants for the event.
     * <p>
     * Launches the {@link OrganizerViewEntrantsOnWaitingListActivity} activity, clicks
     * the "Cancelled" filter button, and verifies that the ListView displays
     * the entrant with status "rejected".
     * <p>
     * User Story Tested:
     *      US 02.06.02 As an organizer I want to see a list of all the cancelled entrants.
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testViewCancelledEntrants() throws InterruptedException {
        // Launch activity with test eventId
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerViewEntrantsOnWaitingListActivity.class
        );
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        // Click "Chosen" filter button
        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_cancelled)).perform(click());

        Thread.sleep(2000);
        // Verify that "Entrant One" (chosen) appears in the ListView
        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant Two"))));
    }

    /**
     * Tests that the organizer can view all confirmed entrants for the event.
     * <p>
     * Launches the {@link OrganizerViewEntrantsOnWaitingListActivity} activity, clicks
     * the "Final" filter button, and verifies that the ListView displays
     * the entrant with status "registered".
     * <p>
     * User Story Tested:
     *      US 02.06.03 As an organizer I want to see a final list of entrants
     *      who enrolled for the event.
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testViewRegisteredEntrants() throws InterruptedException {
        // Launch activity with test eventId
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerViewEntrantsOnWaitingListActivity.class
        );
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        // Click "Chosen" filter button
        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_final)).perform(click());

        Thread.sleep(2000);
        // Verify that "Entrant One" (chosen) appears in the ListView
        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant Three"))));
    }

    /**
     * Tests that the organizer can cancel a waiting entrant for the event.
     * <p>
     * Launches the {@link OrganizerViewEntrantsOnWaitingListActivity} activity, clicks
     * the "waiting" filter button, and verifies that the ListView displays
     * the entrant with status "waiting". selects the entrant. Pushes the cancel button. Conforms
     * the cancel. Verifies the listview show the entrant in the cancelled list with status "rejected"
     * <p>
     * User Story Tested:
     *      US 02.06.04 As an organizer I want to cancel entrants that did
     *      not sign up for the event.
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testCancelWaitingEntrants() throws InterruptedException {
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

        //Select the entrant
        onView(withId(R.id.OrganizerEntrantOnWaitingListSelectAllButton)).perform(click());
        onView(withId(R.id.OrganizerEntrantOnWaitingListBackButton)).perform(click());

        onView(withText("REMOVE")).perform(click());
        Thread.sleep(2000);

        // Click "Cancelled" filter button
        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_cancelled)).perform(click());

        Thread.sleep(2000);
        // Verify that "Entrant One" (chosen) appears in the ListView
        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant Zero"))));
    }

    /**
     * Tests that the organizer can cancel a selected entrant for the event.
     * <p>
     * Launches the {@link OrganizerViewEntrantsOnWaitingListActivity} activity, clicks
     * the "selected" filter button, and verifies that the ListView displays
     * the entrant with status "selected". selects the entrant. Pushes the cancel button. Confirms
     * the cancel. Verifies the listview show the entrant in the cancelled list with status "rejected"
     * <p>
     * User Story Tested:
     *      US 02.06.04 As an organizer I want to cancel entrants that did
     *      not sign up for the event
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testCancelSelectedEntrants() throws InterruptedException {
        // Launch activity with test eventId
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerViewEntrantsOnWaitingListActivity.class
        );
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        // Click "Chosen" filter button
        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_chosen)).perform(click());

        Thread.sleep(2000);
        // Verify that "Entrant One" (chosen) appears in the ListView
        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant One"))));

        onView(withId(R.id.OrganizerEntrantOnWaitingListSelectAllButton)).perform(click());
        onView(withId(R.id.OrganizerEntrantOnWaitingListBackButton)).perform(click());

        onView(withText("REMOVE")).perform(click());
        Thread.sleep(2000);

        // Click "Cancelled" filter button
        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_cancelled)).perform(click());

        Thread.sleep(2000);
        // Verify that "Entrant One" (chosen) appears in the ListView
        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant One"))));

    }

    /**
     * Tests that the organizer can export a CSV file of final entrants
     * <p>
     * Launches the {@link OrganizerViewEntrantsOnWaitingListActivity} activity, clicks
     * the "Final" filter button, and verifies that the ListView displays
     * the entrant with status "registered". Then clicks Export CSV file to export a csv file to
     * the devices downloads.
     * <p>
     * User Story Tested:
     *      US 02.06.05 As an organizer I want to export a final list of entrants who
     *      enrolled for the event in CSV format.
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testExportCsvEntrants() throws InterruptedException {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerViewEntrantsOnWaitingListActivity.class
        );
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_final)).perform(click());
        Thread.sleep(2000);

        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant Three"))));

        onView(withId(R.id.OrganizerEntrantOnWaitingListBackButton)).perform(click());
        Thread.sleep(1000);

        // Verify the CSV file was written to the app's cache directory
        scenario.onActivity(activity -> {
            java.io.File cacheDir = activity.getCacheDir();
            java.io.File[] csvFiles = cacheDir.listFiles(
                    f -> f.getName().startsWith("entrant_final_list_for_") && f.getName().endsWith(".csv")
            );
            assertNotNull("Cache directory should not be null", csvFiles);
            assertTrue("CSV file should exist in cache", csvFiles.length > 0);
            assertTrue("CSV file should not be empty", csvFiles[0].length() > 0);
        });
    }
    /**
     * Tests that the organizer can send a notification to entrants in the waiting list.
     * <p>
     * Launches the {@link OrganizerViewEntrantsOnWaitingListActivity} activity, clicks
     * the "waiting" filter button, and verifies that the ListView displays
     * the entrant with status "waiting". Select all users from the waiting list. clicks the send
     * message button. types message and sends to selcted users
     * <p>
     * User Story Tested:
     *      US 02.07.01 As an organizer I want to send notifications to all entrants on
     *      the waiting list
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testSendMessageToWaitingEntrants() throws InterruptedException {
        // Launch activity with test eventId
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                OrganizerViewEntrantsOnWaitingListActivity.class);
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_waiting)).perform(click());
        Thread.sleep(2000);
        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant Zero"))));

        onView(withId(R.id.OrganizerEntrantOnWaitingListSelectAllButton)).perform(click());
        onView(withId(R.id.OrganizerEntrantOnWaitingListSendNotificationButton)).perform(click());
        onView(withHint("The Message you wish to send"))
                .perform(typeText("Test notification message"), closeSoftKeyboard());
        onView(withText("Send Message")).perform(click());
        Thread.sleep(2000);

        // Verify the notification was saved to Firestore
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> notificationFound = new AtomicReference<>(false);

        FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("eventID", createdEventId)
                .whereEqualTo("message", "Test notification message")
                .get()
                .addOnSuccessListener(query -> {
                    notificationFound.set(!query.isEmpty());
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        latch.await(5, TimeUnit.SECONDS);
        assertTrue("Notification should exist in Firestore", notificationFound.get());
    }
    /**
     * Tests that the organizer can send a notification to entrants in the selected list.
     * <p>
     * Launches the {@link OrganizerViewEntrantsOnWaitingListActivity} activity, clicks
     * the "selected" filter button, and verifies that the ListView displays
     * the entrant with status "selected". Select all users from the selected list. clicks the send
     * message button. types message and sends to selected users
     * <p>
     * User Story Tested:
     *      US 02.07.02 As an organizer I want to send notifications to all selected entrants
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testSendMessageToSelectedEntrants() throws InterruptedException {
        // Launch activity with test eventId
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                OrganizerViewEntrantsOnWaitingListActivity.class);
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_chosen)).perform(click());
        Thread.sleep(2000);
        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant One"))));

        onView(withId(R.id.OrganizerEntrantOnWaitingListSelectAllButton)).perform(click());
        onView(withId(R.id.OrganizerEntrantOnWaitingListSendNotificationButton)).perform(click());
        onView(withHint("The Message you wish to send"))
                .perform(typeText("Test notification message"), closeSoftKeyboard());
        onView(withText("Send Message")).perform(click());
        Thread.sleep(2000);

        // Verify the notification was saved to Firestore
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> notificationFound = new AtomicReference<>(false);

        FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("eventID", createdEventId)
                .whereEqualTo("message", "Test notification message")
                .get()
                .addOnSuccessListener(query -> {
                    notificationFound.set(!query.isEmpty());
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        latch.await(5, TimeUnit.SECONDS);
        assertTrue("Notification should exist in Firestore", notificationFound.get());
    }
    /**
     * Tests that the organizer can send a notification to entrants in the cancelled list.
     * <p>
     * Launches the {@link OrganizerViewEntrantsOnWaitingListActivity} activity, clicks
     * the "cancelled" filter button, and verifies that the ListView displays
     * the entrant with status "cancelled". Select all users from the cancelled list. clicks the send
     * message button. types message and sends to selected users
     * <p>
     * User Story Tested:
     *      US 02.07.03 As an organizer I want to send a notification to
     *      all cancelled entrants
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testSendMessageToCancelledEntrants() throws InterruptedException {
        // Launch activity with test eventId
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                OrganizerViewEntrantsOnWaitingListActivity.class);
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_cancelled)).perform(click());
        Thread.sleep(2000);
        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant Two"))));

        onView(withId(R.id.OrganizerEntrantOnWaitingListSelectAllButton)).perform(click());
        onView(withId(R.id.OrganizerEntrantOnWaitingListSendNotificationButton)).perform(click());
        onView(withHint("The Message you wish to send"))
                .perform(typeText("Test notification message"), closeSoftKeyboard());
        onView(withText("Send Message")).perform(click());
        Thread.sleep(2000);

        // Verify the notification was saved to Firestore
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> notificationFound = new AtomicReference<>(false);

        FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("eventID", createdEventId)
                .whereEqualTo("message", "Test notification message")
                .get()
                .addOnSuccessListener(query -> {
                    notificationFound.set(!query.isEmpty());
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        latch.await(5, TimeUnit.SECONDS);
        assertTrue("Notification should exist in Firestore", notificationFound.get());
    }

    @After
    public void cleanup() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        if (createdEventId != null) {
            String[] entrantIds = {"entrant0", "entrant1", "entrant2", "entrant3"};
            for (String id : entrantIds) {
                db.collection(FirestoreCollections.EVENTS_COLLECTION)
                        .document(createdEventId)
                        .collection("entrants")
                        .document(id)
                        .delete();
                db.collection(FirestoreCollections.USERS_COLLECTION)
                        .document(id)
                        .delete();
            }

            // Delete any notifications created for this event
            db.collection(FirestoreCollections.NOTIFICATIONS_COLLECTION)
                    .whereEqualTo("eventID", createdEventId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot) {
                            doc.getReference().delete();
                        }
                    });

            db.collection(FirestoreCollections.EVENTS_COLLECTION)
                    .document(createdEventId)
                    .delete()
                    .addOnSuccessListener(unused -> latch.countDown())
                    .addOnFailureListener(e -> latch.countDown());
        } else {
            latch.countDown();
        }

        latch.await(5, TimeUnit.SECONDS);
        FirestoreCollections.endTest();
    }
}