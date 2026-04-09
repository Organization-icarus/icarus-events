package com.icarus.events;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import android.util.Log;

/**
 * Activity that allows an organizer to randomly sample attendees
 * from the event entrants subcollection.
 * <p>
 * The organizer specifies how many attendees to select. A random
 * subset of entrants with status {@code waiting} in Firestore
 * is updated to status {@code selected}.
 * <p>
 * AI Use Disclosure:
 * AI tools were used to assist with drafting and revising parts of this activity,
 * including logic structure, wording, and code refinement. All final edits,
 * integration decisions, and testing were reviewed and completed by Yifan Jiao.
 *
 * @author Yifan Jiao
 */
public class SampleAttendeesActivity extends HeaderNavBarActivity {
    private String eventId;
    private Button minusButton, addButton, sampleButton, cancelButton;
    private TextView eventName;
    private EditText attendeesNumber;
    private int waitingListSize, inviteCount;

    private FirebaseFirestore db;
    private CollectionReference entrantsRef;

    /**
     * Initializes the sampling interface and UI controls.
     *
     * @param savedInstanceState previously saved activity state,
     *                           or null if the activity is newly created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_attendees);
        setupNavBar(TAB_NONE);

        eventId = getIntent().getStringExtra("eventId");
        String name = getIntent().getStringExtra("ActivityName");
        db = FirebaseFirestore.getInstance();

        // Assign entrantsRef here so sampleEntrants() can use it without crashing
        entrantsRef = db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(eventId)
                .collection("entrants");

        entrantsRef.whereEqualTo("status", "waiting")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->
                        waitingListSize = queryDocumentSnapshots.size());


        eventName = findViewById(R.id.OrganizerSampleAttendeesEventTitle);
        eventName.setText(name);
        //Create Add, Minus, and EditText
        attendeesNumber = findViewById(R.id.OrganizerSampleAttendeesCountText);
        minusButton = findViewById(R.id.OrganizerSampleAttendeesMinusButton);
        addButton = findViewById(R.id.OrganizerSampleAttendeesPlusButton);
        //Create Cancel and Sample Buttons
        cancelButton = findViewById(R.id.OrganizerSampleAttendeesCancelButton);
        sampleButton = findViewById(R.id.OrganizerSampleAttendeesSampleButton);

        inviteCount = 1;
        attendeesNumber.setText(String.valueOf(inviteCount));

        addButton.setOnClickListener(v -> {
            inviteCount = Integer.parseInt(attendeesNumber.getText().toString());
            if (inviteCount >= waitingListSize) {
                inviteCount = waitingListSize;
                attendeesNumber.setText(String.valueOf(inviteCount));
            } else if (inviteCount < 1) {
                inviteCount = 1;
                attendeesNumber.setText(String.valueOf(inviteCount));
            } else {
                inviteCount++;
                attendeesNumber.setText(String.valueOf(inviteCount));
            }
        });

        minusButton.setOnClickListener(v -> {
            inviteCount = Integer.parseInt(attendeesNumber.getText().toString());
            if (inviteCount >= waitingListSize) {
                inviteCount = waitingListSize;
                attendeesNumber.setText(String.valueOf(inviteCount));
            } else if (inviteCount < 1) {
                inviteCount = 1;
                attendeesNumber.setText(String.valueOf(inviteCount));
            } else if (inviteCount > 1) {
                inviteCount--;
                attendeesNumber.setText(String.valueOf(inviteCount));
            }
        });

        cancelButton.setOnClickListener(v -> finish());

        sampleButton.setOnClickListener(v -> {
            inviteCount = Integer.parseInt(attendeesNumber.getText().toString());
            sampleEntrants();
            finish();
        });

    }


    /**
     * Randomly selects entrants with status "waiting" and updates them to "selected".
     * <p>
     * AI tools were used to help draft and refine portions of the entrant sampling
     * and Firestore batch update logic in this method.
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

                    int numberToMove = Math.min(inviteCount, waitingEntrants.size());
                    Collections.shuffle(waitingEntrants);

                    WriteBatch batch = db.batch();

                    for (int i = 0; i < numberToMove; i++) {
                        QueryDocumentSnapshot entrant = waitingEntrants.get(i);
                        batch.update(entrant.getReference(), "status", "selected");
                        sampledEntrants.add(entrant.getId());
                    }

                    for (QueryDocumentSnapshot entrant : waitingEntrants) {
                        String userId = entrant.getId();
                        if (!sampledEntrants.contains(userId)) {
                            rejectedEntrants.add(userId);
                        }
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(
                                        this,
                                        "Sampled " + numberToMove + " entrants.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                db.collection(FirestoreCollections.EVENTS_COLLECTION)
                                        .document(eventId)
                                        .get()
                                        .addOnSuccessListener(event -> {
                                            if (!sampledEntrants.isEmpty()){
                                                NotificationItem winNotification = new NotificationItem(
                                                        eventId,
                                                        event.getString("name"),
                                                        event.getString("image"),
                                                        UserSession.getInstance().getCurrentUser().getId(),
                                                        true,
                                                        new ArrayList<>(sampledEntrants),
                                                        "Congratulations! You have been selected to attend "
                                                                + event.getString("name") + " on " + event.getDate("startDate"),
                                                        "selected"
                                                );
                                                winNotification.sendNotification(this);
                                            }

                                            if (!rejectedEntrants.isEmpty()){
                                                NotificationItem loseNotification = new NotificationItem(
                                                        eventId,
                                                        event.getString("name"),
                                                        event.getString("image"),
                                                        UserSession.getInstance().getCurrentUser().getId(),
                                                        true,
                                                        rejectedEntrants,
                                                        "Unfortunately, you were not selected to attend "
                                                                + event.getString("name") + " on " + event.getDate("startDate")
                                                                + ". More selections may still be made in the future",
                                                        "not_selected"
                                                );
                                                loseNotification.sendNotification(this);
                                            }
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("SampleAttendees", "Failed to update entrant statuses", e);
                                Toast.makeText(this, "Failed to update entrants.", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load waiting entrants.",
                                Toast.LENGTH_SHORT).show());
    }
}