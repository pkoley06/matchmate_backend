package com.matcher.matchmate_backend.service;

import com.matcher.matchmate_backend.dto.request.AdminLoginRequest;
import com.matcher.matchmate_backend.dto.response.AdminAuthResponse;
import com.matcher.matchmate_backend.model.entity.AdminUser;
import com.matcher.matchmate_backend.repository.AdminUserRepository;
import com.matcher.matchmate_backend.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AdminAuthService(AdminUserRepository adminUserRepository,
                             PasswordEncoder passwordEncoder,
                             JwtTokenProvider jwtTokenProvider) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AdminAuthResponse login(AdminLoginRequest request) {
        AdminUser admin = adminUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        String token = jwtTokenProvider.generateAdminToken(admin.getId(), admin.getUsername());

        return AdminAuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .adminId(admin.getId())
                .username(admin.getUsername())
                .displayName(admin.getDisplayName())
                .build();
    }
}
