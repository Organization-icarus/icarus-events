package com.icarus.events;

import android.Manifest;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.text.SimpleDateFormat;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
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
public class EventDetailsActivity extends HeaderNavBarActivity {

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
            printTicketBtn,
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
    private Date eventRegOpen, eventRegClose, eventStartDate, eventEndDate;
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
        setupHeaderBar("Details");
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
        printTicketBtn = findViewById(R.id.print_ticket_button);
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

        // Give user a shareable ticket for the event
        printTicketBtn.setOnClickListener(v -> printTicket());

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
            CollectionReference eventEntrantsRef = db.collection(FirestoreCollections.EVENTS_COLLECTION).document(finalEventId).collection("entrants");
            CollectionReference eventCommentsRef = db.collection(FirestoreCollections.EVENTS_COLLECTION).document(finalEventId).collection("comments");
            CollectionReference eventNotificationsRef = db.collection(FirestoreCollections.EVENTS_COLLECTION).document(finalEventId).collection("notifications");

            com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> entrantsTask = eventEntrantsRef.get();
            com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> commentsTask = eventCommentsRef.get();
            com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> notificationsTask = eventNotificationsRef.get();

            com.google.android.gms.tasks.Tasks.whenAllSuccess(entrantsTask, commentsTask, notificationsTask)
                    .addOnSuccessListener(results -> {
                        WriteBatch batch = db.batch();

                        com.google.firebase.firestore.QuerySnapshot userSnapshots = (com.google.firebase.firestore.QuerySnapshot) results.get(0);
                        com.google.firebase.firestore.QuerySnapshot commentSnapshots = (com.google.firebase.firestore.QuerySnapshot) results.get(1);
                        com.google.firebase.firestore.QuerySnapshot notificationSnapshots = (com.google.firebase.firestore.QuerySnapshot) results.get(2);

                        // Remove event from each user's events array and delete entrant docs
                        for (DocumentSnapshot userDoc : userSnapshots.getDocuments()) {
                            DocumentReference userRef = db.collection(FirestoreCollections.USERS_COLLECTION).document(userDoc.getId());
                            batch.update(userRef, "events", FieldValue.arrayRemove(finalEventId));
                            batch.delete(userDoc.getReference());
                        }

                        // Delete all comment documents
                        for (DocumentSnapshot commentDoc : commentSnapshots.getDocuments()) {
                            batch.delete(commentDoc.getReference());
                        }

                        // Delete all notification documents
                        for (DocumentSnapshot notificationDoc : notificationSnapshots.getDocuments()) {
                            batch.delete(notificationDoc.getReference());
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

                        // Delete event document and commit
                        batch.delete(db.collection(FirestoreCollections.EVENTS_COLLECTION).document(finalEventId));

                        batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("Firestore", "Event deleted successfully");
                                    finish();
                                })
                                .addOnFailureListener(e -> Log.e("Firestore", "Error deleting event", e));

                    }).addOnFailureListener(e -> Log.e("Firestore", "Error fetching subcollections", e));
        });

        //---------------------------
        // COMMENTS
        //---------------------------
        ImageButton commentsButton = findViewById(R.id.comments_button);

        commentsButton.setOnClickListener(v -> {
            Intent intent = new Intent(EventDetailsActivity.this, EventCommentActivity.class);
            intent.putExtra("EVENT_ID", finalEventId);
            startActivity(intent);
        });

        //---------------------------
        // LOTTERY GUIDELINES DIALOG
        //---------------------------
        ImageButton guidelinesButton = findViewById(R.id.lottery_guidelines);

        guidelinesButton.setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.lottery_guidelines_dialog, null);

            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(EventDetailsActivity.this)
                    .setView(dialogView)
                    .create();

            dialog.show();

            Button okButton = dialogView.findViewById(R.id.dialog_ok_button);
            okButton.setOnClickListener(btn -> dialog.dismiss());
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
                        eventStartDate = doc.getDate("startDate");
                        eventEndDate = doc.getDate("endDate");
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
        printTicketBtn.setVisibility(View.GONE);
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
                printTicketBtn.setVisibility(View.VISIBLE);
                cancelRegistrationBtn.setVisibility(View.VISIBLE);

            }
        }
    }


    private void refreshAdapter(String finalEventId) {
        if (eventName == null) return;

        Event event = new Event(
                finalEventId, eventName, eventCategory, eventCapacity,
                eventRegOpen, eventRegClose, eventStartDate, eventEndDate, eventLocation,
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

    /**
     * Fires when the user allows location permissions for the app.
     * Adds the user to the events 'entrants' sub-collection and stores their location.
     *
     * @param requestCode The request code passed in {@link #requestPermissions}.
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions which is either
     *                     {@link android.content.pm.PackageManager#PERMISSION_GRANTED} or
     *                     {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     */
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

    /**
     * Adds user to the events 'entrants' subcollection with the status "waiting".
     * If geolocation is enabled, it adds the location of where the user joined the waitlist from.
     *
     * @param eventId   Firestore document ID of the event
     * @param userId    Firestore document ID of the user
     * @param geopoint  Location the user is joining the event from
     */
    public void addUserToWaitingList(String eventId, String userId, GeoPoint geopoint) {
        currentStatus = "waiting";
        Map<String, Object> entrant = new HashMap<>();
        entrant.put("status", currentStatus);
        if (geopoint != null) entrant.put("location", geopoint);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (locationEnabled && geopoint != null) {
            // Check if entrant is within valid range
            db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId).get()
                    .addOnSuccessListener(snapshot -> {
                        GeoPoint eventLocation = snapshot.getGeoPoint("coordinates");
                        Double radius = snapshot.getDouble("entrantRange");
                        if (radius == null) {
                            radius = 0.0; // Set to 0 if entrant range is null
                        } else {
                            radius = radius * 1000; // In meters
                        }
                        float[] results = new float[1];
                        Location.distanceBetween(geopoint.getLatitude(),
                                geopoint.getLongitude(),
                                eventLocation.getLatitude(),
                                eventLocation.getLongitude(), results);
                        if (results[0] <= radius) {
                            // WITHIN RANGE
                            // Add user ID to event with status: "waiting" and location
                            db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId)
                                    .collection("entrants").document(userId)
                                    .set(entrant);

                            // Add event to user's own event collection
                            db.collection(FirestoreCollections.USERS_COLLECTION).document(userId)
                                    .update("events", com.google.firebase.firestore.FieldValue.arrayUnion(eventId));
                        } else {
                            // OUT OF RANGE
                            Toast.makeText(this,
                                    "Sorry, you are too far from the event to join.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "Error getting event coordinates, try again later",
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Add user ID to event with status: "waiting" and location
            db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId)
                    .collection("entrants").document(userId)
                    .set(entrant);

            // Add event to user's own event collection
            db.collection(FirestoreCollections.USERS_COLLECTION).document(userId)
                    .update("events", com.google.firebase.firestore.FieldValue.arrayUnion(eventId));
        }
    }
    // Created  and adapted with generation help fromChatGPT April 4th 2026,
    // "Create or reuse a Firestore confirmation ticket using auto-ID and export a PDF ticket with QR code."
    /**
     * Creates or reuses a Firestore confirmation ticket, then exports the PDF ticket.
     *
     * Uses the Firestore document auto-ID as the confirmation ticket ID and encodes
     * that ID into the ticket QR code.
     */
    private void printTicket() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            Toast.makeText(this, "User session not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getId();
        String eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("confirmation-tickets")
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("user_id", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        String confirmationTicketId = querySnapshot.getDocuments().get(0).getId();
                        exportConfirmationTicketPdf(confirmationTicketId, currentUser);
                    } else {
                        Map<String, Object> ticketData = new HashMap<>();
                        ticketData.put("event_id", eventId);
                        ticketData.put("user_id", userId);

                        db.collection("confirmation-tickets")
                                .add(ticketData)
                                .addOnSuccessListener(documentReference ->
                                        exportConfirmationTicketPdf(documentReference.getId(), currentUser))
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to create ticket", Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load ticket", Toast.LENGTH_SHORT).show());
    }

    // Created  and adapted with generation help fromChatGPT April 4th 2026,
    // "Generate a styled ticket-like PDF layout with sections and embedded QR code."
    /**
     * Exports the styled PDF confirmation ticket for an existing ticket ID.
     *
     * @param confirmationTicketId Firestore auto-ID for the confirmation ticket.
     * @param currentUser The currently signed-in user.
     */
    private void exportConfirmationTicketPdf(String confirmationTicketId, User currentUser) {
        String entrantName = currentUser.getName() != null
                ? currentUser.getName()
                : "Entrant";
        String entrantEmail = currentUser.getEmail() != null
                ? currentUser.getEmail()
                : "N/A";

        SimpleDateFormat fullDateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

        String formattedDate = eventStartDate != null ? fullDateFormat.format(eventStartDate) : "N/A";
        String formattedTime = eventStartDate != null ? timeFormat.format(eventStartDate) : "N/A";
        String formattedVenue = eventLocation != null ? eventLocation : "N/A";

        String safeEventName = (eventName == null || eventName.trim().isEmpty())
                ? "confirmation_ticket"
                : eventName.replaceAll("[^a-zA-Z0-9\\-_ ]", "").trim();
        if (safeEventName.isEmpty()) {
            safeEventName = "confirmation_ticket";
        }
        String filename = safeEventName + "_confirmation_ticket.pdf";

        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(850, 1100, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(ContextCompat.getColor(this, R.color.primary));

        Paint leftPanelPaint = new Paint();
        leftPanelPaint.setColor(ContextCompat.getColor(this, R.color.secondary));

        Paint headingPaint = new Paint();
        headingPaint.setColor(ContextCompat.getColor(this, R.color.lightText));
        headingPaint.setTextSize(56f);
        headingPaint.setFakeBoldText(true);

        Paint sectionLabelPaint = new Paint();
        sectionLabelPaint.setColor(ContextCompat.getColor(this, R.color.accent_first));
        sectionLabelPaint.setTextSize(26f);
        sectionLabelPaint.setFakeBoldText(true);

        Paint fieldValuePaint = new Paint();
        fieldValuePaint.setColor(ContextCompat.getColor(this, R.color.lightText));
        fieldValuePaint.setTextSize(26f);

        Paint smallPaint = new Paint();
        smallPaint.setColor(ContextCompat.getColor(this, R.color.lightText));
        smallPaint.setTextSize(20f);

        Paint verticalTextPaint = new Paint();
        verticalTextPaint.setColor(ContextCompat.getColor(this, R.color.lightText));
        verticalTextPaint.setTextSize(24f);

        canvas.drawRect(0, 0, 850, 1100, backgroundPaint);
        canvas.drawRect(0, 0, 100, 1100, leftPanelPaint);

        canvas.save();
        canvas.rotate(-90, 50, 930);
        canvas.drawText("Icarus Event", 50, 930, verticalTextPaint);
        canvas.restore();

        canvas.save();
        canvas.rotate(-90, 50, 430);
        canvas.drawText("Ticket Holder", 50, 430, verticalTextPaint);
        canvas.restore();

        // Header title
        drawWrappedText(canvas,
                eventName != null ? eventName.toUpperCase(Locale.getDefault()) : "EVENT NAME",
                150, 120, 620, headingPaint, 70);

        // (Date chip removed)

        // Standardized field layout
        float leftColumnX = 150f;
        float rightColumnX = 520f;
        float firstRowY = 300f;
        float secondRowY = 450f;
        float thirdRowY = 600f;
        float labelToValueOffset = 36f;
        float fieldWidth = 260f;
        float rowLineHeight = 32f;

        // Left column
        canvas.drawText("Date:", leftColumnX, firstRowY, sectionLabelPaint);
        drawWrappedText(canvas, formattedDate, leftColumnX, firstRowY + labelToValueOffset, fieldWidth, fieldValuePaint, rowLineHeight);

        canvas.drawText("Time:", leftColumnX, secondRowY, sectionLabelPaint);
        drawWrappedText(canvas, formattedTime, leftColumnX, secondRowY + labelToValueOffset, fieldWidth, fieldValuePaint, rowLineHeight);

        canvas.drawText("Location:", leftColumnX, thirdRowY, sectionLabelPaint);
        drawWrappedText(canvas, formattedVenue, leftColumnX, thirdRowY + labelToValueOffset, fieldWidth, fieldValuePaint, rowLineHeight);

        // Right column
        canvas.drawText("Entrant:", rightColumnX, firstRowY, sectionLabelPaint);
        drawWrappedText(canvas, entrantName, rightColumnX, firstRowY + labelToValueOffset, fieldWidth, fieldValuePaint, rowLineHeight);

        canvas.drawText("Email:", rightColumnX, secondRowY, sectionLabelPaint);
        drawWrappedText(canvas, entrantEmail, rightColumnX, secondRowY + labelToValueOffset, fieldWidth, fieldValuePaint, rowLineHeight);

        canvas.drawText("Ticket ID:", rightColumnX, thirdRowY, sectionLabelPaint);
        Paint ticketPaint = new Paint(fieldValuePaint);
        ticketPaint.setTextSize(20f);
        ticketPaint.setColor(ContextCompat.getColor(this, R.color.lightText));
        drawWrappedText(canvas, confirmationTicketId, rightColumnX, thirdRowY + labelToValueOffset, fieldWidth, ticketPaint, rowLineHeight);

        // QR (bottom right, centered at 75% page width)
        Bitmap qrBitmap = createQrBitmap(confirmationTicketId, 220, 220);
        float qrCenterX = pageInfo.getPageWidth() * 0.75f;
        float qrX = qrCenterX - 110f; // half of 220
        float qrY = 760f;

        if (qrBitmap != null) {
            canvas.drawBitmap(qrBitmap, qrX, qrY, null);
        }

        float scanLabelWidth = smallPaint.measureText("Scan for ticket ID");
        canvas.drawText("Scan for ticket ID", qrCenterX - (scanLabelWidth / 2f), qrY + 260f, smallPaint);

        pdfDocument.finishPage(page);

        File shareDir = new File(getCacheDir(), "shared_tickets");
        if (!shareDir.exists() && !shareDir.mkdirs()) {
            pdfDocument.close();
            Toast.makeText(this, "Failed to prepare ticket file", Toast.LENGTH_SHORT).show();
            return;
        }

        File pdfFile = new File(shareDir, filename);

        try (FileOutputStream outputStream = new FileOutputStream(pdfFile)) {
            pdfDocument.writeTo(outputStream);
        } catch (Exception e) {
            Toast.makeText(this, "PDF export failed", Toast.LENGTH_SHORT).show();
            pdfDocument.close();
            return;
        } finally {
            pdfDocument.close();
        }

        try {
            Uri shareUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    pdfFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, shareUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Event Confirmation Ticket");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Share confirmation ticket PDF");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share confirmation ticket PDF"));
        } catch (Exception e) {
            Toast.makeText(this, "Failed to share ticket", Toast.LENGTH_SHORT).show();
        }
    }
    // Created  and adapted with generation help fromChatGPT April 4th 2026,
    // "Encode a string into a QR bitmap using ZXing for ticket verification."
    /**
     * Creates a QR bitmap encoding the supplied ticket ID.
     *
     * @param value The text value to encode in the QR code.
     * @param width Width of the bitmap in pixels.
     * @param height Height of the bitmap in pixels.
     * @return A QR bitmap, or null if generation fails.
     */
    private Bitmap createQrBitmap(String value, int width, int height) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    value,
                    BarcodeFormat.QR_CODE,
                    width,
                    height
            );

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    // Created  and adapted with generation help fromChatGPT April 4th 2026,
    // "Render multi-line wrapped text onto a PDF canvas with width constraints."
    /**
     * Draws wrapped text on the PDF canvas and returns the final y position.
     *
     * @param canvas The PDF canvas to draw on.
     * @param text The text to wrap and draw.
     * @param startX The x coordinate for the text block.
     * @param startY The y coordinate for the first line.
     * @param maxWidth The maximum allowed line width.
     * @param paint The paint used to render the text.
     * @param lineHeight The vertical spacing between wrapped lines.
     * @return The y position of the last rendered line.
     */
    private float drawWrappedText(Canvas canvas, String text, float startX, float startY,
                                  float maxWidth, Paint paint, float lineHeight) {
        if (text == null || text.trim().isEmpty()) {
            canvas.drawText("N/A", startX, startY, paint);
            return startY;
        }

        String[] words = text.split("\\s+");
        StringBuilder lineBuilder = new StringBuilder();
        float y = startY;
        boolean drewLine = false;

        for (String word : words) {
            String testLine = lineBuilder.length() == 0 ? word : lineBuilder + " " + word;
            if (paint.measureText(testLine) <= maxWidth) {
                lineBuilder = new StringBuilder(testLine);
            } else {
                if (lineBuilder.length() > 0) {
                    canvas.drawText(lineBuilder.toString(), startX, y, paint);
                    drewLine = true;
                    y += lineHeight;
                }
                if (paint.measureText(word) > maxWidth) {
                    StringBuilder chunkBuilder = new StringBuilder();
                    for (int i = 0; i < word.length(); i++) {
                        String testChunk = chunkBuilder.toString() + word.charAt(i);
                        if (paint.measureText(testChunk) <= maxWidth) {
                            chunkBuilder.append(word.charAt(i));
                        } else {
                            canvas.drawText(chunkBuilder.toString(), startX, y, paint);
                            drewLine = true;
                            y += lineHeight;
                            chunkBuilder = new StringBuilder(String.valueOf(word.charAt(i)));
                        }
                    }
                    lineBuilder = chunkBuilder;
                } else {
                    lineBuilder = new StringBuilder(word);
                }
            }
        }

        if (lineBuilder.length() > 0) {
            canvas.drawText(lineBuilder.toString(), startX, y, paint);
            drewLine = true;
        }

        return drewLine ? y : startY;
    }
}
