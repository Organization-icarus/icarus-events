package com.icarus.events;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.view.ContextThemeWrapper;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


//@TODO Put filter button on same horizontal level as searhc events, open a pop up menu
// with options for capacity, and other things CHECK ALL US
/**
 * Activity that displays the list of available events for entrants.
 * <p>
 * Retrieves events from Firebase Firestore, displays them in a ListView,
 * and allows users to filter events by search text and category. Users
 * can also navigate to create a new event.
 *
 * @author Alex Alves
 */
public class EntrantEventListActivity extends NavigationBarActivity {
    //Define attributes
    private RecyclerView eventListView;
    private EditText searchTextFilter;
    private String currentSearch = "";
    private MaterialButton filterCategoryButton;
    private MaterialButton qrButton;
    private FloatingActionButton addEvent;
    private FloatingActionButton adminDashboard;
    private ArrayList<Event> eventArrayList;
    private HashMap<String, Boolean> currentFilters;
    private ArrayList<Event> filteredEventArrayList;
    private EntrantEventListArrayAdapter eventListArrayAdapter;
    private CollectionReference eventsRef;
    private FirebaseFirestore db;
    private Double maxCapacityFilter = null;
    private Date startDateFilter = null;
    private Date endDateFilter = null;
    private Boolean fullStatusFilter = null;
    private Map<String, String> categoryColors;
    private HashMap<String, Long> eventWaitingCounts;
    private HashMap<String, ListenerRegistration> waitlistListeners;
    private final SimpleDateFormat filterDateFormat =
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    private static final int MENU_STATUS_ANY = 1;
    private static final int MENU_STATUS_FULL = 2;
    private static final int MENU_STATUS_NOT_FULL = 3;

