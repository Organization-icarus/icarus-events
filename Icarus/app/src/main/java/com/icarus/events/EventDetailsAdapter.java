package com.icarus.events;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Adapter to display an Event's fields in a ListView using ArrayAdapter.
 * Each row shows field name and field value.
 *
 * @author Bradley Bravender
 */
public class EventDetailsAdapter extends ArrayAdapter<EventField> {

    // To convert an XML layout into a View
    private final LayoutInflater inflater;

    public EventDetailsAdapter(@NonNull Context context, Event event) {
        super(context, 0, new ArrayList<>());

        this.inflater = LayoutInflater.from(context);

        // Convert Dates to Strings
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd 'at' HH:mm", Locale.getDefault());
        Date regOpenDate = event.getRegOpen();
        Date regCloseDate = event.getRegClose();
        Date eventDate = event.getDate();
        String regOpen = regOpenDate != null ? sdf.format(regOpenDate) : "TBD";
        String regClose = regCloseDate != null ? sdf.format(regCloseDate) : "TBD";
        String date = eventDate != null ? sdf.format(eventDate) : "TBD";

        // Convert Event fields into EventField list
        add(new EventField("Name", event.getName()));
        add(new EventField("Category", event.getCategory()));
        add(new EventField("Capacity", String.valueOf(event.getCapacity().intValue())));
        add(new EventField("Reg. Opens", regOpen));
        add(new EventField("Reg. Closes", regClose));
        add(new EventField("Date", date));
        add(new EventField("Location", event.getLocation()));
        add(new EventField("Image", event.getImage()));
        add(new EventField("Organizer", event.getOrganizer()));
        add(new EventField("User Status", event.getUser_status()));
        add(new EventField("Waiting List Size", String.valueOf(event.getWaiting_list_size())));
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.event_details_list_content, parent, false);
        }

        EventField field = getItem(position);

        TextView nameView = convertView.findViewById(R.id.field_name);
        TextView valueView = convertView.findViewById(R.id.field_value);

        if (field != null) {
            nameView.setText(field.getName());
            valueView.setText(field.getValue());
        }

        return convertView;
    }
}
