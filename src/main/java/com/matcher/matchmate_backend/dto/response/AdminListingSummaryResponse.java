package com.matcher.matchmate_backend.dto.response;

import com.matcher.matchmate_backend.model.enums.listing.FurnishingStatus;
import com.matcher.matchmate_backend.model.enums.listing.GenderPreference;
import com.matcher.matchmate_backend.model.enums.listing.ListingType;
import com.matcher.matchmate_backend.model.enums.listing.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/** Summary row for GET /api/v1/admin/listings. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminListingSummaryResponse {
    private Long id;
    private String title;
    private Long hostUserId;
    private String hostUserName;
    private ListingType listingType;
    private RoomType roomType;
    private FurnishingStatus furnishingStatus;
    private Integer rentAmount;
    private Integer depositAmount;
    private String localityName;
    private String city;
    private String stateName;
    private String postalCode;
    private GenderPreference genderPreference;
    private Boolean isAvailable;
    private List<String> photoUrls;
    private OffsetDateTime createdAt;
}
