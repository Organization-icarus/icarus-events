package com.icarus.events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

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
 * Instrumented UI tests for {@link EventCommentActivity} and {@link EventCommentAdapter}.
 * <p>
 * User Stories Covered:
 * <ul>
 *   <li>US 01.08.01 As an entrant, I want to post a comment on an event.</li>
 *   <li>US 01.08.02 As an entrant, I want to view comments on an event.</li>
 *   <li>US 02.08.01 As an organizer, I want to view and delete entrant comments on my event.</li>
 *   <li>US 02.08.02 As an organizer, I want to comment on my events.</li>
 *   <li>US 03.10.01 As an administrator, I want to remove event comments that violate app policy.</li>
 * </ul>
 *
 * Tests use temporary Firestore collections via {@link FirestoreCollections#startTest()}
 * and {@link FirestoreCollections#endTest()}.
 *
 * @author Bradley Bravender
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventCommentTest {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ActivityScenario<EventCommentActivity> scenario;

    private String testEventId;
    private String visibleCommentId;

    private final String organizerUserId = "organizer1";

    /**
     * Prepares Firestore test data before each test.
     * <p>
     * This method:
     * <ul>
     *   <li>Switches Firestore into test collections</li>
     *   <li>Initializes Cloudinary support used by the app startup path</li>
     *   <li>Creates an organizer user and places that user in the session</li>
     *   <li>Creates an event owned by that organizer</li>
     *   <li>Creates one visible comment and one already-deleted comment</li>
     * </ul>
     *
     * @throws InterruptedException if waiting for Firestore setup is interrupted
     */
    @Before
    public void setup() throws InterruptedException {
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

        User organizerUser = new User(
                organizerUserId,
                "Organizer User",
                null,
                null,
                "",
                false,
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                null
        );
        UserSession.getInstance().setCurrentUser(organizerUser);

        CountDownLatch userLatch = new CountDownLatch(1);
        db.collection(FirestoreCollections.USERS_COLLECTION)
                .document(organizerUserId)
                .set(Map.of(
                        "name", "Organizer User",
                        "isAdmin", false
                ))
                .addOnSuccessListener(unused -> userLatch.countDown());
        userLatch.await();

        ArrayList<String> organizers = new ArrayList<>();
        organizers.add(organizerUserId);

        CountDownLatch eventLatch = new CountDownLatch(1);
        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .add(Map.of(
                        "name", "Comment Test Event",
                        "category", "Test",
                        "organizers", organizers
                ))
                .addOnSuccessListener(docRef -> {
                    testEventId = docRef.getId();
                    eventLatch.countDown();
                });
        eventLatch.await();

        CountDownLatch commentsLatch = new CountDownLatch(2);

        Comment visibleComment = new Comment(
                organizerUserId,
                "Organizer User",
                "",
                "Visible comment",
                new Date(),
                false
        );

        Comment deletedComment = new Comment(
                organizerUserId,
                "Organizer User",
                "",
                "Soft deleted comment",
                new Date(),
                true
        );

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(testEventId)
                .collection("comments")
                .add(visibleComment)
                .addOnSuccessListener(docRef -> {
                    visibleCommentId = docRef.getId();
                    commentsLatch.countDown();
                });

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(testEventId)
                .collection("comments")
                .add(deletedComment)
                .addOnSuccessListener(docRef -> commentsLatch.countDown());

        commentsLatch.await();
    }

    /**
     * Tests that comments are loaded for the event and deleted comments are excluded.
     * <p>
     * Verifies that visible comments appear in the RecyclerView and that
     * comments marked as deleted do not appear.
     * <p>
     * User Stories Tested:
     * <ul>
     *   <li>US 01.08.02 As an entrant, I want to view comments on an event.</li>
     *   <li>US 02.08.01 As an organizer, I want to view and delete entrant comments on my event.</li>
     * </ul>
     *
     * @throws InterruptedException if the UI wait is interrupted
     */
    @Test
    public void testCommentsLoadAndDeletedOnesAreFilteredOut() throws InterruptedException {
        launchCommentActivity();
        Thread.sleep(2500);

        onView(withId(R.id.event_comments_list))
                .check(matches(hasDescendant(withText("Visible comment"))));

        onView(withText("Soft deleted comment"))
                .check(doesNotExist());
    }

    /**
     * Tests that a user can post a new comment from the comment screen.
     * <p>
     * Types a comment into the input field, sends it, verifies it appears in
     * the UI, and confirms the new Firestore document exists.
     * <p>
     * User Stories Tested:
     * <ul>
     *   <li>US 01.08.01 As an entrant, I want to post a comment on an event.</li>
     *   <li>US 02.08.02 As an organizer, I want to comment on my events.</li>
     * </ul>
     *
     * @throws InterruptedException if Firestore waiting is interrupted
     */
    @Test
    public void testPostCommentAddsComment() throws InterruptedException {
        launchCommentActivity();
        Thread.sleep(2000);

        onView(withId(R.id.comment_input))
                .perform(typeText("Brand new comment"), closeSoftKeyboard());

        onView(withId(R.id.send_comment_button)).perform(click());
        Thread.sleep(2000);

        onView(withId(R.id.event_comments_list))
                .check(matches(hasDescendant(withText("Brand new comment"))));

        CountDownLatch verifyLatch = new CountDownLatch(1);
        final boolean[] found = {false};

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(testEventId)
                .collection("comments")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (var doc : snapshot.getDocuments()) {
                        String text = doc.getString("text");
                        if ("Brand new comment".equals(text)) {
                            found[0] = true;
                            break;
                        }
                    }
                    verifyLatch.countDown();
                });

        verifyLatch.await();
        assertTrue(found[0]);
    }

    /**
     * Tests that an organizer can soft-delete a selected comment.
     * <p>
     * Selects a visible comment using the adapter’s selection icon, presses
     * the delete/manage button, and verifies that Firestore updates the
     * comment document with {@code deleted = true}.
     * <p>
     * User Stories Tested:
     * <ul>
     *   <li>US 02.08.01 As an organizer, I want to view and delete entrant comments on my event.</li>
     *   <li>US 03.10.01 As an administrator, I want to remove event comments that violate app policy.</li>
     * </ul>
     *
     * @throws InterruptedException if Firestore waiting is interrupted
     */
    @Test
    public void testOrganizerCanSoftDeleteComment() throws InterruptedException {
        launchCommentActivity();
        Thread.sleep(2500);

        onView(allOf(
                withId(R.id.comment_select_icon),
                isDescendantOfA(hasDescendant(withText("Visible comment")))
        )).perform(click());

        onView(withId(R.id.manage_button)).perform(click());
        Thread.sleep(1500);

        CountDownLatch verifyLatch = new CountDownLatch(1);
        final boolean[] deleted = {false};

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(testEventId)
                .collection("comments")
                .document(visibleCommentId)
                .get()
                .addOnSuccessListener(doc -> {
                    Boolean value = doc.getBoolean("deleted");
                    deleted[0] = value != null && value;
                    verifyLatch.countDown();
                });

        verifyLatch.await();
        assertTrue(deleted[0]);
    }

    /**
     * Tests that the adapter renders a deleted comment using the deleted label
     * and hides the selection icon for that row.
     * <p>
     * This verifies the adapter-level behavior for soft-deleted comments.
     * <p>
     * User Stories Tested:
     * <ul>
     *   <li>US 02.08.01 As an organizer, I want to view and delete entrant comments on my event.</li>
     *   <li>US 03.10.01 As an administrator, I want to remove event comments that violate app policy.</li>
     * </ul>
     */
    @Test
    public void testCommentAdapterShowsDeletedLabelAndHidesSelection() {
        Comment deletedComment = new Comment(
                "user1",
                "User One",
                "",
                "Original text",
                new Date(),
                true
        );

        ArrayList<Comment> comments = new ArrayList<>();
        comments.add(deletedComment);

        EventCommentAdapter adapter = new EventCommentAdapter(comments, true, null);
        assertEquals(1, adapter.getItemCount());

        FrameLayout parent = new FrameLayout(ApplicationProvider.getApplicationContext());
        EventCommentAdapter.CommentViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("[deleted]", holder.commentTextView.getText().toString());
        assertEquals(View.GONE, holder.selectIcon.getVisibility());
    }

    /**
     * Launches {@link EventCommentActivity} for the prepared test event.
     */
    private void launchCommentActivity() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EventCommentActivity.class
        );
        intent.putExtra("EVENT_ID", testEventId);
        scenario = ActivityScenario.launch(intent);
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