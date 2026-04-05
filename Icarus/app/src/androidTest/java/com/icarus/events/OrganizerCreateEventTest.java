package com.icarus.events;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.anything;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.cloudinary.utils.ObjectUtils;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive instrumented UI tests for {@link OrganizerCreateEventActivity}.
 *
 * <p>Covers:
 * <ul>
 *   <li>US 02.01.04 – As an organizer, I want to set a registration period.</li>
 *   <li>US 02.03.01 – As an organizer I want to OPTIONALLY limit the number
 *       of entrants who can join my waiting list.</li>
 *   <li>US 02.02.03 – As an organizer I want to enable or disable the
 *       geolocation requirement for my event.</li>
 * </ul>
 *
 * <p>All tests redirect Firestore to a temporary test collection so that
 * production data is never affected.
 *
 * <p><strong>Key design notes:</strong>
 * <ul>
 *   <li>Every view interaction uses {@code scrollTo()} first because the form
 *       lives inside a {@code NestedScrollView}. Without this, Espresso types
 *       into whichever view currently has focus — often a different field or
 *       the nav-bar's search input entirely.</li>
 *   <li>Geolocation tests must fully drive the map dialog (tap the map to place
 *       a marker, set the radius slider to a non-zero value, then confirm) before
 *       the dialog will dismiss and the test can continue.</li>
 *   <li>All Firestore {@link CountDownLatch} calls include a 15-second timeout
 *       so a missing document fails fast rather than hanging the test runner.</li>
 *   <li>Assertion errors thrown inside Firestore callbacks are captured via an
 *       {@code assertionFailed[]} flag and re-checked on the test thread so
 *       that JUnit actually sees the failure.</li>
 * </ul>
 *
 * @author Kito Lee Son (original), revised and extended
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerCreateEventTest {

    // -------------------------------------------------------------------------
    //  Shared state
    // -------------------------------------------------------------------------

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ActivityScenario<OrganizerCreateEventActivity> scenario;
    private String uploadedEventImageUrl;

    /** Firestore document ID of any event created during a test run. */
    private String createdEventId;

    // -------------------------------------------------------------------------
    //  Date-picker day constants
    //  All four dates are chosen in April (the month the picker opens to when
    //  tests run in April 2026). Keeping them in the same month avoids having
    //  to navigate the calendar between selections.
    // -------------------------------------------------------------------------

    private static final int REG_START_DAY   = 20;   // April 20
    private static final int REG_END_DAY     = 21;   // April 21
    private static final int EVENT_START_DAY = 22;   // April 22
    private static final int EVENT_END_DAY   = 23;   // April 23

    // -------------------------------------------------------------------------
    //  Setup / teardown
    // -------------------------------------------------------------------------

    /**
     * Redirects Firestore to the temporary test collection and seeds a minimal
     * {@link UserSession} so the activity can resolve the current organizer ID.
     */
    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();

        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "icarus-images");
            config.put("api_key", "291231889216385");
            config.put("api_secret", "ToWWi626oI0M7Ou1pmPQx_vd5x8");
            MediaManager.init(context, config);
        } catch (IllegalStateException ignored) {
            // already initialized
        }

        FirestoreCollections.startTest();

        User testUser = new User(
                "dummyOrganizerId",
                "Test Organizer",
                "organizer@example.com",
                "1234567890",
                "No Image",
                false,
                new ArrayList<>(),
                new ArrayList<>(),
                new HashMap<>()
        );
        UserSession.getInstance().setCurrentUser(testUser);
    }

    /**
     * Closes any open {@link ActivityScenario}, releases Espresso Intents if
     * initialised, and restores the default Firestore collection name.
     */
    @After
    public void cleanup() throws Exception {
        if (scenario != null) {
            scenario.close();
        }

        deleteUploadedImageFromCloudinary(uploadedEventImageUrl);
        FirestoreCollections.endTest();
        uploadedEventImageUrl = null;
        createdEventId = null;
        scenario = null;
    }

    // =========================================================================
    //  1. Field-input acceptance (smoke test)
    // =========================================================================

    /**
     * Verifies that every text field, date/time picker, spinner, and toggle on
     * the event-creation screen can accept input without crashing or clearing.
     *
     * <p>Because the form lives inside a {@code NestedScrollView}, every
     * interaction is preceded by {@code scrollTo()} to guarantee the target is
     * on screen before Espresso tries to tap it.
     *
     * <p>The geolocation switch opens a full-screen map dialog; this test
     * drives the dialog to completion (tap map → set radius → confirm) so that
     * no interaction is left pending when the test finishes.
     */
    @Test
    public void testAllFieldsAcceptInput() {
        scenario = ActivityScenario.launch(buildIntent());

        // --- Text inputs ---
        onView(withId(R.id.OrganizerCreateEventEventTitle))
                .perform(scrollTo(), replaceText("My Test Event"), closeSoftKeyboard())
                .check(matches(withText("My Test Event")));

        onView(withId(R.id.OrganizerCreateEventDescription))
                .perform(scrollTo(), replaceText("A detailed description."), closeSoftKeyboard())
                .check(matches(withText("A detailed description.")));

        onView(withId(R.id.OrganizerCreateEventEventLocation))
                .perform(scrollTo(), replaceText("Convention Centre"), closeSoftKeyboard())
                .check(matches(withText("Convention Centre")));

        // Scroll the NestedScrollView manually, then type
        scrollFieldIntoView(R.id.OrganizerCreateEventLimitWaitingListLimit);

        onView(withId(R.id.OrganizerCreateEventLimitWaitingListLimit))
                .perform(replaceText("100"), closeSoftKeyboard())
                .check(matches(withText("100")));

        // --- Category spinner ---
        scrollFieldIntoView(R.id.OrganizerCreateEventCategory);
        onView(withId(R.id.OrganizerCreateEventCategory)).perform(click());
        onData(anything()).atPosition(1).perform(click());

        // --- Geolocation switch ---
        scrollFieldIntoView(R.id.OrganizerCreateEventGeolocationSwitch);
        onView(withId(R.id.OrganizerCreateEventGeolocationSwitch)).perform(click());
        interactWithGeolocationDialog();

        // --- Private-event switch ---
        scrollFieldIntoView(R.id.OrganizerCreateEventPrivateSwitch);
        onView(withId(R.id.OrganizerCreateEventPrivateSwitch)).perform(click());
        onView(withId(R.id.OrganizerCreateEventPrivateSwitch)).perform(click());

        // --- Dates ---
        selectDates();
    }

    // =========================================================================
    //  2. Full event creation – no image
    // =========================================================================

    /**
     * End-to-end test: fills every field, omits an event poster, submits the
     * form, and confirms that the resulting Firestore document contains the
     * correct values for all stored fields.
     *
     * <p>Assertions cover text fields (name, description, capitalised location),
     * numeric capacity, default boolean states, and all four timestamp fields
     * (day-of-month and chronological ordering).
     *
     * <p>User Stories: US 02.01.04 (registration period), US 02.03.01 (waitlist
     * limit).
     *
     * @throws InterruptedException if the Firestore latch is interrupted
     */
    @Test
    public void testImagelessEventCreationSavedToFirestore() throws InterruptedException {
        final String EVENT_NAME = "Full Creation Test";

        scenario = ActivityScenario.launch(buildIntent());

        fillCoreFields(EVENT_NAME, "A proper description.", "Exhibition Hall", "75");
        selectDates();
        clickAfterScroll(R.id.OrganizerCreateEventCreateEvent);

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] assertionFailed = {false};

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .whereEqualTo("name", EVENT_NAME)
                .get()
                .addOnSuccessListener(query -> {
                    try {
                        assertFalse("Expected event document in Firestore", query.isEmpty());

                        var doc = query.getDocuments().get(0);
                        createdEventId = doc.getId();

                        // --- Text fields ---
                        assertEquals(EVENT_NAME, doc.getString("name"));
                        assertEquals("A proper description.", doc.getString("description"));
                        // Activity uppercases first char and lowercases the rest
                        assertEquals("Exhibition hall", doc.getString("location"));

                        // --- Numeric field ---
                        assertEquals(75.0, doc.getDouble("capacity"), 0.001);

                        // --- Default boolean states (no switches clicked) ---
                        assertFalse("geolocation should default to false",
                                Boolean.TRUE.equals(doc.getBoolean("geolocation")));
                        assertFalse("isPrivate should default to false",
                                Boolean.TRUE.equals(doc.getBoolean("isPrivate")));

                        // --- Timestamp day-of-month checks ---
                        Calendar openCal  = calFrom(doc.getTimestamp("open").toDate());
                        Calendar closeCal = calFrom(doc.getTimestamp("close").toDate());
                        Calendar startCal = calFrom(doc.getTimestamp("startDate").toDate());
                        Calendar endCal   = calFrom(doc.getTimestamp("endDate").toDate());

                        assertEquals(REG_START_DAY,   openCal.get(Calendar.DAY_OF_MONTH));
                        assertEquals(REG_END_DAY,     closeCal.get(Calendar.DAY_OF_MONTH));
                        assertEquals(EVENT_START_DAY, startCal.get(Calendar.DAY_OF_MONTH));
                        assertEquals(EVENT_END_DAY,   endCal.get(Calendar.DAY_OF_MONTH));

                        // --- Chronological ordering ---
                        Date open  = doc.getTimestamp("open").toDate();
                        Date close = doc.getTimestamp("close").toDate();
                        Date start = doc.getTimestamp("startDate").toDate();
                        Date end   = doc.getTimestamp("endDate").toDate();

                        assertTrue("Registration open must precede close",
                                open.before(close));
                        assertTrue("Registration close must not be after event start",
                                !close.after(start));
                        assertTrue("Event start must precede event end",
                                start.before(end));

                    } catch (AssertionError e) {
                        assertionFailed[0] = true;
                        throw e;
                    } finally {
                        latch.countDown();
                    }
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Firestore query timed out", latch.await(15, TimeUnit.SECONDS));
        assertFalse("One or more Firestore field assertions failed", assertionFailed[0]);
    }

    // =========================================================================
    //  3. Image upload from device gallery
    // =========================================================================
    /**
     * Verifies that an organizer can select a poster image from the device
     * gallery and that the resulting Firestore document stores a non-empty
     * image URL after Cloudinary upload.
     *
     * <p>The system image-picker intent is stubbed with Espresso Intents so that
     * no real gallery UI appears. A synthetic 50×50 PNG served through
     * {@link androidx.core.content.FileProvider} acts as the picker result.
     *
     * @throws Exception if the test image cannot be created or the latch is
     *                   interrupted
     */
    @Test
    public void testImageUploadFromDevice() throws Exception {
        final String EVENT_NAME = "Image Upload Test";

        Intents.init();
        try {
            Uri imageUri = createTestImageUri();

            Intent resultData = new Intent();
            resultData.setData(imageUri);
            resultData.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            intending(hasAction(Intent.ACTION_GET_CONTENT))
                    .respondWith(new Instrumentation.ActivityResult(
                            android.app.Activity.RESULT_OK,
                            resultData
                    ));

            scenario = ActivityScenario.launch(buildIntent());

            // Go through the real image picker path
            onView(withId(R.id.OrganizerCreateEventUploadPosterButton))
                    .perform(scrollTo(), click());

            // Optional sanity check: poster preview updated
            onView(withId(R.id.OrganizerCreateEventImage))
                    .check((view, noViewFoundException) -> {
                        assertNotNull("ImageView not found", view);
                        ImageView iv = (ImageView) view;
                        assertNotNull("Poster preview should be set", iv.getDrawable());
                    });

            fillCoreFields(EVENT_NAME, "Event with a poster.", "Art Gallery", "30");
            selectDates();
            clickAfterScroll(R.id.OrganizerCreateEventCreateEvent);

            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] assertionFailed = {false};
            final boolean[] foundCorrectDoc = {false};

            long deadline = System.currentTimeMillis() + 30000;

            while (System.currentTimeMillis() < deadline && !foundCorrectDoc[0]) {
                CountDownLatch queryLatch = new CountDownLatch(1);

                db.collection(FirestoreCollections.EVENTS_COLLECTION)
                        .whereEqualTo("name", EVENT_NAME)
                        .get()
                        .addOnSuccessListener(query -> {
                            try {
                                if (!query.isEmpty()) {
                                    var doc = query.getDocuments().get(0);
                                    createdEventId = doc.getId();

                                    String imageUrl = doc.getString("image");
                                    this.uploadedEventImageUrl = imageUrl;
                                    if (imageUrl != null && !imageUrl.isEmpty()) {
                                        foundCorrectDoc[0] = true;
                                    }
                                }
                            } catch (AssertionError e) {
                                assertionFailed[0] = true;
                            } finally {
                                queryLatch.countDown();
                            }
                        })
                        .addOnFailureListener(e -> {
                            assertionFailed[0] = true;
                            queryLatch.countDown();
                        });

                assertTrue("Firestore query timed out", queryLatch.await(5, TimeUnit.SECONDS));

                if (!foundCorrectDoc[0]) {
                    Thread.sleep(1000);
                }
            }

            latch.countDown();

            assertTrue("Event was not saved with a non-empty image URL", foundCorrectDoc[0]);
            addImageToImagesCollection(uploadedEventImageUrl);
            assertFalse("Assertion failed while checking Firestore", assertionFailed[0]);

        } finally {
            Intents.release();
        }
    }

    /**
     * Confirms that after the user selects a gallery image the poster
     * {@link android.widget.ImageView} displays content other than the default
     * placeholder (i.e., the selected URI is applied to the view).
     *
     * <p>This is a pure UI-layer check — it does not submit the form or inspect
     * Firestore.
     *
     * @throws Exception if the test image cannot be created
     */
    @Test
    public void testSelectedImageDisplayedInPosterView() throws Exception {
        Intents.init();
        try {
            Uri imageUri = createTestImageUri();

            Intent resultData = new Intent();
            resultData.setData(imageUri);
            resultData.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            intending(hasAction(Intent.ACTION_GET_CONTENT))
                    .respondWith(new Instrumentation.ActivityResult(
                            android.app.Activity.RESULT_OK, resultData));

            scenario = ActivityScenario.launch(buildIntent());

            onView(withId(R.id.OrganizerCreateEventUploadPosterButton))
                    .perform(scrollTo(), click());

            // After the stub delivers the URI, ImageView.setImageURI() is called;
            // the resulting drawable must not be null.
            onView(withId(R.id.OrganizerCreateEventImage))
                    .check((view, noViewFoundException) -> {
                        assertNotNull("View not found", view);
                        android.widget.ImageView iv = (android.widget.ImageView) view;
                        assertNotNull(
                                "ImageView drawable must not be null after image selection",
                                iv.getDrawable());
                    });
        } finally {
            Intents.release();
        }
    }

    // =========================================================================
    //  4. Geolocation toggle
    // =========================================================================

    /**
     * Verifies that enabling the geolocation switch, placing a map marker, and
     * setting an entrant radius stores {@code geolocation = true} and a non-null
     * {@code coordinates} GeoPoint in Firestore.
     *
     * <p>The geolocation switch opens a full-screen map dialog. The test must:
     * <ol>
     *   <li>Tap the {@code MapView} to place a marker.</li>
     *   <li>Wait 500 ms for OSMDroid's {@code GestureDetector} to fire
     *       {@code onSingleTapConfirmed} (which has an internal ~300 ms delay
     *       to distinguish from double-taps).</li>
     *   <li>Click the radius {@code SeekBar} at 50 % of its width to set a
     *       non-zero range — this fires {@code onStopTrackingTouch} which
     *       updates {@code selectedEntrantRange}.</li>
     *   <li>Tap the confirm button — which validates both marker and range
     *       before dismissing the dialog.</li>
     * </ol>
     *
     * <p>Skipping any of those steps causes the confirm button to show a Toast
     * and refuse to dismiss, timing out all subsequent Espresso interactions.
     *
     * <p>User Story: US 02.02.03 (enable geolocation).
     *
     * @throws InterruptedException if the Firestore latch or dialog sleep is
     *                              interrupted
     */
    @Test
    public void testGeolocationEnabled() throws InterruptedException {
        final String EVENT_NAME = "Geo Enabled Event";

        scenario = ActivityScenario.launch(buildIntent());

        // Open the map dialog and drive it to completion
        clickAfterScroll(R.id.OrganizerCreateEventGeolocationSwitch);
        interactWithGeolocationDialog();

        fillCoreFields(EVENT_NAME, "Geo test.", "Exhibition Hall", "50");

        selectDates();
        clickAfterScroll(R.id.OrganizerCreateEventCreateEvent);

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] assertionFailed = {false};

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .whereEqualTo("name", EVENT_NAME)
                .get()
                .addOnSuccessListener(query -> {
                    try {
                        assertFalse("Expected event document in Firestore", query.isEmpty());

                        var doc = query.getDocuments().get(0);
                        createdEventId = doc.getId();

                        assertTrue("geolocation must be true when switch is on",
                                Boolean.TRUE.equals(doc.getBoolean("geolocation")));

                        assertNotNull(
                                "coordinates GeoPoint must be stored when geolocation is on",
                                doc.get("coordinates"));

                    } catch (AssertionError e) {
                        assertionFailed[0] = true;
                        throw e;
                    } finally {
                        latch.countDown();
                    }
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Firestore query timed out", latch.await(15, TimeUnit.SECONDS));
        assertFalse("Geolocation-enabled assertion failed", assertionFailed[0]);
    }

    /**
     * Verifies that leaving the geolocation switch off stores
     * {@code geolocation = false} and {@code coordinates = null} in Firestore.
     *
     * <p>User Story: US 02.02.03 (disable geolocation).
     *
     * @throws InterruptedException if the Firestore latch is interrupted
     */
    @Test
    public void testGeolocationDisabled() throws InterruptedException {
        final String EVENT_NAME = "Geo Disabled Event";

        scenario = ActivityScenario.launch(buildIntent());

        fillCoreFields(EVENT_NAME, "No geo.", "Exhibition Hall", "50");
        // Geolocation switch deliberately NOT clicked
        selectDates();
        clickAfterScroll(R.id.OrganizerCreateEventCreateEvent);

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] assertionFailed = {false};

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .whereEqualTo("name", EVENT_NAME)
                .get()
                .addOnSuccessListener(query -> {
                    try {
                        assertFalse(query.isEmpty());
                        createdEventId = query.getDocuments().get(0).getId();

                        assertFalse("geolocation must be false when switch is off",
                                Boolean.TRUE.equals(
                                        query.getDocuments().get(0).getBoolean("geolocation")));

                        assertTrue("coordinates must be null when geolocation is off",
                                query.getDocuments().get(0).get("coordinates") == null);

                    } catch (AssertionError e) {
                        assertionFailed[0] = true;
                        throw e;
                    } finally {
                        latch.countDown();
                    }
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Firestore query timed out", latch.await(15, TimeUnit.SECONDS));
        assertFalse("Geolocation-disabled assertion failed", assertionFailed[0]);
    }

    // =========================================================================
    //  5. Waiting-list capacity
    // =========================================================================

    /**
     * Verifies that the value entered in the capacity field is persisted exactly
     * in Firestore.
     *
     * <p>User Story: US 02.03.01 – optionally limit the waiting list.
     *
     * @throws InterruptedException if the Firestore latch is interrupted
     */
    @Test
    public void testWaitingListLimitStoredCorrectly() throws InterruptedException {
        final String EVENT_NAME = "Capacity Test Event";

        scenario = ActivityScenario.launch(buildIntent());

        fillCoreFields(EVENT_NAME, "Capacity test.", "Venue A", "42");
        selectDates();
        clickAfterScroll(R.id.OrganizerCreateEventCreateEvent);

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] assertionFailed = {false};

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .whereEqualTo("name", EVENT_NAME)
                .get()
                .addOnSuccessListener(query -> {
                    try {
                        assertFalse(query.isEmpty());
                        createdEventId = query.getDocuments().get(0).getId();
                        assertEquals("capacity must match the entered waiting-list limit",
                                42.0,
                                query.getDocuments().get(0).getDouble("capacity"),
                                0.001);
                    } catch (AssertionError e) {
                        assertionFailed[0] = true;
                        throw e;
                    } finally {
                        latch.countDown();
                    }
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Firestore query timed out", latch.await(15, TimeUnit.SECONDS));
        assertFalse("Capacity assertion failed", assertionFailed[0]);
    }

    /**
     * Verifies that omitting the optional capacity field creates an event with
     * {@code capacity = null} in Firestore (must not silently default to 0).
     *
     * @throws InterruptedException if the Firestore latch is interrupted
     */
    @Test
    public void testEventCreatedWithoutWaitingListLimit() throws InterruptedException {
        final String EVENT_NAME = "No Capacity Event";

        scenario = ActivityScenario.launch(buildIntent());

        // Fill fields individually, deliberately omitting capacity
        onView(withId(R.id.OrganizerCreateEventEventTitle))
                .perform(scrollTo(), typeText(EVENT_NAME), closeSoftKeyboard());
        onView(withId(R.id.OrganizerCreateEventDescription))
                .perform(scrollTo(), typeText("No limit."), closeSoftKeyboard());
        onView(withId(R.id.OrganizerCreateEventEventLocation))
                .perform(scrollTo(), typeText("Open Venue"), closeSoftKeyboard());
        clickAfterScroll(R.id.OrganizerCreateEventCategory);
        onData(anything()).atPosition(1).perform(click());

        selectDates();
        clickAfterScroll(R.id.OrganizerCreateEventCreateEvent);

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] assertionFailed = {false};

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .whereEqualTo("name", EVENT_NAME)
                .get()
                .addOnSuccessListener(query -> {
                    try {
                        assertFalse(query.isEmpty());
                        createdEventId = query.getDocuments().get(0).getId();
                        assertTrue("capacity should be null when no limit entered",
                                query.getDocuments().get(0).get("capacity") == null);
                    } catch (AssertionError e) {
                        assertionFailed[0] = true;
                        throw e;
                    } finally {
                        latch.countDown();
                    }
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Firestore query timed out", latch.await(15, TimeUnit.SECONDS));
        assertFalse("Null-capacity assertion failed", assertionFailed[0]);
    }

    // =========================================================================
    //  6. Private event toggle
    // =========================================================================

    /**
     * Verifies that enabling the private-event switch stores
     * {@code isPrivate = true} in Firestore.
     *
     * @throws InterruptedException if the Firestore latch is interrupted
     */
    @Test
    public void testPrivateEventEnabled() throws InterruptedException {
        final String EVENT_NAME = "Private Event";

        scenario = ActivityScenario.launch(buildIntent());

        fillCoreFields(EVENT_NAME, "Secret gathering.", "Hidden Venue", "20");
        onView(withId(R.id.OrganizerCreateEventPrivateSwitch)).perform(scrollTo(), click());
        selectDates();
        clickAfterScroll(R.id.OrganizerCreateEventCreateEvent);

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] assertionFailed = {false};

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .whereEqualTo("name", EVENT_NAME)
                .get()
                .addOnSuccessListener(query -> {
                    try {
                        assertFalse(query.isEmpty());
                        createdEventId = query.getDocuments().get(0).getId();
                        assertTrue("isPrivate must be true when switch is on",
                                Boolean.TRUE.equals(
                                        query.getDocuments().get(0).getBoolean("isPrivate")));
                    } catch (AssertionError e) {
                        assertionFailed[0] = true;
                        throw e;
                    } finally {
                        latch.countDown();
                    }
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Firestore query timed out", latch.await(15, TimeUnit.SECONDS));
        assertFalse("isPrivate=true assertion failed", assertionFailed[0]);
    }

    /**
     * Verifies that leaving the private-event switch off stores
     * {@code isPrivate = false} in Firestore.
     *
     * @throws InterruptedException if the Firestore latch is interrupted
     */
    @Test
    public void testPrivateEventDisabled() throws InterruptedException {
        final String EVENT_NAME = "Public Event";

        scenario = ActivityScenario.launch(buildIntent());

        fillCoreFields(EVENT_NAME, "Open to everyone.", "City Park", "200");
        // Private switch deliberately NOT clicked
        selectDates();
        clickAfterScroll(R.id.OrganizerCreateEventCreateEvent);

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] assertionFailed = {false};

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .whereEqualTo("name", EVENT_NAME)
                .get()
                .addOnSuccessListener(query -> {
                    try {
                        assertFalse(query.isEmpty());
                        createdEventId = query.getDocuments().get(0).getId();
                        assertFalse("isPrivate must be false when switch is off",
                                Boolean.TRUE.equals(
                                        query.getDocuments().get(0).getBoolean("isPrivate")));
                    } catch (AssertionError e) {
                        assertionFailed[0] = true;
                        throw e;
                    } finally {
                        latch.countDown();
                    }
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Firestore query timed out", latch.await(15, TimeUnit.SECONDS));
        assertFalse("isPrivate=false assertion failed", assertionFailed[0]);
    }

    // =========================================================================
    //  7. Input validation guardrails
    // =========================================================================

    /**
     * Verifies that submitting without any dates selected shows a validation
     * error and does NOT write a document to Firestore.
     *
     * @throws InterruptedException if the sleep or latch is interrupted
     */
    @Test
    public void testValidationBlocksSubmitWhenDatesAreMissing() throws InterruptedException {
        final String EVENT_NAME = "No Dates Event";

        scenario = ActivityScenario.launch(buildIntent());

        onView(withId(R.id.OrganizerCreateEventEventTitle))
                .perform(scrollTo(), typeText(EVENT_NAME), closeSoftKeyboard());
        onView(withId(R.id.OrganizerCreateEventEventLocation))
                .perform(scrollTo(), typeText("Some Place"), closeSoftKeyboard());
        clickAfterScroll(R.id.OrganizerCreateEventCategory);
        onData(anything()).atPosition(1).perform(click());

        // Submit without dates — activity should Toast and return early
        clickAfterScroll(R.id.OrganizerCreateEventCreateEvent);

        Thread.sleep(2000); // brief window to confirm no async write occurs

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] documentFound = {false};

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .whereEqualTo("name", EVENT_NAME)
                .get()
                .addOnSuccessListener(query -> {
                    documentFound[0] = !query.isEmpty();
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Firestore query timed out", latch.await(15, TimeUnit.SECONDS));
        assertFalse("No Firestore document should be created when dates are missing",
                documentFound[0]);
    }

    /**
     * Verifies that submitting without a name, category, or location shows a
     * validation error and does NOT write a document to Firestore.
     *
     * @throws InterruptedException if the sleep or latch is interrupted
     */
    @Test
    public void testValidationBlocksSubmitWhenRequiredTextFieldsEmpty()
            throws InterruptedException {
        scenario = ActivityScenario.launch(buildIntent());

        // Fill dates only so we reach the text-field validation branch
        selectDates();

        // Submit without name / category / location
        clickAfterScroll(R.id.OrganizerCreateEventCreateEvent);

        Thread.sleep(2000);

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] documentFound = {false};

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .whereEqualTo("name", "")
                .get()
                .addOnSuccessListener(query -> {
                    documentFound[0] = !query.isEmpty();
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Firestore query timed out", latch.await(15, TimeUnit.SECONDS));
        assertFalse("No document should be saved when required text fields are empty",
                documentFound[0]);
    }

    // =========================================================================
    //  8. Location capitalisation
    // =========================================================================

    /**
     * Verifies that the activity uppercases the first character of the location
     * string and lowercases the remainder before persisting to Firestore.
     *
     * @throws InterruptedException if the Firestore latch is interrupted
     */
    @Test
    public void testLocationIsCapitalisedBeforeSaving() throws InterruptedException {
        final String EVENT_NAME = "Capitalisation Check";

        scenario = ActivityScenario.launch(buildIntent());

        fillCoreFields(EVENT_NAME, "Check casing.", "mOcKINg bIRD hAll", "10");
        selectDates();
        clickAfterScroll(R.id.OrganizerCreateEventCreateEvent);

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] assertionFailed = {false};

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .whereEqualTo("name", EVENT_NAME)
                .get()
                .addOnSuccessListener(query -> {
                    try {
                        assertFalse(query.isEmpty());
                        createdEventId = query.getDocuments().get(0).getId();
                        assertEquals(
                                "Location must have first char upper-case and rest lower-case",
                                "Mocking bird hall",
                                query.getDocuments().get(0).getString("location"));
                    } catch (AssertionError e) {
                        assertionFailed[0] = true;
                        throw e;
                    } finally {
                        latch.countDown();
                    }
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Firestore query timed out", latch.await(15, TimeUnit.SECONDS));
        assertFalse("Capitalisation assertion failed", assertionFailed[0]);
    }

    // =========================================================================
    //  9. Organizer ownership
    // =========================================================================

    /**
     * Verifies that the current user's ID appears in the {@code organizers}
     * array of the created Firestore document.
     *
     * @throws InterruptedException if the Firestore latch is interrupted
     */
    @Test
    public void testOrganizerIdStoredInCreatedEvent() throws InterruptedException {
        final String EVENT_NAME = "Organizer ID Test";

        scenario = ActivityScenario.launch(buildIntent());

        fillCoreFields(EVENT_NAME, "Ownership check.", "Main Stage", "50");
        selectDates();
        clickAfterScroll(R.id.OrganizerCreateEventCreateEvent);

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] assertionFailed = {false};

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .whereEqualTo("name", EVENT_NAME)
                .get()
                .addOnSuccessListener(query -> {
                    try {
                        assertFalse(query.isEmpty());
                        createdEventId = query.getDocuments().get(0).getId();

                        @SuppressWarnings("unchecked")
                        java.util.List<String> organizers =
                                (java.util.List<String>)
                                        query.getDocuments().get(0).get("organizers");

                        assertNotNull("organizers list must not be null", organizers);
                        assertFalse("organizers list must not be empty", organizers.isEmpty());
                        assertTrue("organizers must contain the current user's ID",
                                organizers.contains("dummyOrganizerId"));
                    } catch (AssertionError e) {
                        assertionFailed[0] = true;
                        throw e;
                    } finally {
                        latch.countDown();
                    }
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Firestore query timed out", latch.await(15, TimeUnit.SECONDS));
        assertFalse("Organizer ID assertion failed", assertionFailed[0]);
    }

    // =========================================================================
    //  Private helpers
    // =========================================================================

    /** Returns an {@link Intent} targeting {@link OrganizerCreateEventActivity}. */
    private Intent buildIntent() {
        return new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerCreateEventActivity.class);
    }

    /**
     * Scrolls to and types into the four core text fields, then selects the
     * first non-placeholder category from the spinner.
     *
     * <p><strong>Why {@code scrollTo()} is required:</strong> the form lives
     * inside a {@code NestedScrollView}.  Without scrolling first, Espresso
     * delivers key events to whichever view currently holds focus — often the
     * nav-bar search field or a different input — causing text to appear in
     * entirely the wrong place.  The capacity field in particular sits in the
     * Settings card which is not visible until the user scrolls past the
     * Location card.
     *
     * @param name        event title
     * @param description event description (pass {@code ""} to skip)
     * @param location    location text
     * @param capacity    waiting-list limit as a numeric string
     *                    (pass {@code ""} to leave the field empty)
     */
    private void fillCoreFields(String name, String description,
                                String location, String capacity) {
        enterTextAfterScroll(R.id.OrganizerCreateEventEventTitle, name);

        if (!description.isEmpty()) {
            enterTextAfterScroll(R.id.OrganizerCreateEventDescription, description);
        }

        enterTextAfterScroll(R.id.OrganizerCreateEventEventLocation, location);

        if (!capacity.isEmpty()) {
            enterTextAfterScroll(R.id.OrganizerCreateEventLimitWaitingListLimit, capacity);
        }

        clickAfterScroll(R.id.OrganizerCreateEventCategory);
        onData(anything()).atPosition(1).perform(click());
    }

    /**
     * Scrolls to and interacts with all eight date/time picker buttons
     * (registration start/end, event start/end), selecting the day defined by
     * the {@code *_DAY} constants and confirming each with "OK".
     *
     * <p>Time pickers are confirmed immediately with "OK" (accepting the
     * pre-filled current time), keeping tests focused on date correctness.
     * All date buttons are in the Dates card near the bottom of the scroll
     * view — {@code scrollTo()} is required for each one.
     */
    private void selectDates() {
        clickAfterScroll(R.id.OrganizerCreateEventRegistrationPeriodStartDate);
        onView(withContentDescription(containsString("April " + REG_START_DAY))).perform(click());
        onView(withText("OK")).perform(click());

        selectUpcomingTime(R.id.OrganizerCreateEventRegistrationPeriodStartTime,10);

        clickAfterScroll(R.id.OrganizerCreateEventRegistrationPeriodEndDate);
        onView(withContentDescription(containsString("April " + REG_END_DAY))).perform(click());
        onView(withText("OK")).perform(click());

        clickAfterScroll(R.id.OrganizerCreateEventRegistrationPeriodEndTime);
        onView(withText("OK")).perform(click());

        clickAfterScroll(R.id.OrganizerCreateEventStartDate);
        onView(withContentDescription(containsString("April " + EVENT_START_DAY))).perform(click());
        onView(withText("OK")).perform(click());

        clickAfterScroll(R.id.OrganizerCreateEventStartTime);
        onView(withText("OK")).perform(click());

        clickAfterScroll(R.id.OrganizerCreateEventEndDate);
        onView(withContentDescription(containsString("April " + EVENT_END_DAY))).perform(click());
        onView(withText("OK")).perform(click());

        clickAfterScroll(R.id.OrganizerCreateEventEndTime);
        onView(withText("OK")).perform(click());
    }

    /**
     * Drives the geolocation map dialog to a successful confirmation.
     *
     * <p>The confirm button validates two preconditions before dismissing:
     * <ol>
     *   <li>A marker must have been placed on the map.</li>
     *   <li>The entrant range ({@code SeekBar}) must be greater than zero.</li>
     * </ol>
     * If either is missing, the button shows a Toast and does nothing, which
     * causes all subsequent Espresso interactions to time out.
     *
     * <p><strong>Map tap timing:</strong> OSMDroid's {@link org.osmdroid.views.overlay.MapEventsOverlay}
     * uses Android's {@link android.view.GestureDetector} which fires
     * {@code onSingleTapConfirmed} via {@code Handler.postDelayed} after ~300 ms
     * (to rule out double-taps). We sleep 500 ms on the test thread after the
     * click so that the main-thread handler has time to complete before we
     * attempt to interact with the confirm button.
     *
     * <p><strong>SeekBar interaction:</strong> {@code selectedEntrantRange} is
     * only updated inside {@code onStopTrackingTouch}.  A coordinate-based click
     * via {@link #clickSeekBarAtPercent(int)} fires the full
     * {@code ACTION_DOWN → onStartTrackingTouch → ACTION_UP → onStopTrackingTouch}
     * sequence, which is the only reliable way to ensure the listener sets the
     * field to a non-zero value.
     */
    private void interactWithGeolocationDialog() {
        // 1. Tap the MapView to place a marker at its centre
        onView(withId(R.id.event_location_picker_map)).perform(click());

        // 2. Wait for OSMDroid's GestureDetector to deliver onSingleTapConfirmed
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // 3. Click the SeekBar at 50 % of its width → ~50 km radius.
        //    A coordinate click fires ACTION_DOWN + ACTION_UP, which triggers
        //    both onStartTrackingTouch and onStopTrackingTouch on the SeekBar,
        //    ensuring selectedEntrantRange is set to a non-zero value.
        onView(withId(R.id.event_location_picker_slider))
                .perform(clickSeekBarAtPercent(50));

        // 4. Confirm — dialog validates marker + range before dismissing
        onView(withId(R.id.event_location_picker_confirm_button)).perform(click());
    }

    /**
     * Returns a {@link ViewAction} that clicks a {@link android.widget.SeekBar}
     * at the given percentage of its total width.
     *
     * <p>Clicking at a coordinate rather than calling {@code setProgress()}
     * programmatically is essential: {@code setProgress()} only fires
     * {@code onProgressChanged} but NOT {@code onStartTrackingTouch} or
     * {@code onStopTrackingTouch}.  The dialog's listener sets
     * {@code selectedEntrantRange} exclusively inside
     * {@code onStopTrackingTouch}, so without a real touch event that field
     * stays at its default value and the confirm button refuses to dismiss.
     *
     * @param percent 0–100; the percentage along the SeekBar's width to click
     */
    private static ViewAction clickSeekBarAtPercent(final int percent) {
        return new GeneralClickAction(
                Tap.SINGLE,
                view -> {
                    int[] xy = new int[2];
                    view.getLocationOnScreen(xy);
                    float xClick = xy[0] + (view.getWidth() * percent / 100f);
                    float yClick = xy[1] + view.getHeight() / 2f;
                    return new float[]{xClick, yClick};
                },
                Press.FINGER,
                0,
                0
        );
    }

    /**
     * Opens a time picker via the given button ID, advances it by
     * {@code minutesAhead} minutes through the fragment back-channel, and
     * confirms with "OK".
     *
     * <p>Use this instead of the plain "OK" confirmation in tests where the
     * selected time must pass the "registration start must not be in the past"
     * validation.
     *
     * @param buttonId     view ID of the button that opens the picker
     * @param minutesAhead minutes in the future to advance the picker
     */
    private void selectUpcomingTime(int buttonId, int minutesAhead) {
        onView(withId(buttonId)).perform(scrollTo(), click());

        scenario.onActivity(activity -> {
            MaterialTimePicker picker =
                    (MaterialTimePicker) activity.getSupportFragmentManager()
                            .findFragmentByTag("TIME_PICKER");

            assertNotNull("MaterialTimePicker fragment must be visible", picker);

            Calendar future = Calendar.getInstance();
            future.add(Calendar.MINUTE, minutesAhead);

            picker.setHour(future.get(Calendar.HOUR_OF_DAY));
            picker.setMinute(future.get(Calendar.MINUTE));
        });

        onView(withText("OK")).perform(click());
    }

    /**
     * Creates a minimal 50×50 ARGB PNG in the app's cache directory and
     * returns a content {@link Uri} served through
     * {@link androidx.core.content.FileProvider}.
     *
     * @return a {@code content://} URI the stubbed intent can return as a result
     * @throws Exception if bitmap compression or URI resolution fails
     */
    private Uri createTestImageUri() throws Exception {
        android.content.Context ctx = ApplicationProvider.getApplicationContext();

        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(
                50, 50, android.graphics.Bitmap.Config.ARGB_8888);

        java.io.File imageDir = new java.io.File(ctx.getCacheDir(), "images");
        if (!imageDir.exists() && !imageDir.mkdirs()) {
            throw new IllegalStateException("Could not create cache/images directory");
        }

        java.io.File imageFile = new java.io.File(imageDir, "espresso_test_image.png");

        try (java.io.FileOutputStream out = new java.io.FileOutputStream(imageFile)) {
            bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
        }

        return androidx.core.content.FileProvider.getUriForFile(
                ctx,
                ctx.getPackageName() + ".fileprovider",
                imageFile
        );
    }

    /**
     * Convenience wrapper: wraps a {@link Date} in a {@link Calendar} using
     * the device's default time zone.
     *
     * @param date the date to wrap
     * @return a {@link Calendar} set to that date
     */
    private static Calendar calFrom(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }
    private void scrollFieldIntoView(int viewId) {
        scenario.onActivity(activity -> {
            androidx.core.widget.NestedScrollView scrollView =
                    activity.findViewById(R.id.OrganizerCreateEventScrollView);
            View target = activity.findViewById(viewId);

            if (scrollView == null || target == null) {
                throw new AssertionError("ScrollView or target view not found");
            }

            int y = target.getTop();
            View parent = (View) target.getParent();

            while (parent != null && parent != scrollView) {
                y += parent.getTop();
                if (!(parent.getParent() instanceof View)) {
                    break;
                }
                parent = (View) parent.getParent();
            }

            int finalY = Math.max(y - 100, 0);
            scrollView.post(() -> scrollView.smoothScrollTo(0, finalY));
        });

        onView(isRoot()).perform(waitFor(300));
    }
    private static ViewAction waitFor(long millis) {
        return new ViewAction() {
            @Override
            public org.hamcrest.Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "Wait for " + millis + " milliseconds.";
            }

            @Override
            public void perform(androidx.test.espresso.UiController uiController, View view) {
                uiController.loopMainThreadForAtLeast(millis);
            }
        };
    }
    private void enterTextAfterScroll(int viewId, String text) {
        scrollFieldIntoView(viewId);
        onView(withId(viewId))
                .perform(replaceText(text), closeSoftKeyboard());
    }

    private void clickAfterScroll(int viewId) {
        scrollFieldIntoView(viewId);
        onView(withId(viewId)).perform(click());
    }
    private void deleteUploadedImageFromCloudinary(String imageUrl) throws Exception {
        if (imageUrl == null || imageUrl.isEmpty() || "No Image".equals(imageUrl)) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        final String[] publicId = {null};
        final String[] storedUrl = {null};
        final Throwable[] lookupFailure = {null};

        db.collection(FirestoreCollections.IMAGES_COLLECTION)
                .whereEqualTo("URL", imageUrl)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    try {
                        if (!query.isEmpty()) {
                            DocumentSnapshot doc = query.getDocuments().get(0);
                            publicId[0] = doc.getId();          // file name / Cloudinary public ID
                            storedUrl[0] = doc.getString("URL");
                        }
                    } catch (Exception e) {
                        lookupFailure[0] = e;
                    } finally {
                        latch.countDown();
                    }
                })
                .addOnFailureListener(e -> {
                    lookupFailure[0] = e;
                    latch.countDown();
                });

        if (!latch.await(15, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timed out looking up image in IMAGES collection");
        }

        if (lookupFailure[0] != null) {
            throw new RuntimeException("Failed to look up image metadata", lookupFailure[0]);
        }

        if (publicId[0] == null || publicId[0].isEmpty()) {
            return;
        }

        Map result = MediaManager.get()
                .getCloudinary()
                .uploader()
                .destroy(publicId[0], ObjectUtils.emptyMap());

        if (!"ok".equals(result.get("result"))) {
            throw new RuntimeException(
                    "Cloudinary destroy failed for publicId=" + publicId[0]
                            + ", url=" + storedUrl[0]
                            + ", result=" + result
            );
        }

        CountDownLatch deleteDocLatch = new CountDownLatch(1);
        final Throwable[] deleteFailure = {null};

        db.collection(FirestoreCollections.IMAGES_COLLECTION)
                .document(publicId[0])
                .delete()
                .addOnSuccessListener(unused -> deleteDocLatch.countDown())
                .addOnFailureListener(e -> {
                    deleteFailure[0] = e;
                    deleteDocLatch.countDown();
                });

        if (!deleteDocLatch.await(15, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timed out deleting image document from Firestore");
        }

        if (deleteFailure[0] != null) {
            throw new RuntimeException("Failed to delete image document from Firestore", deleteFailure[0]);
        }
    }
    private void addImageToImagesCollection(String imageUrl) throws Exception {
        if (imageUrl == null || imageUrl.isEmpty() || "No Image".equals(imageUrl)) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] failure = {null};

        db.collection(FirestoreCollections.IMAGES_COLLECTION)
                .whereEqualTo("URL", imageUrl)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        latch.countDown();
                        return;
                    }

                    Map<String, Object> imageData = new HashMap<>();
                    imageData.put("URL", imageUrl);

                    db.collection(FirestoreCollections.IMAGES_COLLECTION)
                            .add(imageData)
                            .addOnSuccessListener(unused -> latch.countDown())
                            .addOnFailureListener(e -> {
                                failure[0] = e;
                                latch.countDown();
                            });
                })
                .addOnFailureListener(e -> {
                    failure[0] = e;
                    latch.countDown();
                });

        if (!latch.await(15, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timed out adding image to IMAGES collection");
        }

        if (failure[0] != null) {
            throw new RuntimeException("Failed to add image to IMAGES collection", failure[0]);
        }
    }
}