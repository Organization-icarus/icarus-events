package com.icarus.events;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class AdministratorDashboardUserArrayAdapter extends ArrayAdapter<User> {
    private ArrayList<User> users;
    private Context context;
    private FirebaseFirestore db;

    public AdministratorDashboardUserArrayAdapter(Context context, ArrayList<User> users){
        super(context, 0, users);
        this.users = users;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
        View view = convertView;
        if (view == null){
            view = LayoutInflater.from(context)
                    .inflate(R.layout.administrator_dashboard_user_list_content, parent, false);
        }

        User user = users.get(position);
        TextView userName = view
                .findViewById(R.id.admin_dashboard_user_list_user_name);
        Button userDetailsButton = view
                .findViewById(R.id.admin_dashboard_user_list_user_details_button);
        Button removeUserButton = view
                .findViewById(R.id.admin_dashboard_user_list_remove_user_button);

        userName.setText(user.getName());

        userDetailsButton.setOnClickListener(v -> {
            // TODO: Have this navigate to the user details activity for the user
        });

        removeUserButton.setOnClickListener(v -> {
            DocumentReference docRef = db.collection("users")
                    .document(user.getId());
            docRef.delete();
        });

        return view;
    }
}
