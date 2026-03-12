package com.icarus.events;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
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
        // TODO: get status
        String status = "waiting";

        //---------------------------
        // GET EVENT DATA
        //---------------------------

        /* Get the correlating event details from Firestore and pass them to
        an array adapter */
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events").document(eventId)
                .get()
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

        Button joinBtn = findViewById(R.id.join_waiting_list);
        Button acceptBtn = findViewById(R.id.accept);
        Button declineBtn = findViewById(R.id.decline);
        Button leaveBtn = findViewById(R.id.leave_event);
        Button manageBtn = findViewById(R.id.manage);
        Button notificationsBtn = findViewById(R.id.notifications);
        Button deleteBtn = findViewById(R.id.delete);

        //---------------------------
        // HIDE BUTTONS, SET LISTENERS
        //---------------------------

        if (role.equals("administrator")) {
            /*
            - show organizer, notifications, delete
            - hide all others
             */
        } else if (role.equals("organizer")) {
            /*
            - show manage, notifications, delete
            - hide all others
             */
        }  else if (role.equals("user") && status.equals("uninitialized")) {
            /*
            If the user is a user and is uninitialized:
            - show join waiting list button
            - hide all others
         */
        } else if (role.equals("user") && status.equals("waiting")) {
          /*
            If the user is a user and is uninitialized:
            - show join waiting list button
            - hide all others
         */
        } else if (role.equals("user") && status.equals("selected")) {
            /*
            If the user is a user and is selected:
                - show the accept and decline buttons
                - hide all others
             */
        } else if (role.equals("user") && status.equals("registered")) {
            /*
            If the user is a user and is registered
                - show leave event
                - hide all others
             */
        } else if (role.equals("user") && status.equals("rejected")) {

        }






        /* Template for click listener to new intent
        // 1️⃣ Find your button
        Button myButton = findViewById(R.id.my_button);

        // 2️⃣ Set a click listener
        myButton.setOnClickListener(v -> {
            // 3️⃣ Create an Intent to navigate to the other Activity
            Intent intent = new Intent(EventDetailsActivity.this, OtherActivity.class);

            // 4️⃣ Optional: pass data to the next Activity
            intent.putExtra("eventId", "abc123"); // Example key-value

            // 5️⃣ Start the Activity
            startActivity(intent);
        });
         */

    }
}
