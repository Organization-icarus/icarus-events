package com.icarus.events;

import java.util.ArrayList;
import java.util.Map;

/**
 * Class for storing user information
 *
 * @author Benjamin Hall
 */
public class User {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private ArrayList<String> events;
    private Map<String, Object> settings;

    public User(String id, String name, String email, String phone, String role,
                ArrayList<String> events, Map<String, Object> settings) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.events = events;
        this.settings = settings;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public ArrayList<String> getEvents() {
        return events;
    }

    public void setEvents(ArrayList<String> events) {
        this.events = events;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }
}
