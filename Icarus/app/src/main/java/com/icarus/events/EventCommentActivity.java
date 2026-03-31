package com.icarus.events;

import android.os.Bundle;
import android.text.TextUtils;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EventCommentActivity extends NavigationBarActivity {

    private RecyclerView recyclerView;
    private EventCommentAdapter adapter;
    private List<Comment> commentList;

    private String username;
    private String userId;

    private EditText commentInput;
    private MaterialButton sendCommentButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_comment);
        setupNavBar();

        String eventId = getIntent().getStringExtra("EVENT_ID");

        recyclerView = findViewById(R.id.event_comments_list);
        commentInput = findViewById(R.id.comment_input);
        sendCommentButton = findViewById(R.id.send_comment_button);

        setupImeInsets();

        User user = UserSession.getInstance().getCurrentUser();
        username = user.getName();
        userId = user.getId();

        commentList = new ArrayList<>();

        commentList.add(new Comment(
                "user1",
                "Alex Alves",
                "This event was crazy good.",
                new Date(),
                false
        ));

        commentList.add(new Comment(
                "user2",
                "Bradley",
                "Looking forward to the next one.",
                new Date(),
                false
        ));

        commentList.add(new Comment(
                "user3",
                "Sam",
                "I couldn’t make it 😢",
                new Date(),
                false
        ));

        // Set up adapter and layout manager
        adapter = new EventCommentAdapter(commentList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        sendCommentButton.setOnClickListener(v -> {
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

            // New comments get shown first
            commentList.add(0, newComment);
            adapter.notifyItemInserted(0);
            recyclerView.scrollToPosition(0);
            commentInput.setText("");
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