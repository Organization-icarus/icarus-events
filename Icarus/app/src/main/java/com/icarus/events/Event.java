package com.icarus.events;

import java.util.ArrayList;
import java.util.Date;

/**
 * Represents an event in the application.
 * <p>
 * Stores event information retrieved from Firebase Firestore, including
 * identifying details, registration dates, location, and organizer.
 *
 * @author Benjamin Hall
 */
public class Event {
    private String id;
    private String name;
    private String category;
    private Double capacity;
    private Date regOpen;
    private Date regClose;
    private Date startDate, endDate;
    private String location;
    private String image;
    private ArrayList<String> organizers;
    private String description;
    private String guidelines;

    private int waiting_list_size;
    private String user_status; // waitlist, selected, registered, or rejected


    /**
     * Creates a new Event object.
     *
     * @param id the unique identifier of the event
     * @param name the name of the event
     * @param category the category of the event (e.g., Sports, Music, Education)
     * @param capacity the maximum number of entrants allowed
     * @param regOpen the date when registration opens
     * @param regClose the date when registration closes
     * @param startDate the date the event starts
     * @param endDate the date the event ends
     * @param location the location where the event is held
     * @param image the image associated with the event
     * @param organizers the identifier of the event organizer
     */
    public Event(String id, String name, String category, Double capacity, Date regOpen,
                 Date regClose, Date startDate, Date endDate, String location, String image, ArrayList<String> organizers) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.capacity = capacity;
        this.regOpen = regOpen;
        this.regClose = regClose;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.image = image;
        this.organizers = organizers;
    }

    /**
     * Creates a new Event object with user-specific waitlist information.
     *
     * @param id               the unique identifier of the event
     * @param name             the name of the event
     * @param category         the category of the event (e.g., Sports, Music, Education)
     * @param capacity         the maximum number of entrants allowed
     * @param regOpen          the date when registration opens
     * @param regClose         the date when registration closes
     * @param startDate        the date the event starts
     * @param endDate          the date the event ends
     * @param location         the location where the event is held
     * @param image            the image associated with the event
     * @param organizers       the identifiers of the event organizers
     * @param user_status      the current user's status for this event
     *                         (e.g., waitlist, selected, registered, or rejected)
     * @param waiting_list_size the current number of entrants on the waitlist
     */
    // A full constructor
    public Event(String id, String name, String category, Double capacity, Date regOpen,
                 Date regClose, Date startDate, Date endDate, String location, String image, ArrayList<String> organizers,
                 String user_status, int waiting_list_size) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.capacity = capacity;
        this.regOpen = regOpen;
        this.regClose = regClose;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.image = image;
        this.organizers = organizers;
        this.user_status = user_status;
        this.waiting_list_size = waiting_list_size;
    }

    /**
     * Returns the unique identifier of the event.
     *
     * @return the event ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the name of the event.
     *
     * @return the event name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the event.
     *
     * @param name the new event name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the category of the event.
     *
     * @return the event category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the category of the event.
     *
     * @param category the new event category
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Returns the capacity of the event.
     *
     * @return the event capacity
     */
    public Double getCapacity() {
        return capacity;
    }

    /**
     * Sets the capacity of the event.
     *
     * @param capacity the new event capacity
     */
    public void setCapacity(Double capacity) {
        this.capacity = capacity;
    }

    /**
     * Returns the date on which registration opens for the event.
     *
     * @return the event registration opening date
     */
    public Date getRegOpen() {
        return regOpen;
    }

    /**
     * Sets the date on which registration opens for the event.
     *
     * @param regOpen the new event registration opening date
     */
    public void setRegOpen(Date regOpen) {
        this.regOpen = regOpen;
    }

    /**
     * Returns the date on which registration closes for the event.
     *
     * @return the event registration closing date
     */
    public Date getRegClose() {
        return regClose;
    }

    /**
     * Sets the date on which registration closes for the event.
     *
     * @param regClose the new event registration closing date
     */
    public void setRegClose(Date regClose) {
        this.regClose = regClose;
    }

    /**
     * Returns the start date of the event.
     *
     * @return the event date
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Sets the start date of the event.
     *
     * @param date the new event date
     */
    public void setStartDate(Date date) {
        this.startDate = date;
    }

    /**
     * Returns the end date of the event.
     *
     * @return the event date
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Sets the end date of the event.
     *
     * @param date the new event date
     */
    public void setEndDate(Date date) {
        this.endDate = date;
    }

    /**
     * Returns the current user's status for this event.
     *
     * @return the user status (e.g., waitlist, selected, registered, or rejected)
     */
    public String getUser_status() { return user_status; }

    /**
     * Sets the current user's status for this event.
     *
     * @param user_status the new user status
     */
    public void setUser_status(String user_status) { this.user_status = user_status; }

    /**
     * Returns the current number of entrants on the waitlist.
     *
     * @return the waiting list size
     */
    public int getWaiting_list_size() { return waiting_list_size; }

    /**
     * Sets the current number of entrants on the waitlist.
     *
     * @param waiting_list_size the new waiting list size
     */
    public void setWaiting_list_size(int waiting_list_size) { this.waiting_list_size = waiting_list_size; }

    /**
     * Returns the location of the event.
     *
     * @return the event location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the location of the event.
     *
     * @param location the new event location
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Returns the reference for an image of the event.
     *
     * @return the event image reference
     */
    public String getImage() {
        return image;
    }

    /**
     * Sets the reference for the image of the event.
     *
     * @param image the new event image reference
     */
    public void setImage(String image) {
        this.image = image;
    }

    /**
     * Returns the id of the organizer of the event.
     *
     * @return the event's organizers id
     */
    public ArrayList<String> getOrganizers() {
        return organizers;
    }

    /**
     * Sets the id of the organizer of the event.
     *
     * @param organizers the new event's organizers ids
     */
    public void setOrganizer(ArrayList<String> organizers) {
        this.organizers = organizers;
    }
}
