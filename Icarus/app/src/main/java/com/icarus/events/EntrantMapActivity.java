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

public class EntrantMapActivity extends NavigationBarActivity {
    private MapView entrantMap;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_entrant_map);
        setupNavBar();

        // Get the eventId
        String eventId = getIntent().getStringExtra("eventId");

        // Get the map view and set properties
        entrantMap = findViewById(R.id.entrant_map);
        entrantMap.setTileSource(TileSourceFactory.MAPNIK);
        entrantMap.setMultiTouchControls(true);

        // TODO: Centre map on event location??? (centres on edmonton for now)
        GeoPoint startPoint = new GeoPoint(53.5461, -113.4938); // example: Edmonton
        entrantMap.getController().setZoom(12.0);   // adjust zoom level (higher = closer)
        entrantMap.getController().setCenter(startPoint);

        // Get all entrant locations and add them to the map
        db = FirebaseFirestore.getInstance();
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
}
