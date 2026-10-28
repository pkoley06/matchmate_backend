package com.matcher.matchmate_backend.dto.request;

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
public class QuestionnaireWeightsUpdateRequest {

    @NotNull(message = "Cleanliness weight is required")
    @DecimalMin(value = "0.0", message = "Weight cannot be negative")
    private Double cleanlinessWeight;

    @NotNull(message = "Sleep weight is required")
    @DecimalMin(value = "0.0", message = "Weight cannot be negative")
    private Double sleepWeight;

    @NotNull(message = "Guest weight is required")
    @DecimalMin(value = "0.0", message = "Weight cannot be negative")
    private Double guestWeight;

    @NotNull(message = "Diet weight is required")
    @DecimalMin(value = "0.0", message = "Weight cannot be negative")
    private Double dietWeight;

    @Builder.Default
    private Boolean smokingDealbreaker = true;
}
