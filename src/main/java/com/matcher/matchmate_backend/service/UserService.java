package com.matcher.matchmate_backend.service;


import com.matcher.matchmate_backend.dto.request.DemographicsRequest;
import com.matcher.matchmate_backend.dto.response.UserDemographicsResponse;
import com.matcher.matchmate_backend.model.entity.AppUser;
import com.matcher.matchmate_backend.repository.AppUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Period;

@Service
public class UserService {

    private final AppUserRepository userRepository;

    public UserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserDemographicsResponse updateDemographics(Long userId, DemographicsRequest request){
        // Enforce Core Product Principle: Age >= 18 validation
        int age = Period.between((request.getDateOfBirth()), LocalDate.now()).getYears();

        if(age < 18){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User must be at least 18 years old");
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setFullName(request.getFullName());
        user.setGender(request.getGender());
        user.setDateOfBirth(request.getDateOfBirth());
        if(request.getAvatarUrl() != null){
            user.setAvatarUrl(request.getAvatarUrl());
        }
        user.setOccupationCategory(request.getOccupationCategory());
        user.setOccupationTitle(request.getOccupationTitle());
        user.setBio(request.getBio());
        user.setOnboardingCompleted(true);

        AppUser updated = userRepository.save(user);

        // Core onboarding completes the bare essentials -> 40% profile strength
        int profileStrength = 40;

        return UserDemographicsResponse.builder()
                .userId(updated.getId())
                .fullName(updated.getFullName())
                .gender(updated.getGender())
                .dateOfBirth(updated.getDateOfBirth())
                .age(age)
                .avatarUrl(updated.getAvatarUrl())
                .occupationCategory(updated.getOccupationCategory())
                .occupationTitle(updated.getOccupationTitle())
                .bio(updated.getBio())
                .profileStrength(profileStrength)
                .isProfileComplete(true)
                .build();

    }
}
