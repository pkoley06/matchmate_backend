package com.matcher.matchmate_backend.controller.admin;

import com.matcher.matchmate_backend.dto.request.AdminBrandingUpdateRequest;
import com.matcher.matchmate_backend.dto.response.AdminBrandingResponse;
import com.matcher.matchmate_backend.service.AdminBrandingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/branding")
public class AdminBrandingController {

    private final AdminBrandingService adminBrandingService;

    public AdminBrandingController(AdminBrandingService adminBrandingService) {
        this.adminBrandingService = adminBrandingService;
    }

    @GetMapping
    public ResponseEntity<AdminBrandingResponse> get() {
        return ResponseEntity.ok(adminBrandingService.getBranding());
    }

    @PutMapping
    public ResponseEntity<AdminBrandingResponse> update(@Valid @RequestBody AdminBrandingUpdateRequest request) {
        return ResponseEntity.ok(adminBrandingService.updateBranding(request));
    }
}
