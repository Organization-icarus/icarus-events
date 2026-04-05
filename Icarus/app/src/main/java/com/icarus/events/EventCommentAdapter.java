package com.icarus.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * RecyclerView adapter for rendering and managing a list of {@link Comment} items.
 * <p>
 * Supports:
 * <ul>
 *   <li>Displaying comment content, author information, and timestamps</li>
 *   <li>Optional selection mode for deletion (admin/organizer)</li>
 *   <li>Soft-deleted comment handling</li>
 * </ul>
 * </p>
 *
 * @author Bradley Bravender
 */
public class EventCommentAdapter extends RecyclerView.Adapter<EventCommentAdapter.CommentViewHolder> {

    /**
     * Callback interface for selection state changes.
     */
    public interface OnSelectionChangedListener {

        /**
         * Called when the number of selected comments changes.
         *
         * @param selectedCount number of currently selected comments
         */
        void onSelectionChanged(int selectedCount);
    }

    private final List<Comment> comments;
    private final boolean canDelete;  // True for the event organizer and admins
    private final OnSelectionChangedListener selectionChangedListener;

    private final Set<Integer> selectedPositions = new HashSet<>();

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

    /**
     * Creates a new adapter instance.
     *
     * @param comments                  list of comments to display
     * @param canDelete                 whether selection (delete mode) is enabled
     * @param selectionChangedListener  listener for selection state updates
     */
    public EventCommentAdapter(List<Comment> comments,
                               boolean canDelete,
                               OnSelectionChangedListener selectionChangedListener) {
        this.comments = comments;
        this.canDelete = canDelete;
        this.selectionChangedListener =selectionChangedListener;
}

    /**
     * Inflates the comment item layout and creates a ViewHolder.
     *
     * @param parent   parent view group
     * @param viewType view type (unused)
     * @return new CommentViewHolder instance
     */
    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_event_comment_content, parent, false);
        return new CommentViewHolder(view);
    }

    /**
     * Binds comment data to the ViewHolder.
     * <p>
     * Handles:
     * <ul>
     *   <li>Profile image loading</li>
     *   <li>Deleted comment display</li>
     *   <li>Date formatting</li>
     *   <li>Selection UI for deletion</li>
     * </ul>
     * </p>
     *
     * @param holder   view holder to bind data to
     * @param position position of the item in the dataset
     */
    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);

        String authorImageURL = comment.getAuthorImage();
        if (authorImageURL != null && !authorImageURL.isEmpty()) {
            Picasso.get()
                    .load(authorImageURL)
                    .placeholder(R.drawable.poster)
                    .error(R.drawable.poster)           // Optional: shows if link fails
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.poster);
        }

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

    /**
     * Returns the total number of comments.
     *
     * @return item count
     */
    @Override
    public int getItemCount() {
        return comments.size();
    }

    /**
     * Toggles the selection state of a comment at the given position.
     *
     * @param position adapter position of the comment
     */
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

    /**
     * Returns the list of currently selected comments.
     *
     * @return selected comments
     */
    public List<Comment> getSelectedComments() {
        List<Comment> selectedComments = new ArrayList<>();

        for (Integer position : selectedPositions) {
            if (position >= 0 && position < comments.size()) {
                selectedComments.add(comments.get(position));
            }
        }

        return selectedComments;
    }

    /**
     * Returns the adapter positions of selected comments.
     *
     * @return list of selected positions
     */
    public List<Integer> getSelectedPositions() {
        return new ArrayList<>(selectedPositions);
    }

    /**
     * Clears all selected comments and updates the UI.
     */
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

    /**
     * ViewHolder for a single comment item.
     * <p>
     * Holds references to UI elements for efficient binding.
     * </p>
     */
    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView authorName, commentTextView, createdAtTextView;
        ImageView selectIcon;
        ShapeableImageView profileImage;

        /**
         * Initializes view references for a comment item.
         *
         * @param itemView root view of the item layout
         */
        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorName          = itemView.findViewById(R.id.comment_author_name);
            commentTextView     = itemView.findViewById(R.id.commentTextView);
            createdAtTextView   = itemView.findViewById(R.id.createdAtTextView);
            selectIcon          = itemView.findViewById(R.id.comment_select_icon);
            profileImage        = itemView.findViewById(R.id.comment_profile_picture);
        }
    }
}