    /**
     * Initializes the entrant event list activity.
     * <p>
     * Sets up the layout, navigation bar, Firestore references, ListView adapter,
     * and UI controls for searching and filtering events. Also registers a
     * snapshot listener to keep the event list synchronized with Firestore.
     *
     * @param savedInstanceState the previously saved activity state, or null if
     *                           the activity is being created for the first time
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_event_list);
        setupNavBar();

        // Initialize database reference and collection references
        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection(FirestoreCollections.EVENTS_COLLECTION);

        // Set views
        eventListView = findViewById(R.id.entrant_event_list_view);

        // Initialize buttons
        filterCategoryButton = findViewById(R.id.entrant_event_list_filter_button);
        addEvent = findViewById(R.id.entrant_event_list_add_event_button);
        adminDashboard = findViewById(R.id.entrant_event_list_admin_dashboard_button);
        qrButton = findViewById(R.id.entrant_event_list_qr_button);

        // Set button colours
        addEvent.setImageTintList(ColorStateList.valueOf(getColor(R.color.primary)));
        adminDashboard.setImageTintList(ColorStateList.valueOf(getColor(R.color.primary)));

        // Retrieve current user role
        User currentUser = UserSession.getInstance().getCurrentUser();
        Boolean isAdmin = (currentUser != null) ? currentUser.getIsAdmin() : false;

        // Show/hide buttons based on users role
        if (isAdmin) {
            adminDashboard.setVisibility(VISIBLE);
        } else {
            adminDashboard.setVisibility(GONE);
        }


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
        categoryColors = new HashMap<>();
        eventWaitingCounts = new HashMap<>();
        waitlistListeners = new HashMap<>();
        eventListArrayAdapter = new EntrantEventListArrayAdapter(this,
                filteredEventArrayList, position -> {
            // Set navigation on click listeners
            Event selected = filteredEventArrayList.get(position);
            Intent intent = new Intent(this, EventDetailsActivity.class);
            intent.putExtra("eventId", selected.getId());
            startActivity(intent);
        }, categoryColors);

        // Initialize current filters and colors
        currentFilters = new HashMap<>();
        db.collection("event-categories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->{
                    for(DocumentSnapshot doc : queryDocumentSnapshots){
                        String category = doc.getString("category");
                        String color = doc.getString("color");
                        //check for null
                        if (category != null){
                            currentFilters.put(category, false);
                        }
                        if (category != null && color != null) {
                            categoryColors.put(category, color);
                        }
                    }
                    eventListArrayAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load categories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // Get all items in the collection
        eventsRef.addSnapshotListener((value,error) -> {
            if (error != null){
                Log.e("Firestore", error.toString());
            }
            if (value != null && !value.isEmpty()) {
                eventArrayList.clear();

                ArrayList<String> activeEventIds = new ArrayList<>();

                for (QueryDocumentSnapshot snapshot : value) {
                    String id = snapshot.getId();
                    String name = snapshot.getString("name");
                    String category  = snapshot.getString("category");
                    Double capacity = snapshot.getDouble("capacity");
                    Date regOpen = snapshot.getDate("open");
                    Date regClose = snapshot.getDate("close");
                    Date date = snapshot.getDate("date");
                    String location = snapshot.getString("location");
                    String image = snapshot.getString("image");
                    ArrayList<String> organizers = (ArrayList<String>) snapshot.get("organizers");

                    activeEventIds.add(id);
                    attachWaitlistListener(id);

                    eventArrayList.add(
                            new Event(
                                    id,
                                    name,
                                    category,
                                    capacity,
                                    regOpen,
                                    regClose,
                                    date,
                                    location,
                                    image,
                                    organizers));
                }

                ArrayList<String> staleIds = new ArrayList<>();
                for (String existingId : waitlistListeners.keySet()) {
                    if (!activeEventIds.contains(existingId)) {
                        staleIds.add(existingId);
                    }
                }

                for (String staleId : staleIds) {
                    ListenerRegistration registration = waitlistListeners.remove(staleId);
                    if (registration != null) {
                        registration.remove();
                    }
                    eventWaitingCounts.remove(staleId);
                }

                applyFilters();
            }
        });

        // Set RecyclerView adapter
        eventListView.setLayoutManager(new LinearLayoutManager(this));
        eventListView.setAdapter(eventListArrayAdapter);

        // Set buttons on click listeners
        addEvent.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrganizerCreateEventActivity.class);
            startActivity(intent);
        });

        adminDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdministratorDashboardActivity.class);
            startActivity(intent);
        });

        filterCategoryButton.setOnClickListener(v -> showFilterDialog());
    }

    /**
     * Handles a category filter button click by toggling its visual state
     * and applying the updated filters to the event list. (Currently not in use)
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
     * Applies the current search text and all active filters to the event list.
     * <p>
     * Updates the filtered event list based on the active filters and refreshes
     * the RecyclerView adapter to display the results.
     */
    private void applyFilters() {
        filteredEventArrayList.clear();
        boolean noCategoriesSelected = !currentFilters.containsValue(true);

        for (Event event : eventArrayList) {
            boolean matchesSearch = currentSearch.isEmpty()
                    || (event.getName() != null
                    && event.getName().toLowerCase().contains(currentSearch));

            boolean matchesCategory = noCategoriesSelected
                    || Boolean.TRUE.equals(currentFilters.get(event.getCategory()));

            boolean matchesCapacity = maxCapacityFilter == null
                    || (event.getCapacity() != null && event.getCapacity() <= maxCapacityFilter);

            Date eventDate = event.getDate();
            boolean matchesStartDate = startDateFilter == null
                    || (eventDate != null && !eventDate.before(startDateFilter));
            boolean matchesEndDate = endDateFilter == null
                    || (eventDate != null && !eventDate.after(endDateFilter));

            Long waitingCount = eventWaitingCounts.get(event.getId());
            boolean isFull = waitingCount != null
                    && waitingCount >= 0
                    && event.getCapacity() != null
                    && waitingCount >= event.getCapacity();
            boolean matchesFullStatus = fullStatusFilter == null
                    || (fullStatusFilter && isFull)
                    || (!fullStatusFilter && waitingCount != null && waitingCount >= 0 && !isFull);

            if (matchesSearch && matchesCategory && matchesCapacity
                    && matchesStartDate && matchesEndDate && matchesFullStatus) {
                filteredEventArrayList.add(event);
            }
        }

        eventListArrayAdapter.notifyDataSetChanged();
        boolean filtersActive = currentFilters.containsValue(true)
                || maxCapacityFilter != null
                || startDateFilter != null
                || endDateFilter != null
                || fullStatusFilter != null;

        filterCategoryButton.setBackgroundTintList(ColorStateList.valueOf(
                filtersActive
                        ? getColor(R.color.accent_first)
                        : getColor(R.color.white)));
    }

    // Taken from ChatGPT March 29th 2026,
    // "Implement waitlist-based capacity tracking using Firestore subcollection listeners"
    /**
     * Attaches a snapshot listener for the waiting-list count of the given event.
     *
     * @param eventId the event id
     */
    private void attachWaitlistListener(String eventId) {
        if (waitlistListeners.containsKey(eventId)) {
            return;
        }

        ListenerRegistration registration = db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(eventId)
                .collection("entrants")
                .whereEqualTo("status", "waiting")
                .addSnapshotListener((query, e) -> {
                    if (e != null) {
                        Log.e("Firestore", e.toString());
                        return;
                    }

                    if (query != null) {
                        eventWaitingCounts.put(eventId, (long) query.size());
                        applyFilters();
                    }
                });

        waitlistListeners.put(eventId, registration);
    }

