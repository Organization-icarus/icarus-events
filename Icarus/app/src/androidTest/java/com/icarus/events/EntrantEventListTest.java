package com.icarus.events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.ViewAssertion;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Instrumented UI tests for {@link EntrantEventListActivity}.
 * <p>
 * User Stories Tested:
 *      US 01.01.03 – As an entrant, I want to be able to see a list
 *      of events that I can join the waiting list for.
 *      US 01.01.04 – As an entrant, I want to filter events based on
 *      my interests and availability.
 * <p>
 * Tests use temporary Firestore test collections and test-only category
 * documents to avoid interfering with production event and user data.
 *
 * @author Kito Lee Son, Updated by Alex Alves for project pt 4
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EntrantEventListTest {

    private ActivityScenario<EntrantEventListActivity> scenario;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<String> insertedEventIds = new ArrayList<>();
    private final List<String> insertedUserIds = new ArrayList<>();
    private final List<String> insertedCategoryDocIds = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private final List<String> insertedEventNames = new ArrayList<>();
    private final List<Date> insertedStartDates = new ArrayList<>();
    private final List<Double> insertedCapacities = new ArrayList<>();
    private String sportsCategory;
    private String artCategory;
    private String musicCategory;

    /**
     * Prepares test data in Firestore before each test runs.
     * <p>
     * This method inserts test category documents and several future public events
     * into the Firestore test collections, sets a valid current user in the app
     * session, then launches {@link EntrantEventListActivity}.
     *
     * @throws InterruptedException if the Firestore insertion wait is interrupted
     */
    @Before
    public void setupFirestoreData() throws InterruptedException {
        FirestoreCollections.startTest();
        String runId = String.valueOf(System.currentTimeMillis());
        sportsCategory = "Sports Test " + runId;
        artCategory = "Art Test " + runId;
        musicCategory = "Music Test " + runId;

        categories.add(sportsCategory);
        categories.add(artCategory);
        categories.add(musicCategory);

        UserSession.getInstance().setCurrentUser(
                new User(
                        "entrant-test-user",
                        "Test Entrant",
                        "entrant@test.com",
                        "7800000000",
                        null,
                        false,
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new HashMap<>())) ;

        CountDownLatch categoryLatch = new CountDownLatch(categories.size());
        for (String category : categories) {
            Map<String, Object> categoryDoc = new HashMap<>();
            categoryDoc.put("category", category);
            categoryDoc.put("color", "#3366CC");
            db.collection(FirestoreCollections.EVENT_CATEGORIES_COLLECTION)
                    .add(categoryDoc)
                    .addOnSuccessListener(doc -> {
                        insertedCategoryDocIds.add(doc.getId());
                        categoryLatch.countDown();
                    });
        }
        categoryLatch.await();

        CountDownLatch organizerLatch = new CountDownLatch(1);
        Map<String, Object> organizer = new HashMap<>();
        organizer.put("name", "Test Organizer");
        organizer.put("isAdmin", false);
        organizer.put("events", new ArrayList<String>());
        organizer.put("organizedEvents", new ArrayList<String>());
        organizer.put("settings", new HashMap<String, Object>());
        db.collection(FirestoreCollections.USERS_COLLECTION)
                .add(organizer)
                .addOnSuccessListener(doc -> {
                    insertedUserIds.add(doc.getId());
                    organizerLatch.countDown();
                });
        organizerLatch.await();

        String organizerId = insertedUserIds.get(0);

        CountDownLatch eventLatch = new CountDownLatch(3);
        for (int i = 0; i < 3; i++) {
            Map<String, Object> event = new HashMap<>();
            String eventName = "Test Event " + (i + 1) + " - " + System.currentTimeMillis();

            double capacity = 20.0 + i;
            Date regOpen = new Date(System.currentTimeMillis() - 60_000);
            Date regClose = new Date(System.currentTimeMillis() + 86_400_000);
            Date startDate = new Date(System.currentTimeMillis() + ((long) (i + 1) * 86_400_000));
            Date endDate = new Date(System.currentTimeMillis() + ((long) (i + 2) * 86_400_000));

            event.put("category", categories.get(i));
            event.put("capacity", capacity);
            event.put("open", regOpen);
            event.put("close", regClose);
            event.put("startDate", startDate);
            event.put("endDate", endDate);
            event.put("name", eventName);
            event.put("location", "Test Location " + (i + 1));
            event.put("image", "");
            event.put("isPrivate", false);

            ArrayList<String> organizers = new ArrayList<>();
            organizers.add(organizerId);
            event.put("organizers", organizers);

            insertedEventNames.add(eventName);
            insertedStartDates.add(startDate);
            insertedCapacities.add(capacity);

            db.collection(FirestoreCollections.EVENTS_COLLECTION)
                    .add(event)
                    .addOnSuccessListener(doc -> {
                        insertedEventIds.add(doc.getId());
                        eventLatch.countDown();
                    });
        }
        eventLatch.await();

        scenario = ActivityScenario.launch(EntrantEventListActivity.class);
        waitForRecyclerViewItems(3, 5000);
    }

    /**
     * Waits until the event RecyclerView is populated with a minimum number
     * of items or until a timeout occurs.
     *
     * @param minCount minimum number of displayed items expected
     * @param timeoutMs maximum time to wait in milliseconds
     * @throws InterruptedException if the wait loop is interrupted
     */
    private void waitForRecyclerViewItems(int minCount, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            final int[] count = {0};
            onView(withId(R.id.entrant_event_list_view)).check((view, e) -> {
                RecyclerView recyclerView = (RecyclerView) view;
                if (recyclerView.getAdapter() == null) {
                    throw new AssertionError("RecyclerView adapter was null");
                }
                count[0] = recyclerView.getAdapter().getItemCount();
            });
            if (count[0] >= minCount) {
                return;
            }
            Thread.sleep(50);
        }

        throw new AssertionError("RecyclerView never populated with at least " + minCount + " items");
    }

    /**
     * Waits until the event RecyclerView contains exactly the expected number
     * of items or until a timeout occurs.
     *
     * @param expectedSize exact number of displayed items expected
     * @param timeoutMs maximum time to wait in milliseconds
     * @throws InterruptedException if the wait loop is interrupted
     */
    private void waitForRecyclerViewExactSize(int expectedSize, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            final int[] count = {0};
            onView(withId(R.id.entrant_event_list_view)).check((view, e) -> {
                RecyclerView recyclerView = (RecyclerView) view;
                if (recyclerView.getAdapter() == null) {
                    throw new AssertionError("RecyclerView adapter was null");
                }
                count[0] = recyclerView.getAdapter().getItemCount();
            });
            if (count[0] == expectedSize) {
                return;
            }
            Thread.sleep(50);
        }

        throw new AssertionError("RecyclerView never reached size " + expectedSize);
    }

    /**
     * Returns a {@link ViewAssertion} that verifies the number of items
     * displayed in a {@link RecyclerView}.
     *
     * @param expectedSize expected number of items in the RecyclerView
     * @return a {@link ViewAssertion} that checks the RecyclerView item count
     */
    public static ViewAssertion withRecyclerViewSize(int expectedSize) {
        return (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }

            RecyclerView recyclerView = (RecyclerView) view;
            if (recyclerView.getAdapter() == null) {
                throw new AssertionError("RecyclerView adapter was null");
            }
            assertEquals(expectedSize, recyclerView.getAdapter().getItemCount());
        };
    }

    /**
     * Returns a {@link ViewAssertion} that verifies a {@link RecyclerView}
     * contains at least the given number of items.
     *
     * @param minSize minimum number of items expected in the RecyclerView
     * @return a {@link ViewAssertion} that checks the RecyclerView item count
     */
    public static ViewAssertion withRecyclerViewMinSize(int minSize) {
        return (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }

            RecyclerView recyclerView = (RecyclerView) view;
            if (recyclerView.getAdapter() == null) {
                throw new AssertionError("RecyclerView adapter was null");
            }
            int actualSize = recyclerView.getAdapter().getItemCount();
            if (actualSize < minSize) {
                throw new AssertionError("expected at least <" + minSize + "> but was <" + actualSize + ">");
            }
        };
    }

    /**
     * Invokes the activity's private applyFilters method using reflection.
     *
     * @param activity the activity instance whose filters should be applied
     */
    private void invokeApplyFilters(EntrantEventListActivity activity) {
        try {
            Method applyFiltersMethod = EntrantEventListActivity.class.getDeclaredMethod("applyFilters");
            applyFiltersMethod.setAccessible(true);
            applyFiltersMethod.invoke(activity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the activity's private maxCapacityFilter field and reapplies filters.
     *
     * @param maxCapacity the maximum capacity value to apply
     */
    private void applyMaxCapacityFilter(Integer maxCapacity) {
        scenario.onActivity(activity -> {
            try {
                Field field = EntrantEventListActivity.class.getDeclaredField("maxCapacityFilter");
                field.setAccessible(true);
                field.set(activity, maxCapacity);
                invokeApplyFilters(activity);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Sets the activity's private start and end date filter fields and reapplies filters.
     *
     * @param startDate the start date filter to apply
     * @param endDate the end date filter to apply
     */
    private void applyDateRangeFilter(Date startDate, Date endDate) {
        scenario.onActivity(activity -> {
            try {
                Field startField = EntrantEventListActivity.class.getDeclaredField("startDateFilter");
                startField.setAccessible(true);
                startField.set(activity, startDate);

                Field endField = EntrantEventListActivity.class.getDeclaredField("endDateFilter");
                endField.setAccessible(true);
                endField.set(activity, endDate);

                invokeApplyFilters(activity);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Verifies that events inserted into Firestore appear in the entrant event list.
     * <p>
     * User Story Tested:
     *     US 01.01.03 – As an entrant, I want to be able to see a list
     *     of events that I can join the waiting list for.
     *
     * @throws InterruptedException if the wait operation is interrupted
     */
    @Test
    public void testDisplayedEventsMatchDatabase() throws InterruptedException {
        waitForRecyclerViewItems(3, 5000);
        onView(withId(R.id.entrant_event_list_view)).check(withRecyclerViewMinSize(3));
    }

    /**
     * Tests that category filtering updates the displayed event list correctly.
     * <p>
     * User Story Tested:
     *     US 01.01.04 – As an entrant, I want to filter events based
     *     on my interests and availability.
     *
     * @throws InterruptedException if waiting for asynchronous UI updates is interrupted
     */
    @Test
    public void testCategoryFiltering() throws InterruptedException {
        waitForRecyclerViewItems(3, 5000);
        onView(withId(R.id.entrant_event_list_view)).check(withRecyclerViewMinSize(3));

        onView(withId(R.id.entrant_event_list_filter_button)).perform(click());
        Thread.sleep(300);
        onView(withText("Categories: Any")).perform(click());

        Thread.sleep(300);
        onView(withText(sportsCategory)).perform(click());
        onView(withText("Apply")).perform(click());
        onView(withText("Apply")).perform(click());
        Thread.sleep(800);

        onView(withId(R.id.entrant_event_list_view)).check(withRecyclerViewSize(1));

        onView(withId(R.id.entrant_event_list_filter_button)).perform(click());
        Thread.sleep(300);
        onView(withText(org.hamcrest.CoreMatchers.startsWith("Categories:"))).perform(click());

        Thread.sleep(300);
        onView(withText(artCategory)).perform(click());
        onView(withText("Apply")).perform(click());
        onView(withText("Apply")).perform(click());
        Thread.sleep(800);

        onView(withId(R.id.entrant_event_list_view)).check(withRecyclerViewSize(2));
    }

    /**
     * Tests that maximum capacity filtering reduces the displayed event list correctly.
     * <p>
     * User Story Tested:
     *     US 01.01.04 – As an entrant, I want to filter events based
     *     on my interests and availability.
     *
     * @throws InterruptedException if waiting for asynchronous UI updates is interrupted
     */
    @Test
    public void testCapacityFiltering() throws InterruptedException {
        waitForRecyclerViewItems(3, 5000);
        onView(withId(R.id.entrant_event_list_view)).check(withRecyclerViewMinSize(3));

        applyMaxCapacityFilter(insertedCapacities.get(0).intValue());
        waitForRecyclerViewExactSize(1, 5000);
        onView(withId(R.id.entrant_event_list_view)).check(withRecyclerViewSize(1));
    }

    /**
     * Tests that date range filtering reduces the displayed event list correctly.
     * <p>
     * User Story Tested:
     *     US 01.01.04 – As an entrant, I want to filter events based
     *     on my interests and availability.
     *
     * @throws InterruptedException if waiting for asynchronous UI updates is interrupted
     */
    @Test
    public void testDateRangeFiltering() throws InterruptedException {
        waitForRecyclerViewItems(3, 5000);
        onView(withId(R.id.entrant_event_list_view)).check(withRecyclerViewMinSize(3));

        Date secondEventStart = insertedStartDates.get(1);
        Date secondEventWindowStart = new Date(secondEventStart.getTime() - 60_000);
        Date secondEventWindowEnd = new Date(secondEventStart.getTime() + 60_000);

        applyDateRangeFilter(secondEventWindowStart, secondEventWindowEnd);
        waitForRecyclerViewExactSize(1, 5000);
        onView(withId(R.id.entrant_event_list_view)).check(withRecyclerViewSize(1));
    }

    /**
     * Removes all test data from Firestore after each test.
     *
     * @throws InterruptedException if the cleanup wait operation is interrupted
     */
    @After
    public void cleanupFirestoreData() throws InterruptedException {
        if (scenario != null) {
            scenario.close();
        }

        UserSession.getInstance().clear();

        CountDownLatch categoryDeleteLatch = new CountDownLatch(insertedCategoryDocIds.size());
        if (insertedCategoryDocIds.isEmpty()) {
            categoryDeleteLatch.countDown();
        } else {
            for (String docId : insertedCategoryDocIds) {
                db.collection(FirestoreCollections.EVENT_CATEGORIES_COLLECTION)
                        .document(docId)
                        .delete()
                        .addOnCompleteListener(task -> categoryDeleteLatch.countDown());
            }
        }
        categoryDeleteLatch.await();

        FirestoreCollections.endTest();
    }
}