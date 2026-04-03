package com.icarus.events;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.anything;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.ViewAssertion;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Test class for verifying the functionality of an entrant
 * joining and leaving the waitlist for events.
 * <p>
 * User Stories Tested:
 *     US 01.01.01 – As an entrant, I want to join the waiting list for a specific event.
 *     US 01.01.02 – As an entrant, I want to leave the waiting list for a specific event.
 * <p>
 * Tests use a temporary Firestore collection ({@code events_test}) to avoid
 * interfering with production data.
 *
 * @author Kito Lee Son
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EntrantWaitlistTest {

    private ActivityScenario<EntrantEventListActivity> scenario;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String eventId;
    private String organizerId;
    private User testUser;

    /**
     * Prepares test data in Firestore before each test runs.
     * <p>
     * This method inserts a test organizer, event, and user into the
     * {@code events_test} collection, then launches
     * {@link EntrantEventListActivity}.
     *
     * @throws InterruptedException if the Firestore insertion wait is interrupted
     */
    @Before
    public void setupFirestoreData() throws InterruptedException {

        FirestoreCollections.startTest();

        // insert organizer into the database
        CountDownLatch organizerLatch = new CountDownLatch(1);
        Map<String, Object> organizer = new HashMap<>();
        organizer.put("name", "Test Organizer");
        organizer.put("isAdmin", false);
        db.collection("users_test")
                .add(organizer)
                .addOnSuccessListener((doc) -> {
                    organizerId = doc.getId();
                    organizerLatch.countDown();
                });
        organizerLatch.await(); // wait until organizer is inserted

        // insert events into the database
        CountDownLatch eventLatch = new CountDownLatch(1);
            Map<String, Object> event = new HashMap<>();
            event.put("capacity", 20);
            event.put("name", "Test Event");
            event.put("organizer", organizerId);
            event.put("category", "Music");

            db.collection("events_test")
                    .add(event)
                    .addOnSuccessListener(doc -> {
                        eventId = doc.getId();
                        eventLatch.countDown();
                    });
        eventLatch.await(); // wait until events are inserted

        CountDownLatch userLatch = new CountDownLatch(1);

        Map<String, Object> entrant = new HashMap<>();
        entrant.put("name", "Test Entrant");
        entrant.put("isAdmin", false);

        db.collection("users_test")
                .add(entrant)
                .addOnSuccessListener(doc -> {
                    // Create User object for session
                    testUser = new User(doc.getId(), "Test Entrant",
                            "entrant@email.com", "123012312", "No Image",
                            false, new ArrayList<String>(),
                            new ArrayList<String>(), new HashMap<String, Object>());
                    UserSession.getInstance().setCurrentUser(testUser);
                    userLatch.countDown();
                });

        userLatch.await();

        scenario = ActivityScenario.launch(EntrantEventListActivity.class);
    }

    /**
     * Waits until the event {@link ListView} is populated with a minimum number
     * of items or until a timeout occurs.
     * <p>
     * This is necessary because Firestore snapshot listeners update the UI
     * asynchronously. The method repeatedly checks the ListView adapter until
     * the expected number of events are displayed.
     *
     * @param minCount minimum number of list items expected
     * @param timeoutMs maximum time to wait in milliseconds
     * @throws InterruptedException if the wait loop is interrupted
     * @throws AssertionError if the list is not populated before timeout
     */
    private void waitForListViewItems(int minCount, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            final int[] count = {0};
            onView(withId(R.id.entrant_event_list_view)).check((view, e) -> {
                ListView listView = (ListView) view;
                if(listView.getAdapter() == null) throw new AssertionError("ListView adapter was null");
                count[0] = listView.getAdapter().getCount();
            });
            if (count[0] >= minCount) return;
            Thread.sleep(50);
        }

        throw new AssertionError("ListView never populated with at least " + minCount + " items");
    }

    /**
     * Tests that an entrant can join the waiting list for an event.
     * <p>
     * The test selects an event from the event list, presses the join
     * waitlist button, and verifies that the entrant document is created
     * in the event's {@code entrants} subcollection in Firestore.
     * <p>
     * User Story Tested:
     *     US 01.01.01 – As an entrant, I want to join the waiting list for a specific event.
     *
     * @throws InterruptedException if the Firestore verification wait is interrupted
     */
    @Test
    public void testJoinWaitlist() throws InterruptedException {

        waitForListViewItems(1, 5000);

        // Click the first event in the list
        onData(anything())
                .inAdapterView(withId(R.id.entrant_event_list_view))
                .atPosition(0)
                .perform(click());

        // Click join waitlist
        onView(withId(R.id.join_waiting_list_button))
                .perform(click());

        String userId = UserSession.getInstance().getCurrentUser().getId();

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] stillExists = {true};

        db.collection("events_test")
                .document(eventId)
                .collection("entrants")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    stillExists[0] = doc.exists();
                    latch.countDown();
                });

        latch.await(); // wait for Firestore to finish

        assertTrue("User was not added to the waitlist", stillExists[0]);
    }

    /**
     * Tests that an entrant can leave the waiting list for an event.
     * <p>
     * The test first inserts the entrant into the event's waitlist in
     * Firestore, then simulates the user leaving the waitlist through
     * the UI and verifies that the entrant document is removed.
     * <p>
     * User Story Tested:
     *     US 01.01.02 – As an entrant, I want to leave the waiting list for a specific event.
     *
     * @throws InterruptedException if the Firestore verification wait is interrupted
     */
    @Test
    public void testLeaveWaitlist() throws InterruptedException {

        waitForListViewItems(1, 5000);

        // Click event
        onData(anything())
                .inAdapterView(withId(R.id.entrant_event_list_view))
                .atPosition(0)
                .perform(click());

        // Click join waitlist
        onView(withId(R.id.join_waiting_list_button))
                .perform(click());

        Thread.sleep(500); // small pause to ensure UI updates

        // Click leave waitlist
        onView(withId(R.id.leave_waiting_list_button))
                .perform(click());

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] stillExists = {true};

        db.collection("events_test")
                .document(eventId)
                .collection("entrants")
                .document(testUser.getId())
                .get()
                .addOnSuccessListener(doc -> {
                    stillExists[0] = doc.exists();
                    latch.countDown();
                });

        latch.await(); // wait for Firestore to finish
        assertFalse("User was not removed from the waitlist", stillExists[0]);
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
    public void cleanupFirestoreData() throws InterruptedException {

        if (scenario != null) {
            scenario.close();
        }

        FirestoreCollections.endTest();

    }
}