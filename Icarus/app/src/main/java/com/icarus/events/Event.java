package com.icarus.events;

import java.util.Date;

/**
 * Class for storing event information
 *
 * @author Benjamin Hall
 */
public class Event {
    // TODO: somehow store event poster?
    private String id;
    private String name;
    private String category;  // "Music", "Sports", etc
    private double capacity;
    private Date regOpen; // When the registration opens
    private Date regClose; // When the registration closes
    private Date date; // When the event starts
    private String user_status; // waitlist, selected, registered, or rejected
    private int waiting_list_size;
    private String location;

    public Event(String id, String name, double capacity, Date regOpen, Date regClose, Date date) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.regOpen = regOpen;
        this.regClose = regClose;
        this.date = date;
    }

    //Added for testing category implementation in EntrantEventListActivity
    public Event(String id, String name, String category, double capacity, Date regOpen, Date regClose, Date date) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.capacity = capacity;
        this.regOpen = regOpen;
        this.regClose = regClose;
        this.date = date;
    }

    public String getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getCapacity() {
        return capacity;
    }
    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public Date getRegOpen() {
        return regOpen;
    }
    public void setRegOpen(Date regOpen) {
        this.regOpen = regOpen;
    }

    public Date getRegClose() {
        return regClose;
    }
    public void setRegClose(Date regClose) {
        this.regClose = regClose;
    }

    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }

    public String getUser_status() { return user_status; }
    public void setUser_status(String user_status) { this.user_status = user_status; }

    public int getWaiting_list_size() { return waiting_list_size; }
    public void setWaiting_list_size(int waiting_list_size) { this.waiting_list_size = waiting_list_size; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
