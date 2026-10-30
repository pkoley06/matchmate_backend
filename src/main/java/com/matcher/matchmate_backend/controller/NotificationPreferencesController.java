package com.matcher.matchmate_backend.controller;

import com.matcher.matchmate_backend.dto.request.NotificationPreferencesRequest;
import com.matcher.matchmate_backend.dto.response.NotificationPreferencesResponse;
import com.matcher.matchmate_backend.security.AuthenticatedUserPrincipal;
import com.matcher.matchmate_backend.service.NotificationPreferencesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Settings → Notification settings: the four push toggles. */
@RestController
@RequestMapping("/api/v1/users/me/notification-preferences")
public class NotificationPreferencesController {

    private final NotificationPreferencesService preferencesService;

    public NotificationPreferencesController(NotificationPreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    @GetMapping
    public ResponseEntity<NotificationPreferencesResponse> get(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(preferencesService.get(principal.getUserId()));
    }

    @PatchMapping
    public ResponseEntity<NotificationPreferencesResponse> update(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestBody NotificationPreferencesRequest request
    ) {
        return ResponseEntity.ok(preferencesService.update(principal.getUserId(), request));
    }
}
