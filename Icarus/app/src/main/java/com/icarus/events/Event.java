package com.icarus.events;

import java.util.Date;

public class Event {
    private String id;
    private String name;
    private double capacity;
    private Date regOpen;
    private Date regClose;
    private Date date;

    public Event(String id, String name, double capacity, Date regOpen, Date regClose, Date date) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.regOpen = regOpen;
        this.regClose = regClose;
        this.date = date;
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
}
