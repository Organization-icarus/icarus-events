package com.icarus.events;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cloudinary.android.MediaManager;
import com.cloudinary.utils.ObjectUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Adapter used in the administrator dashboard to display images in a ListView.
 * <p>
 * Binds Image objects to the image list item layout and provides administrator
 * controls for removing images from Firebase Firestore and Cloudinary.
 *
 * @author Benjamin Hall
 */
public class AdministratorDashboardImageArrayAdapter extends ArrayAdapter<Image> {
    private ArrayList<Image> images;
    private Context context;
    private FirebaseFirestore db;

    /**
     * Constructs an adapter for displaying Image objects in the administrator
     * dashboard image list.
     *
     * @param context the context used to inflate views and access resources
     * @param images the list of images to be displayed by the adapter
     */
    public AdministratorDashboardImageArrayAdapter(Context context, ArrayList<Image> images){
        super(context, 0, images);
        this.images = images;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Returns a view for displaying n image in the administrator image list.
     * <p>
     * Inflates the list item layout if necessary, binds the data to the UI
     * components, and configures actions for viewing user details and
     * removing the user from the database.
     *
     * @param position the position of the user in the adapter's data set
     * @param convertView a view to reuse
     * @param parent the parent view that this view will be attached to
     * @return the view representing the image at the specified position
     */
    @NonNull
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
        View view = convertView;
        if (view == null){
            view = LayoutInflater.from(context)
                    .inflate(R.layout.administrator_dashboard_image_list_content, parent, false);
        }

        Image image = images.get(position);
        ImageView imageView = view
                .findViewById(R.id.admin_dashboard_image_list_image);
        ImageButton removeImageButton = view
                .findViewById(R.id.admin_dashboard_image_list_remove_image_button);

        // Set image
        db.collection(FirestoreCollections.IMAGES_COLLECTION)
                .document(image.getPublicId()).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String imageURL = snapshot.getString("URL");
                        if (imageURL != null && !imageURL.isEmpty()) {
                            Picasso.get()
                                    .load(snapshot.getString("URL"))
                                    .error(R.drawable.poster)           // Optional: shows if link fails
                                    .into(imageView);
                        }
                    }
                })
                .addOnFailureListener( e -> {
                    Toast.makeText(this.context, "Failed to load image", Toast.LENGTH_SHORT).show();
                });

        // remove image
        removeImageButton.setOnClickListener(v -> {
            image.delete(context, db);
            Toast.makeText(context, "Image deleted", Toast.LENGTH_SHORT).show();
        });

        return view;
    }
}
