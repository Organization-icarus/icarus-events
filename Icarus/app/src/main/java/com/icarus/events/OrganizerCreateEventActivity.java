package com.icarus.events;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
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

    private ImageView eventPoster;
    private Button UploadPosterButton;

    private EditText eventName;
    private EditText eventDescription;

    private EditText locationName;
    private SwitchMaterial geolocationSwitch;

    private EditText EventLimit;
    private Spinner categoryNameList;

    private Button RegistrationStartDateButton;
    private Button RegistrationStartTimeButton;
    private Button RegistrationEndDateButton;
    private Button RegistrationEndTimeButton;

    private Button EventDate;
    private Button EventTime;
    private Button CreateEvent;

    private Date startDate;
    private Date endDate;
    private Date eventDate;

    private Date startTime;
    private Date endTime;
    private Date eventTime;

    private ActivityResultLauncher<String> imagePickerLauncher;
    private String posterURL;
    private Uri posterURI;

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

        //Create Image Picker Launcher an initialize posterURI
        posterURI = null;
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        posterURI = uri;
                        eventPoster.setImageURI(uri);
                    }
                }
        );


        //Create ImageView
        eventPoster = findViewById(R.id.OrganizerCreateEventImage);

        //Create EditText
        eventName = findViewById(R.id.OrganizerCreateEventEventTitle);
        eventDescription = findViewById(R.id.OrganizerCreateEventDescription);
        EventLimit= findViewById(R.id.OrganizerCreateEventLimitWaitingListLimit);
        locationName = findViewById(R.id.OrganizerCreateEventEventLocation);

        //Create Switch
        geolocationSwitch = findViewById(R.id.OrganizerCreateEventGeolocationSwitch);

        //Create Buttons
        UploadPosterButton = findViewById(R.id.OrganizerCreateEventUploadPosterButton);

        RegistrationStartDateButton = findViewById(R.id.OrganizerCreateEventRegistrationPeriodStartDate);
        RegistrationStartTimeButton = findViewById(R.id.OrganizerCreateEventRegistrationPeriodStartTime);

        RegistrationEndDateButton = findViewById(R.id.OrganizerCreateEventRegistrationPeriodEndDate);
        RegistrationEndTimeButton = findViewById(R.id.OrganizerCreateEventRegistrationPeriodEndTime);

        EventDate = findViewById(R.id.OrganizerCreateEventDate);
        EventTime = findViewById(R.id.OrganizerCreateEventTime);

        CreateEvent = findViewById(R.id.OrganizerCreateEventCreateEvent);


        //Button Functions
        UploadPosterButton.setOnClickListener(v -> {
            //Upload event poster
            imagePickerLauncher.launch("image/*");
        });

        RegistrationStartDateButton.setOnClickListener(v -> {
            // Set Registration start date
            showDatePicker(date -> {
                this.startDate = date;
                String regStart = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date);
                RegistrationStartDateButton.setText("Start Date: "+ regStart);
            });
        });
        RegistrationStartTimeButton.setOnClickListener(v -> {
            // Set Registration start Time
            showTimePicker(time->{
                startTime = time;
                String showTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(startTime);
                RegistrationStartTimeButton.setText("Start Time: "+showTime );
            });
        });

        RegistrationEndDateButton.setOnClickListener(v -> {
            // Set Registration end date
            showDatePicker(date -> {
                this.endDate = date;
                String regEnd = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date);
                RegistrationEndDateButton.setText("End Date: "+ regEnd);
            });
        });
        RegistrationEndTimeButton.setOnClickListener(v -> {
            // Set Registration end Time
            showTimePicker(time->{
                endTime = time;
                String showTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(endTime);
                RegistrationEndTimeButton.setText("End Time: "+showTime );
            });
        });

        EventDate.setOnClickListener(v -> {
            // Set event date
            showDatePicker(date -> {
                this.eventDate = date;
                String eventDate = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date);
                EventDate.setText("Event Date: "+ eventDate);
            });
        });
        EventTime.setOnClickListener(v -> {
            // Set Registration end Time
            showTimePicker(time->{
                eventTime = time;
                String showTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(eventTime);
                EventTime.setText("Event Time: "+showTime );
            });
        });

        CreateEvent.setOnClickListener(v -> {
            // Confirm creation of event
            String name = eventName.getText().toString().trim();
            String description = eventDescription.getText().toString().trim();
            String category = categoryNameList.getSelectedItem().toString().trim();
            String location = locationName.getText().toString().trim();

            // Check if user filled in all dates before proceeding
            if (startDate == null || startTime == null || endDate == null || endTime == null || eventDate == null || eventTime == null) {
                Toast.makeText(this, "Please select all dates", Toast.LENGTH_SHORT).show();
                return;
            }

            this.startDate = mergeDateAndTime(startDate,startTime);
            this.endDate = mergeDateAndTime(endDate,endTime);
            this.eventDate = mergeDateAndTime(eventDate,eventTime);

            if (startDate.before(new Date())) {
                Toast.makeText(this, "Registration Start Date cannot be in the past.", Toast.LENGTH_SHORT).show();
                return;
            }
            if(startDate.after(endDate)){
                Toast.makeText(this, "Registration Start Date cannot be after Registration End Date.", Toast.LENGTH_SHORT).show();
                return;
            }
            if(endDate.after(eventDate)){
                Toast.makeText(this, "Registration End Date cannot be after Event Date.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Check if user filled all text fields before proceeding
            if (name.isEmpty() || category.isEmpty() || location.isEmpty()) {
                Toast.makeText(this, "Please fill in name, category, and location fields.", Toast.LENGTH_SHORT).show();
                return;
            }



            //Uppercase first letter and lowercase rest of string
            Double numberOfPeople = null;
            if (!EventLimit.getText().toString().trim().isEmpty()) {
                numberOfPeople = Double.parseDouble(EventLimit.getText().toString().trim());
            }
            location = location.substring(0,1).toUpperCase() + location.substring(1).toLowerCase();

            // Upload image to cloudinary and save event to database
            final String finalName = name;
            final String finalDescription = description;
            final String finalCategory = category;
            final String finalLocation = location;
            final Double finalCapacity = numberOfPeople;
            final String finalUserId = userId;
            if (posterURI != null) {
                MediaManager.get().upload(posterURI)
                        .option("upload_preset", "ml_default")
                        .callback(new UploadCallback() {
                            @Override
                            public void onSuccess(String requestId, Map resultData) {
                                posterURL = (String) resultData.get("secure_url");
                                String publicId = (String) resultData.get("public_id");
                                // Create new firebase document for the image
                                Map<String, Object> imageData = new HashMap<>();
                                imageData.put("URL", posterURL);
                                db.collection(FirestoreCollections.IMAGES_COLLECTION)
                                        .document(publicId)
                                        .set(imageData)
                                        .addOnSuccessListener(unused -> {
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(OrganizerCreateEventActivity.this,
                                                    "Failed to add image to firestore", Toast.LENGTH_SHORT).show();
                                        });
                                // Save event to firestore
                                saveEvent(finalName, finalDescription, finalCategory, finalCapacity, posterURL, finalLocation, finalUserId);
                            }

                            @Override
                            public void onError(String requestId, ErrorInfo error) {
                                posterURL = "";
                                Toast.makeText(OrganizerCreateEventActivity.this,
                                        "Failed to Upload Image.", Toast.LENGTH_SHORT).show();
                                Log.e("UPLOAD_ERROR", error.getDescription());
                                // Save event to firestore with empty poster
                                saveEvent(finalName, finalDescription,finalCategory, finalCapacity, posterURL, finalLocation, finalUserId);
                            }

                            @Override public void onStart(String requestId) {}
                            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                            @Override public void onReschedule(String requestId, ErrorInfo error) {}
                        })
                        .dispatch();
            } else {
                // Save event to firestore with empty poster
                saveEvent(finalName, finalDescription, finalCategory, finalCapacity, "", finalLocation, finalUserId);
            }
        });
    }

    private void saveEvent(String name, String description, String category, Double capacity, String posterURL, String location, String userId) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("name", name);
        eventData.put("description", description);
        eventData.put("category", category);
        eventData.put("capacity",capacity);
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
    }

    /**
     * Displays a material design date picker and returns the selected date.
     * <p>
     * The picker restricts selections to current or future dates. The
     * selected date is converted from UTC to the device's local time zone
     * before being returned through the provided callback.
     * <p>
     * This was created by claude AI, March 10, 2026 "How can I create a
     * popup calendar without creating a new XML file"
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

    /**
     * Displays a material design time picker and returns the selected time.
     * <p>
     * The picker uses a clock input mode and returns the selected hour and
     * minute through the provided callback as a Date object set to today's
     * date at the chosen time in the device's local time zone.
     * <p>
     * This was created by claude AI, March 22, 2026 "How can I create a
     * popup clock without creating a new XML file"
     *
     * @param onTimePicked callback executed when the user selects a time
     */
    private void showTimePicker(Consumer<Date> onTimePicked) {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
                .setMinute(Calendar.getInstance().get(Calendar.MINUTE))
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .build();

        timePicker.addOnPositiveButtonClickListener(v -> {
            Calendar local = Calendar.getInstance();
            local.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            local.set(Calendar.MINUTE, timePicker.getMinute());
            local.set(Calendar.SECOND, 0);
            local.set(Calendar.MILLISECOND, 0);

            onTimePicked.accept(local.getTime());
        });

        timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
    }

    /**
     * Merges the date components from one Date and the time components
     * from another into a single combined Date object.
     *<p>
     * @param date the Date to take year, month, and day from
     * @param time the Date to take hour and minute from
     * @return a new Date combining both, or null if either is null
     *<p>
     * This was created by claude AI, March 22, 2026 "How can I combine the Date
     * and Time from 2 different Date objects"
     */
    private Date mergeDateAndTime(Date date, Date time) {
        if (date == null || time == null) return null;

        Calendar dateCal = Calendar.getInstance();
        dateCal.setTime(date);

        Calendar timeCal = Calendar.getInstance();
        timeCal.setTime(time);

        dateCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
        dateCal.set(Calendar.MINUTE,      timeCal.get(Calendar.MINUTE));
        dateCal.set(Calendar.SECOND, 0);
        dateCal.set(Calendar.MILLISECOND, 0);

        return dateCal.getTime();
    }
}

