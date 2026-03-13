package com.icarus.events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.junit.Assert.assertEquals;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

/**
 * Instrumented UI tests for {@link UserRegistrationActivity}.
 * <p>
 *  * User Stories Tested:
 *  *      US 01.07.01 As an entrant I want to be identified by my device,
 *  *      so that I don't have to use a username and password.
 * <p>
 * Tests use a temporary Firestore collection to prevent
 * interference with production user data.
 *
 * @author Kito Lee Son
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserRegistrationTest {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ActivityScenario<UserRegistrationActivity> scenario;
    private final String deviceId = "test_device_123";

    /**
     * Prepares the test environment before each test.
     * <p>
     * This method redirects the application to use a temporary
     * Firestore collection for users so that test data does not
     * interfere with production data.
     */
    @Before
    public void setup() {
        FirestoreCollections.USERS_COLLECTION = "users_test";
    }

    /**
     * Tests that a user can register using their device ID.
     * <p>
     * The test launches the {@link UserRegistrationActivity},
     * enters a name, selects a role, and presses the register button.
     * It then verifies that a Firestore document is created using
     * the device ID as the document identifier.
     * <p>
     * User Story Tested:
     *     US 01.07.01 As an entrant I want to be identified by my device,
     *     so that I don't have to use a username and password.
     *
     * @throws InterruptedException if the Firestore retrieval wait is interrupted
     */
    @Test
    public void testRegistrationByDeviceId() throws InterruptedException {

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                UserRegistrationActivity.class
        );

        intent.putExtra("deviceId", deviceId);

        scenario = ActivityScenario.launch(intent);

        // enter name
        onView(withId(R.id.user_register_name_field))
                .perform(typeText("Test User"), closeSoftKeyboard());

        // select entrant role
        onView(withText("Entrant")).perform(click());

        // press register
        onView(withId(R.id.user_register_button)).perform(click());

        // verify Firestore document
        CountDownLatch latch = new CountDownLatch(1);
        final String[] name = {""};

        db.collection("users_test")
                .document(deviceId)
                .get()
                .addOnSuccessListener(doc -> {
                    name[0] = doc.getString("name");
                    latch.countDown();
                });

        latch.await();

        assertEquals("Test User", name[0]);
    }

    /**
     * Restores the Firestore collection configuration after tests complete.
     * <p>
     * This ensures that the application returns to using the
     * default production user collection.
     */
    @After
    public void teardown() {
        FirestoreCollections.USERS_COLLECTION = "users";
    }
}