package com.matcher.matchmate_backend.model.entity;

import com.matcher.matchmate_backend.model.enums.auth.*;
import com.matcher.matchmate_backend.model.enums.lifestyle.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "app_users")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firebase_uid", nullable = false, unique = true, length = 128)
    private String firebaseUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_auth_provider", nullable = false, length = 20)
    private PrimaryAuthProvider primaryAuthProvider;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "occupation_category", length = 30)
    private OccupationCategory occupationCategory;

    @Column(name = "occupation_title", length = 100)
    private String occupationTitle;

    @Column(name = "bio", length = 500)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "smoking_habit", length = 20)
    private SmokingHabit smokingHabit;

    @Enumerated(EnumType.STRING)
    @Column(name = "dietary_preference", length = 20)
    private DietaryPreference dietaryPreference;

    @Enumerated(EnumType.STRING)
    @Column(name = "sleep_schedule", length = 20)
    private SleepSchedule sleepSchedule;

    @Enumerated(EnumType.STRING)
    @Column(name = "cleanliness_level", length = 20)
    private CleanlinessLevel cleanlinessLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "guest_tolerance", length = 20)
    private GuestTolerance guestTolerance;

    @Column(name = "last_known_location", columnDefinition = "geography(Point,4326)")
    private Point lastKnownLocation;

    @Column(name = "last_known_locality", length = 100)
    @Builder.Default
    private String lastKnownLocality = "Kolkata";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "onboarding_completed", nullable = false)
    @Builder.Default
    private Boolean onboardingCompleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

}
