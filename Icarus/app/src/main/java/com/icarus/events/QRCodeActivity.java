package com.icarus.events;

import android.os.Bundle;
import android.content.Intent;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeView;

import android.widget.FrameLayout;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.Collections;

import com.google.firebase.firestore.FirebaseFirestore;

// Portions of this code were developed and adapted with assistance from ChatGPT generations to
// learn what libraries and functions were needed/helping with debugging on March 29, 2026.
// Javadoc style function headers also generated with chatGPT

/**
 * Activity responsible for scanning QR codes and navigating to the corresponding event.
 * <p>
 * Initializes a camera preview using ZXing's {@link BarcodeView}, scans for QR codes,
 * validates the scanned payload format, verifies the corresponding event in Firebase
 * Firestore, and redirects the user to {@link EventDetailsActivity} when a valid event
 * QR code is detected.
 * <p>
 * Camera permissions are handled at runtime. The scanner lifecycle is managed carefully
 * to release resources properly and help prevent performance issues.
 *
 * @author Alex Alves
 */
public class QRCodeActivity extends HeaderNavBarActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private BarcodeView barcodeScanner;
    private boolean hasScanned = false;
    private boolean scannerInitialized = false;
    private boolean scannerRunning = false;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Called when the activity is first created.
     *
     * Sets up the layout, initializes the navigation bar, and requests camera permission if needed.
     *
     * @param savedInstanceState Bundle containing previous state, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);
        setupNavBar();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Initializes the barcode scanner view and attaches it to the UI.
     *
     * Creates a {@link BarcodeView}, configures it to scan only QR codes, and inserts it into
     * the camera preview container. Ensures initialization happens only once.
     */
    private void initializeScanner() {
        if (scannerInitialized) {
            return;
        }

        FrameLayout cameraPreview = findViewById(R.id.camera_preview);
        barcodeScanner = new BarcodeView(this);
        barcodeScanner.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        barcodeScanner.setDecoderFactory(
                new com.journeyapps.barcodescanner.DefaultDecoderFactory(
                        Collections.singletonList(BarcodeFormat.QR_CODE))
        );

        cameraPreview.removeAllViews();
        cameraPreview.addView(barcodeScanner);
        scannerInitialized = true;
    }

    /**
     * Starts the QR code scanning process.
     * <p>
     * Initializes the scanner if necessary and begins a single scan. When a QR code is detected,
     * the scanned payload is trimmed, validated against the expected event ID format, and checked
     * against Firebase Firestore before the user is navigated to the event details screen.
     *
     * Prevents multiple scanner instances from running simultaneously.
     */
    private void startScanner() {
        if (scannerRunning) {
            return;
        }
        initializeScanner();
        barcodeScanner.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result == null || result.getText() == null || hasScanned) {
                    return;
                }
                hasScanned = true;
                stopScanner();

                String scannedEventId = result.getText() != null ? result.getText().trim() : "";

                if (scannedEventId.isEmpty()) {
                    hasScanned = false;
                    startScanner();
                    return;
                }

                // Reject malformed QR payloads before querying Firestore.
                // Firestore document IDs cannot safely be looked up from arbitrary URL-like strings.
                if (!scannedEventId.matches("^[A-Za-z0-9_-]+$")) {
                    Toast.makeText(QRCodeActivity.this, "Invalid event QR code", Toast.LENGTH_SHORT).show();
                    hasScanned = false;
                    startScanner();
                    return;
                }

                // Check scanned ID against Firestore to verify it is a real event before rerouting.
                db.collection(FirestoreCollections.EVENTS_COLLECTION)
                        .document(scannedEventId)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            if (snapshot.exists()) {
                                Intent intent = new Intent(QRCodeActivity.this, EventDetailsActivity.class);
                                intent.putExtra("eventId", scannedEventId);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(QRCodeActivity.this, "Invalid event QR code", Toast.LENGTH_SHORT).show();
                                hasScanned = false;
                                startScanner();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(QRCodeActivity.this, "Failed to validate event QR code", Toast.LENGTH_SHORT).show();
                            hasScanned = false;
                            startScanner();
                        });
            }
        });

        barcodeScanner.resume();
        scannerRunning = true;
    }

    /**
     * Stops and cleans up the barcode scanner.
     *
     * Halts decoding, pauses the camera preview, removes the scanner view from the layout,
     * and clears references to allow proper resource cleanup and avoid performance issues.
     */
    private void stopScanner() {
        if (barcodeScanner != null) {
            barcodeScanner.stopDecoding();
            barcodeScanner.pause();

            ViewGroup parent = (ViewGroup) barcodeScanner.getParent();
            if (parent != null) {
                parent.removeView(barcodeScanner);
            }

            barcodeScanner = null;
        }

        scannerInitialized = false;
        scannerRunning = false;
    }

    /**
     * Called when the activity becomes visible to the user.
     *
     * Starts the scanner if camera permission is granted and no scan has occurred yet.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED && !hasScanned) {
            startScanner();
        }
    }

    /**
     * Called when the activity is no longer in the foreground.
     *
     * Stops the scanner to release camera resources and prevent background processing.
     */
    @Override
    protected void onPause() {
        super.onPause();
        stopScanner();
    }

    /**
     * Called before the activity is destroyed.
     *
     * Ensures scanner resources are fully released.
     */
    @Override
    protected void onDestroy() {
        stopScanner();
        super.onDestroy();
    }

    /**
     * Handles the result of the camera permission request.
     *
     * If permission is granted, scanning will begin in {@link #onResume()}.
     * If denied, the activity is closed.
     *
     * @param requestCode  The request code passed during permission request.
     * @param permissions  The requested permissions.
     * @param grantResults The results for the corresponding permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Scanner will start in onResume once permission is granted
            } else {
                finish();
            }
        }
    }
}
