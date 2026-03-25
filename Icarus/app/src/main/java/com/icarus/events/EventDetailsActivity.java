package com.icarus.events;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * Activity that displays the details of a single Event.
 *
 * Fetches the Event from Firestore using the eventId passed via Intent,
 * then populates a ListView using EventDetailsAdapter to show all fields
 * of the Event in a readable format.
 *
 * @author Bradley Bravender
 */
public class EventDetailsActivity extends NavigationBarActivity {
    // Initialize all the admin, organizer, and entrant buttons
    private Button organizerBtn, manageBtn, notificationBtn, deleteBtn;
    private Button joinBtn, leaveBtn, declineBtn, registerBtn;

    private ImageView posterView;
    private Boolean isAdmin, isOrganizer;
    private String currentStatus;
    private int currentWaitingCount;

    // Initialize fields to store the event's information
    private String eventName, eventCategory, eventLocation, eventImage;
    private ArrayList<String> eventOrganizers;
    private double eventCapacity;
    private Date EventRegOpen, eventRegClose, eventDate;

    // To prevent the firebase snapshot listener from creating memory leaks
    private ListenerRegistration eventListener;
    private ListenerRegistration userListener;
    private ListenerRegistration entrantStatusListener;
    private ListenerRegistration entrantWaitlistListener;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    // Runs every time a user navigates to this intent
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);
        setupNavBar();
        eventOrganizers = new ArrayList<String>();
        // Retrieve data passed to the intent
        String eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) eventId = "hL8pW5lK9gDloqcWlmqx"; // For testing
        String finalEventId = eventId;

        // Get the current user's role and status
        User user = UserSession.getInstance().getCurrentUser();
        isAdmin = user.getIsAdmin();
        String userId = user.getId();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get location services client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //---------------------------
        // GET ALL BUTTONS
        //---------------------------

        organizerBtn = findViewById(R.id.event_organizer_button);
        manageBtn = findViewById(R.id.manage_button);
        notificationBtn = findViewById(R.id.notification_button);
        deleteBtn = findViewById(R.id.delete_button);
        joinBtn = findViewById(R.id.join_waiting_list_button);
        leaveBtn = findViewById(R.id.leave_waiting_list_button);
        declineBtn = findViewById(R.id.decline_button);
        registerBtn = findViewById(R.id.register_button);

        //---------------------------
        // SET UP POSTER
        //---------------------------
        posterView = findViewById(R.id.eventPoster);
        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(finalEventId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String imageURL = snapshot.getString("image");
                        if (imageURL != null && !imageURL.isEmpty()) {
                            Picasso.get()
                                    .load(snapshot.getString("image"))
                                    .error(R.drawable.poster)           // Optional: shows if link fails
                                    .into(posterView);
                        } else {
                            posterView.setImageResource(R.drawable.poster);
                        }
                    }
                })
                .addOnFailureListener( e -> {
                    Toast.makeText(this, "Failed to load poster", Toast.LENGTH_SHORT).show();
                });

        //---------------------------
        // SET CLICK LISTENERS
        //---------------------------

        // Lets uninitialized users join the waiting list
        joinBtn.setOnClickListener(v -> {
            // TODO: should capacity not be 0, -1, or NULL?
            if (eventCapacity > 0 && currentWaitingCount >= eventCapacity) {
                // No more users can enter
                Toast.makeText(this, "This event is full", Toast.LENGTH_SHORT).show();
                return;
            }

            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                // Permission already granted
                try {
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    if (location != null) {
                                        double latitude = location.getLatitude();
                                        double longitude = location.getLongitude();
                                        GeoPoint geopoint = new GeoPoint(latitude, longitude);
                                        addUserToWaitingList(finalEventId, userId, geopoint);
                                    } else {
                                        Toast.makeText(EventDetailsActivity.this,
                                                "Error getting location, try again later",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                } catch (SecurityException e) {
                    // Handle exception: Log, notify user, or request permission again
                    Toast.makeText(this,
                            "Error getting location, try again later",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                // Request permission
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST
                );
            }
        });


        // Lets waiting users leave the waiting list
        leaveBtn.setOnClickListener(v -> {
            currentStatus = null;
            setupButtons(isAdmin, isOrganizer, currentStatus);
            refreshAdapter(finalEventId);

            // Delete the entrant from the event document
            db.collection(FirestoreCollections.EVENTS_COLLECTION).document(finalEventId)
                    .collection("entrants").document(userId)
                    .delete();

            // Remove event from user's own event collection
            db.collection(FirestoreCollections.USERS_COLLECTION).document(userId)
                    .update("events", com.google.firebase.firestore.FieldValue.arrayRemove(finalEventId));
        });


        // Lets selected users reject their invitation, or already registered users leave an event.
        declineBtn.setOnClickListener(v -> {
            if (currentStatus.equals("registered")) {
                // Remove from event's entrant list
                Map<String, Object> entrant = new HashMap<>();
                entrant.put("status", "rejected");
                db.collection(FirestoreCollections.EVENTS_COLLECTION).document(finalEventId)
                        .collection("entrants").document(userId)
                        .set(entrant);

                currentStatus = "rejected";
            } else {
                // Selected → rejected
                Map<String, Object> entrant = new HashMap<>();
                entrant.put("status", "rejected");
                db.collection(FirestoreCollections.EVENTS_COLLECTION).document(finalEventId)
                        .collection("entrants").document(userId)
                        .set(entrant);

                currentStatus = "rejected";
            }
            setupButtons(isAdmin, isOrganizer, currentStatus);
            refreshAdapter(finalEventId);
        });


        registerBtn.setOnClickListener(v -> {
            currentStatus = "registered";
            setupButtons(isAdmin, isOrganizer, currentStatus);
            refreshAdapter(finalEventId);

            // Update the event's entrant document
            Map<String, Object> entrant = new HashMap<>();
            entrant.put("status", currentStatus);
            db.collection(FirestoreCollections.EVENTS_COLLECTION).document(finalEventId)
                    .collection("entrants").document(userId)
                    .set(entrant);
        });


        // TODO: adjust intent destination
        organizerBtn.setOnClickListener(v -> {
            /* organizer logic */
            // Navigates to the organizer profile
            Intent intent = new Intent(
                    EventDetailsActivity.this,
                    EntrantEventListActivity.class);
            startActivity(intent);
        });


        // TODO: adjust intent destination
        manageBtn.setOnClickListener(v -> {
            /* manage logic */
            // Navigates to the manage page
            Intent intent = new Intent(
                    EventDetailsActivity.this,
                    OrganizerManageEventActivity.class);
            intent.putExtra("eventId", finalEventId);
            startActivity(intent);
        });


        // TODO: adjust intent destination
        notificationBtn.setOnClickListener(v -> {
            /* notification logic */
            // Navigates to the events notification page
            Intent intent = new Intent(
                    EventDetailsActivity.this,
                    EntrantEventListActivity.class);
            startActivity(intent);
        });


        // TODO: adjust intent destination
        deleteBtn.setOnClickListener(v -> {
            /* delete logic */
            // Deletes the event
            Intent intent = new Intent(
                    EventDetailsActivity.this,
                    EntrantEventListActivity.class);
            startActivity(intent);
        });


        //---------------------------
        // SET UP GUIDELINES DIALOG
        //---------------------------

        // Set width to 300dp
        int widthPx = (int) (300 * getResources().getDisplayMetrics().density);
        TextView guidelinesButton = findViewById(R.id.lottery_guidelines);
        guidelinesButton.setOnClickListener(v -> {
            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(EventDetailsActivity.this)
                    .setTitle("Lottery Guidelines")
                    .setMessage(getString(R.string.lottery_guidelines_message))
                    .setPositiveButton("OK", (d, which) -> d.dismiss())
                    .setCancelable(true)
                    .create();

            dialog.show();
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
            dialog.getWindow().setLayout(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT);
        });

        //---------------------------
        // LISTEN TO EVENT DOCUMENT
        // Fires on load + anytime the event document changes
        //---------------------------

        // Will run anytime the database event document changes
        eventListener = db.collection(FirestoreCollections.EVENTS_COLLECTION).document(finalEventId)
                .addSnapshotListener((doc, e) -> {
                    if (doc != null && doc.exists()) {
                        eventName = doc.getString("name");
                        eventCategory = doc.getString("category");
                        Double capacityValue = doc.getDouble("capacity");
                        eventCapacity = capacityValue != null ? capacityValue : -1;
                        EventRegOpen = doc.getDate("open");
                        eventRegClose = doc.getDate("close");
                        eventDate = doc.getDate("date");
                        eventLocation = doc.getString("location");
                        eventImage = doc.getString("image");
                        eventOrganizers = (ArrayList<String>) doc.get("organizers");
                        isOrganizer = eventOrganizers.contains(userId);
                        setupButtons(isAdmin, isOrganizer, currentStatus);

                        TextView eventName = findViewById(R.id.eventName);
                        eventName.setText(this.eventName);

                        refreshAdapter(finalEventId);
                    }
                });


        //---------------------------
        // COUNT THE EVENT'S WAITING LIST
        // Fires on load + anytime the user's entrant document changes
        //---------------------------

        entrantWaitlistListener = db.collection(FirestoreCollections.EVENTS_COLLECTION).document(finalEventId)
                .collection("entrants")
                .whereEqualTo("status", "waiting")
                .addSnapshotListener((query, e) -> {
                    if (query != null) {
                        currentWaitingCount = query.size();
                        refreshAdapter(finalEventId);
                    }
                });


        //---------------------------
        // LISTEN TO USER'S ENTRANT STATUS FOR THIS EVENT
        // Fires on load + anytime the user's entrant document changes
        //---------------------------

        entrantStatusListener = db.collection(FirestoreCollections.EVENTS_COLLECTION).document(finalEventId)
                .collection("entrants").document(userId)
                .addSnapshotListener((doc, e) -> {
                    if (doc != null && doc.exists()) {
                        currentStatus = doc.getString("status"); // could be null if field missing
                    } else {
                        currentStatus = null; // no document = not in event
                    }
                    setupButtons(isAdmin, isOrganizer, currentStatus);
                    refreshAdapter(finalEventId);
                });

        isAdmin = user.getIsAdmin();
        Log.d("DEBUG", "isAdmin from session: " + isAdmin);

        //---------------------------
        // LISTEN TO USER DOCUMENT
        // Handles admin changes while app is running
        //---------------------------

        userListener = db.collection(FirestoreCollections.USERS_COLLECTION).document(userId)
                .addSnapshotListener((doc, e) -> {
                    if (doc != null && doc.exists()) {
                        isAdmin = Objects.requireNonNullElse(doc.getBoolean("isAdmin"), false);
                        Log.d("DEBUG", "isAdmin from firestore: " + isAdmin);
                        setupButtons(isAdmin, isOrganizer, currentStatus);
                    }
                });
    }


    private void setupButtons(Boolean isAdmin, Boolean isOrganizer, String status) {
        // Runs every time the firebase document changes

        Log.d("DEBUG", "setupButtons called — isAdmin: " + isAdmin + ", isOrganizer: " + isOrganizer);

        // Hide all first
        organizerBtn.setVisibility(View.GONE);
        manageBtn.setVisibility(View.GONE);
        notificationBtn.setVisibility(View.GONE);
        deleteBtn.setVisibility(View.GONE);
        joinBtn.setVisibility(View.GONE);
        leaveBtn.setVisibility(View.GONE);
        declineBtn.setVisibility(View.GONE);
        registerBtn.setVisibility(View.GONE);

        // Don't proceed until everything is ready
        if (isAdmin == null || isOrganizer == null) return;

        /* NOTE: Admins and Organizers are users by default. Admins can also be
        organizers. */

        // Shared: admin + organizer
        if (isAdmin || isOrganizer) {
            notificationBtn.setVisibility(View.VISIBLE);
            deleteBtn.setVisibility(View.VISIBLE);
        }

        if (isAdmin) {
            organizerBtn.setVisibility(View.VISIBLE);
        }

        if (isOrganizer) {
            manageBtn.setVisibility(View.VISIBLE);
        }

        // An event organizer cannot join their own event
        if (!isOrganizer) {

            // Allow new (i.e. not rejected) users to join the waiting list
            if (status == null || status.equals("uninitialized")) {
                joinBtn.setVisibility(View.VISIBLE);
            } else if (status.equals("waiting")) {
                leaveBtn.setVisibility(View.VISIBLE);
            } else if (status.equals("selected")) {
                declineBtn.setVisibility(View.VISIBLE);
                registerBtn.setVisibility(View.VISIBLE);
            }

            // TODO: don't let entrants register after the registration period
            else if (status.equals("registered")) {
                declineBtn.setVisibility(View.VISIBLE);
            }
        }
    }


    private void refreshAdapter(String finalEventId) {
        if (eventName == null) return;

        Event event = new Event(
                finalEventId, eventName, eventCategory, eventCapacity,
                EventRegOpen, eventRegClose, eventDate, eventLocation,
                eventImage, eventOrganizers, currentStatus, currentWaitingCount
        ); // unchanged

        RecyclerView recyclerView = findViewById(R.id.event_details_event_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // add this
        recyclerView.setAdapter(new EventDetailsAdapter(this, event));
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (eventListener != null) eventListener.remove();
        if (userListener != null) userListener.remove();
        if (entrantStatusListener != null) entrantStatusListener.remove();
        if (entrantWaitlistListener != null) entrantWaitlistListener.remove();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                User user = UserSession.getInstance().getCurrentUser();
                String userId = user.getId();
                String eventId = getIntent().getStringExtra("eventId");
                try {
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    if (location != null) {
                                        double latitude = location.getLatitude();
                                        double longitude = location.getLongitude();
                                        GeoPoint geopoint = new GeoPoint(latitude, longitude);
                                        addUserToWaitingList(eventId, userId, geopoint);
                                    } else {
                                        Toast.makeText(EventDetailsActivity.this,
                                                "Error getting location, try again later",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                } catch (SecurityException e) {
                    // Handle exception: Log, notify user, or request permission again
                    Toast.makeText(this,
                            "Error getting location, try again later",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                // Permission Denied
                Toast.makeText(this,
                        "Location permission is required to join the event",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void addUserToWaitingList(String eventId, String userId, GeoPoint geopoint) {
        currentStatus = "waiting";
        Map<String, Object> entrant = new HashMap<>();
        entrant.put("status", currentStatus);
        entrant.put("location", geopoint);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Add user ID to event with status: "waiting" and location
        db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId)
                .collection("entrants").document(userId)
                .set(entrant);

        // Add event to user's own event collection
        db.collection(FirestoreCollections.USERS_COLLECTION).document(userId)
                .update("events", com.google.firebase.firestore.FieldValue.arrayUnion(eventId));
    }
}
