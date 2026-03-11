package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class OrganizerCreateEventActivity extends AppCompatActivity {
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
    private EditText categoryName;
    private EditText eventName;
    private EditText locationName;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_create_event);
        db = FirebaseFirestore.getInstance();
        //Create EditText
        eventName = findViewById(R.id.OrganizerCreateEventEventTitle);
        EventLimit= findViewById(R.id.OrganizerCreateEventLimitWaitingListLimit);
        categoryName = findViewById(R.id.OrganizerCreateEventCategory);
        locationName = findViewById(R.id.OrganizerCreateEventEventLocation);
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
            });
        });
        RegistrationPeriodEndButton.setOnClickListener(v -> {
            // Set Registration end date
            showDatePicker(date -> {
                this.endDate = date;
            });
        });
        EventDate.setOnClickListener(v -> {
            // Set Registration end date
            showDatePicker(date -> {
                this.eventDate = date;
            });
        });
        CreateEvent.setOnClickListener(v -> {
            // Confirm creation of event

            String name = eventName.getText().toString().trim();
            String category = categoryName.getText().toString().trim();
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
            Date newDate = new Date(selection);
            onDatePicked.accept(newDate);
        });
        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }
}
