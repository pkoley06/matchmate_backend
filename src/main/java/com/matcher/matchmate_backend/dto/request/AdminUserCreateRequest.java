package com.matcher.matchmate_backend.dto.request;

import com.matcher.matchmate_backend.model.enums.auth.Gender;
import com.matcher.matchmate_backend.model.enums.auth.OccupationCategory;
import com.matcher.matchmate_backend.model.enums.lifestyle.CleanlinessLevel;
import com.matcher.matchmate_backend.model.enums.lifestyle.DietaryPreference;
import com.matcher.matchmate_backend.model.enums.lifestyle.GuestTolerance;
import com.matcher.matchmate_backend.model.enums.lifestyle.SleepSchedule;
import com.matcher.matchmate_backend.model.enums.lifestyle.SmokingHabit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Admin-created user: unlike a real signup there is no Firebase identity, so
 * AdminUserService generates a synthetic placeholder firebase_uid and marks
 * onboarding as already complete (the admin is filling in the bare essentials
 * directly, in one request, instead of through the mobile onboarding flow).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserCreateRequest {

    @NotBlank(message = "Full name cannot be blank")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private String avatarUrl;
    private OccupationCategory occupationCategory;

    @Size(max = 100, message = "Occupation title cannot exceed 100 characters")
    private String occupationTitle;

    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;

    private String lastKnownLocality;

    private SmokingHabit smokingHabit;
    private DietaryPreference dietaryPreference;
    private SleepSchedule sleepSchedule;
    private CleanlinessLevel cleanlinessLevel;
    private GuestTolerance guestTolerance;
}
