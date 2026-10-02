package com.matcher.matchmate_backend.dto.request;


import com.matcher.matchmate_backend.model.enums.auth.PrimaryAuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class TokenVerifyRequest {

    @NotBlank(message = "Firebase ID token is required")
    private String idToken;

    @NotNull(message = "Primary auth provider is required")
    private PrimaryAuthProvider provider;

}
