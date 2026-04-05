package com.icarus.events;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

/* This code was created with the help of Claude on March 26.
The prompt was, "Based on EventDetails.java, I need to create a comment class." */

/**
 * Model representing a comment associated with an event.
 * <p>
 * Each comment is stored as a document within an event's "comments" subcollection
 * in Firestore.
 * </p>
 *
 * @author Bradley Bravender
 */
public class Comment {
    private String documentId;
    private String authorId;
    private String authorName;
    private String authorImage;
    private String text;
    private Date createdAt;
    private boolean deleted;

    /**
     * No-argument constructor required for Firestore deserialization.
     */
    public Comment() {}

    /**
     * Creates a new Comment instance.
     *
     * @param authorId     unique identifier of the comment author
     * @param authorName   display name of the comment author
     * @param authorImage  URL or reference to the author's profile image
     * @param text         comment content
     * @param createdAt    timestamp indicating when the comment was created
     * @param isDeleted    whether the comment is marked as deleted
     */
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
        this.deleted = isDeleted;
    }

    /**
     * Returns the Firestore document ID for this comment.
     *
     * @return document ID
     */
    public String getDocumentId() { return documentId; }

    /**
     * Sets the Firestore document ID for this comment.
     *
     * @param documentId document ID
     */
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    /**
     * Returns the comment text.
     *
     * @return comment content
     */
    public String getText()       { return text; }

    /**
     * Returns the author's unique identifier.
     *
     * @return author ID
     */
    public String getAuthorId()   { return authorId; }

    /**
     * Returns the author's display name.
     *
     * @return author name
     */
    public String getAuthorName() { return authorName; }

    /**
     * Returns the timestamp when the comment was created.
     *
     * @return creation date
     */
    public Date getCreatedAt()    { return createdAt; }

    /**
     * Indicates whether the comment is marked as deleted.
     *
     * @return true if deleted, false otherwise
     */
    public boolean isDeleted() { return deleted; }

    /**
     * Marks the comment as deleted or not.
     *
     * @param deleted true to mark as deleted, false otherwise
     */
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    /**
     * Returns the author's profile image reference.
     *
     * @return author image URL or identifier
     */
    public String getAuthorImage() {return this.authorImage;}
}
