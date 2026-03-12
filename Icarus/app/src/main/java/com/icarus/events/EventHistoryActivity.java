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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import androidx.appcompat.app.AlertDialog;

/**
 * Activity that displays the event history for the current user.
 * <p>
 * Retrieves events associated with the logged-in user from Firebase Firestore
 * and displays them in a ListView. Supports filtering events by search text
 * and category.
 *
 * @author Benjamin Hall
 */
public class EventHistoryActivity extends AppCompatActivity {
    //Define attributes
    private ListView eventHistoryListView;
    private EditText searchTextFilter;
    private String currentSearch = "";
    private Button filterCategoryButton;
    private ArrayList<Event> eventHistoryArrayList;
    private HashMap<String, Boolean> currentFilters;
    private ArrayList<Event> filteredEventHistoryArrayList;
    private EventHistoryListArrayAdapter eventHistoryListArrayAdapter;
    private CollectionReference eventsRef;
    private User user;
    private FirebaseFirestore db;

    /**
     * Initializes the event history activity.
     * <p>
     * Sets up the layout, initializes Firestore references, retrieves the
     * current user's events, and configures the ListView adapter and UI
     * controls for searching and filtering events.
     *
     * @param savedInstanceState the previously saved activity state, or null if
     *                           the activity is being created for the first time
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_history);

        // Initialize database reference and collection references
        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("events");
        user = UserSession.getInstance().getCurrentUser();

        // Set views
        eventHistoryListView = findViewById(R.id.event_history_list_view);

        filterCategoryButton = findViewById(R.id.event_history_filter_button);

        //Initialize text filter
        searchTextFilter = findViewById(R.id.event_history_search_filter);
        searchTextFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString().trim().toLowerCase();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Create normal & filtered event list
        eventHistoryArrayList = new ArrayList<>();
        filteredEventHistoryArrayList = new ArrayList<>();
        eventHistoryListArrayAdapter = new EventHistoryListArrayAdapter(this,
                filteredEventHistoryArrayList);

        // Initialize current filters
        currentFilters = new HashMap<>();
        currentFilters.put("Sports", false);
        currentFilters.put("Music", false);
        currentFilters.put("Education", false);

        // Generated from Claude AI on March 11, 2026
        // "I want to only get events that are in the current users event list"
        // Get the current user's eventIds first, then fetch only those events
        db.collection("users").document(user.getId()).addSnapshotListener((userSnapshot, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
                return;
            }
            if (userSnapshot == null || !userSnapshot.exists()) return;

            ArrayList<String> eventIds = (ArrayList<String>) userSnapshot.get("events");

            if (eventIds == null || eventIds.isEmpty()) {
                eventHistoryArrayList.clear();
                applyFilters();
                return;
            }

            // Firestore whereIn limit is 30 — take the first 30 if needed
            ArrayList<String> queryIds = eventIds.size() > 30 ? new ArrayList<>(eventIds.subList(0, 30)) : eventIds;

            eventsRef.whereIn(FieldPath.documentId(), queryIds)
                    .addSnapshotListener((value, eventsError) -> {
                        if (eventsError != null) {
                            Log.e("Firestore", eventsError.toString());
                            return;
                        }
                        if (value != null) {
                            eventHistoryArrayList.clear();
                            for (QueryDocumentSnapshot snapshot : value) {
                                String id = snapshot.getId();
                                String name = snapshot.getString("name");
                                String category = snapshot.getString("category");
                                Double capacity = snapshot.getDouble("capacity");
                                Date regOpen = snapshot.getDate("open");
                                Date regClose = snapshot.getDate("close");
                                Date date = snapshot.getDate("date");
                                String location = snapshot.getString("location");
                                String image = snapshot.getString("image");
                                String organizer = snapshot.getString("organizer");

                                eventHistoryArrayList.add(new Event(
                                        id, name, category, capacity,
                                        regOpen, regClose, date, location, image, organizer));
                            }
                            applyFilters();
                        }
                    });
        });

        // Set ListView adapter
        eventHistoryListView.setAdapter(eventHistoryListArrayAdapter);

        // Set navigation on click listeners
        /**eventListView.setOnItemClickListener((parent, view, position, id) -> {
         Event selected = filteredEventArrayList.get(position);
         Intent intent = new Intent(this, EntrantEventDetailActivity.class);
         intent.putExtra("eventId", selected.getId());
         startActivity(intent);
         });**/


        filterCategoryButton.setOnClickListener(v -> showCategoryFilterDialog());

        // Check if list is empty, if so hide list and show message
        /**if (eventArrayList.isEmpty()) {
         findViewById(R.id.entrant_event_list_empty_text).setVisibility(VISIBLE);
         eventListView.setVisibility(GONE);
         }**/
    }

    /**
     * Updates the visual state of a category filter button and reapplies
     * the active filters to the event history list.
     *
     * @param filterName the category associated with the button
     * @param button the button that was clicked
     */
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

    /**
     * Applies the current search text and category filters to the event history list.
     * <p>
     * Updates the filtered event list based on the active filters and refreshes
     * the ListView adapter to display the results.
     */
    private void applyFilters() {
        filteredEventHistoryArrayList.clear();
        // Checking if any filters are set before proceeding
        boolean noCategoriesSelected = !currentFilters.containsValue(true);
        // If no filters are set, show all events
        for (Event event : eventHistoryArrayList) {
            boolean matchesSearch = currentSearch.isEmpty()
                    || (event.getName() != null
                    && event.getName().toLowerCase().contains(currentSearch));
            boolean matchesCategory = noCategoriesSelected
                    || Boolean.TRUE.equals(currentFilters.get(event.getCategory()));

            if (matchesSearch && matchesCategory) {
                filteredEventHistoryArrayList.add(event);
            }
        }
        // Update filtered list
        eventHistoryListArrayAdapter.notifyDataSetChanged();
    }

    /**
     * Displays a dialog allowing the user to select event categories to filter.
     * <p>
     * The dialog presents a multi-selection list of categories and updates the
     * active filters when the user applies or clears the selections.
     * <p>
     * Taken from Claude March 10th 2026, "How can I adapt my current filter buttons
     * to be in an alert dialog?"
     */
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
