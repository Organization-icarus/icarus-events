package com.icarus.events;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Activity that allows organizers to view the entrants they can invite to
 * a private event or to add as a Co-Organizer.
 * <p>
 * This activity extends {@link HeaderNavBarActivity} to include
 * the application's reusable navigation bar.
 *
 * @author Ben Salmon
 */
public class OrganizerEntrantSearchActivity extends HeaderNavBarActivity {
    private FirebaseFirestore db;
    private TextView eventName;
    private TextView activityName;
    private EditText searchBar;
    private String currentSearch = "";

    private Button confirmationButton;
    private ListView entrantList;
    private ArrayList<User> entrantUserList;
    private OraganizerEntrantViewListArrayAdapter eventListArrayAdapter;
    private String eventId;
    private String userId;

    /**
     * Initializes the activity, sets up the navigation bar, loads event data,
     * configures the search bar and entrant list, and attaches the confirmation
     * button behavior for adding entrants, assigning co-organizers, or replacing
     * declined entrants.
     *
     * @param savedInstanceState the saved instance state bundle, or null if none exists
     */
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_entrant_search);
        setupNavBar(TAB_NONE);
        User user = UserSession.getInstance().getCurrentUser();
        userId = user.getId();

        db = FirebaseFirestore.getInstance();

        //activityName
        activityName = findViewById(R.id.OrganizerEntrantSearchTitle);
        String screenName = getIntent().getStringExtra("ActivityName");
        activityName.setText(screenName);

        //Create Event Name, Get eventID, and Get event Title from database
        eventName = findViewById(R.id.OrganizerEntrantSearchEventName);
        eventId = getIntent().getStringExtra("eventId");
        db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    String name = value.getString("name");

                    runOnUiThread(() -> {
                        eventName.setText(name);
                    });
                });

        //Create EditText
        searchBar = findViewById(R.id.OrganizerEntrantSearchBar);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString().trim().toLowerCase();
                applySearch();
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        //Create Button
        confirmationButton = findViewById(R.id.OrganizerEntrantConfirmationButton);

        //Create ListView
        entrantList = findViewById(R.id.OrganizerEntrantList);
        entrantUserList = new ArrayList<>();
        eventListArrayAdapter = new OraganizerEntrantViewListArrayAdapter(this, entrantUserList);
        entrantList.setAdapter(eventListArrayAdapter);

        //Fill list with users not in the waiting list
        if(screenName.equals("Replace Declined")){
            loadEntrantList("rejected");
        }else {
            loadList();
        }

        /*TODO:  A NOTIFICATION MUST BE SENT INSTEAD
         * AWAITING NOTIFICATION SET UP FROM KITO AND YIFAN
         * March 25,2026 @ 4:23pm
         */
        confirmationButton.setOnClickListener(v -> {
            Set<String> selectedIds = eventListArrayAdapter.getSelectedIds();
            if (selectedIds.isEmpty()) {
                Toast.makeText(this, "No users selected", Toast.LENGTH_SHORT).show();
                return;
            }


            if (screenName.equals("Entrant Search")) {
                addUsersToEvent(selectedIds, "waiting");

                ArrayList<String> privateRecipients = new ArrayList<>(selectedIds);

                db.collection(FirestoreCollections.EVENTS_COLLECTION)
                        .document(eventId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String eventName = documentSnapshot.getString("name");
                                String eventImage = documentSnapshot.getString("image");

                                NotificationItem privateNotification = new NotificationItem(
                                        eventId,
                                        eventName,
                                        eventImage,
                                        userId,
                                        true,
                                        privateRecipients,
                                        "You have been invited to join a private event waiting list.",
                                        "private_waitlist_invite"
                                );
                                privateNotification.sendNotification(this);
                            } else {
                                Log.e("NotificationError", "Event not found for ID: " + eventId);
                            }
                        });

            } else if (screenName.equals("Find Co-Organizers")) {
                addUserstoOrganizersArray(selectedIds);

                ArrayList<String> coOrganizerRecipients = new ArrayList<>(selectedIds);

                db.collection(FirestoreCollections.EVENTS_COLLECTION)
                        .document(eventId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String eventName = documentSnapshot.getString("name");
                                String eventImage = documentSnapshot.getString("image");

                                NotificationItem coOrganizerNotification = new NotificationItem(
                                        eventId,
                                        eventName,
                                        eventImage,
                                        userId,
                                        true,
                                        coOrganizerRecipients,
                                        "You have been invited to be a co-organizer.",
                                        "co_organizer_invite"
                                );
                                coOrganizerNotification.sendNotification(this);
                            } else {
                                Log.e("NotificationError", "Event not found for ID: " + eventId);
                            }
                        });

            } else if (screenName.equals("Replace Declined")) {
                db.collection(FirestoreCollections.EVENTS_COLLECTION)
                        .document(eventId)
                        .collection("entrants")
                        .get()
                        .addOnSuccessListener(document -> {
                            ArrayList<String> waitingIds = new ArrayList<>();
                            for (QueryDocumentSnapshot snapshot : document) {
                                String status = snapshot.getString("status");
                                if ("waiting".equals(status)) {
                                    waitingIds.add(snapshot.getId());
                                }
                            }

                            if (waitingIds.isEmpty()) {
                                Toast.makeText(this, "Waiting list is Empty, Cannot Replace", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (waitingIds.size() < selectedIds.size()) {
                                Toast.makeText(this,
                                        "More Entrants Selected than in Waiting List, Cannot Replace",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            addUsersToEvent(selectedIds, "rejected");
                            selectNewUsersFromWaitingList(selectedIds.size());
                            createNotification(selectedIds, "replace_declined",
                                    "You have been Selected for the event: " + eventName.getText().toString());
                            finish();
                        });
                return;
            }

            finish();
        });
    }

    /**
     * Loads all users who are eligible to be shown in the entrant selection list.
     * A user is included only if they are not already in the event's entrant list,
     * are not an admin, are not the current user, and are not already an organizer.
     */
    /*
     * Written by Claude, March 25,2026
     * "Can you write a firebase query that find all users in the users
     * collection that aren't in the "entrants" subcollection or have the isadmin bool = true.
     *
     * Changed and implemented by Ben Salmon
     * */
    private void loadList() {
        entrantUserList.clear();
        eventListArrayAdapter.notifyDataSetChanged();

        // Step 1: Get organizers array from event document
        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(eventId)
                .get()
                .addOnSuccessListener(eventSnapshot -> {
                    ArrayList<String> organizerIds = (ArrayList<String>) eventSnapshot.get("organizers");
                    final Set<String> organizerSet = organizerIds != null ? new HashSet<>(organizerIds) : new HashSet<>();

                    // Step 2: Get all entrant IDs from the subcollection
                    db.collection(FirestoreCollections.EVENTS_COLLECTION)
                            .document(eventId)
                            .collection("entrants")
                            .get()
                            .addOnSuccessListener(entrantSnapshots -> {
                                Set<String> entrantIds = new HashSet<>();
                                for (QueryDocumentSnapshot snapshot : entrantSnapshots) {
                                    entrantIds.add(snapshot.getId());
                                }

                                // Step 3: Get all users and filter
                                db.collection(FirestoreCollections.USERS_COLLECTION)
                                        .get()
                                        .addOnSuccessListener(userSnapshots -> {
                                            entrantUserList.clear();

                                            for (QueryDocumentSnapshot userSnapshot : userSnapshots) {
                                                String deviceId = userSnapshot.getId();
                                                String name = userSnapshot.getString("name");
                                                String email = userSnapshot.getString("email");
                                                String phone = userSnapshot.getString("phone");
                                                String image = userSnapshot.getString("image");
                                                Boolean isAdmin = userSnapshot.getBoolean("isAdmin");

                                                boolean userIsAdmin = isAdmin != null && isAdmin;
                                                boolean notInEntrants = !entrantIds.contains(deviceId);
                                                boolean isCurrentUser = deviceId.equals(userId);
                                                boolean isOrganizer = organizerSet.contains(deviceId);

                                                if (notInEntrants && !userIsAdmin && !isCurrentUser && !isOrganizer) {
                                                    entrantUserList.add(new User(deviceId, name, email, phone, image, null,
                                                            null, null, null, null));
                                                }
                                            }
                                            eventListArrayAdapter.notifyDataSetChanged();
                                        })
                                        .addOnFailureListener(e -> Log.e("loadList", "Failed to fetch users", e));
                            })
                            .addOnFailureListener(e -> Log.e("loadList", "Failed to fetch entrants", e));
                })
                .addOnFailureListener(e -> Log.e("loadList", "Failed to fetch event", e));
    }

    /**
     * Adds the selected users to the event's entrant subcollection with the given status.
     * If the new status is {@code rejected}, the entrant is also marked as replaced.
     *
     * @param selectedIds the IDs of the users to add or update
     * @param newStatus the status to assign to each selected user
     */
    /*
     * Written by Claude, March 25,2026
     * "How can I make the listView selectable to add users to an event"
     * */
    private void addUsersToEvent(Set<String> selectedIds, String newStatus) {
        WriteBatch batch = db.batch();

        for (String deviceId : selectedIds) {
            DocumentReference ref = db.collection(FirestoreCollections.EVENTS_COLLECTION)
                    .document(eventId)
                    .collection("entrants")
                    .document(deviceId);

            Map<String, Object> data = new HashMap<>();
            if(newStatus.equals("rejected")){
                data.put("isReplaced", true);
            }
            data.put("status", newStatus);
            batch.set(ref, data);
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    if(newStatus.equals("waiting")){
                        Toast.makeText(this, selectedIds.size() + " users added to waiting list", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e("addUsers", "Failed to add users", e));
    }

    /**
     * Adds the selected user IDs to the event's organizers array, making them
     * co-organizers for the event.
     *
     * @param selectedIds the IDs of the users to add as co-organizers
     */
    private void addUserstoOrganizersArray(Set<String> selectedIds){
        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    ArrayList<String> organizers = (ArrayList<String>) documentSnapshot.get("organizers");
                    organizers.addAll(selectedIds);
                    db.collection(FirestoreCollections.EVENTS_COLLECTION)
                            .document(eventId)
                            .update("organizers", organizers)
                            .addOnSuccessListener(dummy ->{
                                Toast.makeText(this,selectedIds.size()
                                        + " organizers added as Co-Organizers",Toast.LENGTH_SHORT).show();
                            });
                });
    }

    /**
     * Filters the currently loaded entrant list using the text entered in the search bar.
     * The search checks each user's name, email, and phone number for a match.
     * If the search string is empty, the full eligible list is reloaded.
     */
    private void applySearch(){
        if (currentSearch.isEmpty()) {
            loadList();
            return;
        }
        ArrayList<User> filteredEntrants = new ArrayList<User>();
        for (User user : entrantUserList){
            boolean name = user.getName().toLowerCase().contains(currentSearch);
            boolean email = (user.getEmail() != null) && (user.getEmail().toLowerCase().contains(currentSearch));
            boolean phone = (user.getPhone() != null) && (user.getPhone().toLowerCase().contains(currentSearch));
            if(name || email || phone){
                filteredEntrants.add(user);
            }
        }
        entrantUserList.clear();
        entrantUserList.addAll(filteredEntrants);
        eventListArrayAdapter.notifyDataSetChanged();
    }

    /**
     * Loads entrants from the event's entrant subcollection whose status matches
     * the provided status string and who have not already been marked as replaced.
     *
     * @param listStatus the entrant status to filter by
     */
    private void loadEntrantList(String listStatus) {
        //events -> eventID -> entrants -> entrantId -> status
        entrantUserList.clear();
        eventListArrayAdapter.notifyDataSetChanged();
        db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId).collection("entrants")
                .get()
                .addOnSuccessListener(value -> {
                    entrantUserList.clear();
                    for (QueryDocumentSnapshot snapshot : value) {
                        String deviceId = snapshot.getId();
                        String status = snapshot.getString("status");
                        Boolean isReplaced = snapshot.getBoolean("isReplaced");

                        if (Objects.equals(status, listStatus)  && (isReplaced == null || !isReplaced)) {
                            db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId)
                                    .get()
                                    .addOnSuccessListener(userSnapshot -> {
                                        String name = userSnapshot.getString("name");
                                        String email = userSnapshot.getString("email");
                                        String phone = userSnapshot.getString("phone");
                                        String image = userSnapshot.getString("image");
                                        entrantUserList.add(new User(deviceId, name, email, phone, image, null,
                                                null, null, null, null));
                                        eventListArrayAdapter.notifyDataSetChanged();
                                    });
                        }
                    }
                });
    }

    /**
     * Randomly selects a given number of users from the waiting list and updates
     * their entrant status to {@code selected}.
     *
     * @param size the number of waiting-list users to select
     */
    private void selectNewUsersFromWaitingList(int size) {
        db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId).collection("entrants")
                .get()
                .addOnSuccessListener(document ->{
                    ArrayList<String> waitingIds = new ArrayList<>();
                    for (QueryDocumentSnapshot snapshot : document){
                        String deviceId = snapshot.getId();
                        String status = snapshot.getString("status");
                        if("waiting".equals(status)){
                            waitingIds.add(deviceId);
                        }
                    }
                    //Shuffle List
                    Collections.shuffle(waitingIds);
                    List<String> selectedUsers = waitingIds.subList(0,Math.min(size, waitingIds.size()));
                    HashSet<String> selectedSet = new HashSet<>(selectedUsers);
                    //Update
                    for (QueryDocumentSnapshot snapshot : document){
                        String deviceId = snapshot.getId();
                        if(selectedSet.contains(deviceId)){
                            db.collection(FirestoreCollections.EVENTS_COLLECTION)
                                    .document(eventId)
                                    .collection("entrants")
                                    .document(deviceId)
                                    .update("status", "selected");
                        }
                    }
                    Toast.makeText(this,Math.min(size, waitingIds.size())
                            + " Entrants Selected for Event",Toast.LENGTH_SHORT).show();

                });
    }

    /**
     * Creates and stores a notification document for the given recipients.
     *
     * @param Ids the IDs of the users who should receive the notification
     * @param type the notification type
     * @param message the notification message content
     */
    private void createNotification(Set<String> Ids, String type,  String message){
        Date now = new Date();
        ArrayList<String> recipients = new ArrayList<>(Ids);

        Map<String, Object> notification = new HashMap<>();
        notification.put("date", now);
        notification.put("eventID", eventId);
        notification.put("isEvent", true);
        notification.put("isSystem", false);
        notification.put("message", message);
        notification.put("recipients", recipients);
        notification.put("sender", userId);
        notification.put("type", type);

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(dummy -> {
                    Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }
}