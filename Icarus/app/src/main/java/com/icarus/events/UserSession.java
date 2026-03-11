package com.icarus.events;

public class UserSession {
    private static UserSession instance;
    private User currentUser;
    // Private constructor to prevent instantiation from other classes
    private UserSession() {}
    public static UserSession getInstance() {
        // If this is called, will return the already generated instance from that app session
        // Singleton Class
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }
    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User user) { this.currentUser = user; }
}
