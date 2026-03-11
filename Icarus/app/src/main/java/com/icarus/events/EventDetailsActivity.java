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

public class EventDetailsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve data passed to the intent
        String eventId = getIntent().getStringExtra("eventId");

        // Get the correlating event details //
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {

                        // Extract fields from the Firebase event reference
                        String category = doc.getString("category");
                        Date close = doc.getString("close");
                        Date date = doc.getString("date");
                        String name = doc.getString("name");
                        Date open = doc.getString("open");

                        Event event = Event(
                                id=eventId,
                                name=name,
                                capacity=capacity,
                                regOpen=regOpen,
                                reClose=regClose,
                                date=date
                        );

                        // Build a list of EventField objects
                        List<EventField> fields = new ArrayList<>();
                        fields.add(new EventField("Name", name));
                        fields.add(new EventField("Location", location));
                        fields.add(new EventField("Date", date));

                        // Set adapter
                        EventFieldAdapter adapter = new EventFieldAdapter(this, fields);
                        ListView listView = findViewById(R.id.event_details_event_list);
                        listView.setAdapter(adapter);
                    }
                });
        });







    }
}
