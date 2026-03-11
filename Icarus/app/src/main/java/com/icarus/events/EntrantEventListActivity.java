package com.icarus.events;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import androidx.appcompat.app.AlertDialog;


public class EntrantEventListActivity extends NavigationBarActivity {
    //Define attributes
    private ListView eventListView;
    private EditText searchTextFilter;
    private String currentSearch = "";
    //Placeholder filter buttons, will replace later.
    //private Button showSportsFilterButton;
    //private Button showMusicFilterButton;
    //private Button showEducationFilterButton;
    private Button filterCategoryButton;
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
        setupNavBar();

        // Initialize database reference and collection references
        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("events");

        // Set views
        eventListView = findViewById(R.id.entrant_event_list_view);

        // Initialize buttons
        //showSportsFilterButton = findViewById(R.id.entrant_event_list_sports_filter_button);
        //showMusicFilterButton = findViewById(R.id.entrant_event_list_music_filter_button);
        //showEducationFilterButton = findViewById(R.id.entrant_event_list_education_filter_button);
        filterCategoryButton = findViewById(R.id.entrant_event_list_filter_button);

        //Initialize text filter
        searchTextFilter = findViewById(R.id.entrant_event_list_search_filter);
        searchTextFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString().trim().toLowerCase();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

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
            startActivity(intent);
        });**/

        // Set buttons on click listeners
        filterCategoryButton.setOnClickListener(v -> showCategoryFilterDialog());
//        showSportsFilterButton.setOnClickListener(v -> {
//            handleFilterEvent("Sports", showSportsFilterButton);
//        });
//        showMusicFilterButton.setOnClickListener(v -> {
//            handleFilterEvent("Music", showMusicFilterButton);
//        });
//        showEducationFilterButton.setOnClickListener(v -> {
//            handleFilterEvent("Education", showEducationFilterButton);
//        });

        // Check if list is empty, if so hide list and show message
        /**if (eventArrayList.isEmpty()) {
            findViewById(R.id.entrant_event_list_empty_text).setVisibility(VISIBLE);
            eventListView.setVisibility(GONE);
        }**/
    }

    private void handleButtonClick(String filterName, Button button) {
        //Check if already true in filter list
        boolean selected = Boolean.TRUE.equals(currentFilters.get(filterName));
        if (selected) {
            //Set button to be normal colour
            button.setBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.primary_container
                    )
            );
            button.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.primary_container_highlighted
                    )
            );
        } else {
            //Set button to be selected colour
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
        applyFilters();
    }

    private void applyFilters() {
        filteredEventArrayList.clear();
        // Checking if any filters are set before proceeding
        boolean noCategoriesSelected = !currentFilters.containsValue(true);
        // If no filters are set, show all events
        for (Event event : eventArrayList) {
            boolean matchesSearch = currentSearch.isEmpty()
                    || (event.getName() != null
                    && event.getName().toLowerCase().contains(currentSearch));
            boolean matchesCategory = noCategoriesSelected
                    || Boolean.TRUE.equals(currentFilters.get(event.getCategory()));

            if (matchesSearch && matchesCategory) {
                filteredEventArrayList.add(event);
            }
        }
        // Update filtered list
        eventListArrayAdapter.notifyDataSetChanged();
    }

    // Taken from Claude March 10th 2026, "How can I adapt my current filter buttons to
    // be in an alert dialog?"
    private void showCategoryFilterDialog() {
        String[] categories = currentFilters.keySet().toArray(new String[0]);
        boolean[] checkedItems = new boolean[categories.length];
        for (int i = 0; i < categories.length; i++) {
            checkedItems[i] = Boolean.TRUE.equals(currentFilters.get(categories[i]));
        }

        new AlertDialog.Builder(this)
                .setTitle("Filter by Category")
                .setMultiChoiceItems(categories, checkedItems, (dialog, which, isChecked) -> {
                    currentFilters.put(categories[which], isChecked);
                })
                .setPositiveButton("Apply", (dialog, which) -> applyFilters())
                .setNegativeButton("Clear All", (dialog, which) -> {
                    currentFilters.replaceAll((c, v) -> false);
                    applyFilters();
                })
                .show();
    }


}
