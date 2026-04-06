package com.icarus.events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
 * @author Kito Lee Son, Updated by Alex Alves for project pt 4
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
        FirestoreCollections.startTest();
    }

    /**
     * Tests that a user can register using their device ID.
     * <p>
     * The test launches the {@link UserRegistrationActivity},
     * enters the registration fields shown on the current screen and presses
     * the register button.
     * It then verifies that a Firestore document is created using
     * the device ID as the document identifier.
     * <p>
     * User Story Tested:
     *     US 01.07.01 As an entrant I want to be identified by my device,
     *     so that I don't have to use a username and password.
     *
     *     US 01.02.01 – As an entrant, I want to provide my personal information such as name,
     *     email and optional phone number in the app.
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

        // enter registration fields shown on the current screen
        onView(withId(R.id.user_register_name_field))
                .perform(replaceText("Test User"), closeSoftKeyboard());

        onView(withId(R.id.user_register_email_field))
                .perform(replaceText("testuser@example.com"), closeSoftKeyboard());

        onView(withId(R.id.user_register_phone_field))
                .perform(replaceText("7805551234"), closeSoftKeyboard());

        // press register
        onView(withId(R.id.user_register_button))
                .perform(click());
        // verify Firestore document
        CountDownLatch latch = new CountDownLatch(1);
        final String[] name = {null};
        final String[] email = {null};
        final String[] phone = {null};

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 5000 && name[0] == null) {
            CountDownLatch readLatch = new CountDownLatch(1);
            db.collection(FirestoreCollections.USERS_COLLECTION)
                    .document(deviceId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            name[0] = doc.getString("name");
                            email[0] = doc.getString("email");
                            phone[0] = doc.getString("phone");
                        }
                        readLatch.countDown();
                    })
                    .addOnFailureListener(e -> readLatch.countDown());

            readLatch.await();
            if (name[0] == null) {
                Thread.sleep(100);
            }
        }
        latch.countDown();
        latch.await();

        assertEquals("Test User", name[0]);
        assertEquals("testuser@example.com", email[0]);
        assertEquals("7805551234", phone[0]);
    }

    /**
     * Verifies that an entrant can provide personal information in the
     * registration screen, including name, email, and an optional phone number.
     * <p>
     * User Story Tested:
     *     US 01.02.01 – As an entrant, I want to provide my personal
     *     information such as name, email and optional phone number in the app.
     *
     * @throws InterruptedException if asynchronous Firestore operations are interrupted
     */
    @Test
    public void testPersonalInformationOptionalPhone() throws InterruptedException {
        String deviceId = "test_device_optional_phone";

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), UserRegistrationActivity.class);
        intent.putExtra("deviceId", deviceId);
        scenario = ActivityScenario.launch(intent);

        onView(withId(R.id.user_register_name_field))
                .perform(replaceText("Optional Phone User"), closeSoftKeyboard());

        onView(withId(R.id.user_register_email_field))
                .perform(replaceText("optional@example.com"), closeSoftKeyboard());

        onView(withId(R.id.user_register_phone_field))
                .perform(replaceText(""), closeSoftKeyboard());

        onView(withId(R.id.user_register_button))
                .perform(click());

        final String[] name = {null};
        final String[] email = {null};
        final String[] phone = {null};

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 5000 && name[0] == null) {
            CountDownLatch readLatch = new CountDownLatch(1);
            db.collection(FirestoreCollections.USERS_COLLECTION)
                    .document(deviceId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            name[0] = doc.getString("name");
                            email[0] = doc.getString("email");
                            phone[0] = doc.getString("phone");
                        }
                        readLatch.countDown();
                    })
                    .addOnFailureListener(e -> readLatch.countDown());

            readLatch.await();
            if (name[0] == null) {
                Thread.sleep(100);
            }
        }

        assertEquals("Optional Phone User", name[0]);
        assertEquals("optional@example.com", email[0]);
        assertTrue(phone[0] == null || phone[0].isEmpty());
    }

    /**
     * Restores the Firestore collection configuration after tests complete.
     * <p>
     * This ensures that the application returns to using the
     * default production user collection.
     */
    @After
    public void teardown() throws InterruptedException {
        FirestoreCollections.endTest();
    }
}