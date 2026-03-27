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

    private final Context context;
    private final List<Comment> comments;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

    public EventCommentAdapter(Context context, List<Comment> comments) {
        this.context = context;
        this.comments = comments;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.authorName.setText(comment.getAuthorName());
        holder.commentText.setText(comment.getText());
        holder.timestamp.setText(
                comment.getTimestamp() != null
                        ? dateFormat.format(comment.getTimestamp())
                        : ""
        );
    }

    @Override
    public int getItemCount() { return comments.size(); }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView authorName, commentText, timestamp;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorName   = itemView.findViewById(R.id.comment_author_name);
            commentText  = itemView.findViewById(R.id.comment_text);
            timestamp    = itemView.findViewById(R.id.comment_timestamp);
        }
    }
}