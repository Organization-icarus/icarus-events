package com.icarus.events;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.CoreMatchers.anything;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.widget.ListView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Instrumented UI tests for {@link EventHistoryActivity}.
 * <p>
 * User Stories Tested:
 *      US 01.02.03 As an entrant, I want to have a history of events I
 *      have registered for, whether I was selected or not.
 * <p>
 * Tests use a temporary Firestore collection ({@code events_test}) to avoid
 * interfering with production data.
 *
 * @author Kito Lee Son
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventHistoryTest {

    private ActivityScenario<EventHistoryActivity> scenario;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String organizerId;
    private String eventId;
    private User testUser;

    /**
     * Sets up Firestore test data.
     * <p>
     * Creates a test organizer, test event, and test entrant,
     * and adds the user to the event's entrants list.
     *
     * @throws InterruptedException if the Firestore operations are interrupted
     */
    @Before
    public void setupTestData() throws InterruptedException {
        FirestoreCollections.startTest();

        CountDownLatch organizerLatch = new CountDownLatch(1);
        Map<String, Object> organizer = Map.of(
                "name", "Test Organizer",
                "isAdmin", false
        );

        db.collection("users_test")
                .add(organizer)
                .addOnSuccessListener(doc -> {
                    organizerId = doc.getId();
                    organizerLatch.countDown();
                });
        organizerLatch.await();

        CountDownLatch eventLatch = new CountDownLatch(1);
        Map<String, Object> event = new HashMap<>();
        event.put("name", "Test Event");
        event.put("capacity", 20);
        event.put("organizer", organizerId);
        event.put("category", "Music");

        db.collection("events_test")
                .add(event)
                .addOnSuccessListener(doc -> {
                    eventId = doc.getId();
                    eventLatch.countDown();
                });
        eventLatch.await();

        CountDownLatch entrantLatch = new CountDownLatch(1);
        Map<String, Object> entrant = new HashMap<>();
        entrant.put("name", "Test Entrant");
        entrant.put("isAdmin", false);

        db.collection("users_test")
                .add(entrant)
                .addOnSuccessListener(doc -> {
                    String entrantId = doc.getId();
                    db.collection("users_test")
                            .document(entrantId)
                            .set(Map.of("events", List.of(eventId)), SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                testUser = new User(entrantId, "Test Entrant",
                                        "testentrant@email.com",
                                        "1234567890", false,
                                        new ArrayList<>(List.of(eventId)),
                                        new ArrayList<>(),
                                        new HashMap<>());
                                UserSession.getInstance().setCurrentUser(testUser);
                                entrantLatch.countDown();
                            });
                });
        entrantLatch.await();

        scenario = ActivityScenario.launch(EventHistoryActivity.class);
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
            onView(withId(R.id.event_history_list_view)).check((view, e) -> {
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
     * Verifies that the EventHistoryActivity displays the test event
     * for the registered entrant.
     * <p>
     * The test waits for the ListView to populate and then checks that all events
     * in the database are displayed to the user.
     * <p>
     * User Story Tested:
     *     US 01.02.03 As an entrant, I want to have a history of events I
     *     have registered for, whether I was selected or not.
     *
     * @throws InterruptedException if the wait operation is interrupted
     */
    @Test
    public void testEventHistoryDisplayed() throws InterruptedException {
        // Wait for at least one item to populate the ListView
        waitForListViewItems(1, 5000);

        scenario.onActivity(activity -> {
            ListView listView = activity.findViewById(R.id.event_history_list_view);
            Event event = (Event) listView.getAdapter().getItem(0);
            assertEquals("Test Event", event.getName());
        });
    }

    /**
     * Cleans up Firestore test data and closes the activity scenario after each test.
     *
     * @throws InterruptedException if the cleanup wait is interrupted
     */
    @After
    public void cleanup() throws InterruptedException {

        if (scenario != null) scenario.close();
        FirestoreCollections.endTest();
    }
}