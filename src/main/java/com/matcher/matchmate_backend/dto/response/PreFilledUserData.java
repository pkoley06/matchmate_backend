package com.matcher.matchmate_backend.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreFilledUserData {
    private String fullName;
    private String email;
    private String avatarUrl;
}
