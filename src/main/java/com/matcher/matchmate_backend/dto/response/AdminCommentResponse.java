package com.matcher.matchmate_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** One row of the admin comment moderation log (GET /api/v1/admin/comments). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCommentResponse {
    private Long id;
    private Long listingId;
    private String listingTitle;
    private String listingLocality;
    private Long listingHostUserId;
    private Long authorUserId;
    private String authorName;
    private String authorAvatarUrl;
    private Boolean authorSuspended;
    private String content;
    private long likesCount;
    private OffsetDateTime createdAt;
}
