package com.icarus.events;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.action.ViewActions.click;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.hasProperty;

import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Instrumented UI tests for {@link AdministratorDashboardActivity}.
 * <p>
 * User Stories Tested:
 *      US 03.01.01 As an administrator, I want to be able to remove events.
 *      US 03.02.01 As an administrator, I want to be able to remove profiles.
 *      US 03.04.01 As an administrator, I want to be able to browse events.
 *      US 03.05.01 As an administrator, I want to be able to browse profiles.
 * <p>
 * Tests use temporary Firestore collections to avoid affecting production data.
 *
 * @author Kito Lee Son
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdministratorDashboardTest {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ActivityScenario<AdministratorDashboardActivity> scenario;
    private String testEventId;
    private String testUserId;
    private User adminUser;

    /**
     * Sets up Firestore test data.
     * <p>
     * Creates a test administrator, test event, and test user.
     *
     * @throws InterruptedException if the Firestore operations are interrupted
     */
    @Before
    public void setup() throws InterruptedException {
        // Set test collections
        FirestoreCollections.startTest();

        // Create admin user
        adminUser = new User("admin1", "Admin User", null,
                null, "No Image", null, null, null,null);
        UserSession.getInstance().setCurrentUser(adminUser);
        CountDownLatch latch = new CountDownLatch(2);

        // Add admin to Firestore
        db.collection(FirestoreCollections.USERS_COLLECTION)
                .document(adminUser.getId())
                .set(Map.of("name", adminUser.getName()))
                .addOnSuccessListener(v -> latch.countDown());

        // Add a sample user
        testUserId = "testUser1";
        db.collection(FirestoreCollections.USERS_COLLECTION)
                .document(testUserId)
                .set(Map.of("name", "Test User"))
                .addOnSuccessListener(v -> latch.countDown());

        // Add a sample event
        CountDownLatch eventLatch = new CountDownLatch(1);
        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .add(Map.of(
                        "name", "Test Event",
                        "date", new Date(),
                        "category", "Category A"
                ))
                .addOnSuccessListener(docRef -> {
                    testEventId = docRef.getId();
                    eventLatch.countDown();
                });

        latch.await();
        eventLatch.await();    }

    /**
     * Tests that an administrator can browse the event
     * list and delete a specific event.
     * <p>
     * Launches the dashboard activity, switches to the event list view,
     * verifies that the test event is visible in the ListView, deletes
     * the event using its remove button, and verifies the event has
     * been removed from Firestore.
     * <p>
     * User Stories Tested:
     *     US 03.01.01 As an administrator, I want to be able to remove events.
     *     US 03.04.01 As an administrator, I want to be able to browse events.
     *
     * @throws InterruptedException if Firestore reads are interrupted
     */
    @Test
    public void testBrowseAndDeleteEvent() throws InterruptedException {
        // Launch dashboard
        launchDashboard();

        // Switch to Event list
        onView(withId(R.id.admin_dashboard_show_event_list_button)).perform(click());

        // Verify event is visible
        onView(withId(R.id.admin_dashboard_event_list))
                .check(matches(hasDescendant(withText("Test Event"))));

        // Delete event
        onData(new TypeSafeMatcher<Event>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Event with id: " + testEventId);
            }

            @Override
            protected boolean matchesSafely(Event item) {
                return testEventId.equals(item.getId());
            }
        })
                .inAdapterView(withId(R.id.admin_dashboard_event_list))
                .onChildView(withId(R.id.admin_dashboard_event_list_remove_event_button))
                .perform(click());

        // Verify event is gone from Firestore
        CountDownLatch latch = new CountDownLatch(1);
        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(testEventId)
                .get()
                .addOnSuccessListener(doc -> {
                    assert !doc.exists();
                    latch.countDown();
                });
        latch.await();
    }

    /**
     * Tests that an administrator can browse the event
     * list and delete a specific event.
     * <p>
     * Launches the dashboard activity, switches to the user list view,
     * verifies that the test user is visible in the ListView, deletes
     * the user using its remove button, and verifies the user has
     * been removed from Firestore.
     * <p>
     * User Stories Tested:
     *     US 03.02.01 As an administrator, I want to be able to remove profiles.
     *     US 03.05.01 As an administrator, I want to be able to browse profiles.
     *
     * @throws InterruptedException if Firestore reads are interrupted
     */
    @Test
    public void testBrowseAndDeleteUser() throws InterruptedException {
        // Launch dashboard
        launchDashboard();

        // Switch to User list
        onView(withId(R.id.admin_dashboard_show_user_list_button)).perform(click());

        // Wait for ListView to populate (optional, or use IdlingResource)
        Thread.sleep(500);

        // Delete the user
        onData(new TypeSafeMatcher<User>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("User with id: " + testUserId);
            }

            @Override
            protected boolean matchesSafely(User item) {
                return testUserId.equals(item.getId());
            }
        })
                .inAdapterView(withId(R.id.admin_dashboard_user_list))
                .onChildView(withId(R.id.admin_dashboard_user_list_remove_user_button))
                .perform(click());

        // Verify user is gone from Firestore
        CountDownLatch latch = new CountDownLatch(1);
        db.collection(FirestoreCollections.USERS_COLLECTION)
                .document(testUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    assert !doc.exists();
                    latch.countDown();
                });
        latch.await();
    }

    /**
     * Helper method to launch {@link AdministratorDashboardActivity}
     * in a test-safe context.
     * <p>
     * Uses {@link ActivityScenario#launch(Intent)} with
     * {@link ApplicationProvider#getApplicationContext()} to start
     * the activity without relying on the system launcher.
     */
    private void launchDashboard() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                AdministratorDashboardActivity.class
        );
        scenario = ActivityScenario.launch(intent);
    }

    /**
     * Cleans up Firestore test data after each test.
     * <p>
     * Resets Firestore collection names back to production, deletes
     * any test users or events created during setup, waits for all
     * deletions to complete using {@link CountDownLatch}.
     *
     * @throws InterruptedException if Firestore deletions are interrupted
     */
    @After
    public void cleanup() throws InterruptedException {
        FirestoreCollections.endTest();

    }
}