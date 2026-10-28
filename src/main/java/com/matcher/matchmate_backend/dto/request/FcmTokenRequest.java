package com.matcher.matchmate_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FcmTokenRequest {

    @NotBlank(message = "fcmToken must not be blank")
    private String fcmToken;
}
