package com.icarus.events;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class OraganizerEntrantViewListArrayAdapter extends ArrayAdapter<User> {
    private ArrayList<User> entrants;
    private Context context;
    public OraganizerEntrantViewListArrayAdapter(Context context, ArrayList<User> entrants) {
        super(context, 0, entrants);
        this.entrants = entrants;
        this.context = context;
    }

    @NonNull
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
        View view = convertView;
        if (view == null){
            view = LayoutInflater.from(context).inflate(R.layout.organizer_view_entrant_content, parent, false);
        }

        User entrant = entrants.get(position);
        TextView entrantName = view.findViewById(R.id.OrganizerEntrantNameOnViewList);
        TextView entrantEmail = view.findViewById(R.id.OrganizerEntrantEmailOnViewList);
        TextView entrantPhone = view.findViewById(R.id.OrganizerEntrantPhoneOnViewList);

        entrantName.setText(entrant.getName());
        entrantEmail.setText(entrant.getEmail() != null ? "Email: " + entrant.getEmail() : "Email: Not Given");
        entrantPhone.setText(entrant.getPhone() != null ? "Phone: " + entrant.getPhone() : "Phone: Not Given");

        return view;
    }
}
