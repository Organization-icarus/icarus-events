package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

public class OrganizerViewEntrantsOnWaitingList extends NavigationBarActivity{
    private FirebaseFirestore db;
    private ListView entrantsOnWaitingList;
    private ArrayList<User> entrantList;
    private OraganizerEntrantViewListArrayAdapter eventListArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_view_entrants_on_waiting_list);
        setupNavBar();

        db = FirebaseFirestore.getInstance();

        //Create ListView
        entrantsOnWaitingList = findViewById(R.id.OrganizerEntrantOnWaitingList);
        //Initialize ArrayList and ArrayAdapter
        entrantList = new ArrayList<>();
        eventListArrayAdapter = new OraganizerEntrantViewListArrayAdapter(this, entrantList);
        entrantsOnWaitingList.setAdapter(eventListArrayAdapter);


        //TODO: This needs to read from a event collection to get users. Not just users
        //events -> eventID -> entrants -> entrantId -> status

        db.collection("events").document(eventId).collection("entrants")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    entrantList.clear();
                    for (QueryDocumentSnapshot snapshot : value) {
                        String deviceId = snapshot.getId();
                        String status = snapshot.getString("status");

                        if (Objects.equals(status, "waiting")) {
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
    }
}
