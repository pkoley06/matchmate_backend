package com.matcher.matchmate_backend.service;

import com.matcher.matchmate_backend.model.entity.AppUser;
import com.matcher.matchmate_backend.model.enums.auth.Gender;
import com.matcher.matchmate_backend.model.enums.auth.PrimaryAuthProvider;
import com.matcher.matchmate_backend.model.enums.lifestyle.CleanlinessLevel;
import com.matcher.matchmate_backend.model.enums.lifestyle.DietaryPreference;
import com.matcher.matchmate_backend.model.enums.lifestyle.GuestTolerance;
import com.matcher.matchmate_backend.model.enums.lifestyle.SleepSchedule;
import com.matcher.matchmate_backend.model.enums.lifestyle.SmokingHabit;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit test — no Spring context needed, matching the engine's own stateless design. */
class MatchingHeuristicEngineTest {

    private final MatchingHeuristicEngine engine = new MatchingHeuristicEngine();

    private AppUser.AppUserBuilder baseUser() {
        return AppUser.builder()
                .firebaseUid("uid")
                .primaryAuthProvider(PrimaryAuthProvider.GOOGLE)
                .fullName("Test User")
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.of(1998, 1, 1))
                .onboardingCompleted(true);
    }

    @Test
    void identicalLifestylesScoreOneHundredWithAllHighlights() {
        AppUser seeker = baseUser()
                .smokingHabit(SmokingHabit.NON_SMOKER)
                .dietaryPreference(DietaryPreference.VEGETARIAN)
                .sleepSchedule(SleepSchedule.NIGHT_OWL)
                .cleanlinessLevel(CleanlinessLevel.VERY_CLEAN)
                .guestTolerance(GuestTolerance.NEVER)
                .build();
        AppUser candidate = baseUser()
                .smokingHabit(SmokingHabit.NON_SMOKER)
                .dietaryPreference(DietaryPreference.VEGETARIAN)
                .sleepSchedule(SleepSchedule.NIGHT_OWL)
                .cleanlinessLevel(CleanlinessLevel.VERY_CLEAN)
                .guestTolerance(GuestTolerance.NEVER)
                .build();

        MatchingHeuristicEngine.MatchResult result = engine.evaluate(seeker, candidate);

        assertThat(result.matchPercentage()).isEqualTo(100);
        assertThat(result.compatibilityHighlights())
                .contains("Both Non-Smokers", "Both Night Owls", "Meticulous Cleaners",
                        "Both Prefer a Quiet Home", "Both Vegetarian");
    }

    @Test
    void smokingDealbreakerForcesZeroRegardlessOfOtherAlignment() {
        AppUser seeker = baseUser()
                .smokingHabit(SmokingHabit.NON_SMOKER)
                .dietaryPreference(DietaryPreference.VEGETARIAN)
                .sleepSchedule(SleepSchedule.NIGHT_OWL)
                .cleanlinessLevel(CleanlinessLevel.VERY_CLEAN)
                .guestTolerance(GuestTolerance.NEVER)
                .build();
        AppUser candidate = baseUser()
                .smokingHabit(SmokingHabit.REGULAR_SMOKER)
                .dietaryPreference(DietaryPreference.VEGETARIAN)
                .sleepSchedule(SleepSchedule.NIGHT_OWL)
                .cleanlinessLevel(CleanlinessLevel.VERY_CLEAN)
                .guestTolerance(GuestTolerance.NEVER)
                .build();

        MatchingHeuristicEngine.MatchResult result = engine.evaluate(seeker, candidate);

        assertThat(result.matchPercentage()).isZero();
        assertThat(result.compatibilityHighlights()).isEmpty();
    }

    @Test
    void incompleteLifestyleQuestionnaireYieldsNullScore() {
        AppUser seeker = baseUser()
                .smokingHabit(SmokingHabit.NON_SMOKER)
                .dietaryPreference(DietaryPreference.VEGETARIAN)
                .sleepSchedule(SleepSchedule.NIGHT_OWL)
                .cleanlinessLevel(CleanlinessLevel.VERY_CLEAN)
                .guestTolerance(GuestTolerance.NEVER)
                .build();
        AppUser candidateMissingAnswers = baseUser().build(); // no lifestyle fields answered

        MatchingHeuristicEngine.MatchResult result = engine.evaluate(seeker, candidateMissingAnswers);

        assertThat(result.matchPercentage()).isNull();
        assertThat(result.compatibilityHighlights()).isEmpty();
    }

    @Test
    void fullyOppositeLifestylesHitMaxDistanceAndScoreZero() {
        AppUser seeker = baseUser()
                .smokingHabit(SmokingHabit.OCCASIONAL_SMOKER)
                .dietaryPreference(DietaryPreference.VEGAN)
                .sleepSchedule(SleepSchedule.EARLY_BIRD)
                .cleanlinessLevel(CleanlinessLevel.VERY_CLEAN)
                .guestTolerance(GuestTolerance.NEVER)
                .build();
        AppUser candidate = baseUser()
                .smokingHabit(SmokingHabit.OCCASIONAL_SMOKER)
                .dietaryPreference(DietaryPreference.NON_VEGETARIAN)
                .sleepSchedule(SleepSchedule.NIGHT_OWL)
                .cleanlinessLevel(CleanlinessLevel.RELAXED)
                .guestTolerance(GuestTolerance.FREQUENT)
                .build();

        MatchingHeuristicEngine.MatchResult result = engine.evaluate(seeker, candidate);

        // Max possible distance (5.7) -> 0%, exactly what these fully-opposite vectors produce.
        assertThat(result.matchPercentage()).isEqualTo(0);
        assertThat(result.compatibilityHighlights()).isEmpty();
    }
}
