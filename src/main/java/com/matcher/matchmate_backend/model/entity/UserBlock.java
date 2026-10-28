package com.matcher.matchmate_backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UserBlock.UserBlockId.class)
public class UserBlock {

    @Id
    @Column(name = "blocker_id")
    private Long blockerId;

    @Id
    @Column(name = "blocked_id")
    private Long blockedId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", insertable = false, updatable = false)
    private AppUser blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", insertable = false, updatable = false)
    private AppUser blocked;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserBlockId implements Serializable {
        private Long blockerId;
        private Long blockedId;
    }
}
