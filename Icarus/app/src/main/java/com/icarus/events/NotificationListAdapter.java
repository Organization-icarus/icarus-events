package com.icarus.events;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class NotificationListAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final List<String> notifications;

    public NotificationListAdapter(Activity context, List<String> notifications) {
        super(context, R.layout.notification_list_item, notifications);
        this.context = context;
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = context.getLayoutInflater().inflate(R.layout.notification_list_item, parent, false);

        TextView messageText = view.findViewById(R.id.notification_message_text);
        messageText.setText(notifications.get(position));

        return view;
    }
}