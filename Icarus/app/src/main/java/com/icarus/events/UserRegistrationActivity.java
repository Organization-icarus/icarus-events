package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserRegistrationActivity extends AppCompatActivity {
    private EditText nameEditText;
    private RadioGroup roleRadioGroup;
    private Button registerButton;
    private FirebaseFirestore db;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_registration);

        // Initialize database reference and collection references
        db = FirebaseFirestore.getInstance();

        // Initialize buttons
        registerButton = findViewById(R.id.user_register_button);

        // Initialize text field
        nameEditText = findViewById(R.id.user_register_name_field);

        // Initialize radio group
        roleRadioGroup = findViewById(R.id.user_register_role_radio_group);

        // Retrieve device Id
        deviceId = getIntent().getStringExtra("deviceId");

        // Set buttons on click listeners
        registerButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();

            // Check if user entered name in text field
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if user selected role in radio buttons
            int selectedId= roleRadioGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedRole = findViewById(selectedId);
            String role = selectedRole.getText().toString().toLowerCase();

            // Send user data to database
            Map<String, Object> userData = new HashMap<>();
            userData.put("name", name);
            userData.put("role", role);

            db.collection("users").document(deviceId).set(userData)
                    .addOnSuccessListener(unused -> {
                        // Add information into global session and return user to event list
                        User user = new User(deviceId, name, null, null, null, role);
                        UserSession.getInstance().setCurrentUser(user);
                        startActivity(new Intent(this, EntrantEventListActivity.class));
                        finish();
                    });
        });

    }
}
