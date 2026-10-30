package com.matcher.matchmate_backend.event;

import com.matcher.matchmate_backend.dto.response.ListingCommentEvent;

/**
 * Published inside the comment transaction; relayed to STOMP subscribers after commit so
 * clients never see a comment that was rolled back.
 */
public record ListingCommentChangedEvent(ListingCommentEvent payload) {

    public Long listingId() {
        return payload.getListingId();
    }
}
