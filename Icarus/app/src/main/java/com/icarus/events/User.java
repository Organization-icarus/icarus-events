package com.icarus.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a user in the application.
 * <p>
 * Stores user information retrieved from Firebase Firestore, including
 * identifying details, roles, related events, and settings.
 *
 * @author Benjamin Hall
 */
public class User {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String image;
    private Boolean isAdmin;
    private ArrayList<String> events;
    private ArrayList<String> organizedEvents;
    private Map<String, Object> settings;
    private Map<String, String> fcmTokens;

    /**
     * Creates a new User object with the provided information.
     *
     * @param id unique identifier for the user (typically the device ID)
     * @param name user's display name
     * @param email user's email address
     * @param phone user's phone number
     * @param isAdmin role assigned to the user (e.g., entrant, organizer, admin)
     * @param events list of event IDs associated with the user
     * @param settings map of user-specific configuration settings
     * @param fcmTokens map of device tokens
     */
    public User(String id, String name, String email, String phone, String image, Boolean isAdmin,
                ArrayList<String> events, ArrayList<String> organizedEvents,
                Map<String, Object> settings, Map<String, String> fcmTokens) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.image = image;
        this.isAdmin = isAdmin;
        this.events = events;
        this.organizedEvents = organizedEvents;
        this.settings = settings;
        this.fcmTokens = (fcmTokens != null) ? fcmTokens : new HashMap<>();
    }

    /**
     * Returns unique identifier for the user
     *
     * @return the unique id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the name of the user
     *
     * @return the user's name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the user
     *
     * @param name the user's new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the email of the user
     *
     * @return the user's email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email of the user
     *
     * @param email the user's new email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the phone number of the user
     *
     * @return the user's phone number
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the phone number of the user
     *
     * @param phone the user's new phone number
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Returns if the user is an admin
     *
     * @return if the user is an admin
     */
    public Boolean getIsAdmin() {
        return isAdmin;
    }

    /**
     * Sets whether the user is an admin
     *
     * @param isAdmin the user's new admin status
     */
    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    /**
     * Returns the events that the user has registered/attended
     *
     * @return the user's registered/attended events
     */
    public ArrayList<String> getEvents() {
        return events;
    }

    /**
     * Sets the events that the user has registered/attended
     *
     * @param events the user's registered/attended events
     */
    public void setEvents(ArrayList<String> events) {
        this.events = events;
    }

    /**
     * Returns the events that the user has organized
     *
     * @return events the user has organized
     */
    public ArrayList<String> getOrganizedEvents() {
        return organizedEvents;
    }

    /**
     * Sets the events that the user has organized
     *
     * @param organizedEvents events the user has organized
     */
    public void setOrganizedEvents(ArrayList<String> organizedEvents) {
        this.organizedEvents = organizedEvents;
    }

    /**
     * Returns the settings for the user
     *
     * @return the user's settings
     */
    public Map<String, Object> getSettings() {
        return settings;
    }

    /**
     * Sets the settings for the user
     *
     * @param settings the user's new settings
     */
    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }

    /**
     * Returns the profile image for the user
     *
     * @return the user's profile image
     */
    public String getImage() {
        return image;
    }

    /**
     * Sets the profile image for the user
     *
     * @param image the user's new profile image
     */
    public void setImage(String image) {
        this.image = image;
    }

    /**
     * Adds/updates a fcm token for a specific device.
     *
     * @param deviceId the device's unique id number
     * @param token the device's unique fcm token
     */
    public void addFCMToken(String deviceId, String token) { if(token != null) this.fcmTokens.put(deviceId, token); }

    /**
     * Sets the fcm tokens for all the devices for the user
     *
     * @param tokens the user's new fcm tokens (one per device)
     */
    public void setFCMTokens(HashMap<String, String> tokens) { this.fcmTokens = tokens; }

    /**
     * Returns the fcm tokens for the user
     *
     * @return the user's fcm tokens
     */
    public Map<String, String> getFCMTokens() { return fcmTokens; }
}
