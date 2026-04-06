package com.icarus.events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.Intents.times;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
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
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.cloudinary.android.MediaManager;
import com.cloudinary.utils.ObjectUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Instrumented UI tests for {@link OrganizerManageEventActivity}.
 * <p>
 * User Stories Tested:
 *      US 02.01.01 As an organizer I want to create a new public event and generate a unique
 *                  promotional QR code that links to the event description and event poster
 *                  in the app.
 *      US 02.01.02 As an organizer, I want to create a private event that is not visible on the
 *                  event listing and does not generate a promotional QR code
 *      US 02.01.03 As an organizer, I want to invite specific entrants to a private event’s
 *                  waiting list by searching via name, phone number and/or email.
 *      US 02.04.02 As an organizer I want to update an event poster to provide visual
 *                  information to entrants.
 *      US 02.05.02 As an organizer I want to set the system to sample a specified number
 *                  of attendees to register for the event.
 *      US 02.05.03 As an organizer I want to be able to draw a replacement applicant from
 *                  the pooling system when a previously selected applicant cancels or
 *                  rejects the invitation.
 *      US 02.09.01 As an organizer, I want to assign an entrant as a co-organizer for
 *                  my event, which prevents them from joining the entrant pool for that event.
 *
 * <p>
 * Tests use temporary Firestore collections to avoid affecting production data.
 *
 * @author Ben Salmon
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerManageEventTest {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ActivityScenario<OrganizerManageEventActivity> scenario;
    private String createdEventId;
    /**
     * Sets up Firestore test data before each test.
     * <p>
     * Overrides Firestore collections to test versions, adds
     * a sample event with ID {@code createdEventId}, adds entrants
     * with different statuses ("waiting", "selected", "rejected", "registered"),
     * creates user documents to hold entrant names. Also creates user documents to which are
     * not part of the event
     *
     * @throws InterruptedException if Firestore operations are interrupted
     */
    @Before
    public void setup() throws InterruptedException {
        // Set up a test user in UserSession so sendMessage doesn't crash
        User testUser = new User("testOrganizerId", "Test Organizer", null, null, null,
                null, null, null, null, null);
        UserSession.getInstance().setCurrentUser(testUser);

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
        // Use test collection
        FirestoreCollections.startTest();

        // Create a test event
        CountDownLatch eventLatch = new CountDownLatch(1);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("name", "Test Event");
        eventData.put("description", "Test Description");
        eventData.put("category", "Technology");
        eventData.put("location", "Test Location");
        eventData.put("image", "");
        eventData.put("isPrivate", false);
        eventData.put("geolocation", false);
        eventData.put("capacity", null);
        eventData.put("entrantRange", null);
        eventData.put("coordinates", null);
        eventData.put("open", new Date());
        eventData.put("close", new Date());
        eventData.put("startDate", new Date());
        eventData.put("endDate", new Date());
        eventData.put("organizers", List.of("testOrganizerId", "entrant6"));

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .add(eventData)
                .addOnSuccessListener(docRef -> {
                    createdEventId = docRef.getId();

                    // Add entrants subcollection
                    db.collection(FirestoreCollections.EVENTS_COLLECTION)
                            .document(createdEventId)
                            .collection("entrants")
                            .document("entrant0")
                            .set(Map.of("status", "waiting"));
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
                    db.collection(FirestoreCollections.USERS_COLLECTION).document("entrant0")
                            .set(Map.of("name", "Entrant Zero"));
                    db.collection(FirestoreCollections.USERS_COLLECTION).document("entrant1")
                            .set(Map.of("name", "Entrant One"));
                    db.collection(FirestoreCollections.USERS_COLLECTION).document("entrant2")
                            .set(Map.of("name", "Entrant Two"));
                    db.collection(FirestoreCollections.USERS_COLLECTION).document("entrant3")
                            .set(Map.of("name", "Entrant Three"));
                    //Entrants to add as organizer and private.
                    db.collection(FirestoreCollections.USERS_COLLECTION)
                            .document("entrant4")
                            .set(Map.of(
                                    "name", "Entrant Four",
                                    "isAdmin", true
                            ));
                    db.collection(FirestoreCollections.USERS_COLLECTION).document("entrant5")
                            .set(Map.of("name", "Entrant Five"));
                    db.collection(FirestoreCollections.USERS_COLLECTION).document("entrant6")
                            .set(Map.of("name", "Entrant Six"));

                    eventLatch.countDown();
                });
        eventLatch.await();
    }
    //Test 1
    /**
     * Test to see if the quick share menu appears when creating a QR code and the event is public
     * <p>
     * Launches the {@link OrganizerManageEventActivity} activity, clicks
     * the Share QR code, and generates the QR code
     * <p>
     * User Story Tested:
     *      US 02.01.01 As an organizer I want to create a new public event and generate a unique
     *                  promotional QR code that links to the event description and event poster
     *                  in the app.
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testQRCodeCreated() throws InterruptedException {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerManageEventActivity.class
        );
        intent.putExtra("eventId", createdEventId);

        Intents.init();
        try {
            scenario = ActivityScenario.launch(intent);
            Thread.sleep(2000);

            // Stub the share chooser so it doesn't actually open
            intending(hasAction(Intent.ACTION_CHOOSER))
                    .respondWith(new Instrumentation.ActivityResult(
                            android.app.Activity.RESULT_OK, null));

            clickAfterScroll(R.id.OrganizerManageEventShareQRCode);
            Thread.sleep(1000);

            // Verify the share intent was fired
            intended(hasAction(Intent.ACTION_CHOOSER));

        } finally {
            Intents.release();
        }
    }
    //Test 2
    /**
     * Test to see if the quick share menu appears when creating a QR code and the event is private
     * <p>
     * Launches the {@link OrganizerManageEventActivity} activity, clicks
     * the Share QR code, and verifies that a toast message is produced
     * <p>
     * User Story Tested:
     *      US 02.01.02 As an organizer, I want to create a private event that is not visible on the
     *                  event listing and does not generate a promotional QR code
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testQRCodeFailedToShare() throws InterruptedException {

        CountDownLatch privateLatch = new CountDownLatch(1);
        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(createdEventId)
                .update("isPrivate", true)
                .addOnSuccessListener(unused -> privateLatch.countDown())
                .addOnFailureListener(e -> privateLatch.countDown());
        privateLatch.await(5, TimeUnit.SECONDS);

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerManageEventActivity.class
        );
        intent.putExtra("eventId", createdEventId);

        Intents.init();
        try {
            scenario = ActivityScenario.launch(intent);
            Thread.sleep(2000);

            clickAfterScroll(R.id.OrganizerManageEventShareQRCode);
            Thread.sleep(1000);

            // Verify no ACTION_SEND intent was fired (share sheet never opened)
            intended(hasAction(Intent.ACTION_SEND), times(0));

        } finally {
            Intents.release();
        }
    }
    //Test 3
    /**
     * Test to invite an entrant to a private entrant
     * <p>
     * Launches the {@link OrganizerManageEventActivity} activity, clicks
     * the invite specific entrant button, and verifies that the ListView displays
     * the entrants not in waiting list, oragnizers, or admins. Entrant on list is then invited to
     * the event with status selected
     * <p>
     * User Story Tested:
     *      US 02.01.03 As an organizer, I want to invite specific entrants to a private event’s
     *                  waiting list by searching via name, phone number and/or email.
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testInviteEntrant() throws InterruptedException {

        // Make the event private
        CountDownLatch privateLatch = new CountDownLatch(1);
        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(createdEventId)
                .update("isPrivate", true)
                .addOnSuccessListener(unused -> privateLatch.countDown())
                .addOnFailureListener(e -> privateLatch.countDown());
        privateLatch.await(5, TimeUnit.SECONDS);

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerManageEventActivity.class
        );
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        clickAfterScroll(R.id.OrganizerManageEventInviteEntrant);
        Thread.sleep(2000);

        // Entrant Five should be in the list (not already an entrant/organizer/admin)
        onView(withId(R.id.OrganizerEntrantList))
                .check(matches(hasDescendant(withText("Entrant Five"))));

        // Entrants Zero to Four and Six should NOT appear (already in event or excluded)
        onView(withId(R.id.OrganizerEntrantList))
                .check(matches(not(hasDescendant(withText("Entrant Zero")))));
        onView(withId(R.id.OrganizerEntrantList))
                .check(matches(not(hasDescendant(withText("Entrant One")))));
        onView(withId(R.id.OrganizerEntrantList))
                .check(matches(not(hasDescendant(withText("Entrant Two")))));
        onView(withId(R.id.OrganizerEntrantList))
                .check(matches(not(hasDescendant(withText("Entrant Three")))));
        onView(withId(R.id.OrganizerEntrantList))
                .check(matches(not(hasDescendant(withText("Entrant Four")))));
        onView(withId(R.id.OrganizerEntrantList))
                .check(matches(not(hasDescendant(withText("Entrant Six")))));

        // Select Entrant Five from the list
        onView(withText("Entrant Five")).perform(click());

        // Push confirm
        onView(withId(R.id.OrganizerEntrantConfirmationButton)).perform(click());
        Thread.sleep(2000);

        // Verify Entrant Five is now in the selected list in Firestore
        CountDownLatch verifyLatch = new CountDownLatch(1);
        AtomicReference<Boolean> isSelected = new AtomicReference<>(false);

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(createdEventId)
                .collection("entrants")
                .document("entrant5")
                .get()
                .addOnSuccessListener(doc -> {
                    isSelected.set("selected".equals(doc.getString("status")));
                    verifyLatch.countDown();
                })
                .addOnFailureListener(e -> verifyLatch.countDown());

        verifyLatch.await(5, TimeUnit.SECONDS);
        assertTrue("Entrant Five should have status 'selected'", isSelected.get());
    }
    //Test 4
    /**
     * Tests to upload a new image and verify it exists in cloudinary and firestore
     * <p>
     * Launches the {@link OrganizerManageEventActivity} activity, clicks
     * the "Update Poster"  button, creates a blank image, uploads that image. verifies the
     * image is in the imageview, and in the database.
     *
     * Written by Claude, April 5, 2026 "I need to create a test image. click the upload poster
     * button, check that the image is uploaded to cloudinary, check the imageview displays the
     * image, afterwards I need to remove the image from cloudinary the
     * OrganizerCreateEventTest.java files does this. But has too many steps
     * for what I need to do in this test"
     * <p>
     * User Story Tested:
     *      US 02.04.02 As an organizer I want to update an event poster to provide visual
     *                  information to entrants.
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testUpdateImage() throws Exception {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerManageEventActivity.class
        );
        intent.putExtra("eventId", createdEventId);

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

            scenario = ActivityScenario.launch(intent);

            // Click the update poster button
            onView(withId(R.id.OrganizerManageEventUpdatePoster))
                    .perform(scrollTo(), click());

            // Verify ImageView has updated
            onView(withId(R.id.OrganizerManageEventViewImage))
                    .check((view, noViewFoundException) -> {
                        assertNotNull("ImageView not found", view);
                        ImageView iv = (ImageView) view;
                        assertNotNull("Poster preview should be set", iv.getDrawable());
                    });

            // Poll Firestore until image URL is populated (max 30s)
            final boolean[] foundImage = {false};
            final String[] uploadedUrl = {null};
            long deadline = System.currentTimeMillis() + 30000;

            while (System.currentTimeMillis() < deadline && !foundImage[0]) {
                CountDownLatch queryLatch = new CountDownLatch(1);

                db.collection(FirestoreCollections.EVENTS_COLLECTION)
                        .document(createdEventId)
                        .get()
                        .addOnSuccessListener(doc -> {
                            String imageUrl = doc.getString("image");
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                uploadedUrl[0] = imageUrl;
                                foundImage[0] = true;
                            }
                            queryLatch.countDown();
                        })
                        .addOnFailureListener(e -> queryLatch.countDown());

                queryLatch.await(5, TimeUnit.SECONDS);
                if (!foundImage[0]) Thread.sleep(1000);
            }

            assertTrue("Image URL should be saved to Firestore", foundImage[0]);

            // Cleanup: delete image from Cloudinary
            deleteUploadedImageFromCloudinary(uploadedUrl[0]);

        } finally {
            Intents.release();
        }
    }
    //Test 5
    /**
     * Test to sample a single entrant from the waiting list
     * <p>
     * Launches the {@link OrganizerManageEventActivity} activity, clicks
     * the "sample attendees" button, and sets the number to 1. and samples 1 attendee.
     * Entrant Zero is then added to the selected status list.
     * <p>
     * User Story Tested:
     *      US 02.05.02 As an organizer I want to set the system to sample a specified number
     *                  of attendees to register for the event.
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testSampleAttendees() throws InterruptedException {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerManageEventActivity.class
        );
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        clickAfterScroll(R.id.OrganizerManageEventInviteEntrant);
        Thread.sleep(2000);

        // Set sample count to 1
        onView(withId(R.id.OrganizerSampleAttendeesCountText))
                .perform(replaceText("1"), closeSoftKeyboard());

        // Push sample button
        onView(withId(R.id.OrganizerSampleAttendeesSampleButton)).perform(click());
        Thread.sleep(2000);

        // Check that entrant0 now has status "selected" in Firestore
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> isSelected = new AtomicReference<>(false);

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(createdEventId)
                .collection("entrants")
                .document("entrant0")
                .get()
                .addOnSuccessListener(doc -> {
                    isSelected.set("selected".equals(doc.getString("status")));
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        latch.await(5, TimeUnit.SECONDS);
        assertTrue("Entrant Zero should have status 'selected' after sampling", isSelected.get());
    }
    //Test 6
    /**
     * Test to replace cancelled entrants in an event's entrant subcollection with
     * entrants in the waiting list
     * <p>
     * Launches the {@link OrganizerManageEventActivity} activity, clicks
     * the "replaced declined" button, and verifies entrant zero is sampled
     * <p>
     * User Story Tested:
     *      US 02.05.03 As an organizer I want to be able to draw a replacement applicant from
     *                  the pooling system when a previously selected applicant cancels or
     *                  rejects the invitation.
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testReplacedDeclined() throws InterruptedException {
        // Launch activity with test eventId
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerManageEventActivity.class
        );
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        // Click "Chosen" filter button
        clickAfterScroll(R.id.OrganizerManageEventReplaceDeclined);
        Thread.sleep(2000);

        // Entrant Two should be in the list
        onView(withId(R.id.OrganizerEntrantList))
                .check(matches(hasDescendant(withText("Entrant Two"))));

        onView(withText("Entrant Two")).perform(click());

        onView(withId(R.id.OrganizerEntrantConfirmationButton)).perform(click());
        Thread.sleep(2000);

        // Verify Entrant Five is now in the selected list in Firestore
        CountDownLatch verifyLatch = new CountDownLatch(1);
        AtomicReference<Boolean> isSelected = new AtomicReference<>(false);

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(createdEventId)
                .collection("entrants")
                .document("entrant0")
                .get()
                .addOnSuccessListener(doc -> {
                    isSelected.set("selected".equals(doc.getString("status")));
                    verifyLatch.countDown();
                })
                .addOnFailureListener(e -> verifyLatch.countDown());

        verifyLatch.await(5, TimeUnit.SECONDS);
        assertTrue("Entrant Zero should have status 'selected'", isSelected.get());
    }
    //Test 7
    /**
     * Test to add a entrant to the organizer array of an event
     * <p>
     * Launches the {@link OrganizerManageEventActivity} activity, clicks
     * the "Add Organizers" button, and selects a user from the list and verfies they are
     * added to the event as an organizer
     * <p>
     * User Story Tested:
     *      US 02.09.01 As an organizer, I want to assign an entrant as a co-organizer for
     *                  my event, which prevents them from joining the entrant pool for that event.
     *
     * @throws InterruptedException if Firestore reads or UI operations are interrupted
     */
    @Test
    public void testAddOrganizer() throws InterruptedException {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerManageEventActivity.class
        );
        intent.putExtra("eventId", createdEventId);
        scenario = ActivityScenario.launch(intent);

        clickAfterScroll(R.id.OrganizerManageEventAddOrganizer);
        Thread.sleep(2000);

        // Entrant Five should be in the list (not already an entrant/organizer/admin)
        onView(withId(R.id.OrganizerEntrantList))
                .check(matches(hasDescendant(withText("Entrant Five"))));

        // Entrants Zero to Four and Six should NOT appear (already in event or excluded)
        onView(withId(R.id.OrganizerEntrantList))
                .check(matches(not(hasDescendant(withText("Entrant Zero")))));
        onView(withId(R.id.OrganizerEntrantList))
                .check(matches(not(hasDescendant(withText("Entrant One")))));
        onView(withId(R.id.OrganizerEntrantList))
                .check(matches(not(hasDescendant(withText("Entrant Two")))));
        onView(withId(R.id.OrganizerEntrantList))
                .check(matches(not(hasDescendant(withText("Entrant Three")))));
        onView(withId(R.id.OrganizerEntrantList))
                .check(matches(not(hasDescendant(withText("Entrant Four")))));
        onView(withId(R.id.OrganizerEntrantList))
                .check(matches(not(hasDescendant(withText("Entrant Six")))));

        // Select Entrant Five from the list
        onView(withText("Entrant Five")).perform(click());

        // Push confirm
        onView(withId(R.id.OrganizerEntrantConfirmationButton)).perform(click());
        Thread.sleep(2000);

        // Verify entrant5 is now in the organizers array of the event
        CountDownLatch verifyLatch = new CountDownLatch(1);
        AtomicReference<Boolean> isOrganizer = new AtomicReference<>(false);

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(createdEventId)
                .get()
                .addOnSuccessListener(doc -> {
                    @SuppressWarnings("unchecked")
                    java.util.List<String> organizers =
                            (java.util.List<String>) doc.get("organizers");
                    if (organizers != null) {
                        isOrganizer.set(organizers.contains("entrant5"));
                    }
                    verifyLatch.countDown();
                })
                .addOnFailureListener(e -> verifyLatch.countDown());

        verifyLatch.await(5, TimeUnit.SECONDS);
        assertTrue("Entrant Five should be in the organizers array", isOrganizer.get());
    }

    /**
     * Removes test data created during the test run from Firestore.
     * <p>
     * Deletes the test entrants from the event's {@code entrants} subcollection,
     * removes the corresponding user documents, deletes any notifications associated
     * with the created event, and finally deletes the event document itself.
     * <p>
     * This method is executed after each test to ensure the database is cleaned up
     * and does not contain leftover test data.
     *
     * @throws InterruptedException if the cleanup wait is interrupted
     */
    @After
    public void cleanup() throws InterruptedException {
        FirestoreCollections.endTest();
    }
    /**
     * Scrolls to a view inside the activity and clicks it.
     * <p>
     * First scrolls the target view into the visible area of the
     * {@link OrganizerManageEventActivity} scroll view, then performs
     * a click action on that view.
     *
     * @param viewId the resource ID of the view to scroll to and click
     */
    private void clickAfterScroll(int viewId) {
        scrollFieldIntoView(viewId);
        onView(withId(viewId)).perform(click());
    }
    /**
     * Scrolls the activity so the target view is visible on screen.
     * <p>
     * Finds the target view inside the
     * {@link OrganizerManageEventActivity} scroll view, calculates its
     * vertical position relative to the parent
     * {@link androidx.core.widget.NestedScrollView}, and smoothly scrolls
     * to bring it into view.
     *
     * @param viewId the resource ID of the view to scroll into view
     */
    private void scrollFieldIntoView(int viewId) {
        scenario.onActivity(activity -> {
            androidx.core.widget.NestedScrollView scrollView =
                    activity.findViewById(R.id.OrganizerManageEventScrollView);
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
     * Creates a temporary blank image file for use in poster upload tests.
     * <p>
     * Generates a 50x50 bitmap, writes it as a PNG file in the app cache
     * directory, and returns a content {@link Uri} using the app's
     * {@link androidx.core.content.FileProvider}.
     *
     * @return a content URI pointing to the temporary test image file
     * @throws Exception if the image file cannot be created or written
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
     * Deletes an uploaded image from Cloudinary and removes its Firestore record.
     * <p>
     * Looks up the image document in the
     * {@link FirestoreCollections#IMAGES_COLLECTION} collection using the stored
     * image URL, retrieves the Cloudinary public ID from the document ID,
     * deletes the image from Cloudinary, and then deletes the matching image
     * document from Firestore.
     *
     * @param imageUrl the uploaded image URL stored on the event document
     * @throws Exception if the image lookup, Cloudinary deletion, or Firestore
     *                   document deletion fails
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
     * Creates a custom Espresso {@link ViewAction} that waits for a fixed amount of time.
     * <p>
     * This is used to pause test execution briefly to allow UI updates,
     * scrolling, or asynchronous operations to complete before continuing.
     *
     * @param millis the number of milliseconds to wait
     * @return a {@link ViewAction} that delays execution on the main thread
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
}