package com.icarus.events;


import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.checkerframework.checker.units.qual.N;

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
    private Boolean isPrivate;

    /**
     * Initializes the activity, sets up the navigation bar, loads the event data,
     * configures the filter buttons, and sets up the entrant list, messaging,
     * CSV export, and selection controls.
     *
     * @param savedInstanceState the previously saved activity state, or {@code null}
     *                           if no state exists
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_view_entrants_on_waiting_list);
        setupNavBar(TAB_NONE);
        db = FirebaseFirestore.getInstance();

        // Create TextView
        eventName = findViewById(R.id.OrganizerEntrantOnWaitingListEventText);

        // Create Buttons
        filterButtons = findViewById(R.id.OrganizerEntrantOnWaitingListFilterBar);
        backButton = findViewById(R.id.OrganizerEntrantOnWaitingListBackButton);
        messageButton = findViewById(R.id.OrganizerEntrantOnWaitingListSendNotificationButton);
        selectAllButton = findViewById(R.id.OrganizerEntrantOnWaitingListSelectAllButton);

        // Create ListView
        entrantsOnWaitingList = findViewById(R.id.OrganizerEntrantOnWaitingList);

        // Initialize ArrayList and ArrayAdapter
        entrantList = new ArrayList<>();
        eventListArrayAdapter = new OraganizerEntrantViewListArrayAdapter(this, entrantList);
        entrantsOnWaitingList.setAdapter(eventListArrayAdapter);


        AtomicReference<String> status = new AtomicReference<>();

        // get eventId
        eventId = getIntent().getStringExtra("eventId");


        // Set event Title

        MaterialButton waitingButton = findViewById(R.id.OrganizerEntrantOnWaitingListFilterBar_waiting);
        //Set event Title

        db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    String name = value.getString("name");
                    isPrivate = value.getBoolean("isPrivate");
                    if(isPrivate == null){isPrivate = false;}



                    runOnUiThread(() -> {
                        eventName.setText(name);

                        // Only set the default filter on the very first snapshot
                        if (filterButtons.getCheckedButtonId() == View.NO_ID) {
                            if (isPrivate) {
                                waitingButton.setEnabled(false);
                                filterButtons.check(R.id.OrganizerEntrantOnWaitingListFilterBar_chosen);
                                MaterialButton defaultButton = findViewById(R.id.OrganizerEntrantOnWaitingListFilterBar_chosen);
                                defaultButton.setTextColor(getColor(R.color.darkText));
                                status.set("selected");
                            } else {
                                filterButtons.check(R.id.OrganizerEntrantOnWaitingListFilterBar_waiting);
                                MaterialButton defaultButton = findViewById(R.id.OrganizerEntrantOnWaitingListFilterBar_waiting);
                                defaultButton.setTextColor(getColor(R.color.darkText));
                                status.set("waiting");
                            }
                            loadList(status.get());
                        }
                    });

                });

        filterButtons.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            for (int i = 0; i < group.getChildCount(); i++) {
                View view = group.getChildAt(i);
                if (view instanceof MaterialButton) {
                    ((MaterialButton) view).setTextColor(getColor(R.color.lightText));
                }
            }

            MaterialButton selectedButton = findViewById(checkedId);
            selectedButton.setTextColor(getColor(R.color.darkText));

            backButton.setText("Go Back");
            eventListArrayAdapter.clearSelections();
            selectAllButton.setText("Select All");
            status.set(null);

            if (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_waiting) {
                status.set("waiting");
            } else if (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_chosen) {
                status.set("selected");
            } else if (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_cancelled) {
                status.set("rejected");
            } else if (checkedId == R.id.OrganizerEntrantOnWaitingListFilterBar_final) {
                status.set("registered");
                backButton.setText("Export CSV");
            }

            loadList(status.get());
        });

        backButton.setOnClickListener(v -> {
            if (backButton.getText().toString().equals("Export CSV")) {
                createCSV();
            } else {
                finish();
            }
        });

        messageButton.setOnClickListener(v -> {
            Set<String> selectedIds = eventListArrayAdapter.getSelectedIds();

            if (selectedIds.isEmpty()) {
                Toast.makeText(this, "No users selected", Toast.LENGTH_SHORT).show();
                return;
            }


            /*
            * The Material Alert DialogBuilder was created by Claude
            * March 31, 2026. "I need a pop up that will allow a user to
            * type in a message without having to create an additional XML file"
            * */

            EditText input = new EditText(this);
            input.setHint("The Message you wish to send");
            input.setPadding(48, 24, 48, 24);
            input.setHintTextColor(getColor(R.color.lightTextSemi));
            input.setTextColor(getColor(R.color.lightText));

            new MaterialAlertDialogBuilder(this, R.style.CustomAlertDialog)
                    .setTitle("Send Message to Entrants")
                    .setView(input)
                    .setPositiveButton("Send Message", (dialog, which) -> {
                        String message = input.getText().toString().trim();
                        if (message.isEmpty()) {
                            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        sendMessage(message, status.get(), new ArrayList<>(selectedIds));
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        selectAllButton.setOnClickListener(v -> {

            Set<String> currentlySelectedIds = eventListArrayAdapter.getSelectedIds();
            if (currentlySelectedIds.size() == entrantList.size() && !entrantList.isEmpty()) {


                eventListArrayAdapter.clearSelections();
                selectAllButton.setText("Select All");
            } else {
                eventListArrayAdapter.selectAll();
                selectAllButton.setText("Deselect All");
            }
        });
    }



    /**
     * Loads all entrants for the current event whose status matches the
     * provided list status, then populates the list view with their user data.
     *
     * @param listStatus the entrant status to display, such as waiting,
     *                   selected, rejected, or registered
     */

    private void loadList(String listStatus) {
        entrantList.clear();
        eventListArrayAdapter.notifyDataSetChanged();

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(eventId)
                .collection("entrants")
                .get()
                .addOnSuccessListener(value -> {
                    entrantList.clear();
                    for (QueryDocumentSnapshot snapshot : value) {
                        String deviceId = snapshot.getId();
                        String status = snapshot.getString("status");

                        if (Objects.equals(status, listStatus)) {
                            db.collection(FirestoreCollections.USERS_COLLECTION)
                                    .document(deviceId)
                                    .get()
                                    .addOnSuccessListener(userSnapshot -> {
                                        String name = userSnapshot.getString("name");
                                        String email = userSnapshot.getString("email");
                                        String phone = userSnapshot.getString("phone");
                                        String image = userSnapshot.getString("image");
                                        entrantList.add(new User(deviceId, name, email, phone, image, null, null,
                                                null, null, null));
                                        eventListArrayAdapter.notifyDataSetChanged();
                                    });
                        }
                    }
                });
    }


    /**
     * Creates and exports a CSV file of the currently displayed entrant list
     * into the device's Downloads directory.
     */
    private void createCSV(){
        //CSV file
        //row seperated by '\n', columns seperated by ','
        if(entrantList.size() == 0){

            Toast.makeText(this, "List is Empty. Wait for Users to Accept Invite", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder newCSV = new StringBuilder();
        newCSV.append("Name, Email, Phone\n");

        for (User user : entrantList) {
            newCSV.append(user.getName()).append(",");
            newCSV.append(user.getEmail() != null ? user.getEmail() : "").append(",");
            newCSV.append(user.getPhone() != null ? user.getPhone() : "").append("\n");
        }

        String filename = "entrant_final_list_for_" + eventName.getText().toString() + ".csv";
        filename = filename.replace(" ", "_");

        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
        values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");

        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) {
                Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
                return;
            }
            outputStream.write(newCSV.toString().getBytes());
            Toast.makeText(this, "CSV file Exported to Downloads/" + filename, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Creates and sends a notification message to the provided recipients.
     *
     * @param message the message body to send
     * @param type the notification type associated with the selected entrant status
     * @param recipients the list of user IDs that should receive the message
     */
    private void sendMessage(String message, String type, ArrayList<String> recipients) {
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String eventName = documentSnapshot.getString("name");
                        String eventImage = documentSnapshot.getString("image");

                        NotificationItem notification = new NotificationItem(
                                eventId,
                                eventName,
                                eventImage,
                                user.getId(),
                                true,
                                recipients,
                                message,
                                type
                        );
                        notification.sendNotification(this);
                    } else {
                        Log.e("NotificationError", "Event not found for ID: " + eventId);
                    }
                });
    }
}