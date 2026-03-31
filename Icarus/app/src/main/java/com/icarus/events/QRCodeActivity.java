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

import java.util.Collections;

// Portions of this code were developed and adapted with assistance from ChatGPT to learn what libraries and
// functions were needed/helping with debugging on March 29, 2026.
// Javadoc style function headers also generated with chatGPT

/**
 * Activity responsible for scanning QR codes and navigating to the corresponding event.
 *
 * This activity initializes a camera preview using ZXing's {@link BarcodeView}, scans for QR codes,
 * and extracts the event ID encoded in the QR code. Upon a successful scan, the user is redirected
 * to {@link EventDetailsActivity} with the scanned event ID.
 *
 * Camera permissions are handled at runtime. The scanner lifecycle is carefully managed to ensure
 * resources are released properly and to prevent performance issues.
 *
 * Portions of this implementation were developed with assistance from ChatGPT (March 29, 2026).
 */
public class QRCodeActivity extends NavigationBarActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private BarcodeView barcodeScanner;
    private boolean hasScanned = false;
    private boolean scannerInitialized = false;
    private boolean scannerRunning = false;

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
     *
     * Initializes the scanner if necessary and begins a single scan. When a QR code is detected,
     * the event ID is extracted and the user is navigated to the event details screen.
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

                String scannedEventId = result.getText();
                Intent intent = new Intent(QRCodeActivity.this, EventDetailsActivity.class);
                intent.putExtra("eventId", scannedEventId);
                startActivity(intent);
                finish();
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
        stopScanner(); // Testing this to see if it fixes lag issue
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
