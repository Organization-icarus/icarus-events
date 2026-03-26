package com.icarus.events;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Objects;
/**
 * Activity that allows organizers to view the entrants on the waiting,
 * selected, rejected, and registered list .
 * <p>
 * Organziers can filter the entrant list by selecting an
 * option at the top of the activity. The list shown is refreshed
 * when the Organizer selects a filter type
 *
 * <p>
 * This activity extends {@link NavigationBarActivity} to include
 * the application's reusable navigation bar.
 *
 * @author Ben Salmon
 */
public class OrganizerViewEntrantsOnWaitingListActivity extends NavigationBarActivity{
    private FirebaseFirestore db;
    private TextView eventName;
    private Button backButton;
    private ListView entrantsOnWaitingList;
    private MaterialButtonToggleGroup filterButtons;
    private ArrayList<User> entrantList;
    private OraganizerEntrantViewListArrayAdapter eventListArrayAdapter;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_view_entrants_on_waiting_list);
        setupNavBar();
        ArrayList<String> status = new ArrayList<>();
        db = FirebaseFirestore.getInstance();

        //Create TextView
        eventName = findViewById(R.id.OrganizerEntrantOnWaitingListEventText);
        //Create Buttons
        filterButtons = findViewById(R.id.OrganizerEntrantOnWaitingListFilterBar);
        backButton = findViewById(R.id.OrganizerEntrantOnWaitingListBackButton);
        //Create ListView
        entrantsOnWaitingList = findViewById(R.id.OrganizerEntrantOnWaitingList);
        //Initialize ArrayList and ArrayAdapter
        entrantList = new ArrayList<>();
        eventListArrayAdapter = new OraganizerEntrantViewListArrayAdapter(this, entrantList);
        entrantsOnWaitingList.setAdapter(eventListArrayAdapter);

        //get eventId
        eventId = getIntent().getStringExtra("eventId");

        //Set default as waiting
        filterButtons.check(R.id.OrganizerEntrantOnWaitingListFilterBar_waiting);
        status.add("waiting");
        loadList(status);

        //Set event Title
        db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    String name = value.getString("name");

                    runOnUiThread(() -> {
                        eventName.setText(name);
                    });
                });

        filterButtons.addOnButtonCheckedListener((group, checkedId, isChecked) ->{
            if (!isChecked) return; // ← ignore uncheck events entirely

            status.clear();
            if(isChecked && (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_waiting)){
                status.add("waiting");
            }else if(isChecked && (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_chosen)){
                status.add("selected");
            }else if(isChecked && (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_cancelled)){
                status.add("rejected");
                status.add("replaced");
            }else if(isChecked && (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_final)){
                status.add("registered");

            }
            loadList(status);
        });

        backButton.setOnClickListener(v -> {
            finish();
        });
    }
    private void loadList(ArrayList<String> listStatus) {
        //events -> eventID -> entrants -> entrantId -> status
        entrantList.clear();
        eventListArrayAdapter.notifyDataSetChanged();
        db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId).collection("entrants")
                .get()
                .addOnSuccessListener(value -> {
                    entrantList.clear();
                    for (QueryDocumentSnapshot snapshot : value) {
                        String deviceId = snapshot.getId();
                        String status = snapshot.getString("status");

                        if (listStatus.contains(status)) {
                            //If user has waiting role look for name in user collection
                            db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId)
                                    .get()
                                    .addOnSuccessListener(userSnapshot -> {
                                        String name = userSnapshot.getString("name");
                                        String email = userSnapshot.getString("email");
                                        String phone = userSnapshot.getString("phone");
                                        entrantList.add(new User(deviceId, name, email, phone,
                                                null, null, null, null));
                                        eventListArrayAdapter.notifyDataSetChanged();
                                    });
                        }
                    }
                });
    }
}
