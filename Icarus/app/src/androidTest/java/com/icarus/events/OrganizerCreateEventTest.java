package com.icarus.events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.containsString;
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

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

/**
 * Instrumented UI tests for {@link OrganizerCreateEventActivity}.
 * <p>
 * User Stories Tested:
 *      US 02.01.04 As an organizer, I want to set a registration period.
 * <p>
 * Tests use temporary Firestore collections to avoid interfering
 * with production event data.
 *
 * @author Kito Lee Son
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerCreateEventTest {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ActivityScenario<OrganizerCreateEventActivity> scenario;

    private String createdEventId;

    /**
     * Prepares the Firestore environment before each test.
     * <p>
     * This method redirects the application to use a temporary
     * events collection so test data does not affect the
     * production database.
     */
    @Before
    public void setup() {
        FirestoreCollections.EVENTS_COLLECTION = "events_test";
    }

    /**
     * Tests that an organizer can create an event with a
     * registration start and end date.
     * <p>
     * The test simulates an organizer filling out the event
     * creation form, selecting registration start and end
     * dates using date picker dialogs, and creating the event.
     * After creation, the test verifies that the correct
     * registration period was stored in Firestore.
     * <p>
     * User Story Tested:
     *      US 02.01.04 As an organizer, I want to set a registration period.
     *
     * @throws InterruptedException if the Firestore retrieval wait is interrupted
     */
    @Test
    public void testRegistrationPeriodSaved() throws InterruptedException {

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerCreateEventActivity.class
        );

        scenario = ActivityScenario.launch(intent);

        // Enter event name
        onView(withId(R.id.OrganizerCreateEventEventTitle))
                .perform(typeText("Example Event"), closeSoftKeyboard());

        // Enter location
        onView(withId(R.id.OrganizerCreateEventEventLocation))
                .perform(typeText("Example Location"), closeSoftKeyboard());

        // Enter capacity
        onView(withId(R.id.OrganizerCreateEventLimitWaitingListLimit))
                .perform(typeText("50"), closeSoftKeyboard());

        // Open registration start date picker
        onView(withId(R.id.OrganizerCreateEventRegistrationPeriodStart)).perform(click());

        // Select a date in date picker (example day)
        onView(withContentDescription(containsString("March 15"))).perform(click());
        onView(withText("OK")).perform(click());

        // Open registration end date picker
        onView(withId(R.id.OrganizerCreateEventRegistrationPeriodEnd)).perform(click());

        onView(withContentDescription(containsString("March 20"))).perform(click());
        onView(withText("OK")).perform(click());

        // Open event date picker
        onView(withId(R.id.OrganizerCreateEventDate)).perform(click());

        onView(withContentDescription(containsString("March 25"))).perform(click());
        onView(withText("OK")).perform(click());

        // Create event
        onView(withId(R.id.OrganizerCreateEventCreateEvent)).perform(click());
        Thread.sleep(5000);

        // Verify registration dates stored in Firestore
        CountDownLatch latch = new CountDownLatch(1);
        db.collection("events_test")
                .whereEqualTo("name", "Example Event")
                .get()
                .addOnSuccessListener(query -> {
                    createdEventId = query.getDocuments().get(0).getId();

                    Date startDate = query.getDocuments().get(0).getTimestamp("open").toDate();
                    Date endDate = query.getDocuments().get(0).getTimestamp("close").toDate();

                    System.out.println(query.getDocuments().get(0).getData());

                    Calendar startCal = Calendar.getInstance();
                    startCal.setTime(startDate);

                    Calendar endCal = Calendar.getInstance();
                    endCal.setTime(endDate);

                    assertEquals(15, startCal.get(Calendar.DAY_OF_MONTH));
                    assertEquals(20, endCal.get(Calendar.DAY_OF_MONTH));

                    latch.countDown();
                });

        latch.await();
    }

    /**
     * Cleans up Firestore test data after each test.
     * <p>
     * This method removes any events created during testing
     * and restores the default Firestore events collection.
     *
     * @throws InterruptedException if the cleanup wait is interrupted
     */
    @After
    public void cleanup() throws InterruptedException {

        FirestoreCollections.EVENTS_COLLECTION = "events";

        if (scenario != null) scenario.close();

        if (createdEventId != null) {
            CountDownLatch latch = new CountDownLatch(1);

            db.collection("events_test")
                    .document(createdEventId)
                    .delete()
                    .addOnSuccessListener(v -> latch.countDown());

            latch.await();
        }
    }
}