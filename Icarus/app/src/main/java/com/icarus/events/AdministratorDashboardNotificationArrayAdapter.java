package com.icarus.events;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Adapter used in the administrator dashboard to display notifications in a ListView.
 * <p>
 * Binds Notification objects to the notification list item layout and provides administrator
 * controls for viewing user details and removing notifications from Firebase Firestore.
 *
 * @author Kito Lee Son
 */
public class AdministratorDashboardNotificationArrayAdapter extends ArrayAdapter<Notification> {
    private final ArrayList<Notification> notifications;
    private final Context context;
    private final FirebaseFirestore db;

    /**
     * Constructs an adapter for displaying User objects in the administrator
     * dashboard user list.
     *
     * @param context the context used to inflate views and access resources
     * @param notifications the list of users to be displayed by the adapter
     */
    public AdministratorDashboardNotificationArrayAdapter(Context context, ArrayList<Notification> notifications){
        super(context, 0, notifications);
        this.notifications = notifications;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Returns a view for displaying a notification in the administrator notification list.
     * <p>
     * Inflates the list item layout if necessary, binds the data to the UI
     * components, and configures actions for viewing notification details and
     * removing the notification from the database.
     *
     * @param position the position of the notification in the adapter's data set
     * @param convertView a view to reuse
     * @param parent the parent view that this view will be attached to
     * @return the view representing the notification at the specified position
     */
    @NonNull
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
        View view = convertView;
        if (view == null){
            view = LayoutInflater.from(context)
                    .inflate(R.layout.administrator_dashboard_notification_list_content, parent, false);
        }

        Notification notification = notifications.get(position);
        ShapeableImageView eventImage = view
                .findViewById(R.id.admin_dashboard_notification_list_image);
        TextView eventName = view
                .findViewById(R.id.admin_dashboard_notification_list_event_name);
        TextView notificationMessage = view.findViewById((R.id.notification_message_text));
        ImageButton removeNotificationButton = view
                .findViewById(R.id.admin_dashboard_notification_list_remove_notification_button);

        Event event = notification.getEvent();

        // Set event image
        String imageUrl = event.getImage();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.poster)
                    .error(R.drawable.poster)
                    .into(eventImage);
        } else {
            eventImage.setImageResource(R.drawable.poster);
        }

        // Set event name
        eventName.setText(event.getName());

        // Click event name to go to event
        eventName.setOnClickListener(v -> {
            Intent intent = new Intent(context, EventDetailsActivity.class);
//            intent.putExtra("deviceId", user.getId());
            context.startActivity(intent);
        });

        // Remove notification from database
        removeNotificationButton.setOnClickListener(v -> {
            // Remove notification from event notifications subcollections
            db.collection(FirestoreCollections.NOTIFICATIONS_COLLECTION).get()
                    .addOnSuccessListener(notificationSnapshots -> {
                        for (QueryDocumentSnapshot notificationSnapshot : notificationSnapshots) {
                            notificationSnapshot.getReference()
                                    .collection("notifications")
                                    .document(notification.getId())
                                    .delete();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Failed to delete profile", Toast.LENGTH_SHORT).show());
        });

        return view;
    }
}
