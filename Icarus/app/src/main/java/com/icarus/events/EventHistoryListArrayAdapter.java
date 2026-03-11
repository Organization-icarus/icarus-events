package com.icarus.events;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class EventHistoryListArrayAdapter extends ArrayAdapter<Event> {
    private ArrayList<Event> events;
    private Context context;
    public EventHistoryListArrayAdapter(Context context, ArrayList<Event> events) {
        super(context, 0, events);
        this.events = events;
        this.context = context;
    }

    @NonNull
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
        View view = convertView;
        if (view == null){
            view = LayoutInflater.from(context)
                    .inflate(R.layout.event_history_list_content, parent, false);
        }

        Event event = events.get(position);
        TextView eventName = view
                .findViewById(R.id.event_history_list_event_name);
        TextView eventCategory = view
                .findViewById(R.id.event_history_list_event_category);
        TextView eventDate = view
                .findViewById(R.id.event_history_list_event_date);

        eventName.setText(event.getName());
        eventCategory.setText(event.getCategory());
        // Reformatting date to be more readable and convert to string
        if (event.getDate() != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            String dateString = formatter.format(event.getDate());
            eventDate.setText(dateString);
        } else {
            eventDate.setText(R.string.entrant_event_list_missing_date);
        }

        return view;
    }
}
