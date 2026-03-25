package com.icarus.events;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OrganizerEntrantSearchActivity extends NavigationBarActivity{
    private FirebaseFirestore db;
    private TextView eventName;
    private EditText searchBar;
    private ListView entrantList;
    private ArrayList<User> entrantUserList;
    private OraganizerEntrantViewListArrayAdapter eventListArrayAdapter;
    private String eventId;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_entrant_search);
        setupNavBar();
        User user = UserSession.getInstance().getCurrentUser();
        userId = user.getId();

        db = FirebaseFirestore.getInstance();

        //Create Event Name, Get eventID, and Get event Title from database
        eventName = findViewById(R.id.OrganizerEntrantSearchEventName);
        eventId = getIntent().getStringExtra("eventId");
        db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    String name = value.getString("name");

                    runOnUiThread(() -> {
                        eventName.setText(name);
                    });
                });
        //Create EditText
        searchBar = findViewById(R.id.OrganizerEntrantSearchBar);

        //Create ListView
        entrantList = findViewById(R.id.OrganizerEntrantList);
        entrantUserList = new ArrayList<>();
        eventListArrayAdapter = new OraganizerEntrantViewListArrayAdapter(this, entrantUserList);
        entrantList.setAdapter(eventListArrayAdapter);

        //Fill list with users not in the waiting list
        loadList();


    }
    private void loadList() {
        entrantUserList.clear();
        eventListArrayAdapter.notifyDataSetChanged();

        // Step 1: Get all entrant IDs from the subcollection
        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(eventId)
                .collection("entrants")
                .get()
                .addOnSuccessListener(entrantSnapshots -> {

                    // Collect all entrant device IDs into a Set for O(1) lookup
                    Set<String> entrantIds = new HashSet<>();
                    for (QueryDocumentSnapshot snapshot : entrantSnapshots) {
                        entrantIds.add(snapshot.getId());
                    }
                    // Step 2: Get ALL users
                    db.collection(FirestoreCollections.USERS_COLLECTION)
                            .get()
                            .addOnSuccessListener(userSnapshots -> {
                                entrantUserList.clear();

                                for (QueryDocumentSnapshot userSnapshot : userSnapshots) {
                                    String deviceId = userSnapshot.getId();
                                    String name = userSnapshot.getString("name");
                                    Boolean isAdmin = userSnapshot.getBoolean("isAdmin");

                                    boolean userIsAdmin = isAdmin != null && isAdmin;
                                    boolean notInEntrants = !entrantIds.contains(deviceId);

                                    // Include if not an entrant OR is an admin
                                    if (notInEntrants || userIsAdmin) {
                                        entrantUserList.add(new User(deviceId, name, null, null,
                                                null, null, null, null));
                                    }
                                }

                                eventListArrayAdapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("loadList", "Failed to fetch users", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("loadList", "Failed to fetch entrants", e);
                });
    }
}
