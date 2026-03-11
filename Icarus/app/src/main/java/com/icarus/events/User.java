package com.icarus.events;

import java.util.Date;

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
    private Date birthday;

    public User(String id, String name, String email, String phone, Date birthday) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.birthday = birthday;
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

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }
}
