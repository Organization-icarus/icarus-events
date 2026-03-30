package com.icarus.events;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
/**
 * Entrant Array Adapter. Shows the User's name, email, and phone number
 * <p>
 *
 * @author Ben Salmon
 */
public class OraganizerEntrantViewListArrayAdapter extends ArrayAdapter<User> {
    private ArrayList<User> entrants;
    private Context context;
    private Set<String> selectedIds = new HashSet<>();
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
        CheckBox listCheckBox = view.findViewById(R.id.OrganizerEntrantListCheckBox);

        entrantName.setText(entrant.getName());
        entrantEmail.setText(entrant.getEmail() != null ? "Email: " + entrant.getEmail() : "Email: Not Given");
        entrantPhone.setText(entrant.getPhone() != null ? "Phone: " + entrant.getPhone() : "Phone: Not Given");

        listCheckBox.setClickable(false);
        listCheckBox.setFocusable(false);

// Sync checkbox state
        listCheckBox.setOnCheckedChangeListener(null);
        listCheckBox.setChecked(selectedIds.contains(entrant.getId()));

// Clicking anywhere on the row toggles selection
        view.setOnClickListener(v -> {
            String id = entrant.getId();
            if (selectedIds.contains(id)) {
                selectedIds.remove(id);
                listCheckBox.setChecked(false);
            } else {
                selectedIds.add(id);
                listCheckBox.setChecked(true);
            }
        });
        return view;
    }

    public Set<String> getSelectedIds() { return selectedIds; }
    public void clearSelections() {
        selectedIds.clear();
        notifyDataSetChanged();
    }
    public void selectAll() {
        for (User user : entrants) {
            selectedIds.add(user.getId());
        }
        notifyDataSetChanged();
    }
}
