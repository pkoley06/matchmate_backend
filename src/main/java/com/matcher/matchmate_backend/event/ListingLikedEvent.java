package com.matcher.matchmate_backend.event;

/** A user liked (not un-liked) a room; the host may be told, subject to their preferences. */
public record ListingLikedEvent(
        Long listingId,
        String listingTitle,
        String listingLocality,
        Long hostUserId,
        Long likerUserId,
        String likerName
) {
    public boolean likedByHost() {
        return hostUserId != null && hostUserId.equals(likerUserId);
    }
}
