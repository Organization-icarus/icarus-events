package com.icarus.events;

import android.os.Bundle;
import android.widget.Button;
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

public class OrganizerCreateEventActivity extends AppCompatActivity {
    private  FirebaseFirestore db;
    private Button OrganizerCreateEventUploadPosterButton;

    private Button OrganizerCreateEventLimitWaitingListLimitButton;

    private Button OrganizerCreateEventRegistrationPeriodStart;
    private Button OrganizerCreateEventRegistrationPeriodEnd;
    private Button OrganizerCreateEventCreateEvent;
    private Date startDate;
    private Date endDate;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_create_event);

        OrganizerCreateEventUploadPosterButton = findViewById(R.id.OrganizerCreateEventUploadPosterButton);
        OrganizerCreateEventLimitWaitingListLimitButton = findViewById(R.id.OrganizerCreateEventLimitWaitingListLimitButton);
        OrganizerCreateEventRegistrationPeriodStart = findViewById(R.id.OrganizerCreateEventRegistrationPeriodStart);
        OrganizerCreateEventRegistrationPeriodEnd = findViewById(R.id.OrganizerCreateEventRegistrationPeriodEnd);
        OrganizerCreateEventCreateEvent = findViewById(R.id.OrganizerCreateEventCreateEvent);


        OrganizerCreateEventUploadPosterButton.setOnClickListener(v -> {
            //This needs to allow for upload of a image

        });
        OrganizerCreateEventLimitWaitingListLimitButton.setOnClickListener(v -> {
            // Toggle the waiting list limit on/off, or open an input dialog

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
        OrganizerCreateEventCreateEvent.setOnClickListener(v -> {
            // Confirm creation of event
        });


    }
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
