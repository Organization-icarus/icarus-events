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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Instrumented UI tests for {@link OrganizerCreateEventActivity}.
 * <p>
 * User Stories Tested:
 *      US 02.01.04 As an organizer, I want to set a registration period.
 *      US 02.03.01 As an organizer I want to OPTIONALLY limit the number
 *      of entrants who can join my waiting list.
 *      US 02.02.03 As an organizer I want to enable or disable the
 *      geolocation requirement for my event.
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
        FirestoreCollections.startTest();

        User testUser = new User(
                "dummyOrganizerId",
                "Test Organizer",
                "organizer@example.com",
                "1234567890",
                "organizer",
                new ArrayList<>(),
                new HashMap<>()
        );
        UserSession.getInstance().setCurrentUser(testUser);
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
     * Tests that an organizer can optionally set a waiting list limit
     * when creating an event.
     * <p>
     * The test simulates an organizer filling out the event creation
     * form and specifying a maximum number of entrants allowed on the
     * waiting list. After the event is created, the test verifies that
     * the waiting list limit was correctly stored in Firestore.
     * <p>
     * User Story Tested:
     *      US 02.03.01 As an organizer I want to OPTIONALLY limit the number
     *      of entrants who can join my waiting list.
     *
     * @throws InterruptedException if the Firestore retrieval wait is interrupted
     */
    @Test
    public void testWaitingListLimitSaved() throws InterruptedException {

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerCreateEventActivity.class
        );

        scenario = ActivityScenario.launch(intent);

        // Enter event name
        onView(withId(R.id.OrganizerCreateEventEventTitle))
                .perform(typeText("Waiting List Test Event"), closeSoftKeyboard());

        // Enter location
        onView(withId(R.id.OrganizerCreateEventEventLocation))
                .perform(typeText("Example Location"), closeSoftKeyboard());

        // Enter event waiting list limit
        onView(withId(R.id.OrganizerCreateEventLimitWaitingListLimit))
                .perform(typeText("50"), closeSoftKeyboard());

        // Select registration start date
        onView(withId(R.id.OrganizerCreateEventRegistrationPeriodStart)).perform(click());
        onView(withContentDescription(containsString("March 15"))).perform(click());
        onView(withText("OK")).perform(click());

        // Select registration end date
        onView(withId(R.id.OrganizerCreateEventRegistrationPeriodEnd)).perform(click());
        onView(withContentDescription(containsString("March 20"))).perform(click());
        onView(withText("OK")).perform(click());

        // Select event date
        onView(withId(R.id.OrganizerCreateEventDate)).perform(click());
        onView(withContentDescription(containsString("March 25"))).perform(click());
        onView(withText("OK")).perform(click());

        // Create event
        onView(withId(R.id.OrganizerCreateEventCreateEvent)).perform(click());

        // Verify waiting list limit stored in Firestore
        CountDownLatch latch = new CountDownLatch(1);

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .whereEqualTo("name", "Waiting List Test Event")
                .get()
                .addOnSuccessListener(query -> {

                    if (query.isEmpty()) {
                        throw new AssertionError("Event was not created in Firestore");
                    }

                    var doc = query.getDocuments().get(0);

                    long waitingLimit = doc.getLong("capacity");

                    assertEquals(50, waitingLimit);

                    latch.countDown();
                });

        latch.await();
    }

    /**
     * Tests that an organizer can enable the geolocation requirement
     * when creating an event.
     * <p>
     * The test fills out the event creation form, enables the
     * geolocation toggle, creates the event, and verifies that
     * the geolocation requirement is stored as true in Firestore.
     * <p>
     * User Story Tested:
     *      US 02.02.03 As an organizer I want to enable the
     *      geolocation requirement for my event.
     */
    @Test
    public void testGeolocationEnabled() throws InterruptedException {

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerCreateEventActivity.class
        );

        scenario = ActivityScenario.launch(intent);

        onView(withId(R.id.OrganizerCreateEventEventTitle))
                .perform(typeText("Geo Enabled Event"), closeSoftKeyboard());

        onView(withId(R.id.OrganizerCreateEventEventLocation))
                .perform(typeText("Example Location"), closeSoftKeyboard());

        onView(withId(R.id.OrganizerCreateEventLimitWaitingListLimit))
                .perform(typeText("50"), closeSoftKeyboard());

        // Enable geolocation
        onView(withId(R.id.OrganizerCreateEventGeolocationSwitch)).perform(click());

        // Select registration start date
        onView(withId(R.id.OrganizerCreateEventRegistrationPeriodStart)).perform(click());
        onView(withContentDescription(containsString("March 15"))).perform(click());
        onView(withText("OK")).perform(click());

        // Select registration end date
        onView(withId(R.id.OrganizerCreateEventRegistrationPeriodEnd)).perform(click());
        onView(withContentDescription(containsString("March 20"))).perform(click());
        onView(withText("OK")).perform(click());

        // Select event date
        onView(withId(R.id.OrganizerCreateEventDate)).perform(click());
        onView(withContentDescription(containsString("March 25"))).perform(click());
        onView(withText("OK")).perform(click());

        // Create event
        onView(withId(R.id.OrganizerCreateEventCreateEvent)).perform(click());

        CountDownLatch latch = new CountDownLatch(1);

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .whereEqualTo("name", "Geo Enabled Event")
                .get()
                .addOnSuccessListener(query -> {

                    var doc = query.getDocuments().get(0);

                    Boolean geolocation = doc.getBoolean("geolocation");

                    assertTrue(geolocation);

                    latch.countDown();
                });

        latch.await();
    }

    /**
     * Tests that an organizer can disable the geolocation requirement
     * when creating an event.
     * <p>
     * The test fills out the event creation form without enabling
     * the geolocation toggle and verifies that the geolocation
     * requirement is stored as false in Firestore.
     * <p>
     * User Story Tested:
     *      US 02.02.03 As an organizer I want to disable the
     *      geolocation requirement for my event.
     */
    @Test
    public void testGeolocationDisabled() throws InterruptedException {

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerCreateEventActivity.class
        );

        scenario = ActivityScenario.launch(intent);

        onView(withId(R.id.OrganizerCreateEventEventTitle))
                .perform(typeText("Geo Disabled Event"), closeSoftKeyboard());

        onView(withId(R.id.OrganizerCreateEventEventLocation))
                .perform(typeText("Example Location"), closeSoftKeyboard());

        onView(withId(R.id.OrganizerCreateEventLimitWaitingListLimit))
                .perform(typeText("50"), closeSoftKeyboard());

        // Select registration start date
        onView(withId(R.id.OrganizerCreateEventRegistrationPeriodStart)).perform(click());
        onView(withContentDescription(containsString("March 15"))).perform(click());
        onView(withText("OK")).perform(click());

        // Select registration end date
        onView(withId(R.id.OrganizerCreateEventRegistrationPeriodEnd)).perform(click());
        onView(withContentDescription(containsString("March 20"))).perform(click());
        onView(withText("OK")).perform(click());

        // Select event date
        onView(withId(R.id.OrganizerCreateEventDate)).perform(click());
        onView(withContentDescription(containsString("March 25"))).perform(click());
        onView(withText("OK")).perform(click());

        onView(withId(R.id.OrganizerCreateEventCreateEvent)).perform(click());

        CountDownLatch latch = new CountDownLatch(1);

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .whereEqualTo("name", "Geo Disabled Event")
                .get()
                .addOnSuccessListener(query -> {

                    var doc = query.getDocuments().get(0);

                    Boolean geolocation = doc.getBoolean("geolocation");

                    assertFalse(geolocation);

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

        if (scenario != null) scenario.close();
        FirestoreCollections.endTest();
    }
}