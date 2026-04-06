package com.icarus.events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

import android.content.Intent;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.cloudinary.android.MediaManager;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Instrumented UI tests for {@link EventDetailsActivity}.
 * <p>
 * User Stories Covered:
 * <ul>
 *   <li>US 01.01.01 As an entrant, I want to join the waiting list for a specific event.</li>
 *   <li>US 01.05.02 As an entrant, I want to be able to accept the invitation to register/sign up when chosen to participate in an event.</li>
 *   <li>US 01.05.03 As an entrant, I want to be able to decline an invitation when chosen to participate in an event.</li>
 *   <li>US 01.05.04 As an entrant, I want to know how many total entrants are on the waiting list for an event.</li>
 *   <li>US 01.05.05 As an entrant, I want to be informed about the criteria or guidelines for the lottery selection process.</li>
 *   <li>US 01.06.01 As an entrant, I want to view event details within the app by scanning the promotional QR code.</li>
 *   <li>US 01.06.02 As an entrant, I want to be able to sign up for an event from the event details.</li>
 *   <li>US 01.08.02 As an entrant, I want to view comments on an event.</li>
 * </ul>
 *
 * Tests use temporary Firestore collections via {@link FirestoreCollections#startTest()}
 * and {@link FirestoreCollections#endTest()}.
 *
 * @authors Kito Leeson, Bradley Bravender
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventDetailsTest {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String organizerId;
    private String entrantId;
    private String event1Id;
    private String event2Id;
    private String event3Id;

    private ActivityScenario<EventDetailsActivity> scenario;

    /**
     * Prepares Firestore test data before each test.
     * <p>
     * This method:
     * <ul>
     *   <li>Switches Firestore into test collections</li>
     *   <li>Initializes Cloudinary support used by the app startup path</li>
     *   <li>Creates one organizer and one entrant</li>
     *   <li>Creates three test events</li>
     *   <li>Creates entrant subcollection documents needed for selected/waiting scenarios</li>
     * </ul>
     *
     * @throws InterruptedException if waiting for Firestore setup is interrupted
     */
    @Before
    public void setupTestData() throws InterruptedException {
        FirestoreCollections.startTest();

        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", "icarus-images");
            config.put("api_key", "291231889216385");
            config.put("api_secret", "ToWWi626oI0M7Ou1pmPQx_vd5x8");
            MediaManager.init(ApplicationProvider.getApplicationContext(), config);
        } catch (IllegalStateException e) {
            // MediaManager already initialized.
        }

        CountDownLatch organizerLatch = new CountDownLatch(1);
        db.collection(FirestoreCollections.USERS_COLLECTION)
                .add(Map.of(
                        "name", "Test Organizer",
                        "isAdmin", false
                ))
                .addOnSuccessListener(doc -> {
                    organizerId = doc.getId();
                    organizerLatch.countDown();
                });
        organizerLatch.await();

        CountDownLatch entrantLatch = new CountDownLatch(1);
        db.collection(FirestoreCollections.USERS_COLLECTION)
                .add(Map.of(
                        "name", "Test Entrant",
                        "isAdmin", false,
                        "events", new ArrayList<>()
                ))
                .addOnSuccessListener(doc -> {
                    entrantId = doc.getId();

                    User testUser = new User(
                            entrantId,
                            "Test Entrant",
                            "test@email.com",
                            "1234567890",
                            "No Image",
                            false,
                            new ArrayList<>(),
                            new ArrayList<>(),
                            new HashMap<>(),
                            null
                    );

                    UserSession.getInstance().setCurrentUser(testUser);
                    entrantLatch.countDown();
                });
        entrantLatch.await();

        CountDownLatch eventLatch = new CountDownLatch(3);

        ArrayList<String> organizers = new ArrayList<>();
        organizers.add(organizerId);

        Date now = new Date();
        Date oneHourLater = new Date(now.getTime() + 60L * 60L * 1000L);
        Date twoHoursLater = new Date(now.getTime() + 2L * 60L * 60L * 1000L);

        Map<String, Object> event1 = new HashMap<>();
        event1.put("name", "Test Event 1");
        event1.put("capacity", 20.0);
        event1.put("category", "Music");
        event1.put("description", "Description 1");
        event1.put("open", now);
        event1.put("close", oneHourLater);
        event1.put("startDate", oneHourLater);
        event1.put("endDate", twoHoursLater);
        event1.put("location", "Edmonton");
        event1.put("image", "");
        event1.put("geolocation", false);
        event1.put("organizers", organizers);

        Map<String, Object> event2 = new HashMap<>(event1);
        event2.put("name", "Test Event 2");
        event2.put("description", "Description 2");

        Map<String, Object> event3 = new HashMap<>(event1);
        event3.put("name", "Test Event 3");
        event3.put("description", "Description 3");

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .add(event1)
                .addOnSuccessListener(doc -> {
                    event1Id = doc.getId();
                    eventLatch.countDown();
                });

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .add(event2)
                .addOnSuccessListener(doc -> {
                    event2Id = doc.getId();
                    eventLatch.countDown();
                });

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .add(event3)
                .addOnSuccessListener(doc -> {
                    event3Id = doc.getId();
                    eventLatch.countDown();
                });

        eventLatch.await();

        CountDownLatch entrantSetupLatch = new CountDownLatch(4);

        Map<String, Object> selected = Map.of("status", "selected");
        Map<String, Object> waiting = Map.of("status", "waiting");

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(event1Id)
                .collection("entrants")
                .document(entrantId)
                .set(selected)
                .addOnSuccessListener(v -> entrantSetupLatch.countDown());

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(event2Id)
                .collection("entrants")
                .document(entrantId)
                .set(selected)
                .addOnSuccessListener(v -> entrantSetupLatch.countDown());

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(event1Id)
                .collection("entrants")
                .document("waitingUser1")
                .set(waiting)
                .addOnSuccessListener(v -> entrantSetupLatch.countDown());

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(event2Id)
                .collection("entrants")
                .document("waitingUser2")
                .set(waiting)
                .addOnSuccessListener(v -> entrantSetupLatch.countDown());

        entrantSetupLatch.await();
    }

    /**
     * Tests that the event details page displays the core event information.
     * <p>
     * Verifies that the event name, description, and RecyclerView-backed details
     * are visible after the activity loads.
     * <p>
     * User Stories Tested:
     * <ul>
     *   <li>US 01.06.01 As an entrant, I want to view event details within the app by scanning the promotional QR code.</li>
     * </ul>
     *
     * @throws InterruptedException if the UI wait is interrupted
     */
    @Test
    public void testEventDetailsDisplayCoreData() throws InterruptedException {
        launchEventDetails(event1Id);

        onView(withId(R.id.eventName))
                .check(matches(withText("Test Event 1")));

        onView(withId(R.id.eventDescription))
                .check(matches(withText("Description 1")));

        onView(withId(R.id.event_details_event_list))
                .check(matches(hasDescendant(withText("Category"))));

        onView(withId(R.id.event_details_event_list))
                .check(matches(hasDescendant(withText("Music"))));
    }

    /**
     * Tests that the waiting-list count is displayed through the event details adapter.
     * <p>
     * Binds the adapter directly and verifies the waiting-list field text.
     * This is more stable than assuming a fixed on-screen row index.
     * <p>
     * User Stories Tested:
     * <ul>
     *   <li>US 01.05.04 As an entrant, I want to know how many total entrants are on the waiting list for an event.</li>
     * </ul>
     */
    @Test
    public void testWaitingListDisplayed() {
        Date now = new Date();
        ArrayList<String> organizers = new ArrayList<>();
        organizers.add(organizerId);

        Event event = new Event(
                event1Id,
                "Adapter Event",
                "Music",
                20.0,
                now,
                now,
                now,
                now,
                "Edmonton",
                "",
                organizers,
                "selected",
                1
        );

        EventDetailsAdapter adapter =
                new EventDetailsAdapter(ApplicationProvider.getApplicationContext(), event);

        FrameLayout parent = new FrameLayout(ApplicationProvider.getApplicationContext());
        EventDetailsAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 1);

        TextView fieldName = holder.itemView.findViewById(R.id.field_name);
        TextView fieldValue = holder.itemView.findViewById(R.id.field_value);

        assertEquals("Waiting List", fieldName.getText().toString());
        assertEquals("1/20", fieldValue.getText().toString());
    }

    /**
     * Tests that an entrant can accept an invitation to register.
     * <p>
     * Launches the activity for an event where the entrant is currently
     * marked as {@code selected}, triggers the register button directly from the
     * activity, and verifies that Firestore updates the entrant status to
     * {@code registered}.
     * <p>
     * User Stories Tested:
     * <ul>
     *   <li>US 01.05.02 As an entrant, I want to be able to accept the invitation to register/sign up when chosen to participate in an event.</li>
     * </ul>
     *
     * @throws InterruptedException if Firestore waiting is interrupted
     */
    @Test
    public void testAcceptInvitation() throws InterruptedException {
        launchEventDetails(event1Id);

        scenario.onActivity(activity ->
                activity.findViewById(R.id.register_button).performClick()
        );

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Thread.sleep(500);

        CountDownLatch latch = new CountDownLatch(1);
        final String[] status = {""};

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(event1Id)
                .collection("entrants")
                .document(entrantId)
                .get()
                .addOnSuccessListener(doc -> {
                    status[0] = doc.getString("status");
                    latch.countDown();
                });

        latch.await();
        assertEquals("registered", status[0]);
    }

    /**
     * Tests that an entrant can decline an invitation.
     * <p>
     * Launches the activity for an event where the entrant is currently
     * marked as {@code selected}, triggers the decline button directly from the
     * activity, and verifies that Firestore updates the entrant status to
     * {@code rejected}.
     * <p>
     * User Stories Tested:
     * <ul>
     *   <li>US 01.05.03 As an entrant, I want to be able to decline an invitation when chosen to participate in an event.</li>
     * </ul>
     *
     * @throws InterruptedException if Firestore waiting is interrupted
     */
    @Test
    public void testDeclineInvitation() throws InterruptedException {
        launchEventDetails(event2Id);

        scenario.onActivity(activity ->
                activity.findViewById(R.id.decline_invitation).performClick()
        );

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Thread.sleep(500);

        CountDownLatch latch = new CountDownLatch(1);
        final String[] status = {""};

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(event2Id)
                .collection("entrants")
                .document(entrantId)
                .get()
                .addOnSuccessListener(doc -> {
                    status[0] = doc.getString("status");
                    latch.countDown();
                });

        latch.await();
        assertEquals("rejected", status[0]);
    }

    /**
     * Tests that the lottery guidelines dialog can be opened from the event details page.
     * <p>
     * Verifies that tapping the lottery-guidelines button shows the expected message.
     * <p>
     * User Stories Tested:
     * <ul>
     *   <li>US 01.05.05 As an entrant, I want to be informed about the criteria or guidelines for the lottery selection process.</li>
     * </ul>
     *
     * @throws InterruptedException if the UI wait is interrupted
     */
    @Test
    public void testLotteryGuidelinesDisplayed() throws InterruptedException {
        launchEventDetails(event1Id);

        scenario.onActivity(activity ->
                activity.findViewById(R.id.lottery_guidelines).performClick()
        );

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Thread.sleep(500);

        String expectedMessage = ApplicationProvider.getApplicationContext()
                .getString(R.string.lottery_guidelines_message);

        onView(withText(expectedMessage))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
    }

    /**
     * Tests that an entrant can join the waiting list from the event details page.
     * <p>
     * Launches an event where the entrant does not yet have an entrant record,
     * triggers the join-waitlist button directly from the activity, and verifies
     * that Firestore creates a waiting entrant document.
     * <p>
     * User Stories Tested:
     * <ul>
     *   <li>US 01.01.01 As an entrant, I want to join the waiting list for a specific event.</li>
     *   <li>US 01.06.02 As an entrant, I want to be able to sign up for an event from the event details.</li>
     * </ul>
     *
     * @throws InterruptedException if Firestore waiting is interrupted
     */
    @Test
    public void testEntrantSignUpForEvent() throws InterruptedException {
        launchEventDetails(event3Id);

        scenario.onActivity(activity ->
                activity.findViewById(R.id.join_waiting_list_button).performClick()
        );

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Thread.sleep(500);

        CountDownLatch latch = new CountDownLatch(1);
        final String[] status = {""};

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(event3Id)
                .collection("entrants")
                .document(entrantId)
                .get()
                .addOnSuccessListener(doc -> {
                    status[0] = doc.getString("status");
                    latch.countDown();
                });

        latch.await();
        assertEquals("waiting", status[0]);
    }

    /**
     * Tests that an entrant can open the event comments screen from the event details page.
     * <p>
     * Verifies that tapping the comments button launches the comments activity
     * and displays the comments RecyclerView.
     * <p>
     * User Stories Tested:
     * <ul>
     *   <li>US 01.08.02 As an entrant, I want to view comments on an event.</li>
     * </ul>
     *
     * @throws InterruptedException if the UI wait is interrupted
     */
    @Test
    public void testCommentsButtonOpensCommentActivity() throws InterruptedException {
        launchEventDetails(event1Id);

        scenario.onActivity(activity ->
                activity.findViewById(R.id.comments_button).performClick()
        );

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Thread.sleep(1000);

        onView(withId(R.id.event_comments_list))
                .check(matches(isDisplayed()));
    }

    /**
     * Launches {@link EventDetailsActivity} for the supplied event ID and waits briefly
     * for Firestore-backed UI state to settle.
     *
     * @param eventId Firestore document ID of the test event to open
     * @throws InterruptedException if the wait is interrupted
     */
    private void launchEventDetails(String eventId) throws InterruptedException {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EventDetailsActivity.class
        );
        intent.putExtra("eventId", eventId);
        scenario = ActivityScenario.launch(intent);

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Thread.sleep(2500);
    }

    /**
     * Cleans up the active activity scenario and restores normal Firestore collection names.
     *
     * @throws InterruptedException if Firestore cleanup is interrupted
     */
    @After
    public void cleanup() throws InterruptedException {
        if (scenario != null) {
            scenario.close();
        }
        FirestoreCollections.endTest();
    }
}