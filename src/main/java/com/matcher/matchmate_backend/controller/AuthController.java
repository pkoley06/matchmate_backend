package com.matcher.matchmate_backend.controller;


import com.matcher.matchmate_backend.dto.request.TokenVerifyRequest;
import com.matcher.matchmate_backend.dto.response.AuthTokenResponse;
import com.matcher.matchmate_backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/verify-token")
    public ResponseEntity<AuthTokenResponse> verifyToken(@Valid @RequestBody TokenVerifyRequest request){
        AuthTokenResponse response = authService.verifyAndAuthenticate(request);
        return ResponseEntity.ok(response);
    }
}
