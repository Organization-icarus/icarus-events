package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class OrganizerManageEvent extends NavigationBarActivity{
    private Button ViewEntrantMap;
    private Button ViewEntrantList;
    private Button UpdatePoster;
    private Button SampleAttendees;
    private Button ReplaceDeclined;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_view_entrants_on_waiting_list);
        setupNavBar();

        //Create Buttons
        ViewEntrantMap = findViewById(R.id.OrganizerManageEventViewEntrantMap);
        ViewEntrantList = findViewById(R.id.OrganizerManageEventViewEntrantList);
        UpdatePoster = findViewById(R.id.OrganizerManageEventUpdatePoster);
        SampleAttendees = findViewById(R.id.OrganizerManageEventSampleAttendees);
        ReplaceDeclined = findViewById(R.id.OrganizerManageEventReplaceDeclined);

        eventId = getIntent().getStringExtra("eventId");

        ViewEntrantMap.setOnClickListener(v -> {
            // View Entrant Map
//            Intent intent = new Intent(this, UserRegistrationActivity.class);
//            intent.putExtra("deviceId", deviceId);
//            startActivity(intent);
        });
        ViewEntrantList.setOnClickListener(v -> {
            // View Entrant List
            Intent intent = new Intent(this, OrganizerViewEntrantsOnWaitingList.class);
            intent.putExtra("EventId", eventId);
            startActivity(intent);
        });
        UpdatePoster.setOnClickListener(v -> {
            // Update Poster

        });
        SampleAttendees.setOnClickListener(v -> {
            // Sample Attendees
//            Intent intent = new Intent(this, UserRegistrationActivity.class);
//            intent.putExtra("deviceId", deviceId);
//            startActivity(intent);

        });
        ReplaceDeclined.setOnClickListener(v -> {
            // Replaced Declined
//            Intent intent = new Intent(this, UserRegistrationActivity.class);
//            intent.putExtra("deviceId", deviceId);
//            startActivity(intent);

        });
    }
}
