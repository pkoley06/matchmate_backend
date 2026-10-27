package com.matcher.matchmate_backend.repository;

import com.matcher.matchmate_backend.model.entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findByHostUserId(Long hostUserId);
    List<Listing> findByIsAvailableTrue();

    /**
     * Milestone 4 Step 4.1: PostGIS bounding pre-filter (ST_DWithin, GiST-indexed) plus exact
     * geodesic distance (ST_Distance), joined against the host's lifestyle/demographic filters.
     * Scalar-only projection — array/entity mapping happens afterwards via {@link #findAllWithHostByIdIn}.
     * See "MatchMate Technical Architecture.md" §4.3 for the base query this extends.
     */
    @Query(
            value = """
                    SELECT l.id AS listing_id,
                           ST_Distance(l.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) / 1000.0 AS distance_km
                    FROM listings l
                    JOIN app_users u ON u.id = l.host_user_id
                    WHERE l.is_available = TRUE
                      AND u.onboarding_completed = TRUE
                      AND l.host_user_id <> :currentUserId
                      AND ST_DWithin(l.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
                      AND (:gender IS NULL OR u.gender = :gender)
                      AND (:minAge IS NULL OR DATE_PART('year', AGE(u.date_of_birth)) >= :minAge)
                      AND (:maxAge IS NULL OR DATE_PART('year', AGE(u.date_of_birth)) <= :maxAge)
                      AND (:smokingOnly = FALSE OR u.smoking_habit = 'NON_SMOKER')
                      AND (:dietOnly = FALSE OR u.dietary_preference = 'VEGETARIAN')
                      AND (:minRent IS NULL OR l.rent_amount >= :minRent)
                      AND (:maxRent IS NULL OR l.rent_amount <= :maxRent)
                      AND (:listingType IS NULL OR l.listing_type = :listingType)
                    ORDER BY distance_km ASC
                    LIMIT :limit OFFSET :offset
                    """,
            nativeQuery = true)
    List<ListingDistanceProjection> findCandidateIdsWithDistance(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters,
            @Param("currentUserId") Long currentUserId,
            @Param("gender") String gender,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge,
            @Param("smokingOnly") boolean smokingOnly,
            @Param("dietOnly") boolean dietOnly,
            @Param("minRent") Integer minRent,
            @Param("maxRent") Integer maxRent,
            @Param("listingType") String listingType,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /** Same WHERE clause as {@link #findCandidateIdsWithDistance}, without LIMIT/OFFSET, for pagination totals. */
    @Query(
            value = """
                    SELECT COUNT(*)
                    FROM listings l
                    JOIN app_users u ON u.id = l.host_user_id
                    WHERE l.is_available = TRUE
                      AND u.onboarding_completed = TRUE
                      AND l.host_user_id <> :currentUserId
                      AND ST_DWithin(l.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
                      AND (:gender IS NULL OR u.gender = :gender)
                      AND (:minAge IS NULL OR DATE_PART('year', AGE(u.date_of_birth)) >= :minAge)
                      AND (:maxAge IS NULL OR DATE_PART('year', AGE(u.date_of_birth)) <= :maxAge)
                      AND (:smokingOnly = FALSE OR u.smoking_habit = 'NON_SMOKER')
                      AND (:dietOnly = FALSE OR u.dietary_preference = 'VEGETARIAN')
                      AND (:minRent IS NULL OR l.rent_amount >= :minRent)
                      AND (:maxRent IS NULL OR l.rent_amount <= :maxRent)
                      AND (:listingType IS NULL OR l.listing_type = :listingType)
                    """,
            nativeQuery = true)
    long countCandidates(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters,
            @Param("currentUserId") Long currentUserId,
            @Param("gender") String gender,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge,
            @Param("smokingOnly") boolean smokingOnly,
            @Param("dietOnly") boolean dietOnly,
            @Param("minRent") Integer minRent,
            @Param("maxRent") Integer maxRent,
            @Param("listingType") String listingType);

    /** Loads full entities (with the host fetched eagerly) for a page of candidate IDs. */
    @Query("SELECT l FROM Listing l JOIN FETCH l.hostUser WHERE l.id IN :ids")
    List<Listing> findAllWithHostByIdIn(@Param("ids") List<Long> ids);
}
