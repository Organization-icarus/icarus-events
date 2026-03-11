package com.icarus.events;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.function.Consumer;

public class OrganizerCreateEventActivity extends NavigationBarActivity {
    private  FirebaseFirestore db;
    private Button OrganizerCreateEventUploadPosterButton;

    private Button OrganizerCreateEventDate;

    private Button OrganizerCreateEventRegistrationPeriodStart;
    private Button OrganizerCreateEventRegistrationPeriodEnd;
    private Button OrganizerCreateEventCreateEvent;
    private Date startDate;
    private Date endDate;
    private Date eventDate;
    private EditText OrganizerCreateEventLimitWaitingListLimit;
    private EditText categoryName;
    private EditText eventName;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_create_event);
        setupNavBar();

        db = FirebaseFirestore.getInstance();
        //Create EditText
        eventName = findViewById(R.id.OrganizerCreateEventEventTitle);
        OrganizerCreateEventLimitWaitingListLimit= findViewById(R.id.OrganizerCreateEventLimitWaitingListLimit);
        categoryName = findViewById(R.id.OrganizerCreateEventCategory);
        //Create Buttons
        OrganizerCreateEventUploadPosterButton = findViewById(R.id.OrganizerCreateEventUploadPosterButton);
        OrganizerCreateEventDate = findViewById(R.id.OrganizerCreateEventDate);
        OrganizerCreateEventRegistrationPeriodStart = findViewById(R.id.OrganizerCreateEventRegistrationPeriodStart);
        OrganizerCreateEventRegistrationPeriodEnd = findViewById(R.id.OrganizerCreateEventRegistrationPeriodEnd);
        OrganizerCreateEventCreateEvent = findViewById(R.id.OrganizerCreateEventCreateEvent);


        OrganizerCreateEventUploadPosterButton.setOnClickListener(v -> {
            //This needs to allow for upload of a image
            //Currently don't worry about images

        });
        OrganizerCreateEventRegistrationPeriodStart.setOnClickListener(v -> {
            // Set Registration start date
            showDatePicker(date -> {
                this.startDate = date;
            });
        });
        OrganizerCreateEventRegistrationPeriodEnd.setOnClickListener(v -> {
            // Set Registration end date
            showDatePicker(date -> {
                this.endDate = date;
            });
        });
        OrganizerCreateEventDate.setOnClickListener(v -> {
            // Set Registration end date
            showDatePicker(date -> {
                this.eventDate = date;
            });
        });
        OrganizerCreateEventCreateEvent.setOnClickListener(v -> {
            // Confirm creation of event

            String name = eventName.getText().toString().trim();
            String category = categoryName.getText().toString().trim();
            double numberOfPeople = Double.parseDouble(OrganizerCreateEventLimitWaitingListLimit
                    .getText().toString().trim());

            Event event = new Event("",name,category,numberOfPeople, this.startDate,this.endDate,this.eventDate);
            db.collection("events")
                    .add(event)
                    .addOnSuccessListener(documentReference ->{
                        String generatedId = documentReference.getId();
                        documentReference.update("id", generatedId);
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
