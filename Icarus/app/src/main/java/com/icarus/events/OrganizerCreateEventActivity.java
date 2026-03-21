package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.icarus.events.FirestoreCollections;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Consumer;

/**
 * Activity that allows organizers to create a new event.
 * <p>
 * Users can enter event details such as name, category, location,
 * registration period, event date, participant limit, and whether
 * geolocation tracking is enabled. Event information is validated
 * and then stored in the Firestore "events" collection.
 * <p>
 * This activity extends {@link NavigationBarActivity} to include
 * the application's reusable navigation bar.
 *
 * @author Ben Salmon
 */
public class OrganizerCreateEventActivity extends NavigationBarActivity {
    private  FirebaseFirestore db;
    private Button UploadPosterButton;

    private Button EventDate;

    private Button RegistrationPeriodStartButton;
    private Button RegistrationPeriodEndButton;
    private Button CreateEvent;
    private SwitchMaterial geolocationSwitch;
    private Date startDate;
    private Date endDate;
    private Date eventDate;
    private EditText EventLimit;

    private EditText eventName;
    private EditText locationName;
    private Spinner categoryNameList;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private String posterURL;

    /**
     * Initializes the event creation interface.
     * <p>
     * This method sets up all UI components, retrieves event categories
     * from Firestore, and assigns click listeners for selecting dates
     * and submitting the event. When the user confirms creation, the
     * event data is validated and saved to the database.
     *
     * @param savedInstanceState the previously saved activity state,
     *                           or null if the activity is starting fresh
     */
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_create_event);
        setupNavBar();
        User user = UserSession.getInstance().getCurrentUser();
        String userId = user.getId();

        db = FirebaseFirestore.getInstance();
        //Create EditText
        eventName = findViewById(R.id.OrganizerCreateEventEventTitle);
        EventLimit= findViewById(R.id.OrganizerCreateEventLimitWaitingListLimit);
        locationName = findViewById(R.id.OrganizerCreateEventEventLocation);
        //Create Spinner
        categoryNameList = findViewById(R.id.OrganizerCreateEventCategory);
        ArrayList<String> dbCategories = new ArrayList<>();
        db.collection(FirestoreCollections.EVENT_CATEGORIES_COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->{
                    dbCategories.add("Category");
                    for(DocumentSnapshot doc : queryDocumentSnapshots){
                        String category = doc.getString("category");
                        //check for null
                        if(category != null){
                            dbCategories.add(category);
                        }
                    }
                    ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                            this,
                            R.layout.organizer_create_event_spinner_content,
                            dbCategories
                    );
                    categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    categoryNameList.setAdapter(categoryAdapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load categories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        //Create Image Picker Launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        MediaManager.get().upload(uri)
                                .option("upload_preset", "ml_default")
                                .callback(new UploadCallback() {
                                    @Override
                                    public void onSuccess(String requestId, Map resultData) {
                                        posterURL = (String) resultData.get("secure_url");
                                    }

                                    @Override
                                    public void onError(String requestId, ErrorInfo error) {
                                        posterURL = "";
                                        Toast.makeText(OrganizerCreateEventActivity.this,
                                                "Failed to Upload Image.", Toast.LENGTH_SHORT).show();
                                        Log.e("UPLOAD_ERROR", error.getDescription());
                                    }

                                    @Override public void onStart(String requestId) {}
                                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                                })
                                .dispatch();
                    }
                }
        );
        //Create Switch
        geolocationSwitch = findViewById(R.id.OrganizerCreateEventGeolocationSwitch);
        //Create Buttons
        UploadPosterButton = findViewById(R.id.OrganizerCreateEventUploadPosterButton);
        EventDate = findViewById(R.id.OrganizerCreateEventDate);
        RegistrationPeriodStartButton = findViewById(R.id.OrganizerCreateEventRegistrationPeriodStart);
        RegistrationPeriodEndButton = findViewById(R.id.OrganizerCreateEventRegistrationPeriodEnd);
        CreateEvent = findViewById(R.id.OrganizerCreateEventCreateEvent);

        UploadPosterButton.setOnClickListener(v -> {
            //Upload event poster
            imagePickerLauncher.launch("image/*");
        });
        RegistrationPeriodStartButton.setOnClickListener(v -> {
            // Set Registration start date
            showDatePicker(date -> {
                this.startDate = date;
                String regStart = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date);
                RegistrationPeriodStartButton.setText("Start:\n"+ regStart);
            });
        });
        RegistrationPeriodEndButton.setOnClickListener(v -> {
            // Set Registration end date
            showDatePicker(date -> {
                this.endDate = date;
                String regEnd = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date);
                RegistrationPeriodEndButton.setText("End:\n"+ regEnd);
            });
        });
        EventDate.setOnClickListener(v -> {
            // Set Registration end date
            showDatePicker(date -> {
                this.eventDate = date;
                String eventDate = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date);
                EventDate.setText("Event:\n"+ eventDate);
            });
        });
        CreateEvent.setOnClickListener(v -> {
            // Confirm creation of event

            String name = eventName.getText().toString().trim();
            String category = categoryNameList.getSelectedItem().toString().trim();
            String location = locationName.getText().toString().trim();

            // Check if user filled all text fields before proceeding
            if (name.isEmpty() || category.isEmpty() || location.isEmpty()) {
                Toast.makeText(this, "Please fill in name, category, and location fields.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Check if user filled in all dates before proceeding
            if (startDate == null || endDate == null || eventDate == null) {
                Toast.makeText(this, "Please select all dates", Toast.LENGTH_SHORT).show();
                return;
            }

            //Uppercase first letter and lowercase rest of string
            category = category.substring(0, 1).toUpperCase() + category.substring(1).toLowerCase();
            Double numberOfPeople = null;
            if (!EventLimit.getText().toString().trim().isEmpty()) {
                numberOfPeople = Double.parseDouble(EventLimit.getText().toString().trim());
            }
            location = location.substring(0,1).toUpperCase() + location.substring(1).toLowerCase();

            Map<String, Object> eventData = new HashMap<>();
            eventData.put("name", name);
            eventData.put("category", category);
            eventData.put("capacity",numberOfPeople);
            eventData.put("open", startDate);
            eventData.put("close", endDate);
            eventData.put("date", eventDate);
            eventData.put("image", posterURL);
            eventData.put("location", location);
            eventData.put("geolocation",geolocationSwitch.isChecked());
            eventData.put("organizer", userId);

            //Event event = new Event(null,name,category,numberOfPeople, this.startDate,this.endDate,this.eventDate);
            db.collection(FirestoreCollections.EVENTS_COLLECTION).add(eventData)
                    .addOnSuccessListener(unused -> {
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to create event", Toast.LENGTH_SHORT).show();
                    });

        });
    }

    /**
     * Displays a material design date picker and returns the selected date.
     * <p>
     * The picker restricts selections to current or future dates. The
     * selected date is converted from UTC to the device's local time zone
     * before being returned through the provided callback.
     * <p>
     * This was created by claude AI, March 10, 2026 "How can I create a
     * popup calendar with creating a new XML file"
     *
     * @param onDatePicked callback executed when the user selects a date
     */
    private void  showDatePicker(Consumer<Date> onDatePicked){
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build();
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder
                .datePicker()
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraints)
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(selection);

            Calendar local = Calendar.getInstance();
            local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
            local.set(Calendar.MILLISECOND, 0);

            onDatePicked.accept(local.getTime());
        });
        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }
}
