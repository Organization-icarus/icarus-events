package com.icarus.events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Tests User Stories:
 *      US 01.01.03 As an entrant, I want to be able to see a list of events that I can join the waiting list for.
 *      US 01.01.04 As an entrant, I want to filter events based on my interests and availability.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventListTest {

    private ActivityScenario<EntrantEventListActivity> scenario;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<String> insertedIds = new ArrayList<>();
    private String organizerId;

    @Before
    public void setupFirestoreData() throws InterruptedException {

        FirestoreCollections.EVENTS_COLLECTION = "events_test";

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

        CountDownLatch eventLatch = new CountDownLatch(3);

        for (int i = 1; i <= 3; i++) {

            Map<String, Object> event = new HashMap<>();
            event.put("capacity", 20 + (20 * i));
            event.put("name", "Test Event " + i);
            event.put("organizer", organizerId);

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

    private void waitForRecyclerViewItems(int minCount, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            final int[] count = {0};
            onView(withId(R.id.recyclerViewEvents)).check((view, e) -> {
                RecyclerView rv = (RecyclerView) view;
                if (rv.getAdapter() != null) {
                    count[0] = rv.getAdapter().getItemCount();
                }
            });
            if (count[0] >= minCount) return;
            Thread.sleep(50);
        }

        throw new AssertionError("RecyclerView never populated");
    }

    @Test
    public void testDisplayedEventsMatchDatabase() throws InterruptedException {

        waitForRecyclerViewItems(3, 5000);
        onView(withId(R.id.recyclerViewEvents))
                .check((view, e) -> {

                    RecyclerView rv = (RecyclerView) view;
                    assertNotNull(rv.getAdapter());
                    int uiCount = rv.getAdapter().getItemCount();

                    assertEquals(3, uiCount);
                });
    }

    @After
    public void cleanupFirestoreData() throws InterruptedException {

        FirestoreCollections.EVENTS_COLLECTION = "events";

        CountDownLatch latch = new CountDownLatch(insertedIds.size());

        for (String id : insertedIds) {
            db.collection("events_test")
                    .document(id)
                    .delete()
                    .addOnSuccessListener(a -> latch.countDown());
        }

        latch.await();
    }
}