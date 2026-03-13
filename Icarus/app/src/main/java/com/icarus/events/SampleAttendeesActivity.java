package com.icarus.events;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Activity that allows an organizer to randomly sample attendees
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
 * @author Yifan Jiao
 */
public class SampleAttendeesActivity extends NavigationBarActivity {

    private static final String TAG = "SampleAttendeesActivity";
    private static final String EVENT_ID = "lw5v5aQ2g4UD5RB7uJHS";

    private int attendeeCount = 10;

    private FirebaseFirestore db;
    private DocumentReference eventRef;

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

        db = FirebaseFirestore.getInstance();
        eventRef = db.collection(FirestoreCollections.EVENTS_COLLECTION).document(EVENT_ID);

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

        sampleButton.setOnClickListener(v -> {
            Toast.makeText(this, "Sample button clicked", Toast.LENGTH_SHORT).show();
            sampleEntrants();
        });
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
     * Randomly selects entrants from the event waitlist and moves them
     * to the selected entrants list.
     */
    @SuppressWarnings("unchecked")
    private void sampleEntrants() {
        Log.d(TAG, "sampleEntrants called");

        eventRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Log.d(TAG, "Event document does not exist");
                Toast.makeText(this, "Event not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> waitingEntrants =
                    (List<String>) documentSnapshot.get("waitlist_entrants");
            List<String> selectedEntrants =
                    (List<String>) documentSnapshot.get("selected_entrants");

            if (waitingEntrants == null) {
                waitingEntrants = new ArrayList<>();
            } else {
                waitingEntrants = new ArrayList<>(waitingEntrants);
            }

            if (selectedEntrants == null) {
                selectedEntrants = new ArrayList<>();
            } else {
                selectedEntrants = new ArrayList<>(selectedEntrants);
            }

            if (waitingEntrants.isEmpty()) {
                Log.d(TAG, "No waitlist entrants");
                Toast.makeText(this, "No waitlist entrants to sample.", Toast.LENGTH_SHORT).show();
                return;
            }

            int numberToMove = Math.min(attendeeCount, waitingEntrants.size());

            Collections.shuffle(waitingEntrants);

            List<String> movedEntrants = new ArrayList<>(
                    waitingEntrants.subList(0, numberToMove)
            );

            waitingEntrants.removeAll(movedEntrants);
            selectedEntrants.addAll(movedEntrants);

            eventRef.update(
                    "waitlist_entrants", waitingEntrants,
                    "selected_entrants", selectedEntrants
            ).addOnSuccessListener(unused -> {
                Log.d(TAG, "Firestore update success");
                Toast.makeText(
                        this,
                        "Moved " + numberToMove + " entrants to selected_entrants.",
                        Toast.LENGTH_SHORT
                ).show();
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to update sampled entrants", e);
                Toast.makeText(this, "Failed to update entrants.", Toast.LENGTH_SHORT).show();
            });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to fetch event", e);
            Toast.makeText(this, "Failed to load event.", Toast.LENGTH_SHORT).show();
        });
    }
}
