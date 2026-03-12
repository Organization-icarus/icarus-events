package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class OrganizerViewEntrantsOnWaitingList extends NavigationBarActivity{
    private FirebaseFirestore db;
    private Button backButton;
    private ListView entrantsOnWaitingList;
    private ArrayList<User> entrantList;
    private OraganizerEntrantViewListArrayAdapter eventListArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_view_entrants_on_waiting_list);
        setupNavBar();

        db = FirebaseFirestore.getInstance();

        //CreateButton
        backButton = findViewById(R.id.OrganizerEntrantOnWaitingListBackButton);
        //Create ListView
        entrantsOnWaitingList = findViewById(R.id.OrganizerEntrantOnWaitingList);
        //Initialize ArrayList and ArrayAdapter
        entrantList = new ArrayList<>();
        eventListArrayAdapter = new OraganizerEntrantViewListArrayAdapter(this, entrantList);
        entrantsOnWaitingList.setAdapter(eventListArrayAdapter);



        //events -> eventID -> entrants -> entrantId -> status
        String eventId = getIntent().getStringExtra("eventId");
        db.collection("events").document(eventId).collection("entrants")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    entrantList.clear();
                    for (QueryDocumentSnapshot snapshot : value) {
                        String deviceId = snapshot.getId();
                        String status = snapshot.getString("status");

                        if (Objects.equals(status, "waiting")) {
                            //If user has waiting role look for name in user collection
                            db.collection("users").document(deviceId)
                                    .get()
                                    .addOnSuccessListener(userSnapshot -> {
                                        String name = userSnapshot.getString("name");
                                        entrantList.add(new User(deviceId, name, null, null, null, null, null));
                                        eventListArrayAdapter.notifyDataSetChanged();
                                    });
                        }
                    }
                });

        backButton.setOnClickListener(v -> {
            finish();
        });
    }
}
