package com.matcher.matchmate_backend.controller;


import com.matcher.matchmate_backend.dto.request.DemographicsRequest;
import com.matcher.matchmate_backend.dto.request.LifestyleRequest;
import com.matcher.matchmate_backend.dto.response.OtherUserProfileResponse;
import com.matcher.matchmate_backend.dto.response.UserDemographicsResponse;
import com.matcher.matchmate_backend.dto.response.UserProfileResponse;
import com.matcher.matchmate_backend.security.AuthenticatedUserPrincipal;
import com.matcher.matchmate_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/me/demographics")
    public ResponseEntity<UserDemographicsResponse> updateDemographics(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody DemographicsRequest request
            ){
        UserDemographicsResponse response = userService.updateDemographics(principal.getUserId(), request);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/lifestyle")
    public ResponseEntity<UserProfileResponse> updateLifestyle(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestBody LifestyleRequest request
    ) {
        return ResponseEntity.ok(userService.updateLifestyle(principal.getUserId(), request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(userService.getMyProfile(principal.getUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OtherUserProfileResponse> getOtherUserProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getOtherUserProfile(id));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        userService.deleteMyAccount(principal.getUserId());
        return ResponseEntity.noContent().build();
    }

}
