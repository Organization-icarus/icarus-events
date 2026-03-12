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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);
        setupNavBar();

        // Retrieve data passed to the intent
        String eventId = getIntent().getStringExtra("eventId");

        // For testing
        if (eventId == null) eventId = "3O8RgEkz3sVBq31gv5VI";
        String finalEventId = eventId;

        // Get the current user's role and status
        User user = UserSession.getInstance().getCurrentUser();
        String role = user.getRole();

        //---------------------------
        // GET EVENT DATA
        //---------------------------

        /* Get the correlating event details from Firestore and pass them to
        an array adapter */
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Will run anytime the database document changes
        db.collection("events").document(eventId)
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

                        Event event = new Event(
                                finalEventId, name, category, capacity,
                                regOpen, regClose, date, location,
                                image, organizer, status, waiting_list
                        );

                        EventDetailsAdapter adapter = new EventDetailsAdapter(EventDetailsActivity.this, event);
                        ListView listView = findViewById(R.id.event_details_event_list);
                        listView.setAdapter(adapter);

                        setupButtons(role, status);
                    }
                });

        /*
        db.collection("events").document(eventId)
                .get()
                .addSnapshotListener((doc, e) -> {  // swap .get() for addSnapshotListener
                    if (doc != null && doc.exists()) {
                        // ... your existing event setup code ...

                        String status = doc.getString("status"); // fetch real status
                        setupButtons(role, status);
                    }
                })
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {

                        String name = doc.getString("name");
                        String category = doc.getString("category");
                        double capacity = doc.getDouble("capacity");
                        Date regOpen = doc.getDate("open");
                        Date regClose = doc.getDate("close");
                        Date date = doc.getDate("date");
                        String location = doc.getString("location");
                        String image = doc.getString("image");
                        String organizer = doc.getString("organizer");
                        // TODO
                        String user_status = role;
                        int waiting_list = 5;

                        // Update the event name
                        TextView eventName = findViewById(R.id.eventName);
                        eventName.setText(name);

                        Event event = new Event(
                                finalEventId,
                                name,
                                category,
                                capacity,
                                regOpen,
                                regClose,
                                date,
                                location,
                                image,
                                organizer,
                                user_status,
                                waiting_list
                        );

                        // Set adapter
                        EventDetailsAdapter adapter = new EventDetailsAdapter(EventDetailsActivity.this, event);
                        ListView listView = findViewById(R.id.event_details_event_list);
                        listView.setAdapter(adapter);
                    }

                });
         */


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
        // GET AND HIDE ALL BUTTONS
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

        // Then call setupButtons once initially
        setupButtons(role, status);
    }


    private void setupButtons(String role, String status) {
        // Hide all first
        organizerBtn.setVisibility(View.GONE);
        manageBtn.setVisibility(View.GONE);
        notificationBtn.setVisibility(View.GONE);
        deleteBtn.setVisibility(View.GONE);
        joinBtn.setVisibility(View.GONE);
        leaveBtn.setVisibility(View.GONE);
        declineBtn.setVisibility(View.GONE);
        registerBtn.setVisibility(View.GONE);

        // Show relevant buttons
        if (role.equals("administrator") || role.equals("organizer")) {
            notificationBtn.setVisibility(View.VISIBLE);
            deleteBtn.setVisibility(View.VISIBLE);
        }

        if (role.equals("administrator")) {
            organizerBtn.setVisibility(View.VISIBLE);
        } else if (role.equals("organizer")) {
            manageBtn.setVisibility(View.VISIBLE);
        } else if (role.equals("user")) {
            if (status.equals("uninitialized")) {
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
}
