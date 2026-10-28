package com.matcher.matchmate_backend.service;


import com.matcher.matchmate_backend.dto.request.DemographicsRequest;
import com.matcher.matchmate_backend.dto.request.LifestyleRequest;
import com.matcher.matchmate_backend.dto.response.ListingResponse;
import com.matcher.matchmate_backend.dto.response.OtherUserProfileResponse;
import com.matcher.matchmate_backend.dto.response.UserDemographicsResponse;
import com.matcher.matchmate_backend.dto.response.UserProfileResponse;
import com.matcher.matchmate_backend.model.entity.AppUser;
import com.matcher.matchmate_backend.model.entity.Listing;
import com.matcher.matchmate_backend.repository.AppUserRepository;
import com.matcher.matchmate_backend.repository.ListingRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    /**
     * Bare-essentials onboarding alone is worth 40%; each of the five lifestyle
     * questions is worth an equal share of the remaining 60%, so a fully answered
     * profile reaches 100%.
     */
    private static final int BASE_PROFILE_STRENGTH = 40;
    private static final int POINTS_PER_LIFESTYLE_ANSWER = 12;

    private final AppUserRepository userRepository;
    private final ListingRepository listingRepository;
    private final ListingService listingService;

    public UserService(
            AppUserRepository userRepository,
            ListingRepository listingRepository,
            ListingService listingService
    ) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.listingService = listingService;
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
                .profileStrength(computeProfileStrength(updated))
                .isProfileComplete(true)
                .build();

    }

    /**
     * Progressive lifestyle vector update (Milestone 3 Step 3.2): applies only the
     * non-null fields sent, so each lifestyle card can be answered independently
     * without resetting the others.
     */
    @Transactional
    public UserProfileResponse updateLifestyle(Long userId, LifestyleRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.getSmokingHabit() != null) user.setSmokingHabit(request.getSmokingHabit());
        if (request.getDietaryPreference() != null) user.setDietaryPreference(request.getDietaryPreference());
        if (request.getSleepSchedule() != null) user.setSleepSchedule(request.getSleepSchedule());
        if (request.getCleanlinessLevel() != null) user.setCleanlinessLevel(request.getCleanlinessLevel());
        if (request.getGuestTolerance() != null) user.setGuestTolerance(request.getGuestTolerance());

        AppUser updated = userRepository.save(user);
        return toProfileResponse(updated);
    }

    /** GET /api/v1/users/me — full profile, used by the Profile strength screen. */
    public UserProfileResponse getMyProfile(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toProfileResponse(user);
    }

    /** GET /api/v1/users/{id} — other user's public profile and their active listings. */
    public OtherUserProfileResponse getOtherUserProfile(Long targetUserId) {
        AppUser user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!Boolean.TRUE.equals(user.getIsActive()) || Boolean.TRUE.equals(user.getIsSuspended())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile is unavailable");
        }

        int age = Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();

        List<Listing> listings = listingRepository.findByHostUserId(targetUserId)
                .stream()
                .filter(l -> Boolean.TRUE.equals(l.getIsAvailable()))
                .toList();

        List<ListingResponse> listingResponses = listings.stream()
                .map(listingService::toResponse)
                .toList();

        return OtherUserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .gender(user.getGender())
                .age(age)
                .avatarUrl(user.getAvatarUrl())
                .occupationCategory(user.getOccupationCategory())
                .occupationTitle(user.getOccupationTitle())
                .bio(user.getBio())
                .smokingHabit(user.getSmokingHabit())
                .dietaryPreference(user.getDietaryPreference())
                .sleepSchedule(user.getSleepSchedule())
                .cleanlinessLevel(user.getCleanlinessLevel())
                .guestTolerance(user.getGuestTolerance())
                .locality(user.getLastKnownLocality())
                .activeListingsCount(listingResponses.size())
                .activeListings(listingResponses)
                .build();
    }

    /** DELETE /api/v1/users/me — self account deletion. Deactivates user and delists all listings. */
    @Transactional
    public void deleteMyAccount(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setIsActive(false);
        user.setFullName("Deleted User");
        user.setBio(null);
        user.setAvatarUrl(null);
        userRepository.save(user);

        List<Listing> userListings = listingRepository.findByHostUserId(userId);
        for (Listing listing : userListings) {
            listing.setIsAvailable(false);
        }
        listingRepository.saveAll(userListings);
    }

    private UserProfileResponse toProfileResponse(AppUser user) {
        int age = Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();

        return UserProfileResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .age(age)
                .avatarUrl(user.getAvatarUrl())
                .occupationCategory(user.getOccupationCategory())
                .occupationTitle(user.getOccupationTitle())
                .bio(user.getBio())
                .smokingHabit(user.getSmokingHabit())
                .dietaryPreference(user.getDietaryPreference())
                .sleepSchedule(user.getSleepSchedule())
                .cleanlinessLevel(user.getCleanlinessLevel())
                .guestTolerance(user.getGuestTolerance())
                .profileStrength(computeProfileStrength(user))
                .isProfileComplete(user.getOnboardingCompleted())
                .missingLifestyleFields(missingLifestyleFields(user))
                .build();
    }

    private int computeProfileStrength(AppUser user) {
        if (!Boolean.TRUE.equals(user.getOnboardingCompleted())) return 0;
        int answered = 5 - missingLifestyleFields(user).size();
        return Math.min(100, BASE_PROFILE_STRENGTH + answered * POINTS_PER_LIFESTYLE_ANSWER);
    }

    private List<String> missingLifestyleFields(AppUser user) {
        List<String> missing = new ArrayList<>();
        if (user.getSmokingHabit() == null) missing.add("smokingHabit");
        if (user.getDietaryPreference() == null) missing.add("dietaryPreference");
        if (user.getSleepSchedule() == null) missing.add("sleepSchedule");
        if (user.getCleanlinessLevel() == null) missing.add("cleanlinessLevel");
        if (user.getGuestTolerance() == null) missing.add("guestTolerance");
        return missing;
    }
}
