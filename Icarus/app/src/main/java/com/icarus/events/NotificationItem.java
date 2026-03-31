package com.icarus.events;

/**
 * Simple model class for notifications.
 */
public class NotificationItem {
    private final String message;
    private final String type;

    public NotificationItem(String message, String type) {
        this.message = message;
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }
}