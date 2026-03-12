package com.icarus.events;

import static java.sql.Types.NULL;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Activity that displays the details of a single Event.
 *
 * Fetches the Event from Firestore using the eventId passed via Intent,
 * then populates a ListView using EventDetailsAdapter to show all fields
 * of the Event in a readable format.
 *
 * @author Bradley Bravender
 */
public class EventDetailsActivity extends NavigationBarActivity {
    private Button organizerBtn, manageBtn, notificationBtn, deleteBtn;
    private Button joinBtn, leaveBtn, declineBtn, registerBtn;

    private String currentRole;
    private String currentStatus;
    private int currentWaitingCount;

    private String currentName, currentCategory, currentLocation, currentImage, currentOrganizer;
    private double currentCapacity;
    private Date currentRegOpen, currentRegClose, currentDate;

    // To prevent the firebase snapshot listener from creating memory leaks
    private ListenerRegistration eventListener;
    private ListenerRegistration userListener;
    private ListenerRegistration entrantStatusListener;
    private ListenerRegistration entrantWaitlistListener;

    // Runs every time a user navigates to this intent
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);
        setupNavBar();

        // Retrieve data passed to the intent
        String eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) eventId = "hL8pW5lK9gDloqcWlmqx"; // For testing
        String finalEventId = eventId;

        // Get the current user's role and status
        User user = UserSession.getInstance().getCurrentUser();
        currentRole = user.getRole();
        String userId = user.getId();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //---------------------------
        // GET ALL BUTTONS
        //---------------------------

        organizerBtn = findViewById(R.id.event_organizer_button);
        manageBtn = findViewById(R.id.manage_button);
        notificationBtn = findViewById(R.id.notification_button);
        deleteBtn = findViewById(R.id.delete_button);
        joinBtn = findViewById(R.id.join_waiting_list_button);
        leaveBtn = findViewById(R.id.leave_waiting_list_button);
        declineBtn = findViewById(R.id.decline_button);
        registerBtn = findViewById(R.id.register_button);


        //---------------------------
        // SET CLICK LISTENERS
        //---------------------------

        // Lets uninitialized users join the waiting list
        joinBtn.setOnClickListener(v -> {
            // TODO: should not capacity be 0, -1, or NULL?
            if (currentCapacity > 0 && currentWaitingCount >= currentCapacity) {
                // No more users can enter
                Toast.makeText(this, "This event is full", Toast.LENGTH_SHORT).show();
                return;
            }

            currentStatus = "waiting";
            Map<String, Object> entrant = new HashMap<>();
            entrant.put("status", currentStatus);

            // Add user ID to event with status: "waiting"
            db.collection("events").document(finalEventId)
                    .collection("entrants").document(userId)
                    .set(entrant);
        });


        // Lets waiting users leave the waiting list
        leaveBtn.setOnClickListener(v -> {
            currentStatus = null;
            setupButtons(currentRole, currentStatus);
            refreshAdapter(finalEventId);

            // Delete the entrant from the event document
            db.collection("events").document(finalEventId)
                    .collection("entrants").document(userId)
                    .delete();
        });


        // Lets selected users reject their invitation, or registered users remove
        // their event entrant document.
        declineBtn.setOnClickListener(v -> {
            currentStatus = "rejected";
            setupButtons(currentRole, currentStatus);
            refreshAdapter(finalEventId);

            Map<String, Object> entrant = new HashMap<>();
            entrant.put("status", currentStatus);
            db.collection("events").document(finalEventId)
                    .collection("entrants").document(userId)
                    .set(entrant);
        });


        declineBtn.setOnClickListener(v -> {
            if (currentStatus.equals("registered")) {
                // Remove from event's entrant list
                db.collection("events").document(finalEventId)
                        .collection("entrants").document(userId)
                        .delete();

                // Remove event from user's own event collection
                db.collection("users").document(userId)
                        .update("events", com.google.firebase.firestore.FieldValue.arrayRemove(finalEventId));

                currentStatus = null;
            } else {
                // Selected → rejected
                Map<String, Object> entrant = new HashMap<>();
                entrant.put("status", "rejected");
                db.collection("events").document(finalEventId)
                        .collection("entrants").document(userId)
                        .set(entrant);

                currentStatus = "rejected";
            }
            setupButtons(currentRole, currentStatus);
            refreshAdapter(finalEventId);
        });


        registerBtn.setOnClickListener(v -> {
            currentStatus = "registered";
            setupButtons(currentRole, currentStatus);
            refreshAdapter(finalEventId);

            // Update the event's entrant document
            Map<String, Object> entrant = new HashMap<>();
            entrant.put("status", currentStatus);
            db.collection("events").document(finalEventId)
                    .collection("entrants").document(userId)
                    .set(entrant);

            // Add event to user's own event collection
            db.collection("users").document(userId)
                    .update("events", com.google.firebase.firestore.FieldValue.arrayUnion(finalEventId));
        });


        // TODO: adjust intent destination
        organizerBtn.setOnClickListener(v -> {
            /* organizer logic */
            // Navigates to the organizer profile
            Intent intent = new Intent(
                    EventDetailsActivity.this,
                    EntrantEventListActivity.class);
            startActivity(intent);
        });


        // TODO: adjust intent destination
        manageBtn.setOnClickListener(v -> {
            /* manage logic */
            // Navigates to the manage page
            Intent intent = new Intent(
                    EventDetailsActivity.this,
                    OrganizerViewEntrantsOnWaitingList.class);
            intent.putExtra("eventId", finalEventId);
            startActivity(intent);
        });


        // TODO: adjust intent destination
        notificationBtn.setOnClickListener(v -> {
            /* notification logic */
            // Navigates to the events notification page
            Intent intent = new Intent(
                    EventDetailsActivity.this,
                    EntrantEventListActivity.class);
            startActivity(intent);
        });


        // TODO: adjust intent destination
        deleteBtn.setOnClickListener(v -> {
            /* delete logic */
            // Deletes the event
            Intent intent = new Intent(
                    EventDetailsActivity.this,
                    EntrantEventListActivity.class);
            startActivity(intent);
        });


        //---------------------------
        // SET UP GUIDELINES DIALOG
        //---------------------------

        // Set width to 300dp
        int widthPx = (int) (300 * getResources().getDisplayMetrics().density);
        TextView guidelinesButton = findViewById(R.id.lottery_guidelines);
        guidelinesButton.setOnClickListener(v -> {
            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(EventDetailsActivity.this)
                    .setTitle("Lottery Guidelines")
                    .setMessage(getString(R.string.lottery_guidelines_message))
                    .setPositiveButton("OK", (d, which) -> d.dismiss())
                    .setCancelable(true)
                    .create();

            dialog.show();
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
            dialog.getWindow().setLayout(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT);
        });

        //---------------------------
        // LISTEN TO EVENT DOCUMENT
        // Fires on load + anytime the event document changes
        //---------------------------

        // Will run anytime the database event document changes
        eventListener = db.collection("events").document(finalEventId)
                .addSnapshotListener((doc, e) -> {
                    if (doc != null && doc.exists()) {
                        currentName      = doc.getString("name");
                        currentCategory  = doc.getString("category");
                        Double capacityValue = doc.getDouble("capacity");
                        currentCapacity = capacityValue != null ? capacityValue : -1;
                        currentRegOpen   = doc.getDate("open");
                        currentRegClose  = doc.getDate("close");
                        currentDate      = doc.getDate("date");
                        currentLocation  = doc.getString("location");
                        currentImage     = doc.getString("image");
                        currentOrganizer = doc.getString("organizer");

                        TextView eventName = findViewById(R.id.eventName);
                        eventName.setText(currentName);

                        refreshAdapter(finalEventId);
                    }
                });


        //---------------------------
        // COUNT THE EVENT'S WAITING LIST
        // Fires on load + anytime the user's entrant document changes
        //---------------------------

        entrantWaitlistListener = db.collection("events").document(finalEventId)
                .collection("entrants")
                .whereEqualTo("status", "waiting")
                .addSnapshotListener((query, e) -> {
                    if (query != null) {
                        currentWaitingCount = query.size();
                        refreshAdapter(finalEventId);
                    }
                });


        //---------------------------
        // LISTEN TO USER'S ENTRANT STATUS FOR THIS EVENT
        // Fires on load + anytime the user's entrant document changes
        //---------------------------

        entrantStatusListener = db.collection("events").document(finalEventId)
                .collection("entrants").document(userId)
                .addSnapshotListener((doc, e) -> {
                    if (doc != null && doc.exists()) {
                        currentStatus = doc.getString("status"); // could be null if field missing
                    } else {
                        currentStatus = null; // no document = not in event
                    }
                    setupButtons(currentRole, currentStatus);
                    refreshAdapter(finalEventId);
                });


        //---------------------------
        // LISTEN TO USER DOCUMENT
        // Handles role changes while app is running
        //---------------------------

        userListener = db.collection("users").document(userId)
                .addSnapshotListener((doc, e) -> {
                    if (doc != null && doc.exists()) {
                        currentRole = doc.getString("role") != null
                                ? doc.getString("role")
                                : currentRole;
                        setupButtons(currentRole, currentStatus);
                    }
                });
    }


    private void setupButtons(String role, String status) {
        // Runs every time the firebase document changes

        // Hide all first
        organizerBtn.setVisibility(View.GONE);
        manageBtn.setVisibility(View.GONE);
        notificationBtn.setVisibility(View.GONE);
        deleteBtn.setVisibility(View.GONE);
        joinBtn.setVisibility(View.GONE);
        leaveBtn.setVisibility(View.GONE);
        declineBtn.setVisibility(View.GONE);
        registerBtn.setVisibility(View.GONE);

        if (role == null) return; // safety check

        // Shared: admin + organizer
        if (role.equals("administrator") || role.equals("organizer")) {
            notificationBtn.setVisibility(View.VISIBLE);
            deleteBtn.setVisibility(View.VISIBLE);
        }

        if (role.equals("administrator")) {
            organizerBtn.setVisibility(View.VISIBLE);

        } else if (role.equals("organizer")) {
            manageBtn.setVisibility(View.VISIBLE);

        } else if (role.equals("entrant")) {

            // Allow new (i.e. not rejected) users to join the waiting list
            if (status == null || status.equals("uninitialized")) {
                joinBtn.setVisibility(View.VISIBLE);
            }

            else if (status.equals("waiting")) {
                leaveBtn.setVisibility(View.VISIBLE);
            }

            else if (status.equals("selected")) {
                declineBtn.setVisibility(View.VISIBLE);
                registerBtn.setVisibility(View.VISIBLE);
            }

            else if (status.equals("registered")) {
                declineBtn.setVisibility(View.VISIBLE);
            }
        }
    }


    private void refreshAdapter(String finalEventId) {
        if (currentName == null) return; // event data not loaded yet

        Event event = new Event(
                finalEventId, currentName, currentCategory, currentCapacity,
                currentRegOpen, currentRegClose, currentDate, currentLocation,
                currentImage, currentOrganizer, currentStatus, currentWaitingCount
        );

        EventDetailsAdapter adapter = new EventDetailsAdapter(this, event);
        ListView listView = findViewById(R.id.event_details_event_list);
        listView.setAdapter(adapter);
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (eventListener != null) eventListener.remove();
        if (userListener != null) userListener.remove();
        if (entrantStatusListener != null) entrantStatusListener.remove();
        if (entrantWaitlistListener != null) entrantWaitlistListener.remove();
    }
}
