package com.icarus.events;

import android.app.Activity;
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
 * Adapter for displaying notification items.
 */
public class NotificationListAdapter extends ArrayAdapter<NotificationItem> {

    private final Activity context;
    private final ArrayList<NotificationItem> notifications;
    private final FirebaseFirestore db;

    public NotificationListAdapter(Activity context, ArrayList<NotificationItem> notifications) {
        super(context, R.layout.notification_list_item, notifications);
        this.context = context;
        this.notifications = notifications;
        this.db = FirebaseFirestore.getInstance();
    }

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