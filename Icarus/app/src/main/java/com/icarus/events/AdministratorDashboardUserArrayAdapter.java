package com.icarus.events;

import android.app.Activity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

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
        Picasso.get()
                .load(user.getImage())
                .placeholder(R.drawable.poster)
                .error(R.drawable.poster)           // Optional: shows if link fails
                .into(profileImage);

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
}
