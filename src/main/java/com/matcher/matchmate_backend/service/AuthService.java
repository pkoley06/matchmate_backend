package com.matcher.matchmate_backend.service;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.matcher.matchmate_backend.dto.request.TokenVerifyRequest;
import com.matcher.matchmate_backend.dto.response.AuthTokenResponse;
import com.matcher.matchmate_backend.dto.response.PreFilledUserData;
import com.matcher.matchmate_backend.model.entity.AppUser;
import com.matcher.matchmate_backend.model.enums.auth.Gender;
import com.matcher.matchmate_backend.repository.AppUserRepository;
import com.matcher.matchmate_backend.security.JwtTokenProvider;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;


@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private  final FirebaseAuth firebaseAuth;
    private final AppUserRepository userRepository;
    private  final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            FirebaseAuth firebaseAuth,
            AppUserRepository userRepository,
            JwtTokenProvider jwtTokenProvider
    ){
        this.firebaseAuth = firebaseAuth;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthTokenResponse verifyAndAuthenticate(TokenVerifyRequest tokenVerifyRequest){
        FirebaseToken decodedToken;

        try {
            decodedToken = firebaseAuth.verifyIdToken(tokenVerifyRequest.getIdToken());
        } catch (Exception e) {
            log.error("Firebase token verification failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired Firebase ID token");
        }

        String firebaseUid = decodedToken.getUid();
        String name = decodedToken.getName() == null ?  "New Roommate" : decodedToken.getName();
        String email = decodedToken.getEmail();
        String picture = decodedToken.getPicture();

        // Check if user already exists in PostgreSQL
        AppUser user = userRepository
                .findByFirebaseUid(firebaseUid)
                .orElseGet(() -> autoProvisionUser(firebaseUid, tokenVerifyRequest, name, picture));

        // Generate custom MatchMate session JWT
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getFirebaseUid());

       // use the real flag, NOT "gender/DOB are not null" (placeholders make that always true)
        boolean isProfileComplete = Boolean.TRUE.equals(user.getOnboardingCompleted());

        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                .userId(user.getId())
                .isProfileComplete(isProfileComplete)
                .preFilledUserData(PreFilledUserData.builder()
                        .fullName(user.getFullName())
                        .avatarUrl(user.getAvatarUrl())
                        .email(email)
                        .build())
                .build();
    }



    private AppUser autoProvisionUser(
            String firebaseUid,
            TokenVerifyRequest tokenVerifyRequest,
            String name,
            String picture) {
        log.info("Auto-provisioning new user with Firebase UID: {}", firebaseUid);

        AppUser newUser = AppUser.builder()
                .firebaseUid(firebaseUid)
                .primaryAuthProvider(tokenVerifyRequest.getProvider())
                .fullName(name)
                .avatarUrl(picture)
        // Placeholders: the DB columns are NOT NULL. They are replaced when the user submits the
        // onboarding form. Until then onboardingCompleted stays false (C4).
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.OTHERS)
                .onboardingCompleted(false)
                .isActive(true)
                .build();

        return userRepository.save(newUser);

    }


}
