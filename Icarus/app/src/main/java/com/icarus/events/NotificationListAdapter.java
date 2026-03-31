package com.icarus.events;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying notification items.
 */
public class NotificationListAdapter extends ArrayAdapter<NotificationItem> {

    private final Activity context;
    private final ArrayList<NotificationItem> notifications;

    public NotificationListAdapter(Activity context, ArrayList<NotificationItem> notifications) {
        super(context, R.layout.notification_list_item, notifications);
        this.context = context;
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = context.getLayoutInflater().inflate(R.layout.notification_list_item, parent, false);

        TextView typeText = view.findViewById(R.id.notification_type_text);
        TextView messageText = view.findViewById(R.id.notification_message_text);

        NotificationItem item = notifications.get(position);

        String typeLabel;
        switch (item.getType()) {
            case "private_waitlist_invite":
                typeLabel = "Private Event Invite";
                break;
            case "co_organizer_invite":
                typeLabel = "Co-organizer Invite";
                break;
            case "not_selected":
                typeLabel = "Lottery Result";
                break;
            case "selected":
                typeLabel = "Lottery Result";
                break;
            default:
                typeLabel = "Notification";
                break;
        }

        typeText.setText(typeLabel);
        messageText.setText(item.getMessage());

        return view;
    }
}