package com.icarus.events;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.action.ViewActions.click;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.junit.Assert.assertEquals;

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
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Instrumented UI tests for {@link OrganizerViewEntrantsOnWaitingList}.
 * <p>
 * User Stories Tested:
 *      US 02.06.01 As an organizer I want to view a list of all chosen entrants.
 *      US 02.06.02 As an organizer I want to see a list of all cancelled entrants.
 *      US 02.06.03 As an organizer I want to see a final list of enrolled entrants.
 * <p>
 * Tests use temporary Firestore collections to avoid affecting production data.
 *
 * @author Kito Lee Son
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerViewEntrantsTest {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ActivityScenario<OrganizerViewEntrantsOnWaitingList> scenario;
    private String createdEventId;

    @Before
    public void setup() throws InterruptedException {
        // Use test collection
        FirestoreCollections.EVENTS_COLLECTION = "events_test";
        FirestoreCollections.USERS_COLLECTION = "users_test";

        // Create a test event
        CountDownLatch eventLatch = new CountDownLatch(1);
        Map<String, Object> eventData = Map.of(
                "name", "Test Event",
                "date", new Date()
        );

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .add(eventData)
                .addOnSuccessListener(docRef -> {
                    createdEventId = docRef.getId();

                    // Add entrants subcollection
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

    @Test
    public void testViewChosenEntrants() throws InterruptedException {
        // Launch activity with test eventId
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerViewEntrantsOnWaitingList.class
        );
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        // Click "Chosen" filter button
        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_chosen)).perform(click());

        // Verify that "Entrant One" (chosen) appears in the ListView
        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant One"))));
    }

    @Test
    public void testViewCancelledEntrants() throws InterruptedException {
        // Launch activity with test eventId
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerViewEntrantsOnWaitingList.class
        );
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        // Click "Chosen" filter button
        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_cancelled)).perform(click());

        // Verify that "Entrant One" (chosen) appears in the ListView
        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant Two"))));
    }

    @Test
    public void testViewRegisteredEntrants() throws InterruptedException {
        // Launch activity with test eventId
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerViewEntrantsOnWaitingList.class
        );
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        // Click "Chosen" filter button
        onView(withId(R.id.OrganizerEntrantOnWaitingListFilterBar_final)).perform(click());

        // Verify that "Entrant One" (chosen) appears in the ListView
        onView(withId(R.id.OrganizerEntrantOnWaitingList))
                .check(matches(hasDescendant(withText("Entrant Three"))));
    }

    @After
    public void cleanup() throws InterruptedException {
        FirestoreCollections.USERS_COLLECTION = "users";
        FirestoreCollections.EVENTS_COLLECTION = "events";

        // Delete test event
        CountDownLatch latch = new CountDownLatch(1);
        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(createdEventId)
                .delete()
                .addOnSuccessListener(v -> latch.countDown());

        latch.await();
    }
}