    // Taken from ChatGPT March 29th 2026,
    // "Create a unified filter dialog with dropdown-based controls for multiple filters"
    /**
     * Displays the main filter dialog.
     * <p>
     * All filters are configured from this single dialog, including category,
     * full/not full status, max capacity, and date range.
     */
    private void showFilterDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(getColor(R.color.secondary));
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);

        MaterialButton categoryRow = buildDialogRow(getCategorySummaryText(), hasCategoryFilter());
        categoryRow.setOnClickListener(v -> showCategoryDropdown(categoryRow));
        addFilterRow(container, categoryRow);

        MaterialButton statusRow = buildDialogRow(getFullStatusSummaryText(), fullStatusFilter != null);
        statusRow.setOnClickListener(v -> showFullStatusDropdown(statusRow));
        addFilterRow(container, statusRow);

        MaterialButton capacityRow = buildDialogRow(getCapacitySummaryText(), maxCapacityFilter != null);
        capacityRow.setOnClickListener(v -> showMaxCapacityDialog(capacityRow));
        addFilterRow(container, capacityRow);

        MaterialButton startDateRow = buildDialogRow(getStartDateSummaryText(), startDateFilter != null);
        startDateRow.setOnClickListener(v -> showDatePicker(true, startDateRow));
        addFilterRow(container, startDateRow);

        MaterialButton endDateRow = buildDialogRow(getEndDateSummaryText(), endDateFilter != null);
        endDateRow.setOnClickListener(v -> showDatePicker(false, endDateRow));
        addFilterRow(container, endDateRow);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Filters")
                .setView(container)
                .setPositiveButton("Apply", (dialogInterface, which) -> applyFilters())
                .setNeutralButton("Clear All", (dialogInterface, which) -> {
                    currentFilters.replaceAll((c, v) -> false);
                    fullStatusFilter = null;
                    maxCapacityFilter = null;
                    startDateFilter = null;
                    endDateFilter = null;
                    applyFilters();
                })
                .setNegativeButton("Close", null)
                .create();

        dialog.show();
        TextView titleView = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if (titleView != null) {
            titleView.setTextColor(getColor(R.color.lightText));
            titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimension(R.dimen.font_heading_size));
        }
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.secondary);
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.accent_first));
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getColor(R.color.accent_first));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.lightText));
    }

    // Taken from ChatGPT March 29th 2026,
    // "Build reusable styled rows for dialog-based filter UI"
    /**
     * Builds a selectable row used inside the filter dialog.
     *
     * @param text the row text
     * @param active whether the row currently has an active filter
     * @return the configured TextView row
     */
    private MaterialButton buildDialogRow(String text, boolean active) {
        MaterialButton row = new MaterialButton(new ContextThemeWrapper(this, R.style.AppButton), null, 0);
        row.setText(text);
        row.setTextSize(16);
        row.setAllCaps(false);
        row.setTextAlignment(TextView.TEXT_ALIGNMENT_VIEW_START);

        ColorStateList darkTextColor = ColorStateList.valueOf(getColor(R.color.darkText));
        row.setTextColor(darkTextColor);
        row.setIconTint(darkTextColor);
        row.setBackgroundTintList(ColorStateList.valueOf(
                active ? getColor(R.color.accent_third) : getColor(R.color.white)
        ));
        row.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
        row.setStrokeColor(ColorStateList.valueOf(
                active ? getColor(R.color.accent_first) : getColor(R.color.white)
        ));

        int horizontalPadding = (int) (16 * getResources().getDisplayMetrics().density);
        int verticalPadding = (int) (16 * getResources().getDisplayMetrics().density);
        row.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        row.setClickable(true);
        row.setFocusable(true);
        row.setMinHeight((int) (56 * getResources().getDisplayMetrics().density));
        return row;
    }

    /**
     * Adds a styled filter row to the dialog container with spacing.
     *
     * @param container the parent layout
     * @param row the row view to add
     */
    private void addFilterRow(LinearLayout container, MaterialButton row) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = (int) (12 * getResources().getDisplayMetrics().density);
        row.setLayoutParams(params);
        container.addView(row);
    }

    // Taken from ChatGPT March 29th 2026,
    // "Implement dropdown menu for multi-select category filtering"
    /**
     * Displays a dropdown menu for category selection from inside the filter dialog.
     *
     * @param anchor the view to anchor the dropdown to
     */
    private void showCategoryDropdown(MaterialButton anchor) {
        PopupMenu popupMenu = new PopupMenu(new ContextThemeWrapper(this,
                androidx.appcompat.R.style.Theme_AppCompat_Light), anchor);
        Menu menu = popupMenu.getMenu();

        int index = 0;
        for (Map.Entry<String, Boolean> entry : currentFilters.entrySet()) {
            MenuItem item = menu.add(Menu.NONE, index, Menu.NONE, entry.getKey());
            item.setCheckable(true);
            item.setChecked(Boolean.TRUE.equals(entry.getValue()));
            index++;
        }

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString styledTitle = new SpannableString(item.getTitle());
            styledTitle.setSpan(new ForegroundColorSpan(getColor(R.color.darkText)), 0, styledTitle.length(), 0);
            item.setTitle(styledTitle);
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            String category = item.getTitle().toString();
            boolean currentlySelected = Boolean.TRUE.equals(currentFilters.get(category));
            currentFilters.put(category, !currentlySelected);
            anchor.setText(getCategorySummaryText());
            anchor.setTextColor(ColorStateList.valueOf(getColor(R.color.darkText)));
            anchor.setBackgroundTintList(ColorStateList.valueOf(
                    hasCategoryFilter() ? getColor(R.color.accent_third) : getColor(R.color.white)
            ));
            anchor.setStrokeColor(ColorStateList.valueOf(
                    hasCategoryFilter() ? getColor(R.color.accent_first) : getColor(R.color.white)
            ));
            return true;
        });

        popupMenu.show();
    }

    // Taken from ChatGPT March 29th 2026,
    // "Implement dropdown menu for waitlist full/not full filtering"
    /**
     * Displays a dropdown menu for waitlist full/not full status from inside the filter dialog.
     *
     * @param anchor the view to anchor the dropdown to
     */
    private void showFullStatusDropdown(MaterialButton anchor) {
        PopupMenu popupMenu = new PopupMenu(new ContextThemeWrapper(this,
                androidx.appcompat.R.style.Theme_AppCompat_Light), anchor);
        Menu menu = popupMenu.getMenu();

        menu.add(Menu.NONE, MENU_STATUS_ANY, Menu.NONE, "Waitlist Status: Any");
        menu.add(Menu.NONE, MENU_STATUS_FULL, Menu.NONE, "Waitlist Status: Full");
        menu.add(Menu.NONE, MENU_STATUS_NOT_FULL, Menu.NONE, "Waitlist Status: Not Full");

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString styledTitle = new SpannableString(item.getTitle());
            styledTitle.setSpan(new ForegroundColorSpan(getColor(R.color.darkText)), 0, styledTitle.length(), 0);
            item.setTitle(styledTitle);
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == MENU_STATUS_ANY) {
                fullStatusFilter = null;
            } else if (itemId == MENU_STATUS_FULL) {
                fullStatusFilter = true;
            } else if (itemId == MENU_STATUS_NOT_FULL) {
                fullStatusFilter = false;
            }
            anchor.setText(getFullStatusSummaryText());
            anchor.setTextColor(ColorStateList.valueOf(getColor(R.color.darkText)));
            anchor.setBackgroundTintList(ColorStateList.valueOf(
                    fullStatusFilter != null ? getColor(R.color.accent_third) : getColor(R.color.white)
            ));
            anchor.setStrokeColor(ColorStateList.valueOf(
                    fullStatusFilter != null ? getColor(R.color.accent_first) : getColor(R.color.white)
            ));
            return true;
        });

        popupMenu.show();
    }

    // Taken from ChatGPT March 29th 2026,
    // "Implement dialog input for numeric capacity filtering"
    /**
     * Displays a dialog for setting a maximum waitlist capacity filter.
     *
     * @param anchor the row text to refresh after selection
     */
    private void showMaxCapacityDialog(MaterialButton anchor) {
        final EditText input = new EditText(this);
        input.setHint("Enter maximum capacity");
        input.setHintTextColor(getColor(R.color.lightText));
        input.setTextColor(getColor(R.color.lightText));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        if (maxCapacityFilter != null) {
            input.setText(String.valueOf(maxCapacityFilter));
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Maximum Capacity")
                .setView(input)
                .setPositiveButton("Apply", (dialogInterface, which) -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) {
                        maxCapacityFilter = null;
                    } else {
                        try {
                            maxCapacityFilter = Double.parseDouble(value);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Enter a valid number.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    anchor.setText(getCapacitySummaryText());
                    anchor.setTextColor(ColorStateList.valueOf(getColor(R.color.darkText)));
                    anchor.setBackgroundTintList(ColorStateList.valueOf(
                            maxCapacityFilter != null ? getColor(R.color.accent_third) : getColor(R.color.white)
                    ));
                    anchor.setStrokeColor(ColorStateList.valueOf(
                            maxCapacityFilter != null ? getColor(R.color.accent_first) : getColor(R.color.white)
                    ));
                })
                .setNeutralButton("Clear", (dialogInterface, which) -> {
                    maxCapacityFilter = null;
                    anchor.setText(getCapacitySummaryText());
                    anchor.setTextColor(ColorStateList.valueOf(getColor(R.color.darkText)));
                    anchor.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.white)));
                    anchor.setStrokeColor(ColorStateList.valueOf(getColor(R.color.white)));
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.secondary);
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.accent_first));
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getColor(R.color.accent_first));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.lightText));
    }

    // Taken from ChatGPT March 29th 2026,
    // "Integrate date picker for filtering events by date range"
    /**
     * Displays a date picker to choose the start or end date filter.
     *
     * @param isStartDate true to set the start date, false for the end date
     * @param anchor the row text to refresh after selection
     */
    private void showDatePicker(boolean isStartDate, MaterialButton anchor) {
        Calendar calendar = Calendar.getInstance();
        Date currentDate = isStartDate ? startDateFilter : endDateFilter;

        if (currentDate != null) {
            calendar.setTime(currentDate);
        }

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(year, month, dayOfMonth);

            if (isStartDate) {
                setStartOfDay(selectedCalendar);
                startDateFilter = selectedCalendar.getTime();
                anchor.setText(getStartDateSummaryText());
            } else {
                setEndOfDay(selectedCalendar);
                endDateFilter = selectedCalendar.getTime();
                anchor.setText(getEndDateSummaryText());
            }
            anchor.setTextColor(ColorStateList.valueOf(getColor(R.color.darkText)));
            anchor.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.accent_third)));
            anchor.setStrokeColor(ColorStateList.valueOf(getColor(R.color.accent_first)));
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    // Taken from ChatGPT March 29th 2026,
    // "Add helper setters/getters for filter summary text and lifecycle cleanup for generated filter logic"
    /**
     * Normalizes a calendar value to the start of the selected day.
     *
     * @param calendar the calendar instance to update
     */
    private void setStartOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    /**
     * Normalizes a calendar value to the end of the selected day.
     *
     * @param calendar the calendar instance to update
     */
    private void setEndOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
    }

    /**
     * Returns whether any category filter is selected.
     *
     * @return true if at least one category is selected, otherwise false
     */
    private boolean hasCategoryFilter() {
        return currentFilters.containsValue(true);
    }

    /**
     * Returns summary text for the category row.
     *
     * @return summary text for selected categories
     */
    private String getCategorySummaryText() {
        ArrayList<String> selectedCategories = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : currentFilters.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                selectedCategories.add(entry.getKey());
            }
        }

        if (selectedCategories.isEmpty()) {
            return "Categories: Any";
        }

        return "Categories: " + android.text.TextUtils.join(", ", selectedCategories);
    }

    /**
     * Returns summary text for the waitlist full/not full row.
     *
     * @return summary text for waitlist status
     */
    private String getFullStatusSummaryText() {
        if (fullStatusFilter == null) {
            return "Waitlist Status: Any";
        }
        return fullStatusFilter ? "Waitlist Status: Full" : "Waitlist Status: Not Full";
    }

    /**
     * Returns summary text for the max capacity row.
     *
     * @return summary text for capacity filter
     */
    private String getCapacitySummaryText() {
        return maxCapacityFilter == null
                ? "Max Capacity: Any"
                : "Max Capacity: " + maxCapacityFilter;
    }

    /**
     * Returns summary text for the start date row.
     *
     * @return summary text for start date filter
     */
    private String getStartDateSummaryText() {
        return startDateFilter == null
                ? "Start Date: Any"
                : "Start Date: " + filterDateFormat.format(startDateFilter);
    }

    /**
     * Returns summary text for the end date row.
     *
     * @return summary text for end date filter
     */
    private String getEndDateSummaryText() {
        return endDateFilter == null
                ? "End Date: Any"
                : "End Date: " + filterDateFormat.format(endDateFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (ListenerRegistration registration : waitlistListeners.values()) {
            if (registration != null) {
                registration.remove();
            }
        }
        waitlistListeners.clear();
    }
}