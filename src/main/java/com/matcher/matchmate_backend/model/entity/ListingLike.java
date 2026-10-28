package com.matcher.matchmate_backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Entity
@Table(name = "listing_likes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ListingLike.ListingLikeId.class)
public class ListingLike {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "listing_id")
    private Long listingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", insertable = false, updatable = false)
    private Listing listing;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListingLikeId implements Serializable {
        private Long userId;
        private Long listingId;
    }
}
