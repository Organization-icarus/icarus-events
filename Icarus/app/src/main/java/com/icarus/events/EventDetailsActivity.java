package com.icarus.events;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.content.Intent;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.EventListener;
import java.util.Locale;


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

    // To prevent the firebase snapshot listener from creating memory leaks
    private ListenerRegistration eventListener;
    private ListenerRegistration userListener;

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
        String role = user.getRole();
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

        // Set once in onCreate, after findViewById assignments
        joinBtn.setOnClickListener(v -> { /* join logic */ });
        leaveBtn.setOnClickListener(v -> { /* leave logic */ });
        declineBtn.setOnClickListener(v -> { /* decline logic */ });
        registerBtn.setOnClickListener(v -> { /* register logic */ });
        organizerBtn.setOnClickListener(v -> { /* organizer logic */ });
        manageBtn.setOnClickListener(v -> { /* manage logic */ });
        notificationBtn.setOnClickListener(v -> { /* notification logic */ });
        deleteBtn.setOnClickListener(v -> { /* delete logic */ });


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

                        String name         = doc.getString("name");
                        String category     = doc.getString("category");
                        double capacity     = doc.getDouble("capacity");
                        Date regOpen        = doc.getDate("open");
                        Date regClose       = doc.getDate("close");
                        Date date           = doc.getDate("date");
                        String location     = doc.getString("location");
                        String image        = doc.getString("image");
                        String organizer    = doc.getString("organizer");
                        String status       = doc.getString("status");

                        TextView eventName = findViewById(R.id.eventName);
                        eventName.setText(name);

                        // Runs anytime the event document changes in Firebase
                        db.collection("events").document(finalEventId).collection("entrants")
                                .whereEqualTo("status", "waiting")
                                .get()
                                .addOnSuccessListener(query -> {
                                    int waitingCount = query.size();

                                    Event event = new Event(
                                            finalEventId, name, category, capacity,
                                            regOpen, regClose, date, location,
                                            image, organizer, status, waitingCount
                                    );

                                    EventDetailsAdapter adapter = new EventDetailsAdapter(EventDetailsActivity.this, event);
                                    ListView listView = findViewById(R.id.event_details_event_list);
                                    listView.setAdapter(adapter);

                                     // setupButtons(role, status);
                                });
                    }
                });


        //---------------------------
        // LISTEN TO USER'S ENTRANT STATUS FOR THIS EVENT
        // Fires on load + anytime the user's entrant document changes
        //---------------------------

        db.collection("events").document(finalEventId)
                .collection("entrants").document(userId)
                .addSnapshotListener((doc, e) -> {
                    if (doc != null && doc.exists()) {
                        // User is in the event, get their status
                        currentStatus = doc.getString("status") != null
                                ? doc.getString("status")
                                : "uninitialized";
                    } else {
                        // User has no entrant document, which means they haven't joined
                        currentStatus = "uninitialized";
                    }
                    setupButtons(currentRole, currentStatus);
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

        } else if (role.equals("user")) {
            if (status == null || status.equals("uninitialized")) {
                joinBtn.setVisibility(View.VISIBLE);
            } else if (status.equals("waiting")) {
                leaveBtn.setVisibility(View.VISIBLE);
            } else if (status.equals("selected")) {
                declineBtn.setVisibility(View.VISIBLE);
                registerBtn.setVisibility(View.VISIBLE);
            } else if (status.equals("registered")) {
                declineBtn.setVisibility(View.VISIBLE);
            }
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (eventListener != null) eventListener.remove();
        if (userListener != null) userListener.remove();
    }
}
