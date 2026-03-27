package com.icarus.events;

import java.util.Date;

/* This code was created with the help of Claude on March 26.
The prompt was, "Based on EventDetails.java, I need to create a comment class." */

/**
 * Represents a single comment on an Event.
 * Author: Bradley Bravender
 */
public class Comment {
    private final String eventId;
    private final String text;
    private final String authorId;
    private final String authorName;
    private final Date timestamp;

    public Comment(String id, String text, String authorId, String authorName, Date timestamp) {
        this.eventId = id;
        this.text = text;
        this.authorId = authorId;
        this.authorName = authorName;
        this.timestamp = timestamp;
    }

    public String getId()         { return eventId; }
    public String getText()       { return text; }
    public String getAuthorId()   { return authorId; }
    public String getAuthorName() { return authorName; }
    public Date getTimestamp()    { return timestamp; }
}