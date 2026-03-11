package com.icarus.events;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class EntrantEventListActivity extends AppCompatActivity {
    //Define attributes
    private ListView eventListView;
    //private EditText searchTextFilter; Not Adding Yet
    //Placeholder filter buttons, will replace later.
    private Button showSportsFilterButton;
    private Button showMusicFilterButton;
    private Button showEducationFilterButton;
    private ArrayList<Event> eventArrayList;
    private HashMap<String, Boolean> currentFilters;
    private ArrayList<Event> filteredEventArrayList;
    private EntrantEventListArrayAdapter eventListArrayAdapter;
    private CollectionReference eventsRef;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_event_list);

        // Initialize database reference and collection references
        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("events");

        // Set views
        eventListView = findViewById(R.id.entrant_event_list_view);

        // Initialize buttons
        showSportsFilterButton = findViewById(R.id.entrant_event_list_sports_filter_button);
        showMusicFilterButton = findViewById(R.id.entrant_event_list_music_filter_button);
        showEducationFilterButton = findViewById(R.id.entrant_event_list_education_filter_button);

        // Create normal & filtered event list
        eventArrayList = new ArrayList<>();
        filteredEventArrayList = new ArrayList<>();
        eventListArrayAdapter = new EntrantEventListArrayAdapter(this,
                filteredEventArrayList);

        // Initialize current filters
        currentFilters = new HashMap<>();
        currentFilters.put("Sports", false);
        currentFilters.put("Music", false);
        currentFilters.put("Education", false);

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
                    String category  = snapshot.getString("category");
                    double capacity = snapshot.getDouble("capacity");
                    Date regOpen = snapshot.getDate("open");
                    Date regClose = snapshot.getDate("close");
                    Date date = snapshot.getDate("date");


                    eventArrayList.add(
                            new Event(
                                    id,
                                    name,
                                    category,
                                    capacity,
                                    regOpen,
                                    regClose,
                                    date));
                }
                applyFilters();
            }
        });

        // Set ListView adapter
        eventListView.setAdapter(eventListArrayAdapter);

        // Set navigation on click listeners
        /**eventListView.setOnItemClickListener((parent, view, position, id) -> {
            Event selected = filteredEventArrayList.get(position);
            Intent intent = new Intent(this, EntrantEventDetailActivity.class);
            intent.putExtra("eventId", selected.getId());
            intent.putExtra("eventName", selected.getName());
            startActivity(intent);
        });**/

        // Set buttons on click listeners
        showSportsFilterButton.setOnClickListener(v -> {
            handleFilterEvent("Sports", showSportsFilterButton);
        });

        showMusicFilterButton.setOnClickListener(v -> {
            handleFilterEvent("Music", showMusicFilterButton);
        });
        showEducationFilterButton.setOnClickListener(v -> {
            handleFilterEvent("Education", showEducationFilterButton);
        });

        // Check if list is empty, if so hide list and show message
        /**if (eventArrayList.isEmpty()) {
            findViewById(R.id.entrant_event_list_empty_text).setVisibility(VISIBLE);
            eventListView.setVisibility(GONE);
        }**/
    }

    private void handleFilterEvent(String filterName, Button button) {
        //Check if already true in filter list
        boolean selected = Boolean.TRUE.equals(currentFilters.get(filterName));
        if (selected) {
            //Set button to be normal colour
            currentFilters.put(filterName, false);

            button.setBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.primary_container
                    )
            );
            button.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.black
                    )
            );
        } else {
            //Set button to be selected colour
            currentFilters.put(filterName, true);
            button.setBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.primary_container_highlighted
                    )
            );
            button.setTextColor(androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.white
                    )
            );
        }
        applyFilters();
    }

    private void applyFilters() {
        filteredEventArrayList.clear();
        // Checking if any filters are set before proceeding
        boolean noneSelected = !currentFilters.containsValue(true);
        // If no filters are set, show all events
        if (noneSelected) {
            filteredEventArrayList.addAll(eventArrayList);
            eventListArrayAdapter.notifyDataSetChanged();
            return;
        }
        // If filters are set, only show relevant events
        for (Event event : eventArrayList) {
            if ( Boolean.TRUE.equals(currentFilters.get(event.getCategory())) ) {
                filteredEventArrayList.add(event);
            }
        }
        // Update filtered list
        eventListArrayAdapter.notifyDataSetChanged();
    }


}
