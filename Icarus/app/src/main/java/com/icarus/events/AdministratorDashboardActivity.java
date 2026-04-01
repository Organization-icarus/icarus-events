package com.icarus.events;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides the administrator dashboard for managing application data.
 * <p>
 * Displays lists of events, users, and images, and retrieves event and user data
 * from Firebase Firestore. Allows administrators to switch between lists and view
 * the current contents of each collection.
 *
 * @author Benjamin Hall
 */
public class AdministratorDashboardActivity extends HeaderNavBarActivity {
    private RecyclerView eventListView;
    private ListView userListView;
    private ListView imageListView;
    private Button showEventListButton;
    private Button showUserListButton;
    private Button showImageListButton;
    private FirebaseFirestore db;
    private CollectionReference eventsRef;
    private CollectionReference usersRef;
    private CollectionReference imagesRef;
    private ArrayList<Event> eventArrayList;
    private RecyclerView.Adapter eventArrayAdapter;
    private ArrayList<User> userArrayList;
    private ArrayAdapter<User> userArrayAdapter;
    private ArrayList<Image> imageArrayList;
    private ArrayAdapter<Image> imageArrayAdapter;
    private Map<String, String> categoryColors;

    /**
     * Initializes the administrator dashboard activity.
     * <p>
     * Sets the layout, configures Firestore references and snapshot listeners,
     * initializes event and user adapters, and sets up buttons for switching
     * between the event, user, and image lists.
     *
     * @param savedInstanceState the previously saved activity state, or null if
     *                           the activity is being created for the first time
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_administrator_dashboard);
        setupNavBar();

        // Initialize database reference and collection references
        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection(FirestoreCollections.EVENTS_COLLECTION);
        usersRef = db.collection(FirestoreCollections.USERS_COLLECTION);
        imagesRef = db.collection(FirestoreCollections.IMAGES_COLLECTION);

        // Set views
        eventListView = findViewById(R.id.admin_dashboard_event_list);
        userListView = findViewById(R.id.admin_dashboard_user_list);
        imageListView = findViewById(R.id.admin_dashboard_image_list);

        // Initialize current colors
        categoryColors = new HashMap<>();
        db.collection("event-categories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->{
                    for(DocumentSnapshot doc : queryDocumentSnapshots){
                        String category = doc.getString("category");
                        String color = doc.getString("color");
                        //check for null
                        if (category != null && color != null) {
                            categoryColors.put(category, color);
                        }
                    }
                    eventArrayAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load categories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // Initialize buttons
        showEventListButton = findViewById(R.id.admin_dashboard_show_event_list_button);
        showUserListButton = findViewById(R.id.admin_dashboard_show_user_list_button);
        showImageListButton = findViewById(R.id.admin_dashboard_show_image_list_button);

        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.admin_dashboard_list_buttons);
        toggleGroup.check(R.id.admin_dashboard_show_event_list_button);
        eventListView.setVisibility(VISIBLE);
        userListView.setVisibility(GONE);
        imageListView.setVisibility(GONE);

        // create event, user, and image array
        eventArrayList = new ArrayList<>();
        eventArrayAdapter = new AdministratorDashboardEventArrayAdapter(this,
                eventArrayList, categoryColors);
        userArrayList = new ArrayList<>();
        userArrayAdapter = new AdministratorDashboardUserArrayAdapter(this, userArrayList);
        imageArrayList = new ArrayList<>();
        imageArrayAdapter = new AdministratorDashboardImageArrayAdapter(this, imageArrayList);

        // Get all items in the collection
        eventsRef.addSnapshotListener((value,error) -> {
            if (error != null){
                Log.e("Firestore", error.toString());
            }
            if (value != null) {
                eventArrayList.clear();
                for (QueryDocumentSnapshot snapshot : value) {
                    String id = snapshot.getId();
                    String name = snapshot.getString("name");
                    String category = snapshot.getString("category");
                    Date date = snapshot.getDate("date");
                    String posterURL = snapshot.getString("image");

                    eventArrayList.add(new Event(id, name, category, null, null, null, date, null, posterURL, null));
                }
                eventArrayAdapter.notifyDataSetChanged();
            }
        });

        usersRef.addSnapshotListener((value,error) -> {
            if (error != null){
                Log.e("Firestore", error.toString());
            }
            if (value != null) {
                userArrayList.clear();
                for (QueryDocumentSnapshot snapshot : value) {
                    String id = snapshot.getId();
                    String name = snapshot.getString("name");
                    String image = snapshot.getString("image");

                    userArrayList.add(new User(id, name, null, null,
                            image, null, null, null, null));
                }
                userArrayAdapter.notifyDataSetChanged();
            }
        });

        imagesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
            }
            if (value != null) {
                imageArrayList.clear();
                for (QueryDocumentSnapshot snapshot : value) {
                    String URL = snapshot.getString("URL");
                    String publicId = snapshot.getId();

                    imageArrayList.add(new Image(URL, publicId));
                }
                imageArrayAdapter.notifyDataSetChanged();
            }
        });

        // Set ListView adapters
        eventListView.setLayoutManager(new LinearLayoutManager(this));
        eventListView.setAdapter(eventArrayAdapter);
        userListView.setAdapter(userArrayAdapter);
        imageListView.setAdapter(imageArrayAdapter);

        // Setup buttons on click listeners
        showEventListButton.setOnClickListener(v -> {
            eventListView.setVisibility(VISIBLE);
            userListView.setVisibility(GONE);
            imageListView.setVisibility(GONE);
        });
        showUserListButton.setOnClickListener(v -> {
            eventListView.setVisibility(GONE);
            userListView.setVisibility(VISIBLE);
            imageListView.setVisibility(GONE);
        });
        showImageListButton.setOnClickListener(v -> {
            eventListView.setVisibility(GONE);
            userListView.setVisibility(GONE);
            imageListView.setVisibility(VISIBLE);
        });
    }
}
