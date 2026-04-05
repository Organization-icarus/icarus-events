package com.icarus.events;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import androidx.core.util.Pair;
import com.google.android.material.datepicker.MaterialDatePicker;
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
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
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


/**
 * Activity that displays the list of available events for entrants.
 * <p>
 * Retrieves events from Firebase Firestore, displays them in a RecyclerView,
 * and allows users to filter events by search text and category. Users
 * can also navigate to create a new event.
 *
 * @author Alex Alves
 */
public class EntrantEventListActivity extends HeaderNavBarActivity {
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
    private Integer maxCapacityFilter = null;
    private Date startDateFilter = null;
    private Date endDateFilter = null;
    private Boolean fullStatusFilter = null;
    private int currentSortOption = SORT_DATE_ASC;
    private Boolean sortOptionSelected = false;
    private Map<String, String> categoryColors;
    private HashMap<String, Long> eventWaitingCounts;
    private HashMap<String, ListenerRegistration> waitlistListeners;
    private final SimpleDateFormat filterDateFormat =
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
// Sort options selected in the menu
    private static final int MENU_STATUS_ANY = 1;
    private static final int MENU_STATUS_FULL = 2;
    private static final int MENU_STATUS_NOT_FULL = 3;
    private static final int MENU_SORT_DATE_ASC = 4;
    private static final int MENU_SORT_DATE_DESC = 5;
    private static final int MENU_SORT_NAME_ASC = 6;
    private static final int MENU_SORT_NAME_DESC = 7;
// Sort options currently applied
    private static final int SORT_DATE_ASC = 1;
    private static final int SORT_DATE_DESC = 2;
    private static final int SORT_NAME_ASC = 3;
    private static final int SORT_NAME_DESC = 4;

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
        setupNavBar(TAB_EVENTS);

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
        eventListView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                searchTextFilter.clearFocus();
            }
            return false;
        });
        searchTextFilter.setOnEditorActionListener((v, actionId, event) -> {
            boolean isEnterKey = event != null
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;

            boolean isImeAction = actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_GO;

            if (isEnterKey || isImeAction) {
                searchTextFilter.clearFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchTextFilter.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });
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
                    Boolean isPrivate = snapshot.getBoolean("isPrivate");
                    Date regClose = snapshot.getDate("close");

                    // Hide private events
                    if (Boolean.TRUE.equals(isPrivate)) {
                        continue;
                    }

                    // Hide events whose registration date has already passed
                    if (regClose != null && regClose.before(new Date())) {
                        continue;
                    }

                    String name = snapshot.getString("name");
                    String category  = snapshot.getString("category");
                    Double capacity = snapshot.getDouble("capacity");
                    Date regOpen = snapshot.getDate("open");
                    Date startDate = snapshot.getDate("startDate");
                    Date endDate = snapshot.getDate("endDate");
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
                                    startDate,
                                    endDate,
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

        qrButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, QRCodeActivity.class);
            startActivity(intent);
        });

        filterCategoryButton.setOnClickListener(v -> showFilterDialog());
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

            Date eventStartDate = event.getStartDate();
            boolean matchesStartDate = startDateFilter == null
                    || (eventStartDate != null && !eventStartDate.before(startDateFilter));
            boolean matchesEndDate = endDateFilter == null
                    || (eventStartDate != null && !eventStartDate.after(endDateFilter));

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

        filteredEventArrayList.sort((event1, event2) -> {
            if (currentSortOption == SORT_NAME_ASC || currentSortOption == SORT_NAME_DESC) {
                String name1 = event1.getName() == null ? "" : event1.getName().toLowerCase(Locale.getDefault());
                String name2 = event2.getName() == null ? "" : event2.getName().toLowerCase(Locale.getDefault());

                return currentSortOption == SORT_NAME_ASC
                        ? name1.compareTo(name2)
                        : name2.compareTo(name1);
            }

            Date date1 = event1.getStartDate();
            Date date2 = event2.getStartDate();

            if (date1 == null && date2 == null) {
                return 0;
            }
            if (date1 == null) {
                return 1;
            }
            if (date2 == null) {
                return -1;
            }

            return currentSortOption == SORT_DATE_ASC
                    ? date1.compareTo(date2)
                    : date2.compareTo(date1);
        });

        eventListArrayAdapter.notifyDataSetChanged();
        boolean filtersActive = currentFilters.containsValue(true)
                || maxCapacityFilter != null
                || startDateFilter != null
                || endDateFilter != null
                || fullStatusFilter != null
                || sortOptionSelected;

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
        categoryRow.setOnClickListener(v -> showCategoryMultiSelectDialog(categoryRow));
        addFilterRow(container, categoryRow);

        MaterialButton statusRow = buildDialogRow(getFullStatusSummaryText(), fullStatusFilter != null);
        statusRow.setOnClickListener(v -> showFullStatusDropdown(statusRow));
        addFilterRow(container, statusRow);

        MaterialButton capacityRow = buildDialogRow(getCapacitySummaryText(), maxCapacityFilter != null);
        capacityRow.setOnClickListener(v -> showMaxCapacityDialog(capacityRow));
        addFilterRow(container, capacityRow);

        MaterialButton dateRangeRow = buildDialogRow(getDateRangeSummaryText(),
                startDateFilter != null || endDateFilter != null);
        dateRangeRow.setOnClickListener(v -> showDateRangePicker(dateRangeRow));
        addFilterRow(container, dateRangeRow);

        MaterialButton sortRow = buildDialogRow(getSortSummaryText(), sortOptionSelected);
        sortRow.setOnClickListener(v -> showSortDropdown(sortRow));
        addFilterRow(container, sortRow);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Filters")
                .setView(container)
                .setPositiveButton("Apply", (dialogInterface, which) -> applyFilters())
                .setNeutralButton("Clear All", (dialogInterface, which) -> {
                    currentFilters.replaceAll((c, v) -> false);
                    fullStatusFilter = null;
                    currentSortOption = SORT_DATE_ASC;
                    sortOptionSelected = false;
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
                active ? getColor(R.color.accent_first) : getColor(R.color.white)
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
    // "Implement multi-select category dialog for choosing several category filters at once"
    /**
     * Displays a multi-select dialog for category filtering.
     *
     * @param anchor the filter row to refresh after selection
     */
    private void showCategoryMultiSelectDialog(MaterialButton anchor) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int sidePadding = (int) (8 * getResources().getDisplayMetrics().density);
        container.setPadding(sidePadding, 0, sidePadding, 0);

        ArrayList<MaterialCheckBox> checkBoxes = new ArrayList<>();
        ArrayList<String> categories = new ArrayList<>(currentFilters.keySet());

        for (String category : categories) {
            MaterialCheckBox checkBox = new MaterialCheckBox(this);
            checkBox.setText(category);
            checkBox.setChecked(Boolean.TRUE.equals(currentFilters.get(category)));
            checkBox.setTextColor(getColor(R.color.lightText));
            checkBox.setButtonTintList(ColorStateList.valueOf(getColor(R.color.accent_first)));
            int verticalPadding = (int) (8 * getResources().getDisplayMetrics().density);
            checkBox.setPadding(0, verticalPadding, 0, verticalPadding);
            container.addView(checkBox);
            checkBoxes.add(checkBox);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Select Categories")
                .setView(container)
                .setPositiveButton("Apply", (dialogInterface, which) -> {
                    for (int i = 0; i < categories.size(); i++) {
                        currentFilters.put(categories.get(i), checkBoxes.get(i).isChecked());
                    }
                    anchor.setText(getCategorySummaryText());
                    anchor.setTextColor(ColorStateList.valueOf(getColor(R.color.darkText)));
                    anchor.setBackgroundTintList(ColorStateList.valueOf(
                            hasCategoryFilter() ? getColor(R.color.accent_first) : getColor(R.color.white)
                    ));
                    anchor.setStrokeColor(ColorStateList.valueOf(
                            hasCategoryFilter() ? getColor(R.color.accent_first) : getColor(R.color.white)
                    ));
                })
                .setNeutralButton("Clear All", (dialogInterface, which) -> {
                    currentFilters.replaceAll((selectedCategory, selected) -> false);
                    anchor.setText(getCategorySummaryText());
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

        TextView titleView = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if (titleView != null) {
            titleView.setTextColor(getColor(R.color.lightText));
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.accent_first));
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getColor(R.color.accent_first));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.lightText));
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
                    fullStatusFilter != null ? getColor(R.color.accent_first) : getColor(R.color.white)
            ));
            anchor.setStrokeColor(ColorStateList.valueOf(
                    fullStatusFilter != null ? getColor(R.color.accent_first) : getColor(R.color.white)
            ));
            return true;
        });

        popupMenu.show();
    }

    // Taken from ChatGPT March 29th 2026,
    // "Implement dropdown menu for sorting events by date ascending or descending"
    /**
     * Displays a dropdown menu for date sorting from inside the filter dialog.
     *
     * @param anchor the view to anchor the dropdown to
     */
    private void showSortDropdown(MaterialButton anchor) {
        PopupMenu popupMenu = new PopupMenu(new ContextThemeWrapper(this,
                androidx.appcompat.R.style.Theme_AppCompat_Light), anchor);
        Menu menu = popupMenu.getMenu();

        menu.add(Menu.NONE, MENU_SORT_DATE_ASC, Menu.NONE, "Sort: Date Ascending");
        menu.add(Menu.NONE, MENU_SORT_DATE_DESC, Menu.NONE, "Sort: Date Descending");
        menu.add(Menu.NONE, MENU_SORT_NAME_ASC, Menu.NONE, "Sort: Event Name A-Z");
        menu.add(Menu.NONE, MENU_SORT_NAME_DESC, Menu.NONE, "Sort: Event Name Z-A");

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString styledTitle = new SpannableString(item.getTitle());
            styledTitle.setSpan(new ForegroundColorSpan(getColor(R.color.darkText)), 0, styledTitle.length(), 0);
            item.setTitle(styledTitle);
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == MENU_SORT_DATE_ASC) {
                currentSortOption = SORT_DATE_ASC;
            } else if (itemId == MENU_SORT_DATE_DESC) {
                currentSortOption = SORT_DATE_DESC;
            } else if (itemId == MENU_SORT_NAME_ASC) {
                currentSortOption = SORT_NAME_ASC;
            } else if (itemId == MENU_SORT_NAME_DESC) {
                currentSortOption = SORT_NAME_DESC;
            }
            sortOptionSelected = true;
            anchor.setText(getSortSummaryText());
            anchor.setTextColor(ColorStateList.valueOf(getColor(R.color.darkText)));
            anchor.setBackgroundTintList(ColorStateList.valueOf(
                    sortOptionSelected ? getColor(R.color.accent_first) : getColor(R.color.white)
            ));
            anchor.setStrokeColor(ColorStateList.valueOf(
                    sortOptionSelected ? getColor(R.color.accent_first) : getColor(R.color.white)
            ));
            filterCategoryButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.accent_first)));
            return true;
        });

        popupMenu.show();
    }

    // Adapted from a ChatGPT Generation March 29th 2026,
    // "Implement dialog input for numeric capacity filtering"
    /**
     * Displays a dialog for setting a maximum waitlist capacity filter.
     *
     * @param anchor the row text to refresh after selection
     */
    private void showMaxCapacityDialog(MaterialButton anchor) {
        LinearLayout inputContainer = new LinearLayout(this);
        inputContainer.setOrientation(LinearLayout.VERTICAL);
        inputContainer.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

        final EditText input = new EditText(this);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) (300 * getResources().getDisplayMetrics().density),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        input.setLayoutParams(params);
        input.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.accent_first)));

        input.setHint("Enter maximum capacity");
        input.setHintTextColor(getColor(R.color.lightTextSemi));
        input.setTextColor(getColor(R.color.lightText));
        input.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(R.dimen.font_subheading_size));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        if (maxCapacityFilter != null) {
            input.setText(String.valueOf(maxCapacityFilter));
        }

        inputContainer.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Maximum Capacity")
                .setView(inputContainer)
                .setPositiveButton("Apply", (dialogInterface, which) -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) {
                        maxCapacityFilter = null;
                    } else {
                        try {
                            maxCapacityFilter = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Enter a valid whole number.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    anchor.setText(getCapacitySummaryText());
                    anchor.setTextColor(ColorStateList.valueOf(getColor(R.color.darkText)));
                    anchor.setBackgroundTintList(ColorStateList.valueOf(
                            maxCapacityFilter != null ? getColor(R.color.accent_first) : getColor(R.color.white)
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

        TextView titleView = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if (titleView != null) {
            titleView.setTextColor(getColor(R.color.lightText));
        }

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
     * @param anchor the row text to refresh after selection
     */
    private void showDateRangePicker(MaterialButton anchor) {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Event Date Range");

        if (startDateFilter != null && endDateFilter != null) {
            builder.setSelection(new Pair<>(startDateFilter.getTime(), endDateFilter.getTime()));
        }

        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                Long start = selection.first;
                Long end = selection.second;

                if (start != null) {
                    Calendar startCalendar = Calendar.getInstance();
                    startCalendar.setTimeInMillis(start);
                    setStartOfDay(startCalendar);
                    startDateFilter = startCalendar.getTime();
                } else {
                    startDateFilter = null;
                }

                if (end != null) {
                    Calendar endCalendar = Calendar.getInstance();
                    endCalendar.setTimeInMillis(end);
                    setEndOfDay(endCalendar);
                    endDateFilter = endCalendar.getTime();
                } else {
                    endDateFilter = null;
                }

                anchor.setText(getDateRangeSummaryText());
                anchor.setTextColor(ColorStateList.valueOf(getColor(R.color.darkText)));
                boolean hasDateRange = startDateFilter != null || endDateFilter != null;
                anchor.setBackgroundTintList(ColorStateList.valueOf(
                        hasDateRange ? getColor(R.color.accent_first) : getColor(R.color.white)
                ));
                anchor.setStrokeColor(ColorStateList.valueOf(
                        hasDateRange ? getColor(R.color.accent_first) : getColor(R.color.white)
                ));
            }
        });

        picker.show(getSupportFragmentManager(), "event_date_range_picker");
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
     * Returns summary text for the current sort selection.
     *
     * @return summary text for sorting
     */
    private String getSortSummaryText() {
        switch (currentSortOption) {
            case SORT_DATE_DESC:
                return "Sort By: Date Descending";
            case SORT_NAME_ASC:
                return "Sort By: Event Name A-Z";
            case SORT_NAME_DESC:
                return "Sort By: Event Name Z-A";
            case SORT_DATE_ASC:
            default:
                return "Sort By: Date Ascending";
        }
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
     * Returns summary text for the date range row.
     *
     * @return summary text for date range filter
     */
    private String getDateRangeSummaryText() {
        if (startDateFilter == null && endDateFilter == null) {
            return "Date Range: Any";
        }
        if (startDateFilter != null && endDateFilter != null) {
            return "Date Range: " + filterDateFormat.format(startDateFilter)
                    + " - " + filterDateFormat.format(endDateFilter);
        }
        if (startDateFilter != null) {
            return "Date Range: From " + filterDateFormat.format(startDateFilter);
        }
        return "Date Range: Until " + filterDateFormat.format(endDateFilter);
    }

    /**
     * Cleans up active Firestore waitlist listeners when the activity is destroyed.
     */
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