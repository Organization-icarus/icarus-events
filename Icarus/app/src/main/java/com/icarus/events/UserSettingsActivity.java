package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.squareup.picasso.Picasso;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Screen for users to manage personal settings and account deletion.
 * <p>
 * Loads the current user's profile image and notification preferences from
 * Firestore, allows the user to update notification settings, and provides
 * functionality to delete the user's account. During account deletion, the
 * user is removed from all event entrant subcollections, any stored profile
 * image record is deleted, and the app returns to {@link MainActivity} with
 * the session cleared.
 *
 * @author Alex Alves
 */
public class UserSettingsActivity extends HeaderNavBarActivity {
    private Button deleteProfileButton;
    private Button generateDemoDataButton;
    private ImageView profileImage;
    private Switch adminNotificationsSwitch;
    private Switch organizerNotificationsSwitch;
    private User user;
    FirebaseFirestore db;

    /**
     * Initializes the user settings screen.
     * <p>
     * Loads the current user from {@link UserSession}, retrieves and displays the
     * user's profile image, loads notification preferences from Firestore into the
     * corresponding switches, and registers listeners to persist any switch changes.
     * Also configures the delete profile button to remove the user from all event
     * entrant subcollections, delete the user's stored profile image, remove the
     * user document, and return to the main screen.
     *
     * @param savedInstanceState the previously saved activity state,
     *                           or null if the activity is newly created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_settings);
        setupHeaderBar("Settings");
        setupNavBar(TAB_PROFILE);

        // Initialize database reference and collection references
        db = FirebaseFirestore.getInstance();

        // Retrieve device Id/User object
        user = UserSession.getInstance().getCurrentUser();
        String deviceId = user.getId();

        // Initialize user profile image
        profileImage = findViewById(R.id.user_settings_profile_image);
        db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId).get()
                .addOnSuccessListener(snapshot -> {
                    String imageURL = snapshot.getString("image");
                    if (imageURL != null && !imageURL.isEmpty()) {
                        Picasso.get()
                                .load(imageURL)
                                .placeholder(R.drawable.poster)
                                .error(R.drawable.poster)           // Optional: shows if link fails
                                .into(profileImage);
                    } else {
                        profileImage.setImageResource(R.drawable.poster);
                    }
                });

        // Initialize buttons
        deleteProfileButton = findViewById(R.id.user_profile_delete_button);

        // Initialize switches
        adminNotificationsSwitch = findViewById(R.id.user_settings_admin_notifications_switch);
        organizerNotificationsSwitch = findViewById(R.id.user_settings_org_notifications_switch);

        // Set buttons on click listeners
        // Taken from Claude March 11th 2026,
        // "I need to modify my query to also delete the user from the event collection entrant subcollection"
        android.content.Context appContext = getApplicationContext();
        deleteProfileButton.setOnClickListener(v -> {
            db.collection(FirestoreCollections.EVENTS_COLLECTION).get()
                    .addOnSuccessListener(eventSnapshots -> {
                        for (QueryDocumentSnapshot eventSnapshot : eventSnapshots) {
                            eventSnapshot.getReference()
                                    .collection("entrants")
                                    .document(deviceId)
                                    .delete();
                        }

                        // Remove all events organized by ONLY this user
                        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                                .whereArrayContains("organizers", user.getId())
                                .get()
                                .addOnSuccessListener(snapshot -> {
                                    for (QueryDocumentSnapshot eventSnapshot : snapshot) {
                                        java.util.List<String> organizers = (java.util.List<String>) eventSnapshot.get("organizers");
                                        if (organizers != null && organizers.size() == 1) {
                                            removeEvent(eventSnapshot.getId(), eventSnapshot.getString("image"), appContext);
                                        }
                                    }
                                });

                        // Delete profile image
                        db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId).get()
                                .addOnSuccessListener(userSnapshot -> {
                                    String imageURL = userSnapshot.getString("image");
                                    deleteOldProfileImage(imageURL);

                                    // Delete the user document after
                                    db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId).delete()
                                            .addOnSuccessListener(unused -> {
                                                Toast.makeText(appContext, "Profile deleted", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(UserSettingsActivity.this, MainActivity.class);
                                                intent.putExtra("clearSession", true);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                                finish();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(appContext, "Failed to delete profile", Toast.LENGTH_SHORT).show());
                                });
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(appContext, "Failed to delete profile", Toast.LENGTH_SHORT).show());
        });

        // Taken from Claude March 11th 2026, "What query can I use to load in current settings"
        db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Map<String, Object> settings = (Map<String, Object>) snapshot.get("settings");
                        if (settings != null) {
                            Boolean adminNotif = (Boolean) settings.get("adminNotifications");
                            Boolean organizerNotif = (Boolean) settings.get("organizerNotifications");
                            if (adminNotif != null) adminNotificationsSwitch.setChecked(adminNotif);
                            if (organizerNotif != null) organizerNotificationsSwitch.setChecked(organizerNotif);
                        }
                    }
                    // Set listeners AFTER loading so setChecked doesn't trigger writes
                    // Taken from Claude March 11th 2026, "What queries can I use to update currently selected settings"
                    adminNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId)
                                .update("settings.adminNotifications", isChecked)
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to save setting", Toast.LENGTH_SHORT).show());
                    });
                    organizerNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        db.collection(FirestoreCollections.USERS_COLLECTION).document(deviceId)
                                .update("settings.organizerNotifications", isChecked)
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to save setting", Toast.LENGTH_SHORT).show());
                    });
                })
                .addOnFailureListener(e ->
                        Log.e("UserSettings", "Failed to load settings: " + e.getMessage()));

        generateDemoDataButton = findViewById(R.id.generate_demo_data_button);
        generateDemoDataButton.setOnClickListener(v -> {
            Map<String, Object> sam = new HashMap<>();
            sam.put("email", "samw@shire.com");
            sam.put("fcmTokes", null);
            sam.put("image", "https://res.cloudinary.com/icarus-images/image/upload/v1775679849/samwise_wijtel.jpg");
            sam.put("isAdmin", false);
            sam.put("name", "Samwise Gamgee");
            sam.put("phone", null);
            sam.put("settings", Arrays.asList(false, false));
            sam.put("events", Arrays.asList());
            db.collection(FirestoreCollections.USERS_COLLECTION).document("sam-demo-id").set(sam);

            Map<String, Object> legolas = new HashMap<>();
            legolas.put("email", "lego@mirkwood.com");
            legolas.put("fcmTokes", null);
            legolas.put("image", "https://res.cloudinary.com/icarus-images/image/upload/v1775679847/legolas_x5q0za.jpg");
            legolas.put("isAdmin", false);
            legolas.put("name", "Legolas");
            legolas.put("phone", null);
            legolas.put("settings", Arrays.asList(false, false));
            legolas.put("events", Arrays.asList());
            db.collection(FirestoreCollections.USERS_COLLECTION).document("legolas-demo-id").set(legolas);

            Map<String, Object> gollum = new HashMap<>();
            gollum.put("email", "gollum@cave.com");
            gollum.put("fcmTokes", null);
            gollum.put("image", "https://res.cloudinary.com/icarus-images/image/upload/v1775679847/gollum_qui1wv.jpg");
            gollum.put("isAdmin", false);
            gollum.put("name", "Gollum");
            gollum.put("phone", null);
            gollum.put("settings", Arrays.asList(false, false));
            gollum.put("events", Arrays.asList());
            db.collection(FirestoreCollections.USERS_COLLECTION).document("gollum-demo-id").set(gollum);

            Map<String, Object> gimli = new HashMap<>();
            gimli.put("email", "gimli@mines.com");
            gimli.put("fcmTokes", null);
            gimli.put("image", "https://res.cloudinary.com/icarus-images/image/upload/v1775679850/gimli_brvaum.jpg");
            gimli.put("isAdmin", false);
            gimli.put("name", "Gimli");
            gimli.put("phone", null);
            gimli.put("settings", Arrays.asList(false, false));
            gimli.put("events", Arrays.asList());
            db.collection(FirestoreCollections.USERS_COLLECTION).document("gimli-demo-id").set(gimli);

            Map<String, Object> sauron = new HashMap<>();
            sauron.put("email", "sauron@mordor.com");
            sauron.put("fcmTokes", null);
            sauron.put("image", "https://res.cloudinary.com/icarus-images/image/upload/v1775679850/sauron_o88gru.jpg");
            sauron.put("isAdmin", false);
            sauron.put("name", "Sauron");
            sauron.put("phone", null);
            sauron.put("settings", Arrays.asList(false, false));
            sauron.put("events", Arrays.asList());
            db.collection(FirestoreCollections.USERS_COLLECTION).document("sauron-demo-id").set(sauron);

            Map<String, Object> merry = new HashMap<>();
            merry.put("email", "merryb@shire.com");
            merry.put("fcmTokes", null);
            merry.put("image", "https://res.cloudinary.com/icarus-images/image/upload/v1775679847/merry_fxgnmf.jpg");
            merry.put("isAdmin", false);
            merry.put("name", "Merry Brandybuck");
            merry.put("phone", null);
            merry.put("settings", Arrays.asList(false, false));
            merry.put("events", Arrays.asList());
            db.collection(FirestoreCollections.USERS_COLLECTION).document("merry-demo-id").set(merry);

            Map<String, Object> pippin = new HashMap<>();
            pippin.put("email", "pip@shire.com");
            pippin.put("fcmTokes", null);
            pippin.put("image", "https://res.cloudinary.com/icarus-images/image/upload/v1775679848/pippin_fmnuvo.jpg");
            pippin.put("isAdmin", false);
            pippin.put("name", "Pippin Took");
            pippin.put("phone", null);
            pippin.put("settings", Arrays.asList(false, false));
            pippin.put("events", Arrays.asList());
            db.collection(FirestoreCollections.USERS_COLLECTION).document("pippin-demo-id").set(pippin);

            Map<String, Object> bilbo = new HashMap<>();
            bilbo.put("email", "bilbobag@shire.com");
            bilbo.put("fcmTokes", null);
            bilbo.put("image", "https://res.cloudinary.com/icarus-images/image/upload/v1775769958/bilbo_ef64j6.webp");
            bilbo.put("isAdmin", false);
            bilbo.put("name", "Bilbo Baggins");
            bilbo.put("phone", null);
            bilbo.put("settings", Arrays.asList(false, false));
            bilbo.put("events", Arrays.asList());
            db.collection(FirestoreCollections.USERS_COLLECTION).document("bilbo-demo-id").set(bilbo);

            Map<String, Object> secretCouncil = new HashMap<>();
            secretCouncil.put("capacity", 13);
            secretCouncil.put("category", "Education");
            secretCouncil.put("close", toTimestamp(2026, 04, 30));
            secretCouncil.put("coordinates", null);
            secretCouncil.put("endDate", toTimestamp(2026, 05, 04));
            secretCouncil.put("entrantRange", null);
            secretCouncil.put("geolocation", false);
            secretCouncil.put("image", "https://res.cloudinary.com/icarus-images/image/upload/v1775680955/rivendell_lrtvfb.png");
            secretCouncil.put("isPrivate", true);
            secretCouncil.put("location", "Rivendell");
            secretCouncil.put("name", "Secret Council");
            secretCouncil.put("description", "Council of Elrond.");
            secretCouncil.put("open", toTimestamp(2026, 04, 01));
            secretCouncil.put("organizers", Arrays.asList("d1e25fd5419c4e04f37a24829d6ad8892b22ecd7df5dfb9f0563f6d79fcd39c1"));
            secretCouncil.put("startDate", toTimestamp(2026, 05, 03));
            db.collection(FirestoreCollections.EVENTS_COLLECTION).document("secretCouncil-demo-id").set(secretCouncil);

            Map<String, Object> moria = new HashMap<>();
            moria.put("capacity", 13);
            moria.put("category", "Education");
            moria.put("close", toTimestamp(2026, 04, 31));
            moria.put("coordinates", null);
            moria.put("endDate", toTimestamp(2026, 05, 04));
            moria.put("entrantRange", null);
            moria.put("geolocation", false);
            moria.put("image", "https://res.cloudinary.com/icarus-images/image/upload/v1775682570/moria_qg8xxx.jpg");
            moria.put("isPrivate", false);
            moria.put("location", "Mines of Moria");
            moria.put("name", "Investigate Mines of Moria");
            moria.put("description", "Investigating.");
            moria.put("open", toTimestamp(2026, 04, 01));
            moria.put("organizers", Arrays.asList("d1e25fd5419c4e04f37a24829d6ad8892b22ecd7df5dfb9f0563f6d79fcd39c1"));
            moria.put("startDate", toTimestamp(2026, 05, 04));
            db.collection(FirestoreCollections.EVENTS_COLLECTION).document("moria-demo-id").set(moria);

            Map<String, Object> camp = new HashMap<>();
            camp.put("capacity", 13);
            camp.put("category", "Sports");
            camp.put("close", toTimestamp(2026, 04, 30));
            camp.put("coordinates", null);
            camp.put("endDate", toTimestamp(2026, 05, 04));
            camp.put("entrantRange", null);
            camp.put("geolocation", false);
            camp.put("image", "https://res.cloudinary.com/icarus-images/image/upload/v1775682709/camp_ze35x0.webp");
            camp.put("isPrivate", false);
            camp.put("location", "Weathertop");
            camp.put("name", "Camp at Weathertop");
            camp.put("description", "Lets go camping!");
            camp.put("open", toTimestamp(2026, 04, 01));
            camp.put("organizers", Arrays.asList("d1e25fd5419c4e04f37a24829d6ad8892b22ecd7df5dfb9f0563f6d79fcd39c1"));
            camp.put("startDate", toTimestamp(2026, 05, 02));
            db.collection(FirestoreCollections.EVENTS_COLLECTION).document("camp-demo-id").set(camp);

            Map<String, Object> bday = new HashMap<>();
            bday.put("capacity", 13);
            bday.put("category", "Music");
            bday.put("close", toTimestamp(2026, 04, 30));
            bday.put("coordinates", null);
            bday.put("endDate", toTimestamp(2026, 05, 04));
            bday.put("entrantRange", null);
            bday.put("geolocation", false);
            bday.put("image", "https://res.cloudinary.com/icarus-images/image/upload/v1775682645/shire_ouzrjo.jpg");
            bday.put("isPrivate", false);
            bday.put("location", "The Shire");
            bday.put("name", "Bilbo's 111th Birthday Celebration");
            bday.put("description", "Join us for Bilbo's 111th birthday!");
            bday.put("open", toTimestamp(2026, 04, 01));
            bday.put("organizers", Arrays.asList("d1e25fd5419c4e04f37a24829d6ad8892b22ecd7df5dfb9f0563f6d79fcd39c1"));
            bday.put("startDate", toTimestamp(2026, 05, 01));
            DocumentReference bdayRef = db.collection(FirestoreCollections.EVENTS_COLLECTION).document("birthday-demo-id");
            bdayRef.set(bday);

            Map<String, Object> merryComment = new HashMap<>();
            merryComment.put("authorId", "merry-demo-id");
            merryComment.put("authorImage", "https://res.cloudinary.com/icarus-images/image/upload/v1775679847/merry_fxgnmf.jpg");
            merryComment.put("authorName", "Merry Brandybuck");
            merryComment.put("createdAt", Timestamp.now());
            merryComment.put("deleted", false);
            merryComment.put("documentId", null);
            merryComment.put("text", "I can't wait for the fireworks!");
            bdayRef.collection("comments").add(merryComment);

            Map<String, Object> pippinComment = new HashMap<>();
            pippinComment.put("authorId", "pippin-demo-id");
            pippinComment.put("authorImage", "https://res.cloudinary.com/icarus-images/image/upload/v1775679848/pippin_fmnuvo.jpg");
            pippinComment.put("authorName", "Pippin Took");
            pippinComment.put("createdAt", Timestamp.now());
            pippinComment.put("deleted", false);
            pippinComment.put("documentId", null);
            pippinComment.put("text", "There better be at at least three dinners!");
            bdayRef.collection("comments").add(pippinComment);

            Map<String, Object> bilboComment = new HashMap<>();
            bilboComment.put("authorId", "bilbo-demo-id");
            bilboComment.put("authorImage", "https://res.cloudinary.com/icarus-images/image/upload/v1775769958/bilbo_ef64j6.webp");
            bilboComment.put("authorName", "Bilbo Baggins");
            bilboComment.put("createdAt", Timestamp.now());
            bilboComment.put("deleted", false);
            bilboComment.put("documentId", null);
            bilboComment.put("text", "I don't know half of you half as well as I should like; and I like less than half of you half as well as you deserve");
            bdayRef.collection("comments").add(bilboComment);

            Map<String, Object> gollumComment = new HashMap<>();
            gollumComment.put("authorId", "gollum-demo-id");
            gollumComment.put("authorImage", "https://res.cloudinary.com/icarus-images/image/upload/v1775679847/gollum_qui1wv.jpg");
            gollumComment.put("authorName", "Gollum");
            gollumComment.put("createdAt", Timestamp.now());
            gollumComment.put("deleted", false);
            gollumComment.put("documentId", null);
            gollumComment.put("text", "GIVE ME BACK MY RING YO!");
            bdayRef.collection("comments").add(gollumComment);

            Map<String, Object> sauronComment = new HashMap<>();
            sauronComment.put("authorId", "sauron-demo-id");
            sauronComment.put("authorImage", "https://res.cloudinary.com/icarus-images/image/upload/v1775771167/sauron_cgc6pu.jpg");
            sauronComment.put("authorName", "Sauron");
            sauronComment.put("createdAt", Timestamp.now());
            sauronComment.put("deleted", false);
            sauronComment.put("documentId", null);
            sauronComment.put("text", "Enjoy your celebration while you can...");
            bdayRef.collection("comments").add(sauronComment);
        });

    }

    /**
     * Deletes the previously stored profile image associated with the given URL.
     * <p>
     * Looks up matching image records in the Firestore images collection and
     * removes them using the {@link Image} helper.
     *
     * @param URL the URL of the profile image to delete
     */
    private void deleteOldProfileImage(String URL) {
        db.collection(FirestoreCollections.IMAGES_COLLECTION)
                .whereEqualTo("URL", URL)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Image oldImage = new Image(URL, doc.getId());
                        oldImage.delete(this, db);
                    }
                });
    }

    /**
     * Performs logic to remove event from the Firestore Database, including any references to that event
     *
     * @param eventId   Firestore ID of event
     * @param eventImage    Image URL of event poster
     */
    private void removeEvent(String eventId, String eventImage, android.content.Context context) {
        // Code generated by Claude AI March 11, 2026
        // "How to remove event ID from a users list of events for each user document in the
        // events 'entrants' subcollection."
        CollectionReference eventEntrantsRef = db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId).collection("entrants");
        CollectionReference eventCommentsRef = db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId).collection("comments");
        CollectionReference eventNotificationsRef = db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId).collection("notifications");

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
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("events", FieldValue.arrayRemove(eventId));
                        batch.set(userRef, updateData, SetOptions.merge());
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

                    // Remove the event's poster from the database
                    db.collection(FirestoreCollections.IMAGES_COLLECTION)
                            .whereEqualTo("URL", eventImage)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    new Image(eventImage, doc.getId()).delete(context, db);
                                }
                            });

                    // Delete event document and commit
                    batch.delete(db.collection(FirestoreCollections.EVENTS_COLLECTION).document(eventId));

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Firestore", "Event deleted successfully");
                                Toast.makeText(context, "Event deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Firestore", "Error deleting event", e);
                                Toast.makeText(context, "Failed to delete event", Toast.LENGTH_SHORT).show();
                            });

                }).addOnFailureListener(e -> Log.e("Firestore", "Error fetching subcollections", e));
    };

    private Timestamp toTimestamp(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day, 0, 0, 0);
        return new Timestamp(cal.getTime());
    }
}