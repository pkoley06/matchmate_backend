package com.matcher.matchmate_backend.dto.response;

import com.matcher.matchmate_backend.model.enums.moderation.ModerationAction;
import com.matcher.matchmate_backend.model.enums.moderation.ReportReason;
import com.matcher.matchmate_backend.model.enums.moderation.ReportStatus;
import com.matcher.matchmate_backend.model.enums.moderation.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private Long id;
    private Long reporterId;
    private String reporterName;
    private Long reportedUserId;
    private String reportedUserName;
    private Long reportedListingId;
    private String reportedListingTitle;
    private ReportType reportType;
    private ReportReason reason;
    private String details;
    private ReportStatus status;
    private ModerationAction actionTaken;
    private OffsetDateTime createdAt;
    private OffsetDateTime resolvedAt;
}
