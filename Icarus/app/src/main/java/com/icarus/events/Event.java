package com.icarus.events;

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
    private Date date;
    private String location;
    private String image;
    private String organizer;

    /**
     * Creates a new Event object.
     *
     * @param id the unique identifier of the event
     * @param name the name of the event
     * @param category the category of the event (e.g., Sports, Music, Education)
     * @param capacity the maximum number of entrants allowed
     * @param regOpen the date when registration opens
     * @param regClose the date when registration closes
     * @param date the date the event takes place
     * @param location the location where the event is held
     * @param image the image associated with the event
     * @param organizer the identifier of the event organizer
     */
    public Event(String id, String name, String category, Double capacity, Date regOpen,
                 Date regClose, Date date, String location, String image, String organizer) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.capacity = capacity;
        this.regOpen = regOpen;
        this.regClose = regClose;
        this.date = date;
        this.location = location;
        this.image = image;
        this.organizer = organizer;
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
     * Returns the date of the event.
     *
     * @return the event date
     */
    public Date getDate() {
        return date;
    }

    /**
     * Sets the date of the event.
     *
     * @param date the new event date
     */
    public void setDate(Date date) {
        this.date = date;
    }

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
     * @return the event organizer's id
     */
    public String getOrganizer() {
        return organizer;
    }

    /**
     * Sets the id of the organizer of the event.
     *
     * @param organizer the new event organizer's id
     */
    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }
}
