package com.icarus.events;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Custom adapter for displaying a list of notification items in a ListView.
 * <p>
 * This adapter handles the inflation of notification list items and dynamically
 * retrieves sender information (name and profile image) from Firestore based on
 * the sender's unique ID.
 *
 * @author Kito Lee Son
 */
public class NotificationListAdapter extends ArrayAdapter<NotificationItem> {

    private final Activity context;
    private final ArrayList<NotificationItem> notifications;
    private final FirebaseFirestore db;

    /**
     * Constructs a new NotificationListAdapter.
     *
     * @param context       the activity context used for layout inflation
     * @param notifications the list of {@link NotificationItem} objects to display
     */
    public NotificationListAdapter(Activity context, ArrayList<NotificationItem> notifications) {
        super(context, R.layout.notification_list_item, notifications);
        this.context = context;
        this.notifications = notifications;
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Prepares and returns the view for a specific notification item in the list.
     * <p>
     * This method populates the notification message and then performs an
     * asynchronous Firestore lookup to resolve the sender's identity. If the
     * sender is a system account or the data is missing, it applies default
     * branding; otherwise, it loads the user's profile via Picasso.
     *
     * @param position    the position of the item within the data set
     * @param convertView the recycled view to reuse, if available
     * @param parent      the parent view group
     * @return the completed View for the notification item
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = context.getLayoutInflater().inflate(R.layout.notification_list_item, parent, false);
        }

        ImageView senderImage = view.findViewById(R.id.notification_sender_image);
        TextView senderNameText = view.findViewById(R.id.notification_sender_name);
        TextView messageText = view.findViewById(R.id.notification_message_text);

        NotificationItem item = notifications.get(position);

        messageText.setText(item.getMessage());
        senderNameText.setText("Unknown User");
        senderImage.setImageResource(R.drawable.poster);

        String senderId = item.getSender();

        if (senderId == null || senderId.isEmpty() || senderId.equals("system")) {
            senderNameText.setText("System");
            senderImage.setImageResource(R.drawable.poster);
            return view;
        }

        senderNameText.setOnClickListener(v -> {
            Intent intent = new Intent(context, EventDetailsActivity.class);
            intent.putExtra("eventId", item.getEventId());
            context.startActivity(intent);
        });

        db.collection(FirestoreCollections.USERS_COLLECTION)
                .document(senderId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        senderNameText.setText("Unknown User");
                        senderImage.setImageResource(R.drawable.poster);
                        return;
                    }

                    String senderName = snapshot.getString("name");
                    String senderImageUrl = snapshot.getString("image");

                    if (senderName != null && !senderName.isEmpty()) {
                        senderNameText.setText(senderName);
                    } else {
                        senderNameText.setText("Unknown User");
                    }

                    if (senderImageUrl != null && !senderImageUrl.isEmpty()) {
                        Picasso.get()
                                .load(senderImageUrl)
                                .placeholder(R.drawable.poster)
                                .error(R.drawable.poster)
                                .into(senderImage);
                    } else {
                        senderImage.setImageResource(R.drawable.poster);
                    }
                })
                .addOnFailureListener(e -> {
                    senderNameText.setText("Unknown User");
                    senderImage.setImageResource(R.drawable.poster);
                });

        return view;
    }
}