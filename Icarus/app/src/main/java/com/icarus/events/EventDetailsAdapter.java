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

import com.google.firebase.firestore.FirebaseFirestore;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * RecyclerView adapter that displays labeled event details.
 * <p>
 * Each row contains a field name and its corresponding value derived from an
 * {@link Event}. Organizer information is initially populated from the event
 * data and then updated asynchronously from Firestore when available.
 *
 * @author Bradley Bravender
 */
public class EventDetailsAdapter extends RecyclerView.Adapter<EventDetailsAdapter.ViewHolder> {

    // To convert an XML layout into a View
    private final LayoutInflater inflater;
    private final List<EventField> fields;

    /**
     * Creates an adapter for displaying the details of the given event.
     * <p>
     * This constructor formats date fields, builds the list of displayable
     * event fields, and requests the organizer's display name from Firestore.
     *
     * @param context the context used to obtain a {@link LayoutInflater}
     * @param event the event whose details will be displayed
     */
    public EventDetailsAdapter(@NonNull Context context, Event event) {

        this.inflater = LayoutInflater.from(context);
        this.fields = new ArrayList<>();

        // Convert Dates to Strings
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd 'at' HH:mm", Locale.getDefault());
        Date regOpenDate = event.getRegOpen();
        Date regCloseDate = event.getRegClose();
        Date eventStartDate = event.getStartDate();
        Date eventEndDate = event.getEndDate();
        String regOpen = regOpenDate != null ? sdf.format(regOpenDate) : "TBD";
        String regClose = regCloseDate != null ? sdf.format(regCloseDate) : "TBD";
        String startDate = eventStartDate != null ? sdf.format(eventStartDate) : "TBD";
        String endDate = eventEndDate != null ? sdf.format(eventEndDate) : "TBD";

        String waitingListDisplay;
        if (event.getCapacity() == null || event.getCapacity() < 1) {
            waitingListDisplay = event.getWaiting_list_size() + " (no limit)";
        } else {
            waitingListDisplay = event.getWaiting_list_size() + "/" + event.getCapacity().intValue();
        }

        // Convert Event fields into EventField list
        fields.add(new EventField("Category", event.getCategory()));
        fields.add(new EventField("Waiting List", waitingListDisplay));
        fields.add(new EventField("Registration Opens", regOpen));
        fields.add(new EventField("Registration Closes", regClose));
        fields.add(new EventField("Event Start Date", startDate));
        fields.add(new EventField("Event End Date", endDate));
        fields.add(new EventField("Location", event.getLocation()));
        fields.add(new EventField("Organizer", event.getOrganizers().get(0)));
        fields.add(new EventField("User Status", event.getUser_status()));

        // To get the organizer's name from their ID
        FirebaseFirestore.getInstance()
                .collection(FirestoreCollections.USERS_COLLECTION)
                .document(event.getOrganizers().get(0))
                .get()
                .addOnSuccessListener(doc -> {
                    int organizerIndex = -1;
                    // Iterate through the fields array until we find the Organizer field
                    for (int i = 0; i < fields.size(); i++) {
                        if (fields.get(i).getName().equals("Organizer")) {
                            organizerIndex = i;
                            break;
                        }
                    }
                    if (organizerIndex != -1) {
                        fields.set(organizerIndex, new EventField("Organizer", doc.getString("name")));
                        notifyItemChanged(organizerIndex);
                    }
                });
    }

    /**
     * ViewHolder for a single event detail row.
     * <p>
     * Holds references to the label and value views used to display one
     * {@link EventField}.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameView, valueView;

        /**
         * Creates a ViewHolder for an event detail row.
         *
         * @param view the row view containing the field name and value views
         */
        public ViewHolder(View view) {
            super(view);
            nameView = view.findViewById(R.id.field_name);
            valueView = view.findViewById(R.id.field_value);
        }
    }

    /**
     * Creates and inflates a new view holder for an event detail row.
     *
     * @param parent the parent view that the new row will be attached to
     * @param viewType the view type of the new row
     * @return a new {@link ViewHolder} for the inflated row
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.event_details_list_content, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds the event field at the given position to the provided view holder.
     *
     * @param holder the view holder to bind
     * @param position the position of the field in the adapter
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventField field = fields.get(position);
        holder.nameView.setText(field.getName());
        holder.valueView.setText(field.getValue());
    }

    /**
     * Returns the number of event detail rows managed by this adapter.
     *
     * @return the number of fields displayed by the adapter
     */
    @Override
    public int getItemCount() { return fields.size(); }

    /**
     * Returns the EventField at the specified position.
     *
     * @param position the position of the field
     * @return the EventField at the given position
     */
    public EventField getItem(int position) {
        return fields.get(position);
    }
}
