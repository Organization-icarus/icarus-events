package com.icarus.events;

import java.util.Date;

/* This code was created with the help of Claude on March 26.
The prompt was, "Based on EventDetails.java, I need to create a comment class." */

/**
 * Represents a single comment on an Event.
 * Author: Bradley Bravender
 */
public class Comment {
    private String authorId;
    private String authorName;
    private String text;
    private Date createdAt;
    private boolean isDeleted;

    // Required by Firestore
    public Comment() {}

    public Comment(
            String authorId,
            String authorName,
            String text,
            Date createdAt,
            boolean isDeleted
    ) {
        this.text = text;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.isDeleted = isDeleted;
    }

    public String getText()       { return text; }
    public String getAuthorId()   { return authorId; }
    public String getAuthorName() { return authorName; }
    public Date getCreatedAt()    { return createdAt; }
    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
}

// Each Event document will store a comments subcollection, which holds comment documents.