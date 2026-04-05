package com.icarus.events;

import android.content.Context;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

    //    TO DO: add type can only be certain values

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

    public NotificationItem(String id, String eventName, String eventImage, String message) {
        this.id = id;
        this.eventName = eventName;
        this.eventImage = eventImage;
        this.message = message;
        this.db = FirebaseFirestore.getInstance();
    }

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getEventId() {
        return this.eventId;
    }

    public String getEventName() { return eventName; }
    public String getEventImage() { return eventImage; }

    public String getSender() {
        return this.sender;
    }

    public ArrayList<String> getRecipients() {
        return this.recipients;
    }

    public String getMessage() {
        return this.message;
    }

    public boolean isEventNotification() {
        return this.isEvent;
    }

    public boolean isSystemNotification() {
        return this.isSystem;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setEvent() {
        this.isEvent = true;
        this.isSystem = false;
    }

    public void setSystem() {
        this.isSystem = true;
        this.isEvent = false;
    }

    public void setRecipients(ArrayList<String> users) {
        this.recipients = new ArrayList<String>(users);
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void addRecipient(String userId) {
        if (!this.recipients.contains(userId)) {
            this.recipients.add(userId);
        }
    }

    public void removeRecipient(String userId) {
        this.recipients.remove(userId);
    }

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
                        Map<String, String> tokens = (Map<String, String>) snapshot.get("fcmTokens");
                        if (tokens != null) for (String token : tokens.values()) {
                            NotificationHelper.sendPush(context, token, this.eventName, this.message);
                        }
                    });
        });
    }
}