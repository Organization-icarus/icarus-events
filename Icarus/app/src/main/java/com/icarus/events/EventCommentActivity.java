package com.icarus.events;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EventCommentActivity extends HeaderNavBarActivity {

    private static final String TAG = "EventCommentActivity";

    private RecyclerView recyclerView;
    private EventCommentAdapter adapter;
    private List<Comment> commentList;

    private String username;
    private String userId;
    private String eventId;

    private EditText commentInput;
    private MaterialButton sendCommentButton;
    private MaterialButton manageButton;
    private boolean canDelete;

    private FirebaseFirestore db;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_comment);

        //--------------------------------
        // SET UP HEADER AND NAV BAR
        //--------------------------------
        setupHeaderBar("Comments");
        setupNavBar();

        eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId == null || eventId.isEmpty()) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.event_comments_list);
        commentInput = findViewById(R.id.comment_input);
        sendCommentButton = findViewById(R.id.send_comment_button);
        manageButton = findViewById(R.id.manage_button);
        manageButton.setVisibility(View.GONE);

        // So the pop-up keyboard doesn't block the text input
        setupImeInsets();

        User user = UserSession.getInstance().getCurrentUser();
        username = user.getName();
        userId = user.getId();

        canDelete = true;

        commentList = new ArrayList<>();

        adapter = new EventCommentAdapter(commentList, canDelete, selectedCount -> {
            if (selectedCount > 0) {
                manageButton.setVisibility(View.VISIBLE);
                manageButton.setText("Delete (" + selectedCount + ")");
            } else {
                manageButton.setVisibility(View.GONE);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadComments();
        sendCommentButton.setOnClickListener(v -> postComment());
        manageButton.setOnClickListener(v -> deleteComment());

    }

    /* This code was created with the help of ChatGPT on March 31. The prompt
    was, "How do I delete comments in Firebase using the 'isDeleted' field?". */
    private void deleteComment() {
        List<Comment> selectedComments = adapter.getSelectedComments();

        if (selectedComments.isEmpty()) {
            manageButton.setVisibility(View.GONE);
            return;
        }

        for (Comment comment: selectedComments) {
            if (comment.getDocumentId() != null) {
                db.collection("events")
                        .document(eventId)
                        .collection("comments")
                        .document(comment.getDocumentId())
                        .update("deleted", true);
            }
        }
        adapter.clearSelection();
    }


    private void postComment() {
        String text = commentInput.getText().toString().trim();

        if (TextUtils.isEmpty(text)) {
            commentInput.setError("Enter a comment");
            return;
        }

        Comment newComment = new Comment(
                userId,
                username,
                text,
                new Date(),
                false
        );

        sendCommentButton.setEnabled(false);

        db.collection("events")
                .document(eventId)
                .collection("comments")
                .add(newComment)
                .addOnSuccessListener(documentReference -> {
                    commentInput.setText("");
                    sendCommentButton.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    sendCommentButton.setEnabled(true);
                    commentInput.setError("Failed to post comment");
                    Log.e(TAG, "Error adding comment", e);
                });
    }


    private void loadComments() {
        db.collection("events")
                .document(eventId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading comments", error);
                        return;
                    }

                    commentList.clear();

                    // Only display non-deleted comments
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Comment comment = doc.toObject(Comment.class);
                            if (comment != null && !comment.isDeleted()) {
                                comment.setDocumentId(doc.getId());
                                commentList.add(comment);
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                });
    }


    /* This code was created with the help of Claude on March 30.
    The prompt was, "How do I prevent the pop-up keyboard from blocking the input
    text field?" */

    // This is used to dynamically space the text input above the keyboard.
    private void setupImeInsets() {
        View inputBar = findViewById(R.id.comment_input_bar);
        View navBar = findViewById(R.id.nav_bar);

        final int basePaddingLeft = inputBar.getPaddingLeft();
        final int basePaddingTop = inputBar.getPaddingTop();
        final int basePaddingRight = inputBar.getPaddingRight();
        final int basePaddingBottom = inputBar.getPaddingBottom();

        final int extraGap = dpToPx(12); // space between keyboard and input bar

        ViewCompat.setOnApplyWindowInsetsListener(inputBar, (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());

            if (imeVisible) {
                v.setPadding(
                        basePaddingLeft,
                        basePaddingTop,
                        basePaddingRight,
                        basePaddingBottom + imeInsets.bottom + extraGap
                );
                navBar.setVisibility(View.GONE);
            } else {
                v.setPadding(
                        basePaddingLeft,
                        basePaddingTop,
                        basePaddingRight,
                        basePaddingBottom + systemBarInsets.bottom
                );
                navBar.setVisibility(View.VISIBLE);
            }

            return insets;
        });

        ViewCompat.requestApplyInsets(inputBar);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }
}