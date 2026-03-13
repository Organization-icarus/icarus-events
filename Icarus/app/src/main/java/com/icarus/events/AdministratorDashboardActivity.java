package com.icarus.events;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;

/**
 * Provides the administrator dashboard for managing application data.
 * <p>
 * Displays lists of events, users, and images, and retrieves event and user data
 * from Firebase Firestore. Allows administrators to switch between lists and view
 * the current contents of each collection.
 *
 * @author Benjamin Hall
 */
public class AdministratorDashboardActivity extends NavigationBarActivity {
    private ListView eventListView;
    private ListView userListView;
    private ListView imageListView;
    private Button showEventListButton;
    private Button showUserListButton;
    private Button showImageListButton;
    private FirebaseFirestore db;
    private CollectionReference eventsRef;
    private CollectionReference usersRef;
    private ArrayList<Event> eventArrayList;
    private ArrayAdapter<Event> eventArrayAdapter;
    private ArrayList<User> userArrayList;
    private ArrayAdapter<User> userArrayAdapter;

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

        // Set views
        eventListView = findViewById(R.id.admin_dashboard_event_list);
        userListView = findViewById(R.id.admin_dashboard_user_list);
        imageListView = findViewById(R.id.admin_dashboard_image_list);

        // Initialize buttons
        showEventListButton = findViewById(R.id.admin_dashboard_show_event_list_button);
        showUserListButton = findViewById(R.id.admin_dashboard_show_user_list_button);
        showImageListButton = findViewById(R.id.admin_dashboard_show_image_list_button);

        selectButton(showEventListButton);
        deselectButton(showUserListButton);
        deselectButton(showImageListButton);
        eventListView.setVisibility(VISIBLE);
        userListView.setVisibility(GONE);
        imageListView.setVisibility(GONE);

        // create event & user array
        eventArrayList = new ArrayList<>();
        eventArrayAdapter = new AdministratorDashboardEventArrayAdapter(this,
                eventArrayList);
        userArrayList = new ArrayList<>();
        userArrayAdapter = new AdministratorDashboardUserArrayAdapter(this, userArrayList);

        // Get all items in the collection
        eventsRef.addSnapshotListener((value,error) -> {
            if (error != null){
                Log.e("Firestore", error.toString());
            }
            if (value != null && !value.isEmpty()) {
                eventArrayList.clear();
                for (QueryDocumentSnapshot snapshot : value) {
                    String id = snapshot.getId();
                    String name = snapshot.getString("name");
                    String category = snapshot.getString("category");
                    Date date = snapshot.getDate("date");

                    eventArrayList.add(new Event(id, name, category, null, null, null, date, null, null, null));
                }
                eventArrayAdapter.notifyDataSetChanged();
            }
        });

        usersRef.addSnapshotListener((value,error) -> {
            if (error != null){
                Log.e("Firestore", error.toString());
            }
            if (value != null && !value.isEmpty()) {
                userArrayList.clear();
                for (QueryDocumentSnapshot snapshot : value) {
                    String id = snapshot.getId();
                    String name = snapshot.getString("name");

                    userArrayList.add(new User(id, name, null, null, null, null, null));
                }
                userArrayAdapter.notifyDataSetChanged();
            }
        });

        // Set ListView adapters
        eventListView.setAdapter(eventArrayAdapter);
        userListView.setAdapter(userArrayAdapter);

        // Setup buttons on click listeners
        showEventListButton.setOnClickListener(v -> {
            eventListView.setVisibility(VISIBLE);
            userListView.setVisibility(GONE);
            imageListView.setVisibility(GONE);
            selectButton(showEventListButton);
            deselectButton(showUserListButton);
            deselectButton(showImageListButton);
        });
        showUserListButton.setOnClickListener(v -> {
            eventListView.setVisibility(GONE);
            userListView.setVisibility(VISIBLE);
            imageListView.setVisibility(GONE);
            deselectButton(showEventListButton);
            selectButton(showUserListButton);
            deselectButton(showImageListButton);
        });
        showImageListButton.setOnClickListener(v -> {
            eventListView.setVisibility(GONE);
            userListView.setVisibility(GONE);
            imageListView.setVisibility(VISIBLE);
            deselectButton(showEventListButton);
            deselectButton(showUserListButton);
            selectButton(showImageListButton);
        });
    }

    /**
     * Applies a style to a button to indicate that it is currently selected in the UI.
     *
     * @param button the button to apply the 'selected' styling to
     */
    private void selectButton(Button button) {
        button.setBackgroundColor(
                androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.primary_container_highlighted
                )
        );
        button.setTextColor(androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.primary_container
                )
        );
    }

    /**
     * Applies a style to a button to indicate that it is not currently selected in the UI.
     *
     * @param button the button to apply the 'deselected' styling to
     */
    private void deselectButton(Button button) {
        button.setBackgroundColor(
                androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.primary_container
                )
        );
        button.setTextColor(androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.primary_container_highlighted
                )
        );
    }
}
