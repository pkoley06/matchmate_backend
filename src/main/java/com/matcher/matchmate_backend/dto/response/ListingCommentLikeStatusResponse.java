package com.matcher.matchmate_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** POST /api/v1/listings/comments/{id}/like */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingCommentLikeStatusResponse {
    private Long commentId;
    private boolean likedByMe;
    private long likesCount;
}
