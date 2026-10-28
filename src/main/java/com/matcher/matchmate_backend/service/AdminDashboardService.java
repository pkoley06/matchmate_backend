package com.matcher.matchmate_backend.service;

import com.matcher.matchmate_backend.dto.response.AdminDashboardStatsResponse;
import com.matcher.matchmate_backend.dto.response.AdminListingSummaryResponse;
import com.matcher.matchmate_backend.dto.response.AdminUserSummaryResponse;
import com.matcher.matchmate_backend.model.entity.AppUser;
import com.matcher.matchmate_backend.model.entity.Listing;
import com.matcher.matchmate_backend.repository.AppUserRepository;
import com.matcher.matchmate_backend.repository.ListingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
public class AdminDashboardService {

    private final AppUserRepository userRepository;
    private final ListingRepository listingRepository;

    public AdminDashboardService(AppUserRepository userRepository, ListingRepository listingRepository) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
    }

    public AdminDashboardStatsResponse getStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();
        long completedOnboardingUsers = userRepository.countByOnboardingCompletedTrue();

        long totalListings = listingRepository.count();
        long availableListings = listingRepository.countByIsAvailableTrue();

        List<AdminUserSummaryResponse> recentUsers = userRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toUserSummary)
                .toList();

        List<AdminListingSummaryResponse> recentListings = listingRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toListingSummary)
                .toList();

        return AdminDashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .completedOnboardingUsers(completedOnboardingUsers)
                .totalListings(totalListings)
                .availableListings(availableListings)
                .recentUsers(recentUsers)
                .recentListings(recentListings)
                .build();
    }

    private AdminUserSummaryResponse toUserSummary(AppUser user) {
        return AdminUserSummaryResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .gender(user.getGender())
                .age(user.getDateOfBirth() != null ? Period.between(user.getDateOfBirth(), LocalDate.now()).getYears() : null)
                .lastKnownLocality(user.getLastKnownLocality())
                .primaryAuthProvider(user.getPrimaryAuthProvider())
                .isActive(user.getIsActive())
                .onboardingCompleted(user.getOnboardingCompleted())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AdminListingSummaryResponse toListingSummary(Listing listing) {
        return AdminListingSummaryResponse.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .hostUserId(listing.getHostUser().getId())
                .hostUserName(listing.getHostUser().getFullName())
                .listingType(listing.getListingType())
                .roomType(listing.getRoomType())
                .furnishingStatus(listing.getFurnishingStatus())
                .rentAmount(listing.getRentAmount())
                .depositAmount(listing.getDepositAmount())
                .localityName(listing.getLocalityName())
                .city(listing.getCity())
                .isAvailable(listing.getIsAvailable())
                .photoUrls(listing.getPhotoUrls())
                .createdAt(listing.getCreatedAt())
                .build();
    }
}
