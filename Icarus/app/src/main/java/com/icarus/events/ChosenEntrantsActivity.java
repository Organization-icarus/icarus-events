package com.icarus.events;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

/**
 * Screen for organizers to view chosen entrants for an event.
 *
 * Outstanding issues:
 * - Event ID is currently hardcoded for testing.
 * - Entrants are currently displayed by device ID only.
 */
public class ChosenEntrantsActivity extends AppCompatActivity {

    private static final String TAG = "ChosenEntrantsActivity";
    private static final String EVENT_ID = "JQRdL6qGdb8blRJubee0";

    private FirebaseFirestore db;
    private CollectionReference entrantsRef;

    private ArrayList<String> chosenEntrantsList;
    private ArrayAdapter<String> chosenEntrantsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chosen_entrants);

        db = FirebaseFirestore.getInstance();
        entrantsRef = db.collection("events")
                .document(EVENT_ID)
                .collection("entrants");

        TextView backButton = findViewById(R.id.chosenEntrantsBackButton);
        ListView chosenEntrantsListView = findViewById(R.id.chosenEntrantsListView);

        chosenEntrantsList = new ArrayList<>();
        chosenEntrantsAdapter = new ArrayAdapter<>(
                this,
                R.layout.chosen_entrant_list_item,
                R.id.chosenEntrantName,
                chosenEntrantsList
        );
        chosenEntrantsListView.setAdapter(chosenEntrantsAdapter);

        backButton.setOnClickListener(v -> finish());

        loadChosenEntrants();
    }

    /**
     * Loads entrants whose status is "selected" from Firestore and displays them.
     */
    private void loadChosenEntrants() {
        entrantsRef.whereEqualTo("status", "selected")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    chosenEntrantsList.clear();

                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                        chosenEntrantsList.add(snapshot.getId());
                    }

                    chosenEntrantsAdapter.notifyDataSetChanged();

                    if (chosenEntrantsList.isEmpty()) {
                        Toast.makeText(this, "No chosen entrants found.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(
                                this,
                                "Loaded " + chosenEntrantsList.size() + " chosen entrants.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load chosen entrants", e);
                    Toast.makeText(this, "Failed to load chosen entrants.", Toast.LENGTH_SHORT).show();
                });
    }
}