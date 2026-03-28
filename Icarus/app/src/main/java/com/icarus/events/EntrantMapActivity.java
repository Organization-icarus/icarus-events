package com.icarus.events;

import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

public class EntrantMapActivity extends NavigationBarActivity {
    private MapView entrantMap;
    private com.google.firebase.firestore.GeoPoint eventLocation;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_entrant_map);
        setupNavBar();
        db = FirebaseFirestore.getInstance();

        // Get the map view and set properties
        entrantMap = findViewById(R.id.entrant_map);
        entrantMap.setTileSource(TileSourceFactory.MAPNIK);
        entrantMap.setMultiTouchControls(true);

        // Get the eventId and get the event coordinates
        String eventId = getIntent().getStringExtra("eventId");
        db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    eventLocation = snapshot.getGeoPoint("coordinates");
                    Double entrantRange = snapshot.getDouble("entrantRange");
                    // Centre map on event location and draw a circle to show entrant restriction radius
                    GeoPoint startPoint = new GeoPoint(eventLocation.getLatitude(), eventLocation.getLongitude());
                    drawCircle(eventLocation.getLatitude(), eventLocation.getLongitude(), entrantRange * 1000);
                    addMarker(eventLocation.getLatitude(), eventLocation.getLongitude());
                    entrantMap.getController().setZoom(12.0);
                    entrantMap.getController().setCenter(startPoint);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EntrantMapActivity.this,
                            "Failed to load event coordinates", Toast.LENGTH_SHORT).show();
                });

        // Get all entrant locations and add them to the map
        db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId)
                .collection("entrants")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                   for (DocumentSnapshot entrant : querySnapshot.getDocuments()) {
                       // Get entrants location
                       com.google.firebase.firestore.GeoPoint location = entrant.getGeoPoint("location");

                       // Add marker
                       if (location != null) {
                           double lat = location.getLatitude();
                           double lon = location.getLongitude();
                           addMarker(lat, lon);
                       }
                   }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EntrantMapActivity.this,
                            "Failed to load entrant locations", Toast.LENGTH_SHORT).show();
                });
    }

    private void addMarker(double lat, double lon) {
        Marker marker = new Marker(entrantMap);
        marker.setPosition(new GeoPoint(lat, lon));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        entrantMap.getOverlays().add(marker);
    }

    @Override
    protected void onResume() {
        super.onResume();
        entrantMap.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        entrantMap.onPause();
    }

    // Created by Claude AI, March 28, 2026
    // "How to draw a circle around a point on the map using the osmdroid library in java"
    private void drawCircle(double lat, double lon, double radiusMeters) {
        Polygon circle = new Polygon();
        circle.setPoints(Polygon.pointsAsCircle(new GeoPoint(lat, lon), radiusMeters));

        // Style the circle
        circle.getFillPaint().setColor(0x300078FF);   // semi-transparent blue fill (ARGB)
        circle.getOutlinePaint().setColor(0xFF0078FF); // solid blue border
        circle.getOutlinePaint().setStrokeWidth(3f);

        entrantMap.getOverlays().add(circle);
    }
}
