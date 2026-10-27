package com.matcher.matchmate_backend.repository;

/** Scalar-only projection for the spatial candidate query — avoids array/entity mapping in native SQL. */
public interface ListingDistanceProjection {
    Long getListingId();

    Double getDistanceKm();
}
