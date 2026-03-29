package com.icarus.events;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class QRCodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);

        // 1. Request camera permission if needed.
        // 2. Start the QR scanner when this activity opens.
        // 3. Read the scanned QR value as the event ID.
        // 4. Open the matching event details screen.
    }
}
