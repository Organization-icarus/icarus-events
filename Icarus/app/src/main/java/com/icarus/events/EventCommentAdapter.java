package com.icarus.events;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying a list of Comments.
 */
public class EventCommentAdapter extends RecyclerView.Adapter<EventCommentAdapter.CommentViewHolder> {

    private final List<Comment> comments;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

    public EventCommentAdapter(List<Comment> comments) {
        this.comments = comments;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_event_comment_content, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);

        holder.authorName.setText(comment.getAuthorName());

        if (comment.isDeleted()) {
            holder.commentTextView.setText("[deleted]");
        } else{
            holder.commentTextView.setText(comment.getText());
        }

        if (comment.getCreatedAt() != null) {
            holder.createdAtTextView.setText(dateFormat.format(comment.getCreatedAt()));
        } else {
            holder.createdAtTextView.setText("");
        }
    }

    @Override
    public int getItemCount() { return comments.size(); }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView authorName, commentTextView, createdAtTextView;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorName          = itemView.findViewById(R.id.comment_author_name);
            commentTextView     = itemView.findViewById(R.id.commentTextView);
            createdAtTextView   = itemView.findViewById(R.id.createdAtTextView);
        }
    }
}