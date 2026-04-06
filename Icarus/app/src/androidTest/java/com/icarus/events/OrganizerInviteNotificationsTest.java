package com.icarus.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented UI tests for invitation-specific notifications.
 * <p>
 * This class validates that the system generates correct notification records
 * when an organizer interacts with specific users (Private Invites and Co-Organizers).
 * </p>
 * US 01.05.06: Entrant receives notification for private event invitation.
 * US 01.09.01: Entrant receives notification for co-organizer invitation.
 *
 * @author Kito Lee Son
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerInviteNotificationsTest {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String eventId;
    private final String targetUserId = "invited_user_123";
    private final String organizerId = "main_org_id";

    /**
     * Sets up the test environment.
     * Redirects to test collections and initializes a dummy event.
     */
    @Before
    public void setup() throws InterruptedException {
        FirestoreCollections.startTest();

        // Create a dummy organizer session
        User organizer = new User(organizerId, "Main Org", "org@test.com", "555", null, false,
                new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new HashMap<>());
        UserSession.getInstance().setCurrentUser(organizer);

        // Create a private test event
        CountDownLatch latch = new CountDownLatch(1);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("name", "Private Secret Event");
        eventData.put("isPrivate", true);
        eventData.put("organizers", new ArrayList<>(Collections.singletonList(organizerId)));

        db.collection("events_test").add(eventData).addOnSuccessListener(doc -> {
            eventId = doc.getId();
            latch.countDown();
        });
        latch.await();
    }

    /**
     * Verifies US 01.05.06: Entrant receives notification for private event invitation.
     * <p>
     * Logic: Manually triggers a private invitation notification and verifies its
     * existence in the test database.
     * </p>
     */
    @Test
    public void testPrivateEventInvitationNotification() throws InterruptedException {
        NotificationItem inviteNotification = new NotificationItem(
                eventId,
                "Private Secret Event",
                "no_image",
                organizerId,
                true,
                new ArrayList<>(Collections.singletonList(targetUserId)),
                "You have been invited to join the waitlist for a private event!",
                "invited" // Change from "private_invite" to "invited"
        );

        inviteNotification.sendNotification(null);

        // Verify using the updated type
        verifyNotificationInDb("invited");
    }

    /**
     * Verifies US 01.09.01: Entrant receives notification for co-organizer invitation.
     * <p>
     * Logic: Constructs a notification of type 'promotion' (standard for role changes)
     * and verifies record creation in the notifications_test collection.
     * </p>
     */
    @Test
    public void testCoOrganizerInvitationNotification() throws InterruptedException {
        NotificationItem coOrgNotification = new NotificationItem(
                eventId,
                "Private Secret Event",
                "no_image",
                organizerId,
                true,
                new ArrayList<>(Collections.singletonList(targetUserId)),
                "You have been invited to be a co-organizer!",
                "promotion" // Changed to "promotion"
        );

        coOrgNotification.sendNotification(null);

        verifyNotificationInDb("promotion");
    }

    /**
     * Helper to verify notification record exists in Firestore.
     * This version performs a broader search if the specific type query fails,
     * helping to identify if the document exists but under a different field name.
     * * @param expectedType The notification type string (e.g., "invited", "promotion").
     */
    private void verifyNotificationInDb(String expectedType) throws InterruptedException {
        CountDownLatch verifyLatch = new CountDownLatch(1);
        final boolean[] found = {false};

        // Standard latency wait for Firestore writes
        Thread.sleep(3000);

        db.collection("notifications_test")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String actualType = doc.getString("type");

                        // Check if this is the type we are looking for
                        if (expectedType.equals(actualType)) {
                            // Check all possible recipient field names
                            List<String> recipients = (List<String>) doc.get("recipients");
                            if (recipients == null) recipients = (List<String>) doc.get("receivers");
                            if (recipients == null) recipients = (List<String>) doc.get("recipientIds");

                            if (recipients != null && recipients.contains(targetUserId)) {
                                found[0] = true;
                            }
                        }
                    }
                    verifyLatch.countDown();
                })
                .addOnFailureListener(e -> verifyLatch.countDown());

        assertTrue("Timed out waiting for " + expectedType + " notification",
                verifyLatch.await(10, TimeUnit.SECONDS));
        assertTrue("Notification of type " + expectedType + " not found for user " + targetUserId, found[0]);
    }

    /**
     * Cleans up test data and restores production references.
     */
    @After
    public void tearDown() throws InterruptedException {
        FirestoreCollections.endTest();
    }
}