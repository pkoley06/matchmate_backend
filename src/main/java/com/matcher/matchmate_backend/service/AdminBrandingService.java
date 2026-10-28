package com.matcher.matchmate_backend.service;

import com.matcher.matchmate_backend.dto.request.AdminBrandingUpdateRequest;
import com.matcher.matchmate_backend.dto.response.AdminBrandingResponse;
import com.matcher.matchmate_backend.model.entity.AppBranding;
import com.matcher.matchmate_backend.repository.AppBrandingRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class AdminBrandingService {

    private final AppBrandingRepository appBrandingRepository;

    public AdminBrandingService(AppBrandingRepository appBrandingRepository) {
        this.appBrandingRepository = appBrandingRepository;
    }

    public AdminBrandingResponse getBranding() {
        AppBranding branding = getOrCreateDefault();
        return toResponse(branding);
    }

    @Transactional
    public AdminBrandingResponse updateBranding(AdminBrandingUpdateRequest request) {
        AppBranding branding = getOrCreateDefault();

        if (request.getAppName() != null) branding.setAppName(request.getAppName());
        if (request.getTagline() != null) branding.setTagline(request.getTagline());
        if (request.getPrimaryColor() != null) branding.setPrimaryColor(request.getPrimaryColor());
        if (request.getLogoUrl() != null) branding.setLogoUrl(request.getLogoUrl());
        if (request.getSplashImageUrl() != null) branding.setSplashImageUrl(request.getSplashImageUrl());
        if (request.getSupportEmail() != null) branding.setSupportEmail(request.getSupportEmail());
        if (request.getTermsUrl() != null) branding.setTermsUrl(request.getTermsUrl());
        if (request.getPrivacyUrl() != null) branding.setPrivacyUrl(request.getPrivacyUrl());
        if (request.getMaintenanceMode() != null) branding.setMaintenanceMode(request.getMaintenanceMode());
        if (request.getGoogleMapsApiKey() != null) branding.setGoogleMapsApiKey(request.getGoogleMapsApiKey());
        if (request.getDeveloperUpiId() != null) branding.setDeveloperUpiId(request.getDeveloperUpiId());
        if (request.getDeveloperQrCodeUrl() != null) branding.setDeveloperQrCodeUrl(request.getDeveloperQrCodeUrl());
        if (request.getDeveloperSupportNote() != null) branding.setDeveloperSupportNote(request.getDeveloperSupportNote());

        return toResponse(appBrandingRepository.save(branding));
    }

    private AppBranding getOrCreateDefault() {
        return appBrandingRepository.findById(1L).orElseGet(() -> {
            AppBranding defaults = AppBranding.builder()
                    .id(1L)
                    .appName("MatchMate")
                    .tagline("Find your ideal flatmate & room")
                    .primaryColor("#6750A4")
                    .supportEmail("support@matchmate.com")
                    .maintenanceMode(false)
                    .developerUpiId("matchmate.dev@upi")
                    .developerSupportNote("Made with ❤️ for flatmates everywhere. If MatchMate helped you find a home, buy the developer a coffee!")
                    .build();
            return appBrandingRepository.save(defaults);
        });
    }

    private AdminBrandingResponse toResponse(AppBranding branding) {
        return AdminBrandingResponse.builder()
                .id(branding.getId())
                .appName(branding.getAppName())
                .tagline(branding.getTagline())
                .primaryColor(branding.getPrimaryColor())
                .logoUrl(branding.getLogoUrl())
                .splashImageUrl(branding.getSplashImageUrl())
                .supportEmail(branding.getSupportEmail())
                .termsUrl(branding.getTermsUrl())
                .privacyUrl(branding.getPrivacyUrl())
                .maintenanceMode(branding.getMaintenanceMode())
                .googleMapsApiKey(branding.getGoogleMapsApiKey())
                .developerUpiId(branding.getDeveloperUpiId())
                .developerQrCodeUrl(branding.getDeveloperQrCodeUrl())
                .developerSupportNote(branding.getDeveloperSupportNote())
                .updatedAt(branding.getUpdatedAt())
                .build();
    }
}
