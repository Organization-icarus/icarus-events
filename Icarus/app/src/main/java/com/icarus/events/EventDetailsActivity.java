package com.icarus.events;

import static android.content.Intent.getIntent;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
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

/**
 * Activity that displays the details of a single Event.
 *
 * Fetches the Event from Firestore using the eventId passed via Intent,
 * then populates a ListView using EventDetailsAdapter to show all fields
 * of the Event in a readable format.
 *
 * @author Bradley Bravender
 */
public class EventDetailsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        // Retrieve data passed to the intent
        String eventId = getIntent().getStringExtra("eventId");

        /* Get the correlating event details from Firestore and pass them to
        an array adapter */
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {

                        Event event = new Event(
                                doc.getString("id"),            // String
                                doc.getString("name"),          // String
                                doc.getString("category"),      // String
                                doc.getDouble("capacity"),      // double
                                doc.getDate("regOpen"),         // Date
                                doc.getDate("regClose"),        // Date
                                doc.getDate("date"),            // Date
                                doc.getString("user_status"),   // String
                                doc.getLong("waiting_list_size").intValue(), // int
                                doc.getString("location")       // String
                        );

                        // Set adapter
                        EventDetailsAdapter adapter = new EventDetailsAdapter(EventDetailsActivity.this, event);
                        ListView listView = findViewById(R.id.event_details_event_list);
                        listView.setAdapter(adapter);
                    }
                });

        /*
        TODO:
        - Parse firebase to count the waiting list size
        - Update the event name
        - Set up an event dialogue for the lottery guidelines
        - Determine the user (user, org, admin). Accordingly, use intents to:
          - Join waiting list (user:uninitialized)
          - Accept or decline (user:selected)
          - Leave waiting list (user:waiting)
          - Notifications (admin/organizer)
          - Delete (admin/organizer)
          - Manage (organizer)
          - View organizer (admin)
         */

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

        /* Template for a dialogue box when I click a button
        // 1️⃣ Find your button
        Button guidelinesButton = findViewById(R.id.lottery_guidelines);

        // 2️⃣ Set a click listener
        guidelinesButton.setOnClickListener(v -> {
            // 3️⃣ Build and show the AlertDialog
            new androidx.appcompat.app.AlertDialog.Builder(EventDetailsActivity.this)
                    .setTitle("Lottery Guidelines")       // Optional title
                    .setMessage("Here is some information about the lottery...") // The text you want
                    .setPositiveButton("OK", (dialog, which) -> {
                        // This runs when user taps OK
                        dialog.dismiss();
                    })
                    .setCancelable(true)                  // Allows user to tap outside to dismiss
                    .show();
        });
         */
    }
}
