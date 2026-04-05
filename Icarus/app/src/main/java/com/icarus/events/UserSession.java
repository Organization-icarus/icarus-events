package com.icarus.events;

/**
 * Singleton class for storing the currently logged-in user across the app session.
 * <p>
 * Provides a global access point to the {@link User} object. Use
 * {@link #getInstance()} to retrieve the singleton instance.
 *
 * @author Alex Alves
 */
public class UserSession {
    private static UserSession instance;
    private User currentUser;


    /**
     * Private constructor to prevent external instantiation.
     */
    private UserSession() {}

    /**
     * Returns the singleton instance of {@link UserSession}.
     *
     * @return the global {@link UserSession} instance
     */
    public static UserSession getInstance() {
        // If this is called, will return the already generated instance from that app session
        // Singleton Class
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    /**
     * Returns the current user stored in this session.
     *
     * @return the current {@link User}, or null if no user is logged in
     */
    public User getCurrentUser() { return currentUser; }

    /**
     * Sets the current user for this session.
     *
     * @param user the {@link User} to store
     */
    public void setCurrentUser(User user) { this.currentUser = user; }

    /**
     * Clears the current user from the session.
     */
    public void clear() { currentUser = null; }
}
