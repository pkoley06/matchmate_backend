package com.matcher.matchmate_backend.service;

import com.matcher.matchmate_backend.dto.request.SearchPreferencesRequest;
import com.matcher.matchmate_backend.dto.response.SearchPreferencesResponse;
import com.matcher.matchmate_backend.model.entity.AppUser;
import com.matcher.matchmate_backend.repository.AppUserRepository;
import jakarta.transaction.Transactional;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SearchPreferencesService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final AppUserRepository userRepository;
    private final NotificationService notificationService;

    public SearchPreferencesService(AppUserRepository userRepository, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void updateFcmToken(Long userId, String token) {
        AppUser user = getUser(userId);
        user.setFcmToken(token);
        userRepository.save(user);
    }

    public SearchPreferencesResponse getPreferences(Long userId) {
        AppUser user = getUser(userId);
        Point loc = user.getTargetSearchLocation() != null ? user.getTargetSearchLocation() : user.getLastKnownLocation();

        return SearchPreferencesResponse.builder()
                .latitude(loc != null ? loc.getY() : 22.5726)
                .longitude(loc != null ? loc.getX() : 88.3639)
                .locality(user.getTargetSearchLocality() != null ? user.getTargetSearchLocality() : user.getLastKnownLocality())
                .searchRadiusKm(user.getSearchRadiusKm() != null ? user.getSearchRadiusKm() : 10.0)
                .isLookingForRoom(user.getIsLookingForRoom() != null ? user.getIsLookingForRoom() : true)
                .notifyRoomFinder(user.getNotifyRoomFinder() != null ? user.getNotifyRoomFinder() : true)
                .notifyRoommateFinder(user.getNotifyRoommateFinder() != null ? user.getNotifyRoommateFinder() : true)
                .build();
    }

    @Transactional
    public SearchPreferencesResponse updatePreferences(Long userId, SearchPreferencesRequest request) {
        AppUser user = getUser(userId);

        Point targetPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(request.getLongitude(), request.getLatitude()));
        user.setTargetSearchLocation(targetPoint);
        if (request.getLocality() != null && !request.getLocality().isBlank()) {
            user.setTargetSearchLocality(request.getLocality().trim());
        }
        if (request.getSearchRadiusKm() != null) {
            user.setSearchRadiusKm(request.getSearchRadiusKm());
        }
        if (request.getIsLookingForRoom() != null) {
            user.setIsLookingForRoom(request.getIsLookingForRoom());
        }
        if (request.getNotifyRoomFinder() != null) {
            user.setNotifyRoomFinder(request.getNotifyRoomFinder());
        }
        if (request.getNotifyRoommateFinder() != null) {
            user.setNotifyRoommateFinder(request.getNotifyRoommateFinder());
        }

        AppUser saved = userRepository.save(user);

        // If the user is actively searching, notify nearby active listing hosts about the new matching seeker!
        if (Boolean.TRUE.equals(saved.getIsLookingForRoom())) {
            notificationService.notifyHostsForNewSeeker(saved);
        }

        return getPreferences(saved.getId());
    }

    private AppUser getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
