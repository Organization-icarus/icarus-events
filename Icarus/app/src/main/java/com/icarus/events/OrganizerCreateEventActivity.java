package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Consumer;

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


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_create_event);
        setupNavBar();

        db = FirebaseFirestore.getInstance();
        //Create EditText
        eventName = findViewById(R.id.OrganizerCreateEventEventTitle);
        EventLimit= findViewById(R.id.OrganizerCreateEventLimitWaitingListLimit);
        locationName = findViewById(R.id.OrganizerCreateEventEventLocation);
        //Create Spinner
        categoryNameList = findViewById(R.id.OrganizerCreateEventCategory);
        ArrayList<String> dbCategories = new ArrayList<>();
        db.collection("event-categories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->{
                    dbCategories.add("Category");
                    for(DocumentSnapshot doc : queryDocumentSnapshots){
                        Log.d("SPINNER", "All fields: " + doc.getData());
                        String category = doc.getString("category");
                        //check for null
                        if(category != null){
                            dbCategories.add(category);
                            Log.d("SPINNER", category);
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
                    Log.d("SPINNER", "FAILED TO CREATE SPINNER");
                });
        //Create Switch
        geolocationSwitch = findViewById(R.id.OrganizerCreateEventGeolocationSwitch);
        //Create Buttons
        UploadPosterButton = findViewById(R.id.OrganizerCreateEventUploadPosterButton);
        EventDate = findViewById(R.id.OrganizerCreateEventDate);
        RegistrationPeriodStartButton = findViewById(R.id.OrganizerCreateEventRegistrationPeriodStart);
        RegistrationPeriodEndButton = findViewById(R.id.OrganizerCreateEventRegistrationPeriodEnd);
        CreateEvent = findViewById(R.id.OrganizerCreateEventCreateEvent);


        UploadPosterButton.setOnClickListener(v -> {
            //This needs to allow for upload of a image
            //Currently don't worry about images

        });
        RegistrationPeriodStartButton.setOnClickListener(v -> {
            // Set Registration start date
            showDatePicker(date -> {
                this.startDate = date;
                String regStart = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date);
                RegistrationPeriodStartButton.setText("Start: "+ regStart);
            });
        });
        RegistrationPeriodEndButton.setOnClickListener(v -> {
            // Set Registration end date
            showDatePicker(date -> {
                this.endDate = date;
                String regEnd = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date);
                RegistrationPeriodEndButton.setText("End: "+ regEnd);
            });
        });
        EventDate.setOnClickListener(v -> {
            // Set Registration end date
            showDatePicker(date -> {
                this.eventDate = date;
                String eventDate = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date);
                EventDate.setText("Event: "+ eventDate);
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
            eventData.put("image", "IMAGE REFERENCE");
            eventData.put("location", location);
            eventData.put("geolocation",geolocationSwitch.isChecked());

            //Event event = new Event(null,name,category,numberOfPeople, this.startDate,this.endDate,this.eventDate);
            db.collection("events").add(eventData)
                    .addOnSuccessListener(unused -> {
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to create event", Toast.LENGTH_SHORT).show();
                    });

        });
    }
    /*
     * This was created by claude AI, March 10, 2026
     * "How can I create a popup calendar with creating a new XML file"*/
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
            Date newDate = new Date(selection + TimeZone.getDefault().getOffset(selection));
            onDatePicked.accept(newDate);
        });
        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }
}
