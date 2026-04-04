package com.icarus.events;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapter used in the administrator dashboard to display users in a ListView.
 * <p>
 * Binds User objects to the user list item layout and provides administrator
 * controls for viewing user details and removing users from Firebase Firestore.
 *
 * @author Benjamin Hall
 */
public class AdministratorDashboardUserArrayAdapter extends ArrayAdapter<User> {
    private ArrayList<User> users;
    private Context context;
    private FirebaseFirestore db;

    /**
     * Constructs an adapter for displaying User objects in the administrator
     * dashboard user list.
     *
     * @param context the context used to inflate views and access resources
     * @param users the list of users to be displayed by the adapter
     */
    public AdministratorDashboardUserArrayAdapter(Context context, ArrayList<User> users){
        super(context, 0, users);
        this.users = users;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Returns a view for displaying a user in the administrator user list.
     * <p>
     * Inflates the list item layout if necessary, binds the data to the UI
     * components, and configures actions for viewing user details and
     * removing the user from the database.
     *
     * @param position the position of the user in the adapter's data set
     * @param convertView a view to reuse
     * @param parent the parent view that this view will be attached to
     * @return the view representing the user at the specified position
     */
    @NonNull
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
        View view = convertView;
        if (view == null){
            view = LayoutInflater.from(context)
                    .inflate(R.layout.administrator_dashboard_user_list_content, parent, false);
        }

        User user = users.get(position);
        ShapeableImageView profileImage = view
                .findViewById(R.id.admin_dashboard_user_list_image);
        TextView userName = view
                .findViewById(R.id.admin_dashboard_user_list_user_name);
        ImageButton removeUserButton = view
                .findViewById(R.id.admin_dashboard_user_list_remove_user_button);

        // Set profile image
        String imageUrl = user.getImage();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.poster)
                    .error(R.drawable.poster)
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.poster);
        }

        // Set username
        userName.setText(user.getName());

        // Click username to go to profile
        userName.setOnClickListener(v -> {
            if (user.getId().equals(UserSession.getInstance().getCurrentUser().getId())) {
                Toast.makeText(context, "You cannot access your own account from this menu", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(context, UserProfileActivity.class);
            intent.putExtra("deviceId", user.getId());
            context.startActivity(intent);
        });

        // Remove user from database
        removeUserButton.setOnClickListener(v -> {
            // Prevent admin from deleting their own profile from here
            if (user.getId().equals(UserSession.getInstance().getCurrentUser().getId())) {
                Toast.makeText(context, "You cannot delete your own account", Toast.LENGTH_SHORT).show();
                return;
            }
            // First remove user from all event entrant subcollections
            db.collection(FirestoreCollections.EVENTS_COLLECTION).get()
                    .addOnSuccessListener(eventSnapshots -> {
                        for (QueryDocumentSnapshot eventSnapshot : eventSnapshots) {
                            eventSnapshot.getReference()
                                    .collection("entrants")
                                    .document(user.getId())
                                    .delete();
                        }

                        // Remove all events organized by ONLY this user
                        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                                .whereArrayContains("organizers", user.getId())
                                .get()
                                .addOnSuccessListener(snapshot -> {
                                    for (QueryDocumentSnapshot eventSnapshot : snapshot) {
                                        java.util.List<String> organizers = (java.util.List<String>) eventSnapshot.get("organizers");
                                        if (organizers != null && organizers.size() == 1) {
                                            removeEvent(eventSnapshot.getId(), eventSnapshot.getString("image"));
                                        }
                                    }
                                });

                        // Remove the users poster from the database
                        db.collection(FirestoreCollections.IMAGES_COLLECTION)
                                .whereEqualTo("URL", user.getImage())
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                        new Image(user.getImage(), doc.getId()).delete(context, db);
                                    }
                                    // Then delete the user document itself
                                    db.collection(FirestoreCollections.USERS_COLLECTION).document(user.getId()).delete()
                                            .addOnSuccessListener(unused -> {
                                                Toast.makeText(context, "Profile deleted", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(context, "Failed to delete profile", Toast.LENGTH_SHORT).show());
                                });
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Failed to delete profile", Toast.LENGTH_SHORT).show());
        });

        return view;
    }

    /**
     * Performs logic to remove event from the Firestore Database, including any references to that event
     *
     * @param eventId   Firestore ID of event
     * @param eventImage    Image URL of event poster
     */
    private void removeEvent(String eventId, String eventImage) {
        // Code generated by Claude AI March 11, 2026
        // "How to remove event ID from a users list of events for each user document in the
        // events 'entrants' subcollection."
        CollectionReference eventEntrantsRef = db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId).collection("entrants");
        CollectionReference eventCommentsRef = db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId).collection("comments");
        CollectionReference eventNotificationsRef = db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId).collection("notifications");

        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> entrantsTask = eventEntrantsRef.get();
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> commentsTask = eventCommentsRef.get();
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> notificationsTask = eventNotificationsRef.get();

        com.google.android.gms.tasks.Tasks.whenAllSuccess(entrantsTask, commentsTask, notificationsTask)
                .addOnSuccessListener(results -> {
                    WriteBatch batch = db.batch();

                    com.google.firebase.firestore.QuerySnapshot userSnapshots = (com.google.firebase.firestore.QuerySnapshot) results.get(0);
                    com.google.firebase.firestore.QuerySnapshot commentSnapshots = (com.google.firebase.firestore.QuerySnapshot) results.get(1);
                    com.google.firebase.firestore.QuerySnapshot notificationSnapshots = (com.google.firebase.firestore.QuerySnapshot) results.get(2);

                    // Remove event from each user's events array and delete entrant docs
                    for (DocumentSnapshot userDoc : userSnapshots.getDocuments()) {
                        DocumentReference userRef = db.collection(FirestoreCollections.USERS_COLLECTION).document(userDoc.getId());
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("events", FieldValue.arrayRemove(eventId));
                        batch.set(userRef, updateData, SetOptions.merge());
                        batch.delete(userDoc.getReference());
                    }

                    // Delete all comment documents
                    for (DocumentSnapshot commentDoc : commentSnapshots.getDocuments()) {
                        batch.delete(commentDoc.getReference());
                    }

                    // Delete all notification documents
                    for (DocumentSnapshot notificationDoc : notificationSnapshots.getDocuments()) {
                        batch.delete(notificationDoc.getReference());
                    }

                    // Remove the event's poster from the database
                    db.collection(FirestoreCollections.IMAGES_COLLECTION)
                            .whereEqualTo("URL", eventImage)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    new Image(eventImage, doc.getId()).delete(context, db);
                                }
                            });

                    // Delete event document and commit
                    batch.delete(db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId));

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Firestore", "Event deleted successfully");
                                Toast.makeText(context, "Event deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Firestore", "Error deleting event", e);
                                Toast.makeText(context, "Failed to delete event", Toast.LENGTH_SHORT).show();
                            });

                }).addOnFailureListener(e -> Log.e("Firestore", "Error fetching subcollections", e));
    };
}
