package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.provider.Settings;

import com.google.firebase.firestore.FirebaseFirestore;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView deviceIdText = findViewById(R.id.main_device_id_text);

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        deviceIdText.setText("Device ID: " + deviceId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(deviceId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        // if user exists, load that user into global session and go to event list
                        User user = new User(
                                snapshot.getId(),
                                snapshot.getString("name"),
                                snapshot.getString("email"),
                                snapshot.getString("phone"),
                                snapshot.getDate("birthday"),
                                snapshot.getString("role")
                        );
                        UserSession.getInstance().setCurrentUser(user);
                        startActivity(new Intent(this, EntrantEventListActivity.class));
                    } else {
                        // if user doesn't exist, go to sign up page
                        Intent intent = new Intent(this, UserRegistrationActivity.class);
                        intent.putExtra("deviceId", deviceId);
                        startActivity(intent);
                    }
                    finish();
                });

    }
}