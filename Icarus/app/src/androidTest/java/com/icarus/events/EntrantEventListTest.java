package com.icarus.events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.widget.ListView;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Instrumented UI tests for {@link EntrantEventListActivity}.
 * <p>
 * User Stories Tested:
 *      US 01.01.03 – As an entrant, I want to be able to see a list
 *      of events that I can join the waiting list for.
 *      US 01.01.04 – As an entrant, I want to filter events based on
 *      my interests and availability.
 * <p>
 * Tests use a temporary Firestore collection ({@code events_test}) to avoid
 * interfering with production data.
 *
 * @author Kito Lee Son
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EntrantEventListTest {

    private ActivityScenario<EntrantEventListActivity> scenario;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<String> insertedIds = new ArrayList<>();
    private String organizerId;
    private List<String> categories = new ArrayList<String>();

    /**
     * Prepares test data in Firestore before each test runs.
     * <p>
     * This method inserts a test organizer and several events into the
     * {@code events_test} collection, then launches
     * {@link EntrantEventListActivity}.
     *
     * @throws InterruptedException if the Firestore insertion wait is interrupted
     */
    @Before
    public void setupFirestoreData() throws InterruptedException {

        FirestoreCollections.EVENTS_COLLECTION = "events_test";
        FirestoreCollections.USERS_COLLECTION = "users_test";

        // insert organizer into the database
        CountDownLatch organizerLatch = new CountDownLatch(1);
        Map<String, String> organizer = new HashMap<String, String>();
        organizer.put("name", "Test Organizer");
        organizer.put("role", "organizer");
        db.collection("users_test")
                .add(organizer)
                .addOnSuccessListener((doc) -> {
                    organizerId = doc.getId();
                    organizerLatch.countDown();
                });
        organizerLatch.await(); // wait until organizer is inserted

        // insert events into the database
        int numEvents = 3;
        categories.add("Sports");
        categories.add("Art");
        categories.add("Music");
        CountDownLatch eventLatch = new CountDownLatch(numEvents);
        for (int i = 1; i <= numEvents; i++) {

            Map<String, Object> event = new HashMap<>();
            event.put("capacity", 20 + (20 * i));
            event.put("name", "Test Event " + i);
            event.put("organizer", organizerId);
            event.put("category", categories.get(i % numEvents));

            db.collection("events_test")
                    .add(event)
                    .addOnSuccessListener(doc -> {
                        insertedIds.add(doc.getId());
                        eventLatch.countDown();
                    });
        }
        eventLatch.await(); // wait until events are inserted

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
     * Returns a {@link ViewAssertion} that verifies the number of items
     * displayed in a {@link ListView}.
     * <p>
     * This helper method simplifies UI assertions by allowing tests to
     * confirm that the ListView contains an expected number of events.
     *
     * @param expectedSize expected number of items in the ListView
     * @return a {@link ViewAssertion} that checks the ListView item count
     */
    public static ViewAssertion withListSize(int expectedSize) {
        return (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }

            ListView listView = (ListView) view;
            assertEquals(expectedSize, listView.getAdapter().getCount());
        };
    }

    /**
     * Verifies that events inserted into Firestore appear in the entrant event list.
     * <p>
     * The test waits for the ListView to populate and then checks that all events
     * in the database are displayed to the user.
     * <p>
     * User Story Tested:
     *     US 01.01.03 – As an entrant, I want to be able to see a list
     *     of events that I can join the waiting list for.
     *
     * @throws InterruptedException if the wait operation is interrupted
     */
    @Test
    public void testDisplayedEventsMatchDatabase() throws InterruptedException {
        waitForListViewItems(3, 5000);
        onView(withId(R.id.entrant_event_list_view)).check(withListSize(3));
    }

    /**
     * Tests that category filtering updates the displayed event list correctly.
     * <p>
     * The test opens the category filter dialog, selects categories, and
     * verifies that only events matching those categories remain visible.
     * <p>
     * User Story Tested:
     *     US 01.01.04 – As an entrant, I want to filter events based
     *     on my interests and availability.
     */
    @Test
    public void testCategoryFiltering() {
        // Initially 3 events
        onView(withId(R.id.entrant_event_list_view))
                .check(withListSize(3));

        // Open filter popup
        onView(withId(R.id.entrant_event_list_filter_button))
                .perform(click());

        // Select "Sports"
        onView(withText("Sports"))
                .perform(click());

        // Click Apply again
        onView(withText("Apply"))
                .perform(click());

        // Verify filtered list
        onView(withId(R.id.entrant_event_list_view))
                .check(withListSize(1));

        // Open filter again
        onView(withId(R.id.entrant_event_list_filter_button))
                .perform(click());

        // Add "Art"
        onView(withText("Art"))
                .perform(click());

        // Click Apply again
        onView(withText("Apply"))
                .perform(click());

        // Now should show 2 events
        onView(withId(R.id.entrant_event_list_view))
                .check(withListSize(2));
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

        FirestoreCollections.EVENTS_COLLECTION = "events";
        FirestoreCollections.USERS_COLLECTION = "users";

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
        latch.await();
    }
}