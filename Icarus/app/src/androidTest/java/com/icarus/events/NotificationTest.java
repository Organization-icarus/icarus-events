package com.icarus.events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.widget.ListView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

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
 * Instrumented UI tests for {@link UserNotificationsActivity}.
 * <p>
 * User Stories Tested:
 * US 01.04.01 – As an entrant, I want to receive notification when I am chosen
 * to participate from the waiting list (when I "win" the lottery).
 * <p>
 * US 01.04.02 – As an entrant, I want to receive notification of when I am not chosen
 * on the app (when I "lose" the lottery).
 * <p>
 * US 02.05.01 – As an organizer, I want to send a notification to chosen entrants
 * to sign up for events. This is the notification that they "won" the lottery.
 *
 * Editor: Yifan Jiao
 * Tests use temporary Firestore collections through {@link FirestoreCollections#startTest()}
 * so they do not affect production data.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class NotificationTest {

    private ActivityScenario<UserNotificationsActivity> scenario;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private User testUser;

    /**
     * Sets up a test user in Firestore and stores that user in {@link UserSession}.
     *
     * @throws InterruptedException if Firestore setup waits are interrupted
     */
    @Before
    public void setupFirestoreData() throws InterruptedException {
        FirestoreCollections.startTest();

        CountDownLatch userLatch = new CountDownLatch(1);

        Map<String, Object> entrant = new HashMap<>();
        entrant.put("name", "Test Entrant");
        entrant.put("email", "entrant@email.com");
        entrant.put("isAdmin", false);
        entrant.put("phone", "123456789");

        db.collection("users_test")
                .add(entrant)
                .addOnSuccessListener(doc -> {
                    testUser = new User(
                            doc.getId(),
                            "Test Entrant",
                            "entrant@email.com",
                            "123456789",
                            "No Image",
                            false,
                            new ArrayList<>(),
                            new ArrayList<>(),
                            new HashMap<>(),
                            new HashMap<>()
                    );
                    UserSession.getInstance().setCurrentUser(testUser);
                    userLatch.countDown();
                });

        userLatch.await();
    }

    /**
     * Inserts a single notification document into the test notifications collection.
     *
     * @param eventId event id
     * @param eventName event name
     * @param sender sender id
     * @param message notification message
     * @param type notification type
     * @throws InterruptedException if Firestore insertion waits are interrupted
     */
    private void insertNotification(
            String eventId,
            String eventName,
            String sender,
            String message,
            String type
    ) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ArrayList<String> recipients = new ArrayList<>();
        recipients.add(testUser.getId());

        Map<String, Object> notification = new HashMap<>();
        notification.put("eventId", eventId);
        notification.put("eventName", eventName);
        notification.put("eventImage", "");
        notification.put("sender", sender);
        notification.put("isEvent", true);
        notification.put("isSystem", false);
        notification.put("recipients", recipients);
        notification.put("message", message);
        notification.put("type", type);
        notification.put("date", new Date());

        db.collection("notifications_test")
                .add(notification)
                .addOnSuccessListener(doc -> latch.countDown())
                .addOnFailureListener(e -> latch.countDown());

        latch.await();
    }

    /**
     * Waits until the notifications ListView is populated.
     *
     * @param minCount minimum number of items expected
     * @param timeoutMs max time to wait
     * @throws InterruptedException if interrupted while waiting
     */
    private void waitForListViewItems(int minCount, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < timeoutMs) {
            final int[] count = {0};

            scenario.onActivity(activity -> {
                ListView listView = activity.findViewById(R.id.notifications_list_view);
                if (listView != null && listView.getAdapter() != null) {
                    count[0] = listView.getAdapter().getCount();
                }
            });

            if (count[0] >= minCount) {
                return;
            }

            Thread.sleep(100);
        }

        throw new AssertionError("Notification list never populated with at least " + minCount + " item(s)");
    }



    /**
     * Tests that an entrant can see a notification when chosen from the waiting list.
     * <p>
     * User Story Tested:
     * US 01.04.01 – As an entrant, I want to receive notification when I am chosen
     * to participate from the waiting list (when I "win" the lottery).
     *
     * @throws InterruptedException if Firestore/UI waits are interrupted
     */
    @Test
    public void testChosenFromWaitlistNotificationDisplayed() throws InterruptedException {
        String message = "You have been chosen from the waiting list to participate in this event.";

        insertNotification(
                "event_1",
                "Lottery Event",
                "system",
                message,
                "lottery_win"
        );

        scenario = ActivityScenario.launch(UserNotificationsActivity.class);



        waitForListViewItems(1, 5000);

        onView(withText(message))
                .check(matches(isDisplayed()));
    }

    /**
     * Tests that an entrant can see a notification when not chosen in the lottery.
     * <p>
     * User Story Tested:
     * US 01.04.02 – As an entrant, I want to receive notification of when I am not chosen
     * on the app (when I "lose" the lottery).
     *
     * @throws InterruptedException if Firestore/UI waits are interrupted
     */
    @Test
    public void testNotChosenNotificationDisplayed() throws InterruptedException {
        String message = "You were not chosen for this event.";

        insertNotification(
                "event_2",
                "Lottery Event",
                "system",
                message,
                "lottery_lose"
        );

        scenario = ActivityScenario.launch(UserNotificationsActivity.class);



        waitForListViewItems(1, 5000);

        onView(withText(message))
                .check(matches(isDisplayed()));
    }

    /**
     * Tests that a winner notification sent by an organizer is displayed to the entrant.
     * <p>
     * User Story Tested:
     * US 02.05.01 – As an organizer, I want to send a notification to chosen entrants
     * to sign up for events. This is the notification that they "won" the lottery.
     *
     * @throws InterruptedException if Firestore/UI waits are interrupted
     */
    @Test
    public void testOrganizerSentWinnerNotificationDisplayedToEntrant() throws InterruptedException {
        String message = "You won the lottery. Please sign up for the event.";

        insertNotification(
                "event_3",
                "Organizer Event",
                "system",
                message,
                "organizer_winner_notification"
        );

        scenario = ActivityScenario.launch(UserNotificationsActivity.class);


        waitForListViewItems(1, 5000);

        onView(withText(message))
                .check(matches(isDisplayed()));
    }

    /**
     * Cleans up test state after each test.
     *
     * @throws InterruptedException if cleanup waits are interrupted
     */
    @After
    public void cleanupFirestoreData() throws InterruptedException {
        if (scenario != null) {
            scenario.close();
        }

        UserSession.getInstance().clear();
        FirestoreCollections.endTest();
    }
}