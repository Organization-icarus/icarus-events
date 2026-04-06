package com.icarus.events;

import android.widget.ListView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

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

    @Before
    public void setupFirestoreData() throws InterruptedException {
        FirestoreCollections.startTest();

        CountDownLatch userLatch = new CountDownLatch(1);

        Map<String, Object> entrant = new HashMap<>();
        entrant.put("name", "Test Entrant");
        entrant.put("email", "entrant@email.com");
        entrant.put("isAdmin", false);
        entrant.put("phone", "123456789");

        db.collection(FirestoreCollections.USERS_COLLECTION)
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
                })
                .addOnFailureListener(e -> {
                    throw new AssertionError("Failed to create test user: " + e.getMessage());
                });

        userLatch.await();
    }

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

        db.collection(FirestoreCollections.NOTIFICATIONS_COLLECTION)
                .add(notification)
                .addOnSuccessListener(doc -> latch.countDown())
                .addOnFailureListener(e -> {
                    throw new AssertionError("Failed to insert notification: " + e.getMessage());
                });

        latch.await();

        CountDownLatch syncLatch = new CountDownLatch(1);
        db.waitForPendingWrites()
                .addOnSuccessListener(unused -> syncLatch.countDown())
                .addOnFailureListener(e -> syncLatch.countDown());
        syncLatch.await();
    }

    private void assertNotificationInserted() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final int[] count = {0};
        final String[] error = {null};

        db.collection(FirestoreCollections.NOTIFICATIONS_COLLECTION)
                .whereArrayContains("recipients", testUser.getId())
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(result -> {
                    count[0] = result.size();
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    error[0] = e.getMessage();
                    latch.countDown();
                });

        latch.await();

        if (error[0] != null) {
            throw new AssertionError("Notification query failed: " + error[0]);
        }

        if (count[0] < 1) {
            throw new AssertionError("Inserted notification was not found in Firestore");
        }
    }

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

    private void assertNotificationMessageInList(String expectedMessage) {
        scenario.onActivity(activity -> {
            if (activity.isFinishing()) {
                throw new AssertionError("UserNotificationsActivity finished immediately");
            }

            User currentUser = UserSession.getInstance().getCurrentUser();
            if (currentUser == null) {
                throw new AssertionError("UserSession currentUser is null inside activity");
            }

            ListView listView = activity.findViewById(R.id.notifications_list_view);
            if (listView == null) {
                throw new AssertionError("notifications_list_view is null");
            }

            if (listView.getAdapter() == null) {
                throw new AssertionError("notifications_list_view adapter is null");
            }

            boolean found = false;
            for (int i = 0; i < listView.getAdapter().getCount(); i++) {
                Object item = listView.getAdapter().getItem(i);
                if (item instanceof NotificationItem) {
                    NotificationItem notificationItem = (NotificationItem) item;
                    if (expectedMessage.equals(notificationItem.getMessage())) {
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                throw new AssertionError("Expected notification message not found in adapter: " + expectedMessage);
            }
        });
    }

    /**
     * Tests that an entrant can see a notification when chosen from the waiting list.
     * <p>
     * User Story Tested:
     * US 01.04.01 – As an entrant, I want to receive notification when I am chosen
     * to participate from the waiting list (when I "win" the lottery).
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

        assertNotificationInserted();

        Thread.sleep(1000);
        scenario = ActivityScenario.launch(UserNotificationsActivity.class);
        Thread.sleep(2500);

        waitForListViewItems(1, 10000);
        assertNotificationMessageInList(message);
    }

    /**
     * Tests that an entrant can see a notification when not chosen in the lottery.
     * <p>
     * User Story Tested:
     * US 01.04.02 – As an entrant, I want to receive notification of when I am not chosen
     * on the app (when I "lose" the lottery).
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

        assertNotificationInserted();

        Thread.sleep(500);
        scenario = ActivityScenario.launch(UserNotificationsActivity.class);
        Thread.sleep(1500);

        waitForListViewItems(1, 5000);
        assertNotificationMessageInList(message);
    }

    /**
     * Tests that a winner notification sent by an organizer is displayed to the entrant.
     * <p>
     * User Story Tested:
     * US 02.05.01 – As an organizer, I want to send a notification to chosen entrants
     * to sign up for events. This is the notification that they "won" the lottery.
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

        assertNotificationInserted();

        Thread.sleep(500);
        scenario = ActivityScenario.launch(UserNotificationsActivity.class);
        Thread.sleep(1500);

        waitForListViewItems(1, 5000);
        assertNotificationMessageInList(message);
    }

    @After
    public void cleanupFirestoreData() throws InterruptedException {
        if (scenario != null) {
            scenario.close();
        }

        UserSession.getInstance().clear();
        FirestoreCollections.endTest();
    }
}