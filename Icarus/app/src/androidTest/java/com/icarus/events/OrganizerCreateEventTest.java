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
 * <p>This suite exercises the full event-creation flow via Espresso, from
 * entering form data through to verifying persisted Firestore documents. Each
 * test redirects writes to a temporary Firestore collection (managed by
 * {@link FirestoreCollections#startTest()} / {@link FirestoreCollections#endTest()})
 * so that production data is never affected.
 *
 * <h2>User Stories Covered</h2>
 * <ul>
 *   <li><strong>US 02.01.04</strong> – As an organizer, I want to set a
 *       registration period (open and close dates).</li>
 *   <li><strong>US 02.03.01</strong> – As an organizer, I want to optionally
 *       limit the number of entrants who can join my waiting list.</li>
 *   <li><strong>US 02.02.03</strong> – As an organizer, I want to enable or
 *       disable the geolocation requirement for my event.</li>
 * </ul>
 *
 * <h2>Key Design Decisions</h2>
 * <ul>
 *   <li><strong>Scroll-before-interact pattern:</strong> every view interaction
 *       calls {@code scrollTo()} (or the internal {@link #scrollFieldIntoView}
 *       helper) before acting on a view.  The entire form lives inside a
 *       {@code NestedScrollView}; without scrolling first, Espresso dispatches
 *       key events to whichever view currently holds focus — frequently the
 *       nav-bar search field or a completely different input — silently
 *       corrupting test data.</li>
 *   <li><strong>Geolocation dialog must be fully driven:</strong> the confirm
 *       button validates that a map marker has been placed <em>and</em> the
 *       radius SeekBar is non-zero before dismissing the dialog.  Any test that
 *       enables the geolocation switch must call
 *       {@link #interactWithGeolocationDialog()} to drive the dialog to
 *       completion, otherwise all subsequent Espresso interactions will time
 *       out waiting for the dialog to disappear.</li>
 *   <li><strong>Firestore latch timeout:</strong> every {@link CountDownLatch}
 *       waiting on a Firestore result uses a 15-second timeout so that a
 *       missing document fails fast rather than stalling the test runner
 *       indefinitely.</li>
 *   <li><strong>Callback assertion capture:</strong> {@code AssertionError}
 *       instances thrown inside Firestore callbacks are flagged via an
 *       {@code assertionFailed[]} boolean and re-checked on the test thread,
 *       ensuring JUnit registers the failure correctly.</li>
 * </ul>
 *
 * @author Ben Salmon
 * JavaDoc comments written by Cluade, April 4, 2026
 * "Can update all javadoc comments in the following code"
 *
 * Helper functions and test cases were written by ChatGPT, April 4,2026
 * "Can you write me some test cases for my create event activity and any
 * helper functions that are needed"
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerCreateEventTest {

    // -------------------------------------------------------------------------
    //  Shared state
    // -------------------------------------------------------------------------

    /** Live Firestore instance, redirected to the test collection during runs. */
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Scenario handle kept so {@link #cleanup()} can close it unconditionally. */
    private ActivityScenario<OrganizerCreateEventActivity> scenario;

    /**
     * Cloudinary URL of any event poster uploaded during a test run.
     * Stored here so {@link #cleanup()} can delete the remote asset and its
     * corresponding Firestore document after each test.
     */
    private String uploadedEventImageUrl;

    /**
     * Firestore document ID of the event created during the current test run.
     * Set inside Firestore success callbacks and used by {@link #cleanup()} to
     * target teardown operations.
     */
    private String createdEventId;

    // -------------------------------------------------------------------------
    //  Date-picker day constants
    //
    //  All four dates are pinned to April (the month the calendar picker opens
    //  to when tests execute in April 2026).  Keeping them within the same
    //  calendar month avoids the need to navigate forward or backward between
    //  picker selections, which significantly reduces test fragility.
    // -------------------------------------------------------------------------

    /** Day-of-month for the registration open date (April 20). */
    private static final int REG_START_DAY   = 20;

    /** Day-of-month for the registration close date (April 21). */
    private static final int REG_END_DAY     = 21;

    /** Day-of-month for the event start date (April 22). */
    private static final int EVENT_START_DAY = 22;

    /** Day-of-month for the event end date (April 23). */
    private static final int EVENT_END_DAY   = 23;

    // -------------------------------------------------------------------------
    //  Setup / teardown
    // -------------------------------------------------------------------------

    /**
     * Prepares the test environment before each test method.
     *
     * <p>Specifically, this method:
     * <ol>
     *   <li>Initialises the Cloudinary {@link MediaManager} with test
     *       credentials (silently ignored if already initialised).</li>
     *   <li>Calls {@link FirestoreCollections#startTest()} to redirect all
     *       Firestore reads and writes to a temporary test collection,
     *       protecting production data.</li>
     *   <li>Seeds {@link UserSession} with a minimal {@link User} whose ID is
     *       {@code "dummyOrganizerId"} so that the activity can resolve the
     *       current organizer without a real sign-in flow.</li>
     * </ol>
     */
    @Before
    public void setup() {
        FirestoreCollections.startTest();
        Context context = ApplicationProvider.getApplicationContext();

        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "icarus-images");
            config.put("api_key", "291231889216385");
            config.put("api_secret", "ToWWi626oI0M7Ou1pmPQx_vd5x8");
            MediaManager.init(context, config);
        } catch (IllegalStateException ignored) {
            // MediaManager.init() throws if called more than once per process;
            // this is harmless — the existing instance is reused.
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
                new HashMap<>(),
                null
        );
        UserSession.getInstance().setCurrentUser(testUser);
    }

    /**
     * Tears down the test environment after each test method.
     *
     * <p>In order:
     * <ol>
     *   <li>Closes the {@link ActivityScenario} if one was opened.</li>
     *   <li>Deletes any Cloudinary asset whose URL was captured in
     *       {@link #uploadedEventImageUrl}, along with its Firestore
     *       {@code images} document, to avoid accumulating orphaned data.</li>
     *   <li>Calls {@link FirestoreCollections#endTest()} to restore the default
     *       (production) Firestore collection name.</li>
     *   <li>Resets all shared-state fields to {@code null} so that a partially
     *       failed test cannot bleed state into the next one.</li>
     * </ol>
     *
     * @throws Exception if the Cloudinary or Firestore cleanup requests fail
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
     * Smoke test: verifies that every interactive control on the event-creation
     * screen accepts input without throwing, resetting, or routing input to the
     * wrong field.
     *
     * <p>Controls exercised:
     * <ul>
     *   <li>Event title, description, location, and capacity text fields.</li>
     *   <li>Category spinner (selects the first non-placeholder option).</li>
     *   <li>Geolocation toggle — the full map dialog is driven to completion
     *       (place marker → set radius → confirm) because leaving it open would
     *       block all subsequent interactions.</li>
     *   <li>Private-event toggle clicked on then off, confirming it returns to
     *       its default state.</li>
     *   <li>All four date/time pickers via {@link #selectDates()}.</li>
     * </ul>
     *
     * <p>This test does <em>not</em> submit the form or assert Firestore state;
     * it is solely a UI-layer sanity check.
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

        // The capacity field sits inside the Settings card, below the fold;
        // scrollFieldIntoView() is required before Espresso can type into it.
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

        // --- Private-event switch (toggle on then back off) ---
        scrollFieldIntoView(R.id.OrganizerCreateEventPrivateSwitch);
        onView(withId(R.id.OrganizerCreateEventPrivateSwitch)).perform(click());
        onView(withId(R.id.OrganizerCreateEventPrivateSwitch)).perform(click());

        // --- Date / time pickers ---
        selectDates();
    }

    // =========================================================================
    //  2. Full event creation – no image
    // =========================================================================

    /**
     * End-to-end test: fills every form field, omits an event poster, submits
     * the form, and asserts that the resulting Firestore document contains the
     * correct values for all persisted fields.
     *
     * <p>Assertions cover:
     * <ul>
     *   <li>Text fields: {@code name}, {@code description}, and
     *       {@code location} (which the activity normalises to title-case).</li>
     *   <li>Numeric capacity stored as a {@code double}.</li>
     *   <li>Boolean defaults: both {@code geolocation} and {@code isPrivate}
     *       must be {@code false} when neither switch is toggled.</li>
     *   <li>Timestamp day-of-month: each of the four date fields must match the
     *       day constant selected in the picker ({@link #REG_START_DAY},
     *       {@link #REG_END_DAY}, {@link #EVENT_START_DAY},
     *       {@link #EVENT_END_DAY}).</li>
     *   <li>Chronological ordering: registration open &lt; close &lt;= event
     *       start &lt; event end.</li>
     * </ul>
     *
     * <p>User Stories: US 02.01.04 (registration period), US 02.03.01
     * (waiting-list limit).
     *
     * @throws InterruptedException if the Firestore {@link CountDownLatch} is
     *                              interrupted before the query completes
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
                        // Activity uppercases the first character and lowercases the rest.
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
     * Verifies the full image-upload path: selecting a poster from the device
     * gallery triggers a Cloudinary upload, and the resulting Firestore event
     * document stores a non-empty image URL.
     *
     * <p>The system image-picker intent ({@link Intent#ACTION_GET_CONTENT}) is
     * stubbed with Espresso Intents so no real gallery UI appears.  A synthetic
     * 50×50 PNG created in the app cache and served through
     * {@link androidx.core.content.FileProvider} acts as the picker result.
     *
     * <p>Because Cloudinary uploads are asynchronous, the test polls Firestore
     * for up to 30 seconds, checking once per second until the {@code image}
     * field is populated.  This avoids hard-coded sleeps while still bounding
     * the worst-case wait time.
     *
     * @throws Exception if the test image cannot be created, the Cloudinary
     *                   upload fails, or the Firestore latch is interrupted
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

            // Trigger the image picker, which the stub intercepts immediately.
            onView(withId(R.id.OrganizerCreateEventUploadPosterButton))
                    .perform(scrollTo(), click());

            // Sanity-check that the poster ImageView now has a drawable set.
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

            // Poll Firestore until the image URL is populated (max 30 s).
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
     * Verifies that selecting a gallery image causes the poster
     * {@link android.widget.ImageView} to display content other than the
     * default placeholder.
     *
     * <p>This is a pure UI-layer check.  The form is not submitted and
     * Firestore is not inspected.  The test confirms only that the activity
     * calls {@code ImageView.setImageURI()} (or equivalent) with the URI
     * returned by the stubbed picker, resulting in a non-null drawable.
     *
     * @throws Exception if the synthetic test image cannot be created or the
     *                   FileProvider URI cannot be resolved
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
            // the resulting drawable must be non-null.
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
     * Verifies that enabling the geolocation switch and completing the map
     * dialog stores {@code geolocation = true} and a non-null
     * {@code coordinates} GeoPoint in Firestore.
     *
     * <p>The geolocation switch launches a full-screen map dialog that requires
     * three steps before its confirm button will dismiss it:
     * <ol>
     *   <li>Tap the {@code MapView} to place a marker at the tapped location.</li>
     *   <li>Wait ≥ 500 ms for OSMDroid's {@link android.view.GestureDetector}
     *       to fire {@code onSingleTapConfirmed} (internally delayed ~300 ms to
     *       distinguish a single tap from a double-tap).</li>
     *   <li>Click the radius SeekBar at 50 % of its width so that
     *       {@code onStopTrackingTouch} sets {@code selectedEntrantRange} to a
     *       non-zero value.</li>
     * </ol>
     * All three steps are handled by {@link #interactWithGeolocationDialog()}.
     * Skipping any of them causes the confirm button to display a Toast and
     * refuse to close the dialog, timing out all further Espresso interactions.
     *
     * <p>User Story: US 02.02.03 – enable geolocation requirement.
     *
     * @throws InterruptedException if the Firestore {@link CountDownLatch} or
     *                              the dialog-setup sleep is interrupted
     */
    @Test
    public void testGeolocationEnabled() throws InterruptedException {
        final String EVENT_NAME = "Geo Enabled Event";

        scenario = ActivityScenario.launch(buildIntent());

        // Enable geolocation and drive the map dialog to completion.
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
     * Verifies that leaving the geolocation switch in its default-off state
     * stores {@code geolocation = false} and a {@code null} {@code coordinates}
     * field in Firestore.
     *
     * <p>User Story: US 02.02.03 – disable geolocation requirement.
     *
     * @throws InterruptedException if the Firestore {@link CountDownLatch} is
     *                              interrupted before the query completes
     */
    @Test
    public void testGeolocationDisabled() throws InterruptedException {
        final String EVENT_NAME = "Geo Disabled Event";

        scenario = ActivityScenario.launch(buildIntent());

        fillCoreFields(EVENT_NAME, "No geo.", "Exhibition Hall", "50");
        // Geolocation switch deliberately NOT clicked — default state is off.
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
     * Verifies that the numeric value entered in the capacity field is
     * persisted exactly in the Firestore {@code capacity} field.
     *
     * <p>User Story: US 02.03.01 – optionally limit the waiting list.
     *
     * @throws InterruptedException if the Firestore {@link CountDownLatch} is
     *                              interrupted before the query completes
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
     * Verifies that leaving the optional capacity field empty creates an event
     * whose Firestore {@code capacity} field is {@code null}.
     *
     * <p>The field must not silently default to zero, as a capacity of zero
     * would incorrectly bar all entrants from joining the waiting list.
     *
     * @throws InterruptedException if the Firestore {@link CountDownLatch} is
     *                              interrupted before the query completes
     */
    @Test
    public void testEventCreatedWithoutWaitingListLimit() throws InterruptedException {
        final String EVENT_NAME = "No Capacity Event";

        scenario = ActivityScenario.launch(buildIntent());

        // Fill required fields individually, deliberately omitting capacity.
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
     * Verifies that toggling the private-event switch on stores
     * {@code isPrivate = true} in the Firestore event document.
     *
     * @throws InterruptedException if the Firestore {@link CountDownLatch} is
     *                              interrupted before the query completes
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
     * Verifies that leaving the private-event switch in its default-off state
     * stores {@code isPrivate = false} in the Firestore event document.
     *
     * @throws InterruptedException if the Firestore {@link CountDownLatch} is
     *                              interrupted before the query completes
     */
    @Test
    public void testPrivateEventDisabled() throws InterruptedException {
        final String EVENT_NAME = "Public Event";

        scenario = ActivityScenario.launch(buildIntent());

        fillCoreFields(EVENT_NAME, "Open to everyone.", "City Park", "200");
        // Private switch deliberately NOT clicked — default state is off.
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
     * Verifies that submitting the form without any dates selected triggers
     * activity-level validation and does <em>not</em> write a document to
     * Firestore.
     *
     * <p>The test sleeps for 2 seconds after tapping submit to confirm that no
     * asynchronous Firestore write occurs during that window.  This is
     * intentionally conservative: the activity's validation guard should return
     * synchronously before any write is attempted.
     *
     * @throws InterruptedException if the sleep or Firestore latch is
     *                              interrupted
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

        // Submit without any dates — activity should Toast and return early.
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
     * Verifies that submitting the form with name, category, and location all
     * empty triggers activity-level validation and does <em>not</em> write a
     * document to Firestore.
     *
     * <p>Dates are selected to ensure the test reaches the text-field
     * validation branch rather than being blocked earlier by the date check.
     *
     * @throws InterruptedException if the sleep or Firestore latch is
     *                              interrupted
     */
    @Test
    public void testValidationBlocksSubmitWhenRequiredTextFieldsEmpty()
            throws InterruptedException {
        scenario = ActivityScenario.launch(buildIntent());

        // Populate dates only so validation reaches the text-field check.
        selectDates();

        // Submit without name, category, or location.
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
     * Verifies that the activity normalises the location string before saving
     * it to Firestore: the first character is uppercased and all remaining
     * characters are lowercased, regardless of the casing entered by the user.
     *
     * @throws InterruptedException if the Firestore {@link CountDownLatch} is
     *                              interrupted before the query completes
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
     * Verifies that the current user's ID is included in the {@code organizers}
     * array of the Firestore event document created by the activity.
     *
     * <p>The expected ID ({@code "dummyOrganizerId"}) matches the value seeded
     * into {@link UserSession} by {@link #setup()}.
     *
     * @throws InterruptedException if the Firestore {@link CountDownLatch} is
     *                              interrupted before the query completes
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

    /**
     * Constructs an explicit {@link Intent} targeting
     * {@link OrganizerCreateEventActivity} using the instrumentation context.
     *
     * @return a launch {@link Intent} ready for use with
     *         {@link ActivityScenario#launch(Intent)}
     */
    private Intent buildIntent() {
        return new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerCreateEventActivity.class);
    }

    /**
     * Scrolls to and fills the four core text fields, then selects the first
     * non-placeholder option from the category spinner.
     *
     * <p><strong>Why {@code scrollTo()} is mandatory:</strong> the entire form
     * is contained in a {@code NestedScrollView}.  Without scrolling first,
     * Espresso dispatches key events to whichever view currently holds focus —
     * often the nav-bar search field or a different input entirely — causing
     * text to appear in the wrong place.  The capacity field is particularly
     * prone to this because the Settings card that contains it is not visible
     * until the user scrolls past the Location card.
     *
     * @param name        event title text
     * @param description event description text; pass {@code ""} to leave the
     *                    field untouched
     * @param location    venue location text
     * @param capacity    waiting-list limit as a numeric string; pass
     *                    {@code ""} to leave the field empty (no limit)
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
     * Interacts with all eight date/time picker buttons — registration open,
     * registration close, event start, and event end — selecting the day
     * defined by the {@code *_DAY} constants and confirming each dialog with
     * "OK".
     *
     * <p>The registration-start time picker is advanced by 10 minutes into the
     * future via {@link #selectUpcomingTime} so that the activity's "start must
     * not be in the past" validation does not reject the chosen time.  All other
     * time pickers are confirmed with the pre-filled current time.
     *
     * <p>All picker buttons reside in the Dates card near the bottom of the
     * scroll view, so {@code scrollTo()} is called before each interaction.
     */
    private void selectDates() {
        clickAfterScroll(R.id.OrganizerCreateEventRegistrationPeriodStartDate);
        onView(withContentDescription(containsString("April " + REG_START_DAY))).perform(click());
        onView(withText("OK")).perform(click());

        selectUpcomingTime(R.id.OrganizerCreateEventRegistrationPeriodStartTime, 10);

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
     * Drives the geolocation map dialog to a successful confirmation by
     * satisfying its two preconditions:
     * <ol>
     *   <li><strong>Map marker</strong> – taps the {@code MapView} centre to
     *       place a marker at that location.</li>
     *   <li><strong>Entrant radius</strong> – clicks the radius SeekBar at 50 %
     *       of its width, triggering {@code onStopTrackingTouch} and setting
     *       {@code selectedEntrantRange} to a non-zero value.</li>
     * </ol>
     * After both preconditions are met, the confirm button is tapped and the
     * dialog dismisses normally.
     *
     * <p><strong>Map tap timing:</strong> OSMDroid's
     * {@link org.osmdroid.views.overlay.MapEventsOverlay} uses Android's
     * {@link android.view.GestureDetector}, which delays delivery of
     * {@code onSingleTapConfirmed} by ~300 ms to rule out double-taps.  The
     * test sleeps 500 ms after the tap so the main-thread handler finishes
     * before we interact with the confirm button.
     *
     * <p><strong>SeekBar interaction:</strong> calling {@code setProgress()}
     * programmatically only fires {@code onProgressChanged} — it does
     * <em>not</em> trigger {@code onStartTrackingTouch} or
     * {@code onStopTrackingTouch}.  {@link #clickSeekBarAtPercent(int)} uses a
     * coordinate-based {@code ACTION_DOWN → ACTION_UP} touch sequence that
     * fires the full callback chain, which is the only reliable way to update
     * {@code selectedEntrantRange}.
     */
    private void interactWithGeolocationDialog() {
        // Step 1: tap the MapView to place a marker at its centre.
        onView(withId(R.id.event_location_picker_map)).perform(click());

        // Step 2: sleep to allow OSMDroid's GestureDetector to deliver
        // onSingleTapConfirmed on the main thread (~300 ms internal delay).
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // Step 3: click the SeekBar at 50 % width (≈ 50 km radius).
        // A coordinate click fires ACTION_DOWN + ACTION_UP, triggering
        // onStartTrackingTouch and onStopTrackingTouch on the listener,
        // which sets selectedEntrantRange to a non-zero value.
        onView(withId(R.id.event_location_picker_slider))
                .perform(clickSeekBarAtPercent(50));

        // Step 4: confirm — the dialog validates marker + range before closing.
        onView(withId(R.id.event_location_picker_confirm_button)).perform(click());
    }

    /**
     * Returns a {@link ViewAction} that delivers a single tap to a
     * {@link android.widget.SeekBar} at the given percentage of its total
     * rendered width.
     *
     * <p>A programmatic call to {@code SeekBar.setProgress()} only fires
     * {@code onProgressChanged}; it does <em>not</em> invoke
     * {@code onStartTrackingTouch} or {@code onStopTrackingTouch}.  The
     * geolocation dialog's listener updates {@code selectedEntrantRange}
     * exclusively inside {@code onStopTrackingTouch}, so a real touch event
     * that produces the full {@code ACTION_DOWN → ACTION_UP} sequence is
     * required to guarantee the range is set to a non-zero value before the
     * confirm button is tapped.
     *
     * @param percent a value from 0 to 100 indicating how far along the
     *                SeekBar's width to place the tap
     * @return a {@link ViewAction} that performs the coordinate-based click
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
     * Opens the time picker identified by {@code buttonId}, advances the
     * selected time by {@code minutesAhead} minutes via the fragment back-
     * channel, and confirms with "OK".
     *
     * <p>Use this helper instead of a plain "OK" confirmation whenever the
     * chosen time must pass the activity's "registration start must not be in
     * the past" validation.  Directly tapping "OK" accepts the pre-filled
     * current time, which may already be in the past by the time the
     * validation runs.
     *
     * @param buttonId     view ID of the button that opens the time picker
     * @param minutesAhead number of minutes in the future to set on the picker
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
     * returns a {@code content://} URI served through
     * {@link androidx.core.content.FileProvider}.
     *
     * <p>The resulting URI can be returned as the data payload of a stubbed
     * {@link Intent#ACTION_GET_CONTENT} result, simulating a user selecting an
     * image from the device gallery without launching the real picker UI.
     *
     * @return a {@code content://} URI pointing to the generated PNG
     * @throws Exception if the cache directory cannot be created, the bitmap
     *                   cannot be written, or the FileProvider URI cannot be
     *                   resolved for the test package
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
     * Wraps a {@link Date} in a {@link Calendar} set to the device's default
     * time zone.
     *
     * @param date the {@link Date} to wrap
     * @return a {@link Calendar} instance representing the same instant
     */
    private static Calendar calFrom(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }

    /**
     * Scrolls the {@link androidx.core.widget.NestedScrollView} so that the
     * view identified by {@code viewId} is brought into the visible viewport,
     * then waits 300 ms for the smooth-scroll animation to settle.
     *
     * <p>The scroll position is calculated by walking up the view hierarchy
     * from the target view to the root {@code NestedScrollView}, accumulating
     * each ancestor's {@code getTop()} offset.  A 100 px margin is subtracted
     * from the final Y coordinate so the target is not flush against the top
     * edge of the screen.
     *
     * <p>This helper is preferred over Espresso's built-in {@code scrollTo()}
     * action for views nested inside a {@code NestedScrollView}, where
     * {@code scrollTo()} can sometimes fail to scroll far enough.
     *
     * @param viewId the resource ID of the view to scroll into view
     * @throws AssertionError if either the {@code NestedScrollView} or the
     *                        target view cannot be located in the activity's
     *                        layout
     */
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

    /**
     * Returns a {@link ViewAction} that blocks the Espresso UI controller for
     * the given number of milliseconds by looping the main thread.
     *
     * <p>This is preferred over {@link Thread#sleep} inside Espresso
     * interactions because it keeps the main thread alive and responsive,
     * allowing pending {@code Handler} messages — such as OSMDroid's
     * {@code onSingleTapConfirmed} callback — to be delivered during the wait.
     *
     * @param millis duration to wait in milliseconds
     * @return a {@link ViewAction} that idles for the specified duration
     */
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

    /**
     * Scrolls the given view into the visible viewport and then replaces its
     * content with {@code text}, closing the soft keyboard afterwards.
     *
     * <p>Uses {@link #scrollFieldIntoView(int)} rather than Espresso's built-in
     * {@code scrollTo()} to ensure the target is fully visible inside the
     * {@code NestedScrollView} before typing begins.
     *
     * @param viewId the resource ID of the {@link android.widget.EditText} to
     *               type into
     * @param text   the text to enter; replaces any existing content
     */
    private void enterTextAfterScroll(int viewId, String text) {
        scrollFieldIntoView(viewId);
        onView(withId(viewId))
                .perform(replaceText(text), closeSoftKeyboard());
    }

    /**
     * Scrolls the given view into the visible viewport and then performs a
     * click on it.
     *
     * @param viewId the resource ID of the view to click
     */
    private void clickAfterScroll(int viewId) {
        scrollFieldIntoView(viewId);
        onView(withId(viewId)).perform(click());
    }

    /**
     * Deletes a previously uploaded Cloudinary asset and its corresponding
     * Firestore {@code images} document, identified by the asset's public URL.
     *
     * <p>The method:
     * <ol>
     *   <li>Queries the {@code images} Firestore collection for a document
     *       whose {@code URL} field matches {@code imageUrl} to obtain the
     *       Cloudinary public ID (stored as the document ID).</li>
     *   <li>Calls {@link com.cloudinary.Cloudinary#uploader()}.destroy() with
     *       that public ID to remove the asset from Cloudinary.</li>
     *   <li>Deletes the corresponding Firestore document.</li>
     * </ol>
     *
     * <p>If {@code imageUrl} is {@code null}, empty, or equal to
     * {@code "No Image"}, the method returns immediately without making any
     * network calls.
     *
     * @param imageUrl the Cloudinary asset URL to delete; safe to pass
     *                 {@code null} (no-op)
     * @throws Exception if the Firestore lookup, the Cloudinary destroy call,
     *                   or the Firestore document deletion fails, or if any
     *                   latch times out
     */
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
                            publicId[0] = doc.getId();       // document ID == Cloudinary public ID
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
            throw new RuntimeException("Failed to delete image document from Firestore",
                    deleteFailure[0]);
        }
    }

    /**
     * Adds an entry to the Firestore {@code images} collection for the given
     * Cloudinary URL if one does not already exist.
     *
     * <p>This is called after {@link #testImageUploadFromDevice()} confirms the
     * event was saved with a non-empty image URL so that {@link #cleanup()} can
     * later locate the asset by URL and delete it via
     * {@link #deleteUploadedImageFromCloudinary(String)}.
     *
     * <p>If {@code imageUrl} is {@code null}, empty, or equal to
     * {@code "No Image"}, the method returns immediately without contacting
     * Firestore.
     *
     * @param imageUrl the Cloudinary asset URL to register; safe to pass
     *                 {@code null} (no-op)
     * @throws Exception if the Firestore read or write fails, or if the latch
     *                   times out
     */
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
                        // Document already exists — nothing to add.
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