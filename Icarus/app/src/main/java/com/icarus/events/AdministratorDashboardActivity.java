package com.icarus.events;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;

public class AdministratorDashboardActivity extends AppCompatActivity {
    private ListView eventListView;
    private FirebaseFirestore db;
    private CollectionReference eventsRef;
    private ArrayList<Event> eventArrayList;
    private ArrayAdapter<Event> eventArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_administrator_dashboard);

        // Initialize database reference and collection references
        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("events");

        // Set views
        eventListView = findViewById(R.id.admin_dashboard_event_list);

        // create event array
        eventArrayList = new ArrayList<>();
        eventArrayAdapter = new AdministratorDashboardEventArrayAdapter(this,
                eventArrayList);

        // Get all items in the collection
        eventsRef.addSnapshotListener((value,error) -> {
            if (error != null){
                Log.e("Firestore", error.toString());
            }
            if (value != null && !value.isEmpty()) {
                eventArrayList.clear();
                for (QueryDocumentSnapshot snapshot : value) {
                    String id = snapshot.getId();
                    String name = snapshot.getString("name");
                    double capacity = snapshot.getDouble("capacity");
                    Date regOpen = snapshot.getDate("open");
                    Date regClose = snapshot.getDate("close");
                    Date date = snapshot.getDate("date");

                    eventArrayList.add(new Event(id, name, capacity, regOpen, regClose, date));
                }
                eventArrayAdapter.notifyDataSetChanged();
            }
        });

        eventListView.setAdapter(eventArrayAdapter);
    }
}
