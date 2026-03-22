package com.icarus.events;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

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
public class EventDetailsAdapter extends RecyclerView.Adapter<EventDetailsAdapter.ViewHolder> {

    // To convert an XML layout into a View
    private final LayoutInflater inflater;
    private final List<EventField> fields;

    public EventDetailsAdapter(@NonNull Context context, Event event) {

        this.inflater = LayoutInflater.from(context);
        this.fields = new ArrayList<>();

        // Convert Dates to Strings
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd 'at' HH:mm", Locale.getDefault());
        Date regOpenDate = event.getRegOpen();
        Date regCloseDate = event.getRegClose();
        Date eventDate = event.getDate();
        String regOpen = regOpenDate != null ? sdf.format(regOpenDate) : "TBD";
        String regClose = regCloseDate != null ? sdf.format(regCloseDate) : "TBD";
        String date = eventDate != null ? sdf.format(eventDate) : "TBD";

        // Convert Event fields into EventField list
        fields.add(new EventField("Name", event.getName()));
        fields.add(new EventField("Category", event.getCategory()));
        fields.add(new EventField("Capacity", event.getCapacity() < 1 ? "Unlimited" : String.valueOf(event.getCapacity().intValue())));
        fields.add(new EventField("Reg. Opens", regOpen));
        fields.add(new EventField("Reg. Closes", regClose));
        fields.add(new EventField("Date", date));
        fields.add(new EventField("Location", event.getLocation()));
        fields.add(new EventField("Image", event.getImage()));
        fields.add(new EventField("Organizer", event.getOrganizer()));
        fields.add(new EventField("User Status", event.getUser_status()));
        fields.add(new EventField("Waiting List Size", String.valueOf(event.getWaiting_list_size())));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameView, valueView;
        public ViewHolder(View view) {
            super(view);
            nameView = view.findViewById(R.id.field_name);
            valueView = view.findViewById(R.id.field_value);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.event_details_list_content, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventField field = fields.get(position);
        holder.nameView.setText(field.getName());
        holder.valueView.setText(field.getValue());
    }

    @Override
    public int getItemCount() { return fields.size(); }

}
