package com.icarus.events;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EventCommentActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EventCommentAdapter adapter;
    private List<Comment> commentList;

    private EditText commentInput;
    private MaterialButton sendCommentButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_comment); // <-- your XML file

        recyclerView = findViewById(R.id.event_comments_list);
        commentInput = findViewById(R.id.comment_input);
        sendCommentButton = findViewById(R.id.send_comment_button);

        commentList = new ArrayList<>();

        // TODO: Fill out the list from Firebase

        // 3. Add dummy comments (for testing UI)
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

            // TODO: Get real user ID and name
            Comment newComment = new Comment(
                    "currentUserId",
                    "Bradley",
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
}