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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
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
    //---------------------------
    // BUTTONS
    //---------------------------

    private Button
            organizerBtn,
            manageBtn,
            notificationBtn,
            deleteBtn,
            joinWaitingBtn,
            leaveWaitingBtn,
            declineInviteBtn,
            registerBtn,
            cancelRegistrationBtn;

    //---------------------------
    // USER DETAILS
    //---------------------------

    private Boolean isAdmin, isOrganizer, locationEnabled;
    private String currentStatus;

    //---------------------------
    // EVENT DETAILS
    //---------------------------

    private ImageView posterView;
    private int currentWaitingCount;
    private ArrayList<String> eventOrganizers;
    private double eventCapacity;
    private Date eventRegOpen, eventRegClose, eventDate;
    private String
            eventName,
            eventCategory,
            eventLocation,
            eventImage,
            eventDescription;

    //---------------------------
    // LISTENERS
    //---------------------------

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
        joinWaitingBtn = findViewById(R.id.join_waiting_list_button);
        leaveWaitingBtn = findViewById(R.id.leave_waiting_list_button);
        declineInviteBtn = findViewById(R.id.decline_invitation);
        registerBtn = findViewById(R.id.register_button);
        cancelRegistrationBtn = findViewById(R.id.cancel_registration);

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
        joinWaitingBtn.setOnClickListener(v -> {
            // TODO: should capacity not be 0, -1, or NULL?
            if (eventCapacity > 0 && currentWaitingCount >= eventCapacity) {
                // No more users can enter
                Toast.makeText(this, "This event is full", Toast.LENGTH_SHORT).show();
                return;
            }
            if (locationEnabled) {
                // Event requires geolocation, check permission and add user if they allow it.
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
            } else {
                // Add user to waiting list with no geopoint
                addUserToWaitingList(finalEventId, userId, null);
            }
        });


        // Lets waiting users leave the waiting list
        leaveWaitingBtn.setOnClickListener(v -> {
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


        // Lets users decline an invitation to register
        declineInviteBtn.setOnClickListener(v -> {
            Map<String, Object> entrant = new HashMap<>();
            entrant.put("status", "rejected");
            db.collection(FirestoreCollections.EVENTS_COLLECTION).document(finalEventId)
                    .collection("entrants").document(userId)
                    .set(entrant);

            currentStatus = "rejected";

            setupButtons(isAdmin, isOrganizer, currentStatus);
            refreshAdapter(finalEventId);
        });


        // Lets users register for an event they've been sampled for
        registerBtn.setOnClickListener(v -> {
            // First make sure that the user is in the registration window
            Date now = new Date();

            // For testing:
            /* Calendar cal = Calendar.getInstance();
            cal.set(2026, Calendar.APRIL, 1, 14, 30, 0); // Year, Month, Day, Hour, Min, Sec
            Date now = cal.getTime(); */

            if (eventRegOpen != null && now.before(eventRegOpen)) {
                Toast.makeText(
                        v.getContext(),
                        "Registration has not opened yet.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (eventRegClose != null && now.after(eventRegClose)) {
                Toast.makeText(
                        v.getContext(),
                        "Registration is closed.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            currentStatus = "registered";
            setupButtons(isAdmin, isOrganizer, currentStatus);
            refreshAdapter(finalEventId);

            // Update the event's entrant document
            Map<String, Object> entrant = new HashMap<>();
            entrant.put("status", currentStatus);
            db.collection(FirestoreCollections.EVENTS_COLLECTION)
                    .document(finalEventId)
                    .collection("entrants")
                    .document(userId)
                    .set(entrant);
        });


        // Lets users cancel their registration
        cancelRegistrationBtn.setOnClickListener(v-> {
            Map<String, Object> entrant = new HashMap<>();
            entrant.put("status", "rejected");
            db.collection(FirestoreCollections.EVENTS_COLLECTION).document(finalEventId)
                    .collection("entrants").document(userId)
                    .set(entrant);

            currentStatus = "rejected";
            setupButtons(isAdmin, isOrganizer, currentStatus);
            refreshAdapter(finalEventId);
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
            Intent intent = new Intent(
                    EventDetailsActivity.this,
                    EventNotificationsActivity.class
            );
            intent.putExtra("eventId", finalEventId);
            startActivity(intent);
        });

        // TODO: adjust intent destination
        deleteBtn.setOnClickListener(v -> {
            /* delete logic */
            // Code generated by Claude AI March 11, 2026
            // "How to remove event ID from a users list of events for each user document in the
            // events 'entrants' subcollection."
            CollectionReference eventEntrantsRef = db.collection(FirestoreCollections.EVENTS_COLLECTION)
                    .document(finalEventId)
                    .collection("entrants");
            eventEntrantsRef.get().addOnSuccessListener(userSnapshots -> {
                WriteBatch batch = db.batch();

                // remove event from each user's events array
                for (DocumentSnapshot userDoc : userSnapshots.getDocuments()) {
                    String entrant = userDoc.getId();
                    DocumentReference userRef = db.collection(FirestoreCollections.USERS_COLLECTION).document(entrant);
                    batch.update(userRef, "events", FieldValue.arrayRemove(finalEventId));
                    // Delete the entrant document from the 'entrants' subcollection
                    batch.delete(userDoc.getReference());
                }

                // remove the events poster from the database
                db.collection(FirestoreCollections.IMAGES_COLLECTION)
                        .whereEqualTo("URL", eventImage)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                Image poster = new Image(eventImage, doc.getId());
                                poster.delete(this, db);
                            }
                        });

                // remove event document
                DocumentReference eventRef = db.collection(FirestoreCollections.EVENTS_COLLECTION).document(finalEventId);
                batch.delete(eventRef);

                // commit the batch
                batch.commit()
                        .addOnSuccessListener(aVoid -> Log.d("Firestore", "Event deleted successfully"))
                        .addOnFailureListener(e -> Log.e("Firestore", "Error deleting event", e));
            }).addOnFailureListener(e -> Log.e("Firestore", "Error fetching event users", e));
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
        //---------------------------

        // Will run anytime the database event document changes
        eventListener = db.collection(FirestoreCollections.EVENTS_COLLECTION).document(finalEventId)
                .addSnapshotListener((doc, e) -> {
                    if (doc != null && doc.exists()) {

                        //---------------------------
                        // READ THE FIREBASE VALUES
                        //---------------------------

                        eventName = doc.getString("name");
                        eventCategory = doc.getString("category");
                        eventDescription = doc.getString("description");
                        Double capacityValue = doc.getDouble("capacity");
                        eventCapacity = capacityValue != null ? capacityValue : -1;
                        eventRegOpen = doc.getDate("open");
                        eventRegClose = doc.getDate("close");
                        eventDate = doc.getDate("date");
                        locationEnabled = doc.getBoolean("geolocation");
                        eventLocation = doc.getString("location");
                        eventImage = doc.getString("image");
                        eventOrganizers = (ArrayList<String>) doc.get("organizers");
                        isOrganizer = eventOrganizers.contains(userId);
                        setupButtons(isAdmin, isOrganizer, currentStatus);

                        //---------------------------
                        // SET TEXT FIELDS
                        //---------------------------

                        TextView eventNameView = findViewById(R.id.eventName);
                        eventNameView.setText(this.eventName);

                        TextView descriptionView = findViewById(R.id.eventDescription);
                        descriptionView.setText(this.eventDescription);

                        refreshAdapter(finalEventId);
                    }
                });


        //---------------------------
        // COUNT THE EVENT'S WAITING LIST
        //---------------------------

        entrantWaitlistListener = db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(finalEventId)
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

        // Hide all first
        organizerBtn.setVisibility(View.GONE);
        manageBtn.setVisibility(View.GONE);
        notificationBtn.setVisibility(View.GONE);
        deleteBtn.setVisibility(View.GONE);
        joinWaitingBtn.setVisibility(View.GONE);
        leaveWaitingBtn.setVisibility(View.GONE);
        declineInviteBtn.setVisibility(View.GONE);
        registerBtn.setVisibility(View.GONE);
        cancelRegistrationBtn.setVisibility(View.GONE);

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
                joinWaitingBtn.setVisibility(View.VISIBLE);
            } else if (status.equals("waiting")) {
                leaveWaitingBtn.setVisibility(View.VISIBLE);
            } else if (status.equals("selected")) {
                registerBtn.setVisibility(View.VISIBLE);
                declineInviteBtn.setVisibility(View.VISIBLE);
            }

            else if (status.equals("registered")) {
                cancelRegistrationBtn.setVisibility(View.VISIBLE);
            }
        }
    }


    private void refreshAdapter(String finalEventId) {
        if (eventName == null) return;

        Event event = new Event(
                finalEventId, eventName, eventCategory, eventCapacity,
                eventRegOpen, eventRegClose, eventDate, eventLocation,
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
        if (geopoint != null) entrant.put("location", geopoint);
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
