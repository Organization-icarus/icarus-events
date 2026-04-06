package com.icarus.events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertFalse;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Instrumented UI tests for {@link UserSettingsActivity}.
 * <p>
 * User Stories Tested:
 *     US 01.02.04 – As an entrant, I want to delete my profile if I no longer wish to use the app.
 *     US 01.04.03 – As an entrant, I want to opt out of receiving notifications from organizers and admins.
 * <p>
 * Tests use temporary Firestore test collections to avoid interfering with
 * production data.
 *
 * @author Kito Lee Son,  Updated by Alex Alves for project pt 4
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserSettingsTest {

    private ActivityScenario<UserSettingsActivity> scenario;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private User testUser;

    /**
     * Sets up a test user in Firestore and sets it as the current user
     * in the session before each test.
     *
     * @throws InterruptedException if the Firestore insert wait is interrupted
     */
    @Before
    public void setupTestUser() throws InterruptedException {

        FirestoreCollections.startTest();

        CountDownLatch latch = new CountDownLatch(1);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", "Test Entrant");
        userMap.put("isAdmin", false);
        userMap.put("email", "testentrant@email.com");
        userMap.put("phone", "1234567890");

        db.collection(FirestoreCollections.USERS_COLLECTION)
                .add(userMap)
                .addOnSuccessListener(doc -> {
                    testUser = new User(doc.getId(), "Test Entrant",
                            "testentrant@email.com", "1234567890", "No Image",
                            false, new ArrayList<>(), new ArrayList<>(),
                            Map.of("adminNotifications", true, "organizerNotifications", true),
                            null);
                    UserSession.getInstance().setCurrentUser(testUser);
                    latch.countDown();
                });

        latch.await();

        scenario = ActivityScenario.launch(UserSettingsActivity.class);
    }

    /**
     * Tests that an entrant can opt out of receiving notifications
     * from admins and organizers.
     * <p>
     * The test toggles each notification switch and verifies
     * that Firestore reflects the change.
     * <p>
     * User Story Tested:
     *     US 01.04.03 – As an entrant, I want to opt out of receiving notifications from organizers and admins.
     *
     * @throws InterruptedException if the Firestore update wait is interrupted
     */
    @Test
    public void testToggleNotifications() throws InterruptedException {
        // Click admin notifications toggle
        onView(withId(R.id.user_settings_admin_notifications_switch))
                .perform(click());

        // Wait for Firestore update
        final boolean[] adminNotifications = {true};
        long adminStart = System.currentTimeMillis();
        while (System.currentTimeMillis() - adminStart < 5000 && adminNotifications[0]) {
            CountDownLatch adminLatch = new CountDownLatch(1);

            db.collection(FirestoreCollections.USERS_COLLECTION)
                    .document(testUser.getId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        Boolean value = doc.getBoolean("adminNotifications");
                        adminNotifications[0] = value != null && value;
                        adminLatch.countDown();
                    })
                    .addOnFailureListener(e -> adminLatch.countDown());

            adminLatch.await();
            if (adminNotifications[0]) {
                Thread.sleep(100);
            }
        }
        assertFalse("Admin notifications were not turned off", adminNotifications[0]);

        // Click organizer notifications toggle
        onView(withId(R.id.user_settings_org_notifications_switch))
                .perform(click());

        // Wait for Firestore update
        final boolean[] organizerNotifications = {true};
        long organizerStart = System.currentTimeMillis();
        while (System.currentTimeMillis() - organizerStart < 5000 && organizerNotifications[0]) {
            CountDownLatch organizerLatch = new CountDownLatch(1);

            db.collection(FirestoreCollections.USERS_COLLECTION)
                    .document(testUser.getId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        Boolean value = doc.getBoolean("organizerNotifications");
                        organizerNotifications[0] = value != null && value;
                        organizerLatch.countDown();
                    })
                    .addOnFailureListener(e -> organizerLatch.countDown());

            organizerLatch.await();
            if (organizerNotifications[0]) {
                Thread.sleep(100);
            }
        }
        assertFalse("Organizer notifications were not turned off", organizerNotifications[0]);
    }

    /**
     * Tests that an entrant can delete their profile from the settings page.
     * <p>
     * The test clicks the delete profile button and verifies that the
     * user document is removed from Firestore.
     * <p>
     * User Story Tested:
     *     US 01.02.04 – As an entrant, I want to delete my profile if I no longer wish to use the app.
     *
     * @throws InterruptedException if the Firestore deletion wait is interrupted
     */
    @Test
    public void testDeleteProfile() throws InterruptedException {
        // Click the delete profile button
        onView(withId(R.id.user_profile_delete_button))
                .perform(click());

        // Wait for Firestore deletion
        final boolean[] userExists = {true};
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 5000 && userExists[0]) {
            CountDownLatch latch = new CountDownLatch(1);

            db.collection(FirestoreCollections.USERS_COLLECTION)
                    .document(testUser.getId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        userExists[0] = doc.exists();
                        latch.countDown();
                    })
                    .addOnFailureListener(e -> latch.countDown());

            latch.await();
            if (userExists[0]) {
                Thread.sleep(100);
            }
        }

        // Assert that the user document no longer exists
        assertFalse("User was not deleted from Firestore", userExists[0]);
    }

    /**
     * Cleans up Firestore test data and closes the activity scenario
     * after each test.
     *
     * @throws InterruptedException if the cleanup wait is interrupted
     */
    @After
    public void cleanup() throws InterruptedException {

        if (scenario != null) scenario.close();
        FirestoreCollections.endTest();
    }
}