package com.icarus.events;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.TextView;

public class SampleAttendeesActivity extends NavigationBarActivity {

    private int attendeeCount = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sample_attendees);
        setupNavBar();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView attendeeCountText = findViewById(R.id.attendeeCountText);
        Button minusButton = findViewById(R.id.minusButton);
        Button plusButton = findViewById(R.id.plusButton);
        Button cancelButton = findViewById(R.id.cancelButton);
        Button sampleButton = findViewById(R.id.sampleButton);

        attendeeCountText.setText(String.valueOf(attendeeCount));

        plusButton.setOnClickListener(v -> {
            attendeeCount++;
            attendeeCountText.setText(String.valueOf(attendeeCount));
        });
        TextView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        minusButton.setOnClickListener(v -> {
            if (attendeeCount > 1) {
                attendeeCount--;
                attendeeCountText.setText(String.valueOf(attendeeCount));
            }
        });

        cancelButton.setOnClickListener(v -> finish());

        sampleButton.setOnClickListener(v ->
                Toast.makeText(this, "Sampling " + attendeeCount + " attendees...", Toast.LENGTH_SHORT).show()
        );
    }
}