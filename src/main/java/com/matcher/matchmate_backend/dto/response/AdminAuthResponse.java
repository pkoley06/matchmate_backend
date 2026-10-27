package com.matcher.matchmate_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuthResponse {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private Long adminId;
    private String username;
    private String displayName;
}
