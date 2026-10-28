package com.matcher.matchmate_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingLikeStatusResponse {
    private Long listingId;
    private Boolean isLiked;
    private Long likesCount;
}
