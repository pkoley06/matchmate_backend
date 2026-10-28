package com.matcher.matchmate_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingCommentResponse {
    private Long id;
    private Long listingId;
    private Long userId;
    private String authorName;
    private String authorAvatarUrl;
    private String content;
    private OffsetDateTime createdAt;
}
