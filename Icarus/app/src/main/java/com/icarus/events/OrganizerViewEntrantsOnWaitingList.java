package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
/**
 * Activity that allows organizers to view the entrants on the waiting,
 * selected, rejected, and registered list .
 * <p>
 * Organziers can filter the entrant list by selecting a
 * option at the top of the activity. The list shown is refreshed
 * when the Organizer selects a filter type
 *
 * <p>
 * This activity extends {@link NavigationBarActivity} to include
 * the application's reusable navigation bar.
 *
 * @author Ben Salmon
 */
public class OrganizerViewEntrantsOnWaitingList extends NavigationBarActivity{
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
        loadList("waiting");

        //Set event Title
        db.collection("events").document(eventId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    String name = value.getString("name");

                    runOnUiThread(() -> {
                        eventName.setText(name);
                    });
                });

        filterButtons.addOnButtonCheckedListener((group, checkedId, isChecked) ->{
            String status = null;
            if(isChecked && (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_waiting)){
                status = "waiting";
                loadList(status);
            }else if(isChecked && (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_chosen)){
                status = "selected";
                loadList(status);
            }else if(isChecked && (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_cancelled)){
                status = "rejected";
                loadList(status);
            }else if(isChecked && (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_final)){
                status = "registered";
                loadList(status);
            }

        });

        backButton.setOnClickListener(v -> {
            finish();
        });
    }
    private void loadList(String listStatus) {
        //events -> eventID -> entrants -> entrantId -> status
        entrantList.clear();
        eventListArrayAdapter.notifyDataSetChanged();
        db.collection("events").document(eventId).collection("entrants")
                .get()
                .addOnSuccessListener(value -> {
                    entrantList.clear();
                    for (QueryDocumentSnapshot snapshot : value) {
                        String deviceId = snapshot.getId();
                        String status = snapshot.getString("status");

                        if (Objects.equals(status, listStatus)) {
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
    }
}
