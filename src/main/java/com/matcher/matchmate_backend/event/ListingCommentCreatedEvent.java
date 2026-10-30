package com.matcher.matchmate_backend.event;

/**
 * Scalar snapshot of a freshly posted comment, published inside the write transaction and
 * consumed after commit to push a notification to the room's host. Carries no entities so
 * the listener never touches a lazy proxy outside a session.
 */
public record ListingCommentCreatedEvent(
        Long listingId,
        String listingTitle,
        String listingLocality,
        Long hostUserId,
        Long authorUserId,
        String authorName,
        String content
) {
    public boolean authoredByHost() {
        return hostUserId != null && hostUserId.equals(authorUserId);
    }
}
