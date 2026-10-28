package com.matcher.matchmate_backend.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchPreferencesRequest {

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;

    private String locality;

    @DecimalMin("1.0")
    @DecimalMax("50.0")
    @Builder.Default
    private Double searchRadiusKm = 10.0;

    @Builder.Default
    private Boolean isLookingForRoom = true;

    @Builder.Default
    private Boolean notifyRoomFinder = true;

    @Builder.Default
    private Boolean notifyRoommateFinder = true;
}
