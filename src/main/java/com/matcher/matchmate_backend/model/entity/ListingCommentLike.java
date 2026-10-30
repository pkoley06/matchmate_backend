package com.matcher.matchmate_backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.OffsetDateTime;

/** A user's like on a single room comment (V12 {@code listing_comment_likes}). */
@Entity
@Table(name = "listing_comment_likes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ListingCommentLike.ListingCommentLikeId.class)
public class ListingCommentLike {

    @Id
    @Column(name = "comment_id")
    private Long commentId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListingCommentLikeId implements Serializable {
        private Long commentId;
        private Long userId;
    }
}
