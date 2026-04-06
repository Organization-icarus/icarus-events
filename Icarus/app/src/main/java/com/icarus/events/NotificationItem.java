package com.icarus.events;

import android.content.Context;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a notification item and manages the dispatching of notifications to users.
 * <p>
 * This class acts as a data model for notification content (title, message, recipients)
 * and provides functionality to persist the notification to Firebase Firestore and
 * trigger push notifications via {@link NotificationHelper}.
 *
 * @author Kito Lee Son
 */
public class NotificationItem {

    private String id;
    private String eventId;
    private final String eventName;
    private final String eventImage;
    private String sender;
    private boolean isEvent;
    private boolean isSystem;
    private ArrayList<String> recipients;
    private String message;
    private String type;
    private final FirebaseFirestore db;

    /**
     * Constructs a full NotificationItem with detailed event and recipient information.
     *
     * @param eventId the unique identifier of the related event
     * @param eventName the display name of the event
     * @param eventImage the URL or path to the event's promotional image
     * @param sender the identifier of the user or system sending the notification
     * @param isEvent true if this is an event-related alert, false for system alerts
     * @param recipients a list of user IDs who should receive this notification
     * @param message the text body of the notification
     * @param type the category/type of notification (e.g., "invite", "update")
     */
    public NotificationItem(String eventId, String eventName, String eventImage, String sender, Boolean isEvent, ArrayList<String> recipients, String message, String type) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventImage = eventImage;
        this.sender = sender;
        this.isEvent = isEvent;
        this.isSystem = !isEvent;
        this.recipients = recipients;
        this.message = message;
        this.type = type;
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Constructs a simplified NotificationItem typically used for display in lists.
     *
     * @param id the unique document ID from Firestore
     * @param eventName the name of the event associated with the notification
     * @param eventImage the image associated with the notification
     * @param message the content of the message
     */
    public NotificationItem(String id, String eventId, String eventName, String eventImage, String message) {
        this.id = id;
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventImage = eventImage;
        this.message = message;
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Returns the unique document ID for this notification.
     *
     * @return the Firestore document ID
     */
    public String getId() { return id; }

    /**
     * Sets the unique document ID for this notification.
     *
     * @param id the unique identifier to set
     */
    public void setId(String id) { this.id = id; }

    /**
     * Returns the unique identifier of the event associated with this notification.
     *
     * @return the event ID string
     */
    public String getEventId() {
        return this.eventId;
    }

    /**
     * Returns the name of the event associated with this notification.
     *
     * @return the event name string
     */
    public String getEventName() { return eventName; }

    /**
     * Returns the image URL or resource path for the associated event.
     *
     * @return the event image string
     */
    public String getEventImage() { return eventImage; }

    /**
     * Returns the identifier of the user or system that sent the notification.
     *
     * @return the sender's identifier
     */
    public String getSender() {
        return this.sender;
    }

    /**
     * Returns the list of user IDs designated as recipients of this notification.
     *
     * @return an ArrayList of recipient user IDs
     */
    public ArrayList<String> getRecipients() {
        return this.recipients;
    }

    /**
     * Returns the main message content of the notification.
     *
     * @return the notification message string
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Indicates whether this is an event-specific notification.
     *
     * @return true if it is an event notification, false otherwise
     */
    public boolean isEventNotification() {
        return this.isEvent;
    }

    /**
     * Indicates whether this is a system-generated notification.
     *
     * @return true if it is a system notification, false otherwise
     */
    public boolean isSystemNotification() {
        return this.isSystem;
    }

    /**
     * Returns the category or type of the notification.
     *
     * @return the notification type string
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the category or type of the notification.
     *
     * @param type the notification type to set (e.g., "INVITE", "CANCEL")
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Configures this notification as an event-related alert.
     * <p>
     * Sets the event flag to true and automatically disables the system flag.
     */
    public void setEvent() {
        this.isEvent = true;
        this.isSystem = false;
    }

    /**
     * Configures this notification as a system-generated alert.
     * <p>
     * Sets the system flag to true and automatically disables the event flag.
     */
    public void setSystem() {
        this.isSystem = true;
        this.isEvent = false;
    }

    /**
     * Sets the full list of recipients for this notification.
     * <p>
     * Creates a new ArrayList from the provided collection to prevent external
     * modification of the internal list.
     *
     * @param users the list of user IDs to receive the notification
     */
    public void setRecipients(ArrayList<String> users) {
        this.recipients = new ArrayList<String>(users);
    }

    /**
     * Sets the body text of the notification.
     *
     * @param message the message content to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Adds a single user to the recipient list if they are not already present.
     *
     * @param userId the unique identifier of the user to add
     */
    public void addRecipient(String userId) {
        if (!this.recipients.contains(userId)) {
            this.recipients.add(userId);
        }
    }

    /**
     * Removes a specific user from the recipient list.
     *
     * @param userId the unique identifier of the user to remove
     */
    public void removeRecipient(String userId) {
        this.recipients.remove(userId);
    }

    /**
     * Dispatches the notification to all specified recipients.
     * <p>
     * This method performs a two-step process:
     * 1. It saves the notification data to the Firestore "notifications" collection.
     * 2. It iterates through the recipient list, retrieves their FCM tokens from the
     * database, and sends a push notification to each of their registered devices
     * using the {@link NotificationHelper}.
     *
     * @param context the context required to trigger network requests and access assets
     * @throws IllegalArgumentException if the message is null or empty
     * @throws IllegalStateException if the recipient list is empty
     */
    public void sendNotification(Context context) {
        if (this.message == null || this.message.trim().isEmpty()) {
            throw new IllegalArgumentException("Notification message cannot be null or empty");
        }
        if (this.recipients.isEmpty()) {
            throw new IllegalStateException("Notification must have at least one recipient");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("eventId", this.eventId);
        data.put("eventName", this.eventName);
        data.put("eventImage", this.eventImage);
        data.put("sender", this.sender);
        data.put("isEvent", this.isEvent);
        data.put("isSystem", this.isSystem);
        data.put("recipients", this.recipients);
        data.put("message", this.message);
        data.put("type", this.type);
        data.put("date", new Date());

        this.db.collection(FirestoreCollections.NOTIFICATIONS_COLLECTION)
                .add(data)
                .addOnSuccessListener(docRef -> {
                })
                .addOnFailureListener(Throwable::printStackTrace);

        this.recipients.forEach(recipient -> {
            this.db.collection(FirestoreCollections.USERS_COLLECTION).document(recipient).get()
                    .addOnSuccessListener(snapshot -> {
                        Map<String, Boolean> settings = (Map<String, Boolean>) snapshot.get("settings");
                        if (settings.get("organizerNotifications")) {
                            Map<String, String> tokens = (Map<String, String>) snapshot.get("fcmTokens");
                            if (tokens != null) for (String token : tokens.values()) {
                                NotificationHelper.sendPush(context, token, this.eventName, this.message);
                            }
                        }
                    });
        });
    }
}