package com.matcher.matchmate_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** GET /api/v1/discovery response envelope — see "MatchMate Technical Architecture.md" §6.4. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveryFeedResponse {
    private List<DiscoveryCardResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    // Explicit @JsonProperty: Jackson would otherwise strip the "is" prefix from the
    // isLast() getter and serialize this as "last", not the "isLast" the API contract specifies.
    @JsonProperty("isLast")
    private boolean isLast;
}
