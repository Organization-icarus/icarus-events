package com.icarus.events;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Activity that allows organizers to view the entrants on the waiting,
 * selected, rejected, and registered list .
 * <p>
 * Organziers can filter the entrant list by selecting an
 * option at the top of the activity. The list shown is refreshed
 * when the Organizer selects a filter type
 *
 * <p>
 * This activity extends {@link HeaderNavBarActivity} to include
 * the application's reusable navigation bar.
 *
 * @author Ben Salmon
 */
public class OrganizerViewEntrantsOnWaitingListActivity extends HeaderNavBarActivity {
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
        AtomicReference<String> status = new AtomicReference<>();

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
            status.set(null);
            if(isChecked && (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_waiting)){
                status.set("waiting");
            }else if(isChecked && (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_chosen)){
                status.set("selected");
            }else if(isChecked && (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_cancelled)){
                status.set("rejected");
            }else if(isChecked && (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_final)){
                status.set("registered");
                backButton.setText("Export CSV");
            }
            loadList(status.get());
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
            if (selectedIds.isEmpty()) {
                Toast.makeText(this, "No users selected", Toast.LENGTH_SHORT).show();
                return;
            }

            /*
            * The Material Alert DialogBuilder was created by Claude
            * March 31, 2026. "I need a pop up that will allow a user to
            * type in a message without having to create an additional XML file"*/
            EditText input = new EditText(this);
            input.setHint("The Message you wish to send");
            input.setPadding(48, 24, 48, 24);
            input.setHintTextColor(0x80FFFFFF);
            new MaterialAlertDialogBuilder(this, R.style.CustomAlertDialog)
                    .setTitle("Send Message to Entrants")
                    .setView(input)
                    .setPositiveButton("Send Message", (dialog, which) -> {
                        String message = input.getText().toString().trim();
                        if (message.isEmpty()) {
                            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        sendMessage(message, String.valueOf(status), new ArrayList<>(selectedIds));

                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
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

    private void sendMessage(String message, String type, ArrayList<String> recipients) {
        User user = UserSession.getInstance().getCurrentUser();
        String userId = user.getId();
        Date now = new Date();

        Map<String, Object> notification = new HashMap<>();
        notification.put("date", now);
        notification.put("eventID", eventId);
        notification.put("isEvent", true);
        notification.put("isSystem", false);
        notification.put("message", message);
        notification.put("recipients", recipients);
        notification.put("sender", userId);
        notification.put("type", type);

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(dummy -> {
                    Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }
}
