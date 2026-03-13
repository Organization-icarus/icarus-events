package com.icarus.events;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Activity that allows an organizer to randomly sample attendees
 * from the event entrants subcollection.
 * <p>
 * The organizer specifies how many attendees to select. A random
 * subset of entrants with status {@code waiting} in Firestore
 * is updated to status {@code selected}.
 * from the event waitlist.
 * <p>
 * The organizer specifies how many attendees to select. A random
 * subset of users from the {@code waitlist_entrants} field in Firestore
 * is moved to the {@code selected_entrants} field.
 * <p>
 * Note: The event ID is currently hardcoded for testing purposes
 * and should be replaced with a dynamic value when integrated with
 * the full event management flow.
 *
 * Outstanding issues:
 * - Event ID is currently hardcoded for testing.
 * - Entrants are sampled by device ID from the entrants subcollection.
 *
 * @author Yifan Jiao
 */
public class SampleAttendeesActivity extends NavigationBarActivity {

    private static final String TAG = "SampleAttendeesActivity";
    private String eventId;

    private int attendeeCount = 10;

    private FirebaseFirestore db;
    private CollectionReference entrantsRef;

    /**
     * Initializes the sampling interface and UI controls.
     * <p>
     * This method configures the attendee count selector, navigation
     * buttons, and sampling functionality. It also initializes the
     * Firestore reference used to retrieve and update event data.
     *
     * @param savedInstanceState previously saved activity state,
     *                           or null if the activity is newly created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sample_attendees);
        setupNavBar();

        eventId = getIntent().getStringExtra("eventId");

        db = FirebaseFirestore.getInstance();
        entrantsRef = db.collection("events")
                .document(eventId)
                .collection("entrants");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView attendeeCountText = findViewById(R.id.attendeeCountText);
        TextView backButton = findViewById(R.id.backButton);
        Button minusButton = findViewById(R.id.minusButton);
        Button plusButton = findViewById(R.id.plusButton);
        Button cancelButton = findViewById(R.id.cancelButton);
        Button sampleButton = findViewById(R.id.sampleButton);

        updateAttendeeCount(attendeeCountText);

        backButton.setOnClickListener(v -> finish());

        plusButton.setOnClickListener(v -> {
            attendeeCount++;
            updateAttendeeCount(attendeeCountText);
        });

        minusButton.setOnClickListener(v -> {
            if (attendeeCount > 1) {
                attendeeCount--;
                updateAttendeeCount(attendeeCountText);
            }
        });

        cancelButton.setOnClickListener(v -> finish());

        sampleButton.setOnClickListener(v -> sampleEntrants());
    }

    /**
     * Updates the attendee count displayed on screen.
     *
     * @param attendeeCountText TextView used to display the current
     *                          number of attendees to sample
     */
    private void updateAttendeeCount(TextView attendeeCountText) {
        attendeeCountText.setText(String.valueOf(attendeeCount));
    }

    /**
     * Randomly selects entrants with status "waiting" and updates them to "selected".
     * Randomly selects entrants from the event waitlist and moves them
     * to the selected entrants list.
     */
    private void sampleEntrants() {
        entrantsRef.whereEqualTo("status", "waiting")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<QueryDocumentSnapshot> waitingEntrants = new ArrayList<>();

                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                        waitingEntrants.add(snapshot);
                    }

                    if (waitingEntrants.isEmpty()) {
                        Toast.makeText(this, "No waiting entrants to sample.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int numberToMove = Math.min(attendeeCount, waitingEntrants.size());

                    Collections.shuffle(waitingEntrants);

                    WriteBatch batch = db.batch();

                    for (int i = 0; i < numberToMove; i++) {
                        batch.update(waitingEntrants.get(i).getReference(), "status", "selected");
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> Toast.makeText(
                                    this,
                                    "Sampled " + numberToMove + " entrants.",
                                    Toast.LENGTH_SHORT
                            ).show())
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update entrant statuses", e);
                                Toast.makeText(this, "Failed to update entrants.", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load waiting entrants", e);
                    Toast.makeText(this, "Failed to load waiting entrants.", Toast.LENGTH_SHORT).show();
                });
    }
}