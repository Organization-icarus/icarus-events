package com.icarus.events;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class AdministratorDashboardEventArrayAdapter extends ArrayAdapter<Event> {
    private ArrayList<Event> events;
    private Context context;
    private FirebaseFirestore db;

    public AdministratorDashboardEventArrayAdapter(Context context, ArrayList<Event> events){
        super(context, 0, events);
        this.events = events;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
        View view = convertView;
        if (view == null){
            view = LayoutInflater.from(context)
                    .inflate(R.layout.administrator_dashboard_event_list_content, parent, false);
        }

        Event event = events.get(position);
        TextView eventName = view
                .findViewById(R.id.admin_dashboard_event_list_event_name);
        Button eventDetailsButton = view
                .findViewById(R.id.admin_dashboard_event_list_event_details_button);
        ImageButton removeEventButton = view
                .findViewById(R.id.admin_dashboard_event_list_remove_event_button);

        eventName.setText(event.getName());

        eventDetailsButton.setOnClickListener(v -> {
//            Intent intent = new Intent(context, EventDetailsActivity.class);
//            intent.putExtra("eventId", event.getId());
//            context.startActivity(intent);
        });

        removeEventButton.setOnClickListener(v -> {
            DocumentReference docRef = db.collection("events")
                                        .document(event.getId());
            docRef.delete();
        });

        return view;
    }
}
