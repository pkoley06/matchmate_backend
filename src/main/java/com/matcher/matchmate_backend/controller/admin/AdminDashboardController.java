package com.matcher.matchmate_backend.controller.admin;

import com.matcher.matchmate_backend.dto.response.AdminDashboardStatsResponse;
import com.matcher.matchmate_backend.service.AdminDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardStatsResponse> getStats() {
        return ResponseEntity.ok(adminDashboardService.getStats());
    }
}
