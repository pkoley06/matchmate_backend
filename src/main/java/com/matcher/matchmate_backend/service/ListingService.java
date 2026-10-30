package com.matcher.matchmate_backend.service;

import com.matcher.matchmate_backend.dto.request.ListingCreateRequest;
import com.matcher.matchmate_backend.dto.request.ListingUpdateRequest;
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

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Listings owned by app users: create / read / list-mine plus owner-only update and delete.
 * No media-upload pipeline yet (see plan notes).
 */
@Service
public class ListingService {

    /** SRID 4326 = WGS 84, same standard used by every column/query in this codebase. */
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final ListingRepository listingRepository;
    private final AppUserRepository userRepository;
    private final NotificationService notificationService;

    public ListingService(
            ListingRepository listingRepository,
            AppUserRepository userRepository,
            NotificationService notificationService
    ) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
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

        Listing saved = listingRepository.save(listing);
        notificationService.notifyMatchingSeekersForNewListing(saved);
        return toResponse(saved);
    }

    /**
     * Owner-only partial update. Fields left {@code null} in the request keep their stored
     * value, so the client may send the whole pre-filled form or only the delta.
     */
    @Transactional
    public ListingResponse update(Long requesterUserId, Long listingId, ListingUpdateRequest request) {
        Listing listing = findOwnedListing(requesterUserId, listingId);

        if (request.getListingType() != null) listing.setListingType(request.getListingType());
        if (request.getTitle() != null && !request.getTitle().isBlank()) listing.setTitle(request.getTitle().trim());
        if (request.getRentAmount() != null) listing.setRentAmount(request.getRentAmount());
        if (request.getDepositAmount() != null) listing.setDepositAmount(request.getDepositAmount());
        if (request.getRoomType() != null) listing.setRoomType(request.getRoomType());
        if (request.getFurnishingStatus() != null) listing.setFurnishingStatus(request.getFurnishingStatus());
        if (request.getLatitude() != null && request.getLongitude() != null) {
            listing.setLocation(toPoint(request.getLatitude(), request.getLongitude()));
        }
        if (request.getLocalityName() != null && !request.getLocalityName().isBlank()) {
            listing.setLocalityName(request.getLocalityName().trim());
        }
        if (request.getCity() != null && !request.getCity().isBlank()) listing.setCity(request.getCity().trim());
        if (request.getDescription() != null) listing.setDescription(request.getDescription());
        if (request.getPhotoUrls() != null) listing.setPhotoUrls(new ArrayList<>(request.getPhotoUrls()));
        if (request.getMoveInDate() != null) listing.setMoveInDate(request.getMoveInDate());
        if (request.getAmenities() != null) listing.setAmenities(new ArrayList<>(request.getAmenities()));
        if (request.getOpenToAllGenders() != null) listing.setOpenToAllGenders(request.getOpenToAllGenders());
        if (request.getIsAvailable() != null) listing.setIsAvailable(request.getIsAvailable());

        return toResponse(listingRepository.save(listing));
    }

    /** Owner-only hard delete. Likes, comments and reports cascade at the database level (V8). */
    @Transactional
    public void delete(Long requesterUserId, Long listingId) {
        Listing listing = findOwnedListing(requesterUserId, listingId);
        listingRepository.delete(listing);
    }

    @Transactional
    public ListingResponse getById(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        AppUser host = listing.getHostUser();
        if (host != null && (!Boolean.TRUE.equals(host.getIsActive()) || Boolean.TRUE.equals(host.getIsSuspended()))) {
            // Disabled, deleted or suspended hosts are excluded from discovery; keep direct links consistent.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found");
        }
        return toResponse(listing);
    }

    @Transactional
    public List<ListingResponse> getMine(Long hostUserId) {
        return listingRepository.findByHostUserId(hostUserId).stream().map(this::toResponse).toList();
    }

    private Listing findOwnedListing(Long requesterUserId, Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (listing.getHostUser() == null || !listing.getHostUser().getId().equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage your own listings");
        }
        return listing;
    }

    private Point toPoint(double latitude, double longitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    /**
     * Maps an entity to its API shape. Must run inside a transaction (or with the host
     * already fetched) because {@code hostUser} is lazy and open-in-view is disabled.
     */
    public ListingResponse toResponse(Listing listing) {
        Point location = listing.getLocation();
        AppUser host = listing.getHostUser();

        String hostName = null;
        Integer hostAge = null;
        String hostAvatarUrl = null;
        String hostOccupationTitle = null;
        Boolean hostIsSeeker = null;
        if (host != null) {
            hostName = host.getFullName();
            hostAvatarUrl = host.getAvatarUrl();
            hostOccupationTitle = host.getOccupationTitle();
            hostIsSeeker = host.getIsLookingForRoom();
            if (host.getDateOfBirth() != null) {
                hostAge = Period.between(host.getDateOfBirth(), LocalDate.now()).getYears();
            }
        }

        return ListingResponse.builder()
                .id(listing.getId())
                .hostUserId(host != null ? host.getId() : null)
                .status(Boolean.TRUE.equals(listing.getIsAvailable()) ? "PUBLISHED" : "UNAVAILABLE")
                .listingType(listing.getListingType())
                .title(listing.getTitle())
                .rentAmount(listing.getRentAmount())
                .depositAmount(listing.getDepositAmount())
                .roomType(listing.getRoomType())
                .furnishingStatus(listing.getFurnishingStatus())
                .latitude(location != null ? location.getY() : null)
                .longitude(location != null ? location.getX() : null)
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
                .hostName(hostName)
                .hostAge(hostAge)
                .hostAvatarUrl(hostAvatarUrl)
                .hostOccupationTitle(hostOccupationTitle)
                .isRoomSeekerHost(hostIsSeeker)
                .build();
    }
}
