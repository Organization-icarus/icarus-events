package com.icarus.events;

import java.util.Date;

/**
 * Class for storing event information
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

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getCapacity() {
        return capacity;
    }

    public void setCapacity(Double capacity) {
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }
}
