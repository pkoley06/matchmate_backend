package com.matcher.matchmate_backend.dto.request;

import com.matcher.matchmate_backend.model.enums.lifestyle.CleanlinessLevel;
import com.matcher.matchmate_backend.model.enums.lifestyle.DietaryPreference;
import com.matcher.matchmate_backend.model.enums.lifestyle.GuestTolerance;
import com.matcher.matchmate_backend.model.enums.lifestyle.SleepSchedule;
import com.matcher.matchmate_backend.model.enums.lifestyle.SmokingHabit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Progressive lifestyle vector update: any subset of the five fields may be sent
 * (the Flutter app submits one at a time, one per lifestyle card tapped). Only
 * non-null fields are applied; an existing answer is never cleared by omission.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LifestyleRequest {
    private SmokingHabit smokingHabit;
    private DietaryPreference dietaryPreference;
    private SleepSchedule sleepSchedule;
    private CleanlinessLevel cleanlinessLevel;
    private GuestTolerance guestTolerance;
}
