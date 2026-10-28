package com.matcher.matchmate_backend.dto.request;

import com.matcher.matchmate_backend.model.enums.moderation.ModerationAction;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReportResolveRequest {

    @NotNull(message = "Moderation action is required")
    private ModerationAction action;

    private String notes;
}
