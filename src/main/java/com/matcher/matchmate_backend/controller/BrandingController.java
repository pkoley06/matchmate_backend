package com.matcher.matchmate_backend.controller;

import com.matcher.matchmate_backend.dto.response.AdminBrandingResponse;
import com.matcher.matchmate_backend.service.AdminBrandingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/branding")
public class BrandingController {

    private final AdminBrandingService adminBrandingService;

    public BrandingController(AdminBrandingService adminBrandingService) {
        this.adminBrandingService = adminBrandingService;
    }

    @GetMapping
    public ResponseEntity<AdminBrandingResponse> get() {
        return ResponseEntity.ok(adminBrandingService.getBranding());
    }
}
