package com.icarus.events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.cloudinary.android.MediaManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Instrumented UI tests for {@link EntrantMapActivity}.
 * <p>
 * User Stories Tested:
 *     US 02.02.02 As an organizer I want to see on a map where entrants
 *                 joined my event waiting list from.
 * <p>
 * Tests use temporary Firestore collections to avoid affecting production data.
 *
 * @author Benjamin Hall
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EntrantMapTest {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ActivityScenario<EntrantMapActivity> scenario;

    private String testEventId;

    // The Edmonton Convention Centre — used as a fake event location
    private static final double EVENT_LAT = 53.5444;
    private static final double EVENT_LON = -113.4909;

    // Two fake entrant join locations (within the event range)
    private static final double ENTRANT_1_LAT = 53.5461;
    private static final double ENTRANT_1_LON = -113.4938;

    private static final double ENTRANT_2_LAT = 53.5430;
    private static final double ENTRANT_2_LON = -113.4870;

    /**
     * Sets up Firestore test data before each test.
     * <p>
     * Creates a test event with geolocation enabled, an entrant range,
     * and two entrant documents each containing a location GeoPoint.
     *
     * @throws InterruptedException if any Firestore operations are interrupted
     */
    @Before
    public void setup() throws InterruptedException {
        FirestoreCollections.startTest();

        // Initialize Cloudinary since MainActivity is bypassed in tests
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", "icarus-images");
            config.put("api_key", "291231889216385");
            config.put("api_secret", "ToWWi626oI0M7Ou1pmPQx_vd5x8");
            MediaManager.init(ApplicationProvider.getApplicationContext(), config);
        } catch (IllegalStateException e) {
            // MediaManager already initialized, safe to ignore
        }

        // Create and register a fake organizer in the session
        User organizerUser = new User(
                "organizer1", "Organizer User", null,
                null, "No Image", false,
                new ArrayList<>(), new ArrayList<>(), null, null
        );
        UserSession.getInstance().setCurrentUser(organizerUser);

        // Create a test event with geolocation coordinates and an entrant range
        CountDownLatch eventLatch = new CountDownLatch(1);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("name", "Geo Test Event");
        eventData.put("category", "Test");
        eventData.put("coordinates", new GeoPoint(EVENT_LAT, EVENT_LON));
        eventData.put("entrantRange", 5.0); // 5 km radius

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .add(eventData)
                .addOnSuccessListener(docRef -> {
                    testEventId = docRef.getId();
                    eventLatch.countDown();
                });
        eventLatch.await();

        // Add two entrants with location data to the event's entrants subcollection
        CountDownLatch entrantLatch = new CountDownLatch(2);

        Map<String, Object> entrant1 = new HashMap<>();
        entrant1.put("userId", "user1");
        entrant1.put("location", new GeoPoint(ENTRANT_1_LAT, ENTRANT_1_LON));

        Map<String, Object> entrant2 = new HashMap<>();
        entrant2.put("userId", "user2");
        entrant2.put("location", new GeoPoint(ENTRANT_2_LAT, ENTRANT_2_LON));

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(testEventId)
                .collection("entrants")
                .document("user1")
                .set(entrant1)
                .addOnSuccessListener(v -> entrantLatch.countDown());

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(testEventId)
                .collection("entrants")
                .document("user2")
                .set(entrant2)
                .addOnSuccessListener(v -> entrantLatch.countDown());

        entrantLatch.await();
    }

    /**
     * Tests that the entrant map view is displayed when opening an event
     * with geolocation data.
     * <p>
     * Verifies the map view renders correctly, satisfying the basic
     * requirement that the organizer can see the map.
     * <p>
     * User Stories Tested:
     *     US 02.02.02 As an organizer I want to see on a map where entrants
     *                 joined my event waiting list from.
     *
     * @throws InterruptedException if the thread sleep is interrupted
     */
    @Test
    public void testMapIsDisplayed() throws InterruptedException {
        launchEntrantMap();
        Thread.sleep(2000); // Allow time for Firestore reads and map tiles to initialize

        onView(withId(R.id.entrant_map))
                .check(matches(isDisplayed()));
    }

    /**
     * Tests that the entrant map loads without crashing when the event has
     * two entrants, each with a location GeoPoint.
     * <p>
     * Launches the activity and waits for both the event coordinates and
     * entrant location queries to complete. Confirms the map is still
     * displayed after markers have been added for each entrant.
     * <p>
     * User Stories Tested:
     *     US 02.02.02 As an organizer I want to see on a map where entrants
     *                 joined my event waiting list from.
     *
     * @throws InterruptedException if the thread sleep is interrupted
     */
    @Test
    public void testEntrantMarkersLoadWithoutCrash() throws InterruptedException {
        launchEntrantMap();

        // Allow sufficient time for both Firestore queries to complete:
        // (1) loading event coordinates/range, (2) loading entrant locations
        Thread.sleep(3000);

        // If the activity is still displayed after loading entrant markers
        // for two entrants, the map has rendered without crashing
        onView(withId(R.id.entrant_map))
                .check(matches(isDisplayed()));
    }

    /**
     * Tests that the entrant map loads correctly for an event where one
     * entrant has a null location (e.g., joined without geolocation enabled
     * or location was unavailable).
     * <p>
     * EntrantMapActivity skips null locations rather than crashing, so the
     * map must still be displayed after loading.
     * <p>
     * User Stories Tested:
     *     US 02.02.02 As an organizer I want to see on a map where entrants
     *                 joined my event waiting list from.
     *
     * @throws InterruptedException if Firestore writes or thread sleep are interrupted
     */
    @Test
    public void testMapHandlesEntrantWithNullLocation() throws InterruptedException {
        // Add a third entrant with no location field
        CountDownLatch latch = new CountDownLatch(1);
        Map<String, Object> entrantNoLocation = new HashMap<>();
        entrantNoLocation.put("userId", "user3");
        // Deliberately omit "location" to simulate a null GeoPoint

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(testEventId)
                .collection("entrants")
                .document("user3")
                .set(entrantNoLocation)
                .addOnSuccessListener(v -> latch.countDown());
        latch.await();

        launchEntrantMap();
        Thread.sleep(3000);

        // Map must still render even though one entrant had a null location
        onView(withId(R.id.entrant_map))
                .check(matches(isDisplayed()));
    }

    /**
     * Helper method to launch {@link EntrantMapActivity} with the test event ID.
     */
    private void launchEntrantMap() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantMapActivity.class
        );
        intent.putExtra("eventId", testEventId);
        scenario = ActivityScenario.launch(intent);
    }

    /**
     * Cleans up Firestore test data after each test.
     * <p>
     * Resets Firestore collection names back to production and deletes
     * all documents created in the temporary test collections.
     *
     * @throws InterruptedException if Firestore deletions are interrupted
     */
    @After
    public void cleanup() throws InterruptedException {
        if (scenario != null) {
            scenario.close();
        }
        FirestoreCollections.endTest();
    }
}