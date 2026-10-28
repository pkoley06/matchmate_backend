package com.matcher.matchmate_backend.controller;

import com.matcher.matchmate_backend.dto.request.FcmTokenRequest;
import com.matcher.matchmate_backend.dto.request.SearchPreferencesRequest;
import com.matcher.matchmate_backend.dto.response.SearchPreferencesResponse;
import com.matcher.matchmate_backend.security.AuthenticatedUserPrincipal;
import com.matcher.matchmate_backend.service.SearchPreferencesService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users/me")
public class SearchPreferencesController {

    private final SearchPreferencesService searchPreferencesService;

    public SearchPreferencesController(SearchPreferencesService searchPreferencesService) {
        this.searchPreferencesService = searchPreferencesService;
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<Map<String, String>> updateFcmToken(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody FcmTokenRequest request
    ) {
        searchPreferencesService.updateFcmToken(principal.getUserId(), request.getFcmToken());
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "FCM token updated successfully"));
    }

    @GetMapping("/search-preferences")
    public ResponseEntity<SearchPreferencesResponse> getSearchPreferences(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        SearchPreferencesResponse response = searchPreferencesService.getPreferences(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/search-preferences")
    public ResponseEntity<SearchPreferencesResponse> updateSearchPreferences(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody SearchPreferencesRequest request
    ) {
        SearchPreferencesResponse response = searchPreferencesService.updatePreferences(principal.getUserId(), request);
        return ResponseEntity.ok(response);
    }
}
