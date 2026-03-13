package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class OrganizerManageEvent extends NavigationBarActivity{
    private Button ViewEntrantMap;
    private Button ViewEntrantList;
    private Button UpdatePoster;
    private Button SampleAttendees;
    private Button ReplaceDeclined;
    private TextView eventTitle;
    private String eventId;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_manage_event);
        setupNavBar();

        //Create Buttons
        ViewEntrantMap = findViewById(R.id.OrganizerManageEventViewEntrantMap);
        ViewEntrantList = findViewById(R.id.OrganizerManageEventViewEntrantList);
        UpdatePoster = findViewById(R.id.OrganizerManageEventUpdatePoster);
        SampleAttendees = findViewById(R.id.OrganizerManageEventSampleAttendees);
        ReplaceDeclined = findViewById(R.id.OrganizerManageEventReplaceDeclined);

        //Create textView
        eventTitle = findViewById(R.id.OrganizerManageEventTitle);

        eventId = getIntent().getStringExtra("eventId");

        db = FirebaseFirestore.getInstance();

        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    String eventName = document.getString("name");
                    eventTitle.setText(eventName);
                });

        ViewEntrantMap.setOnClickListener(v -> {
            // View Entrant Map
//            Intent intent = new Intent(this, UserRegistrationActivity.class);
//            intent.putExtra("deviceId", deviceId);
//            startActivity(intent);
        });
        ViewEntrantList.setOnClickListener(v -> {
            // View Entrant List
            Intent intent = new Intent(this, OrganizerViewEntrantsOnWaitingList.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });
        UpdatePoster.setOnClickListener(v -> {
            // Update Poster

        });
        SampleAttendees.setOnClickListener(v -> {
            // Sample Attendees
            Intent intent = new Intent(this, SampleAttendeesActivity.class);
            startActivity(intent);

        });
        ReplaceDeclined.setOnClickListener(v -> {
            // Replaced Declined
//            Intent intent = new Intent(this, UserRegistrationActivity.class);
//            intent.putExtra("deviceId", deviceId);
//            startActivity(intent);

        });
    }
}
