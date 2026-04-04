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
 * Entrant Array Adapter. Shows the user's name, email, and phone number
 * in a list and allows rows to be selected using a checkbox.
 *
 * @author Ben Salmon
 */
public class OraganizerEntrantViewListArrayAdapter extends ArrayAdapter<User> {
    private ArrayList<User> entrants;
    private Context context;
    private Set<String> selectedIds = new HashSet<>();

    /**
     * Constructs a new adapter for displaying entrant information in a list.
     *
     * @param context  the context used to inflate the list item layout
     * @param entrants the list of entrants to display
     */
    public OraganizerEntrantViewListArrayAdapter(Context context, ArrayList<User> entrants) {
        super(context, 0, entrants);
        this.entrants = entrants;
        this.context = context;
    }

    /**
     * Returns the view used to display an entrant at a given position in the list.
     * This method binds the entrant's name, email, and phone number to the row view,
     * restores the checkbox state based on current selections, and allows the user
     * to toggle selection by clicking anywhere on the row.
     *
     * @param position    the position of the entrant in the list
     * @param convertView the recycled view to reuse, if available
     * @param parent      the parent view group
     * @return the view representing the entrant at the given position
     */
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

    /**
     * Returns the set of selected entrant IDs.
     *
     * @return a set containing the IDs of all currently selected entrants
     */
    public Set<String> getSelectedIds() {
        return selectedIds;
    }

    /**
     * Clears all current selections and refreshes the list so the checkboxes
     * reflect the updated state.
     */
    public void clearSelections() {
        selectedIds.clear();
        notifyDataSetChanged();
    }

    /**
     * Selects all entrants currently contained in the adapter and refreshes
     * the list so all checkboxes appear checked.
     */
    public void selectAll() {
        for (User user : entrants) {
            selectedIds.add(user.getId());
        }
        notifyDataSetChanged();
    }
}