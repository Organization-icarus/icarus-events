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

/**
 * Adapter used to display a user's event history in a ListView.
 * <p>
 * Binds Event objects to the event history list item layout and displays
 * basic event information such as the event name, category, and formatted date.
 *
 * @author Benjamin Hall
 */
public class EventHistoryListArrayAdapter extends ArrayAdapter<Event> {
    private ArrayList<Event> events;
    private Context context;

    /**
     * Constructs an adapter for displaying Event objects in the event history list.
     *
     * @param context the context used to inflate views and access resources
     * @param events the list of events to be displayed by the adapter
     */
    public EventHistoryListArrayAdapter(Context context, ArrayList<Event> events) {
        super(context, 0, events);
        this.events = events;
        this.context = context;
    }

    /**
     * Returns a view for displaying an event in the event history list.
     * Inflates the list item layout if necessary and binds the event's
     * name, category, and formatted date to the corresponding UI elements.
     *
     * @param position the position of the event in the adapter's data set
     * @param convertView a recycled view to reuse if available
     * @param parent the parent view that this view will be attached to
     * @return the view representing the event at the specified position
     */
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
        if (event.getStartDate() != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            String dateString = formatter.format(event.getStartDate());
            eventDate.setText(dateString);
        } else {
            eventDate.setText(R.string.entrant_event_list_missing_date);
        }

        return view;
    }
}
