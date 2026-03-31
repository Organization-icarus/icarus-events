package com.icarus.events;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.datepicker.OnSelectionChangedListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * RecyclerView adapter for displaying a list of Comments.
 */
public class EventCommentAdapter extends RecyclerView.Adapter<EventCommentAdapter.CommentViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    private final List<Comment> comments;
    private final boolean canDelete;  // True for the event organizer and admins
    private final OnSelectionChangedListener selectionChangedListener;

    private final Set<Integer> selectedPositions = new HashSet<>();

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

    public EventCommentAdapter(List<Comment> comments,
                               boolean canDelete,
                               OnSelectionChangedListener selectionChangedListener) {
        this.comments = comments;
        this.canDelete = canDelete;
        this.selectionChangedListener =selectionChangedListener;
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

        if (canDelete && !comment.isDeleted()) {
            holder.selectIcon.setVisibility(View.VISIBLE);

            boolean isSelected = selectedPositions.contains(position);
            holder.selectIcon.setImageResource(
                    isSelected ? R.drawable.ic_checked_circle : R.drawable.ic_circle
            );
            holder.selectIcon.setColorFilter(
                    ContextCompat.getColor(
                            holder.itemView.getContext(),
                            isSelected ? R.color.attention : R.color.lightTextSemi
                    )
            );

            holder.selectIcon.setOnClickListener(v -> toggleSelection(holder.getAdapterPosition()));
        } else {
            holder.selectIcon.setVisibility(View.GONE);
            holder.selectIcon.setOnClickListener(null);
        }
    }


    @Override
    public int getItemCount() {
        return comments.size();
    }


    private void toggleSelection(int position) {
        if (position == RecyclerView.NO_POSITION) return;

        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }

        notifyItemChanged(position);

        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedPositions.size());
        }
    }


    public List<Comment> getSelectedComments() {
        List<Comment> selectedComments = new ArrayList<>();

        for (Integer position : selectedPositions) {
            if (position >= 0 && position < comments.size()) {
                selectedComments.add(comments.get(position));
            }
        }

        return selectedComments;
    }


    public List<Integer> getSelectedPositions() {
        return new ArrayList<>(selectedPositions);
    }


    public void clearSelection() {
        List<Integer> oldSelections = new ArrayList<>(selectedPositions);
        selectedPositions.clear();

        for (Integer position : oldSelections) {
            notifyItemChanged(position);
        }

        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(0);
        }
    }


    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView authorName, commentTextView, createdAtTextView;
        ImageView selectIcon;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorName          = itemView.findViewById(R.id.comment_author_name);
            commentTextView     = itemView.findViewById(R.id.commentTextView);
            createdAtTextView   = itemView.findViewById(R.id.createdAtTextView);
            selectIcon          = itemView.findViewById(R.id.comment_select_icon);
        }
    }
}