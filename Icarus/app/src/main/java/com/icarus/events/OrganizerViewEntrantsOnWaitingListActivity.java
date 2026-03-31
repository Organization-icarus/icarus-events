package com.icarus.events;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

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
    private Button messageButton;
    private Button selectAllButton;
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
        messageButton = findViewById(R.id.OrganizerEntrantOnWaitingListSendNotificationButton);
        selectAllButton = findViewById(R.id.OrganizerEntrantOnWaitingListSelectAllButton);
        //Create ListView
        entrantsOnWaitingList = findViewById(R.id.OrganizerEntrantOnWaitingList);
        //Initialize ArrayList and ArrayAdapter
        entrantList = new ArrayList<>();
        eventListArrayAdapter = new OraganizerEntrantViewListArrayAdapter(this, entrantList);
        entrantsOnWaitingList.setAdapter(eventListArrayAdapter);
        Set<String> selectedIds = eventListArrayAdapter.getSelectedIds();

        //get eventId
        eventId = getIntent().getStringExtra("eventId");

        //Set default as waiting
        filterButtons.check(R.id.OrganizerEntrantOnWaitingListFilterBar_waiting);
        loadList("waiting");

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
            backButton.setText("Go Back");
            eventListArrayAdapter.clearSelections();
            selectAllButton.setText("Select All");
            String status = null;
            if(isChecked && (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_waiting)){
                status = "waiting";
            }else if(isChecked && (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_chosen)){
                status = "selected";
            }else if(isChecked && (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_cancelled)){
                status = "rejected";
            }else if(isChecked && (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_final)){
                status = "registered";
                backButton.setText("Export CSV");
            }
            loadList(status);
        });

        backButton.setOnClickListener(v -> {
            if(backButton.getText().toString().equals("Export CSV")){
                createCSV();
            }
            else{
                finish();
            }

        });
        messageButton.setOnClickListener(v -> {
            if (selectedIds.isEmpty() && entrantList.isEmpty()) {
                Toast.makeText(this, "No users to notify", Toast.LENGTH_SHORT).show();
                return;
            }

            String organizerId = UserSession.getInstance().getCurrentUser().getId();

            NotificationItem notification = new NotificationItem(eventId, organizerId);
            notification.setEvent();
            notification.setRecipients(new ArrayList<>(selectedIds));

            // popup to get the message
            EditText input = new EditText(this);
            input.setHint("Type your message here");

            new AlertDialog.Builder(this)
                    .setTitle("Send Message")
                    .setView(input)
                    .setPositiveButton("Send", (dialog, which) -> {
                        String message = input.getText().toString().trim();
                        notification.setMessage(message);

                        try {
                            notification.sendNotification();
                            Toast.makeText(this, "Notification sent", Toast.LENGTH_SHORT).show();
                        } catch (IllegalArgumentException e) {
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            try {
                notification.sendNotification();
                Toast.makeText(this, "Notification sent", Toast.LENGTH_SHORT).show();
            } catch (IllegalArgumentException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        selectAllButton.setOnClickListener(v -> {
        //Select all users from the list
            if (selectedIds.size() == entrantList.size() && !entrantList.isEmpty()) {
                eventListArrayAdapter.clearSelections();
                selectAllButton.setText("Select All");
            } else {
                eventListArrayAdapter.selectAll();
                selectAllButton.setText("Deselect All");
            }
        });
    }
    private void loadList(String listStatus) {
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

                        if (Objects.equals(status, listStatus)) {
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

    private void createCSV(){
        //CSV file
        //row seperated by '\n', columns seperated by ','
        if(entrantList.size() == 0){
            Toast.makeText(this, "List is Empty. Wait for Users to Accept Invite", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder newCSV = new StringBuilder();
        newCSV.append("Name, Email, Phone\n");
        //list is already filtered
        for(User user : entrantList){
            newCSV.append(user.getName()).append(",");
            newCSV.append(user.getEmail() != null ? user.getEmail() : "").append(",");
            newCSV.append(user.getPhone() != null ? user.getPhone() : "").append("\n");
        }
        String filename = "entrant_final_list_for_" + eventName.getText().toString()+ ".csv";
        filename = filename.replace(" ", "_");
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
        values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            outputStream.write(newCSV.toString().getBytes());
            Toast.makeText(this, "CSV file Exported to Downloads/" + filename, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
        }
    }
}
