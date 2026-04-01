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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
 * <p>
 * Outstanding issues:
 * - Event ID is currently hardcoded for testing.
 * - Entrants are sampled by device ID from the entrants' subcollection.
 * AI Use Disclosure:
 * AI tools were used to assist with drafting and revising parts of this activity,
 * including logic structure, wording, and code refinement. All final edits,
 * integration decisions, and testing were reviewed and completed by Yifan Jiao.
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
        entrantsRef = db.collection(FirestoreCollections.EVENTS_COLLECTION)
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
     * AI tools were used to help draft and refine portions of the entrant sampling
     * and Firestore batch update logic in this method. Final implementation decisions
     */
    private void sampleEntrants() {
        entrantsRef.whereEqualTo("status", "waiting")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<QueryDocumentSnapshot> waitingEntrants = new ArrayList<>();
                    Set<String> sampledEntrants = new HashSet<>();
                    ArrayList<String> rejectedEntrants = new ArrayList<>();

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
                        QueryDocumentSnapshot entrant = waitingEntrants.get(i);
                        batch.update(entrant.getReference(), "status", "selected");
                        sampledEntrants.add(entrant.getId());
                    }

                    for (QueryDocumentSnapshot entrant : waitingEntrants) {
                        String userId = entrant.getId();
                        if (!sampledEntrants.contains(userId)) rejectedEntrants.add(userId);
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(
                                        this,
                                        "Sampled " + numberToMove + " entrants.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId).get()
                                        .addOnSuccessListener(event -> {
                                            NotificationItem notification = new NotificationItem(
                                                    eventId,
                                                    UserSession.getInstance().getCurrentUser().getId(),
                                                    true,
                                                    new ArrayList<>(sampledEntrants),
                                                    "Congratulations! You have been selected to attend " +
                                                            event.getString("name") + " on " + event.getDate("date"),
                                                    "selected"
                                            );
                                            notification.sendNotification();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update entrant statuses", e);
                                Toast.makeText(this, "Failed to update entrants.", Toast.LENGTH_SHORT).show();
                            });
                });
    }
}


