package com.matcher.matchmate_backend.service;

import com.matcher.matchmate_backend.dto.request.ListingCreateRequest;
import com.matcher.matchmate_backend.dto.response.ListingResponse;
import com.matcher.matchmate_backend.model.entity.AppUser;
import com.matcher.matchmate_backend.model.entity.Listing;
import com.matcher.matchmate_backend.repository.AppUserRepository;
import com.matcher.matchmate_backend.repository.ListingRepository;
import jakarta.transaction.Transactional;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/** Milestone 5 (minimal): create/get/list-mine. No media-upload pipeline yet (see plan notes). */
@Service
public class ListingService {

    /** SRID 4326 = WGS 84, same standard used by every column/query in this codebase. */
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final ListingRepository listingRepository;
    private final AppUserRepository userRepository;

    public ListingService(ListingRepository listingRepository, AppUserRepository userRepository) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ListingResponse create(Long hostUserId, ListingCreateRequest request) {
        AppUser host = userRepository.findById(hostUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Listing listing = Listing.builder()
                .hostUser(host)
                .listingType(request.getListingType())
                .title(request.getTitle())
                .rentAmount(request.getRentAmount())
                .depositAmount(request.getDepositAmount())
                .roomType(request.getRoomType())
                .furnishingStatus(request.getFurnishingStatus())
                .location(toPoint(request.getLatitude(), request.getLongitude()))
                .localityName(request.getLocalityName())
                .city(request.getCity())
                .description(request.getDescription())
                .photoUrls(request.getPhotoUrls() != null ? new ArrayList<>(request.getPhotoUrls()) : new ArrayList<>())
                .moveInDate(request.getMoveInDate())
                .amenities(request.getAmenities() != null ? new ArrayList<>(request.getAmenities()) : new ArrayList<>())
                .openToAllGenders(request.getOpenToAllGenders() != null ? request.getOpenToAllGenders() : true)
                .isAvailable(true)
                .build();

        return toResponse(listingRepository.save(listing));
    }

    public ListingResponse getById(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        return toResponse(listing);
    }

    public List<ListingResponse> getMine(Long hostUserId) {
        return listingRepository.findByHostUserId(hostUserId).stream().map(this::toResponse).toList();
    }

    private Point toPoint(double latitude, double longitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    private ListingResponse toResponse(Listing listing) {
        Point location = listing.getLocation();
        return ListingResponse.builder()
                .id(listing.getId())
                .hostUserId(listing.getHostUser().getId())
                .status(Boolean.TRUE.equals(listing.getIsAvailable()) ? "PUBLISHED" : "UNAVAILABLE")
                .listingType(listing.getListingType())
                .title(listing.getTitle())
                .rentAmount(listing.getRentAmount())
                .depositAmount(listing.getDepositAmount())
                .roomType(listing.getRoomType())
                .furnishingStatus(listing.getFurnishingStatus())
                .latitude(location.getY())
                .longitude(location.getX())
                .localityName(listing.getLocalityName())
                .city(listing.getCity())
                .description(listing.getDescription())
                .photoUrls(listing.getPhotoUrls())
                .moveInDate(listing.getMoveInDate())
                .amenities(listing.getAmenities())
                .openToAllGenders(listing.getOpenToAllGenders())
                .isAvailable(listing.getIsAvailable())
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .build();
    }
}
