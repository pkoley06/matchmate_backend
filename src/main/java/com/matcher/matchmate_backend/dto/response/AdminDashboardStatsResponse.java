package com.matcher.matchmate_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsResponse {
    private long totalUsers;
    private long activeUsers;
    private long completedOnboardingUsers;
    private long totalListings;
    private long availableListings;
    private List<AdminUserSummaryResponse> recentUsers;
    private List<AdminListingSummaryResponse> recentListings;
}
