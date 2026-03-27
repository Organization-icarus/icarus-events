package com.icarus.events;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity that displays and manages comments for a single Event.
 *
 * Listens to the event's "comments" subcollection in real-time,
 * renders them in a RecyclerView, and allows users to post new comments.
 *
 * Comments are stored at: events/{eventId}/comments/{commentId}
 * Each comment document has: { text, authorId, authorName, timestamp }
 *
 * @author [Your Name]
 */
public class EventCommentActivity extends NavigationBarActivity {

    private RecyclerView recyclerView;
    private EditText commentInput;
    private ImageButton sendBtn;

    private EventCommentAdapter adapter;
    private final List<Comment> commentList = new ArrayList<>();

    private ListenerRegistration commentListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_comment);
        setupNavBar();

        String eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) eventId = "hL8pW5lK9gDloqcWlmqx"; // For testing
        String finalEventId = eventId;

        User user = UserSession.getInstance().getCurrentUser();
        String userId = user.getId();
        String userName = user.getName(); // adjust to match your User getter

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //---------------------------
        // BIND VIEWS
        //---------------------------

        recyclerView = findViewById(R.id.comments_recycler_view);
        commentInput = findViewById(R.id.comment_input);
        sendBtn = findViewById(R.id.send_comment_button);

        //---------------------------
        // SET UP RECYCLER VIEW
        //---------------------------

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // newest comments scroll to bottom
        recyclerView.setLayoutManager(layoutManager);

        adapter = new EventCommentAdapter(this, commentList);
        recyclerView.setAdapter(adapter);

        //---------------------------
        // SEND BUTTON
        //---------------------------

        sendBtn.setOnClickListener(v -> {
            String text = commentInput.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> comment = new HashMap<>();
            comment.put("text", text);
            comment.put("authorId", userId);
            comment.put("authorName", userName);
            comment.put("timestamp", new Date());

            db.collection(FirestoreCollections.EVENTS_COLLECTION)
                    .document(finalEventId)
                    .collection("comments")
                    .add(comment)
                    .addOnSuccessListener(ref -> commentInput.setText(""))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to post comment", Toast.LENGTH_SHORT).show());
        });

        //---------------------------
        // LISTEN FOR COMMENTS
        //---------------------------

        commentListener = db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(finalEventId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((query, e) -> {
                    if (e != null || query == null) return;

                    commentList.clear();
                    for (var doc : query.getDocuments()) {
                        Comment comment = new Comment(
                                doc.getId(),
                                doc.getString("text"),
                                doc.getString("authorId"),
                                doc.getString("authorName"),
                                doc.getDate("timestamp")
                        );
                        commentList.add(comment);
                    }

                    adapter.notifyDataSetChanged();

                    // Auto-scroll to the newest comment
                    if (!commentList.isEmpty()) {
                        recyclerView.scrollToPosition(commentList.size() - 1);
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (commentListener != null) commentListener.remove();
    }
}