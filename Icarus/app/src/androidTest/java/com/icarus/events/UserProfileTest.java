package com.icarus.events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
 * Instrumented UI tests for {@link UserProfileActivity}.
 * <p>
 * User Stories Tested:
 *     US 01.02.02 As an entrant I want to update information such as name, email and contact information on my profile
 * <p>
 * Tests use a temporary Firestore collection ({@code events_test}) to avoid
 * interfering with production data.
 *
 * @author Kito Lee Son
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserProfileTest {

    private ActivityScenario<UserProfileActivity> scenario;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private User testUser;

    /**
     * Sets up test Firestore data and launches the {@link UserProfileActivity}.
     * <p>
     * Inserts a test entrant user into Firestore and sets the session user.
     *
     * @throws InterruptedException if Firestore setup waits are interrupted
     */
    @Before
    public void setupFirestoreData() throws InterruptedException {

        FirestoreCollections.startTest();

        CountDownLatch latch = new CountDownLatch(1);
        Map<String, Object> entrant = new HashMap<>();
        entrant.put("name", "Test Entrant");
        entrant.put("email", "entrant@email.com");
        entrant.put("isAdmin", false);
        entrant.put("phone", "123456789");

        db.collection("users_test")
                .add(entrant)
                .addOnSuccessListener((doc) -> {
                    testUser = new User(
                            doc.getId(),
                            "Test Entrant",
                            "entrant@email.com",
                            "123456789",
                            "",
                            false,
                            new ArrayList<>(),
                            new ArrayList<>(),
                            new HashMap<>()
                    );
                    UserSession.getInstance().setCurrentUser(testUser);
                    latch.countDown();
                });

        latch.await(); // wait until Firestore document is created

        scenario = ActivityScenario.launch(UserProfileActivity.class);
    }

    /**
     * Tests that an entrant can update their profile information.
     * <p>
     * Simulates user editing name, email, and contact info in the UI,
     * and verifies that Firestore is updated correctly.
     * <p>
     * User Story Tested:
     *     US 01.02.02 – As an entrant, I want to update information such as
     *     name, email, and contact information on my profile.
     *
     * @throws InterruptedException if Firestore verification waits are interrupted
     */
    @Test
    public void testUpdateProfile() throws InterruptedException {
        // Click edit button
        onView(withId(R.id.user_profile_edit_confirm_button))
                .perform(click());

        // Simulate UI input changes
        onView(withId(R.id.user_profile_name_edit))
                .perform(clearText(), replaceText("Updated Name"));
        onView(withId(R.id.user_profile_email_edit))
                .perform(clearText(), replaceText("updated@email.com"));
        onView(withId(R.id.user_profile_phone_edit))
                .perform(clearText(), replaceText("987654321"));

        // Click save button
        onView(withId(R.id.user_profile_edit_confirm_button))
                .perform(click());

        // Verify Firestore updated
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] updated = {false};

        db.collection("users_test")
                .document(testUser.getId())
                .get()
                .addOnSuccessListener(doc -> {
                    if(doc.exists()){
                        assertEquals("Updated Name", doc.getString("name"));
                        assertEquals("updated@email.com", doc.getString("email"));
                        assertEquals("987654321", doc.getString("phone"));
                        updated[0] = true;
                    }
                    latch.countDown();
                });

        latch.await();
        assertTrue("User profile was not updated correctly", updated[0]);
    }

    /**
     * Cleans up all test Firestore data after each test.
     * <p>
     * Deletes test entrant users from {@code users_test} collection to
     * avoid polluting the database.
     *
     * @throws InterruptedException if Firestore cleanup waits are interrupted
     */
    @After
    public void cleanupFirestoreData() throws InterruptedException {

        if (scenario != null) scenario.close();
        FirestoreCollections.endTest();
    }
}