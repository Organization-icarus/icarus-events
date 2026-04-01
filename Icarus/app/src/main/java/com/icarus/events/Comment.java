package com.icarus.events;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

/* This code was created with the help of Claude on March 26.
The prompt was, "Based on EventDetails.java, I need to create a comment class." */

/**
 * Represents a single comment on an Event.
 * Author: Bradley Bravender
 */
public class Comment {
    private String documentId;
    private String authorId;
    private String authorName;
    private String authorImage;
    private String text;
    private Date createdAt;
    private boolean deleted;

    // Required by Firestore
    public Comment() {}

    public Comment(
            String authorId,
            String authorName,
            String authorImage,
            String text,
            Date createdAt,
            boolean isDeleted
    ) {
        this.text = text;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorImage = authorImage;
        this.createdAt = createdAt;
        this.deleted = deleted;
    }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getText()       { return text; }
    public String getAuthorId()   { return authorId; }
    public String getAuthorName() { return authorName; }
    public Date getCreatedAt()    { return createdAt; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public String getAuthorImage() {return this.authorImage;}
}

// Each Event document will store a comments subcollection, which holds comment documents.