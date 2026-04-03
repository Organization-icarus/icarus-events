package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrganizerMyEventsActivity extends NavigationBarActivity {

    private FirebaseFirestore db;
    private RecyclerView eventListView;
    private ArrayList<Event> organizerEvents;
    private EntrantEventListArrayAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_my_events);


        db = FirebaseFirestore.getInstance();
        eventListView = findViewById(R.id.organizer_my_events_list);

        organizerEvents = new ArrayList<>();

        adapter = new EntrantEventListArrayAdapter(
                this,
                organizerEvents,
                position -> {
                    Event selected = organizerEvents.get(position);
                    Intent intent = new Intent(this, EventDetailsActivity.class);
                    intent.putExtra("eventId", selected.getId());
                    startActivity(intent);
                },
                new java.util.HashMap<>()
        );

        eventListView.setLayoutManager(new LinearLayoutManager(this));
        eventListView.setAdapter(adapter);

        loadOrganizerEvents();
    }

    private void loadOrganizerEvents() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            Toast.makeText(this, "User session not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String currentUserId = currentUser.getId();

        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .whereArrayContains("organizers", currentUserId)
                .get()
                .addOnSuccessListener(query -> {
                    organizerEvents.clear();

                    for (QueryDocumentSnapshot snapshot : query) {
                        String id = snapshot.getId();
                        String name = snapshot.getString("name");
                        String category = snapshot.getString("category");
                        Double capacity = snapshot.getDouble("capacity");
                        Date regOpen = snapshot.getDate("open");
                        Date regClose = snapshot.getDate("close");
                        Date date = snapshot.getDate("date");
                        String location = snapshot.getString("location");
                        String image = snapshot.getString("image");

                        List<String> rawOrganizers = (List<String>) snapshot.get("organizers");
                        ArrayList<String> organizers = rawOrganizers == null
                                ? new ArrayList<>()
                                : new ArrayList<>(rawOrganizers);

                        organizerEvents.add(new Event(
                                id,
                                name,
                                category,
                                capacity,
                                regOpen,
                                regClose,
                                date,
                                location,
                                image,
                                organizers
                        ));
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load organizer events", Toast.LENGTH_SHORT).show());
    }
}