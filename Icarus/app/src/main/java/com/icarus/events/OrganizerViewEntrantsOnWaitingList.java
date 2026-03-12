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
        db.collection("users")
        //db.collection("events").document(eventId).collection("waitingList")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    entrantList.clear();
                    for (QueryDocumentSnapshot snapshot : value) {
                            //public User(String id, String name, String email, String phone, String role,
                            //    ArrayList<String> events, Map<String, Object> settings) {
                        String id = snapshot.getId();
                        String name = snapshot.getString("name");
                        String email = snapshot.getString("email");
                        String phone = snapshot.getString("phone");
                        String role = snapshot.getString("role");

                        // populate with whatever fields your User class needs
                        if(Objects.equals(role, "entrant")){
                            entrantList.add(new User(id, name, email, phone, role, null, null));
                        }
                    }
                    eventListArrayAdapter.notifyDataSetChanged();
                });
    }
}
