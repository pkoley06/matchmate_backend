package com.matcher.matchmate_backend.dto.request;

import com.matcher.matchmate_backend.model.enums.auth.Gender;
import com.matcher.matchmate_backend.model.enums.auth.OccupationCategory;
import com.matcher.matchmate_backend.model.enums.lifestyle.CleanlinessLevel;
import com.matcher.matchmate_backend.model.enums.lifestyle.DietaryPreference;
import com.matcher.matchmate_backend.model.enums.lifestyle.GuestTolerance;
import com.matcher.matchmate_backend.model.enums.lifestyle.SleepSchedule;
import com.matcher.matchmate_backend.model.enums.lifestyle.SmokingHabit;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Every field is optional - only non-null fields are applied (same pattern as LifestyleRequest). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserUpdateRequest {

    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    private Gender gender;

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
