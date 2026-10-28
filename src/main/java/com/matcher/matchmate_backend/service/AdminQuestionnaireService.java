package com.matcher.matchmate_backend.service;

import com.matcher.matchmate_backend.dto.request.QuestionnaireWeightsUpdateRequest;
import com.matcher.matchmate_backend.dto.response.AdminQuestionnaireOverviewResponse;
import com.matcher.matchmate_backend.dto.response.QuestionnaireCategoryResponse;
import com.matcher.matchmate_backend.dto.response.QuestionnaireOptionDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AdminQuestionnaireService {

    private final AtomicReference<QuestionnaireWeightsUpdateRequest> currentWeights =
            new AtomicReference<>(QuestionnaireWeightsUpdateRequest.builder()
                    .cleanlinessWeight(2.0)
                    .sleepWeight(1.5)
                    .guestWeight(1.2)
                    .dietWeight(1.0)
                    .smokingDealbreaker(true)
                    .build());

    public AdminQuestionnaireOverviewResponse getOverview() {
        QuestionnaireWeightsUpdateRequest weights = currentWeights.get();

        List<QuestionnaireCategoryResponse> categories = List.of(
                QuestionnaireCategoryResponse.builder()
                        .key("smoking")
                        .title("Smoking Habit")
                        .headline("What is your smoking preference?")
                        .description("Determines whether smoking is permitted inside or on the premises.")
                        .weight(0.0) // Handled as hard disqualifier
                        .isDealbreaker(weights.getSmokingDealbreaker())
                        .options(List.of(
                                new QuestionnaireOptionDto("NON_SMOKER", "Non-smoker", "Never smokes, prefers smoke-free environment"),
                                new QuestionnaireOptionDto("OCCASIONAL_SMOKER", "Occasional / Outside only", "Smokes occasionally, only outdoors/balcony"),
                                new QuestionnaireOptionDto("REGULAR_SMOKER", "Regular smoker", "Regular smoker comfortable in smoking-friendly spaces")
                        ))
                        .build(),

                QuestionnaireCategoryResponse.builder()
                        .key("cleanliness")
                        .title("Cleanliness Level")
                        .headline("How tidy do you like common areas to be?")
                        .description("Scores shared kitchen, bathroom, and hall chores.")
                        .weight(weights.getCleanlinessWeight())
                        .isDealbreaker(false)
                        .options(List.of(
                                new QuestionnaireOptionDto("VERY_CLEAN", "Very Clean", "Everything wiped down immediately, strict chore rotation"),
                                new QuestionnaireOptionDto("MODERATE", "Moderate", "Tidy overall with casual weekly cleaning"),
                                new QuestionnaireOptionDto("RELAXED", "Relaxed", "Laid back, dishes can wait till morning")
                        ))
                        .build(),

                QuestionnaireCategoryResponse.builder()
                        .key("sleep")
                        .title("Sleep Schedule")
                        .headline("What are your typical sleeping and waking hours?")
                        .description("Prevents conflicts between early birds and night owls.")
                        .weight(weights.getSleepWeight())
                        .isDealbreaker(false)
                        .options(List.of(
                                new QuestionnaireOptionDto("EARLY_BIRD", "Early Bird", "Asleep before 11 PM, up early for work/gym"),
                                new QuestionnaireOptionDto("FLEXIBLE", "Flexible", "Standard schedule, adapts to routine"),
                                new QuestionnaireOptionDto("NIGHT_OWL", "Night Owl", "Productive late at night, sleeps in")
                        ))
                        .build(),

                QuestionnaireCategoryResponse.builder()
                        .key("guest")
                        .title("Guest Tolerance")
                        .headline("How often are visitors and friends welcome?")
                        .description("Regulates parties, sleepovers, and visitors.")
                        .weight(weights.getGuestWeight())
                        .isDealbreaker(false)
                        .options(List.of(
                                new QuestionnaireOptionDto("FREQUENT", "Frequent guests", "Loves hosting friends, gatherings welcome"),
                                new QuestionnaireOptionDto("OCCASIONAL", "Occasional guests", "Occasional dinner or weekend visitor"),
                                new QuestionnaireOptionDto("NO_GUESTS", "Quiet / Few guests", "Home is a private quiet sanctuary")
                        ))
                        .build(),

                QuestionnaireCategoryResponse.builder()
                        .key("diet")
                        .title("Dietary Preference")
                        .headline("What food habits are observed in the kitchen?")
                        .description("Essential for shared utensils, cookware, and fridge space.")
                        .weight(weights.getDietWeight())
                        .isDealbreaker(false)
                        .options(List.of(
                                new QuestionnaireOptionDto("VEGETARIAN", "Vegetarian", "Pure vegetarian cookware"),
                                new QuestionnaireOptionDto("NON_VEGETARIAN", "Non-Vegetarian", "Comfortable cooking all food items"),
                                new QuestionnaireOptionDto("VEGAN", "Vegan", "Strict plant-based lifestyle"),
                                new QuestionnaireOptionDto("EGGETARIAN", "Eggetarian", "Consumes vegetarian food plus eggs"),
                                new QuestionnaireOptionDto("NO_PREFERENCE", "No Preference", "No personal dietary restriction")
                        ))
                        .build()
        );

        return AdminQuestionnaireOverviewResponse.builder()
                .categories(categories)
                .currentWeights(weights)
                .build();
    }

    public QuestionnaireWeightsUpdateRequest updateWeights(QuestionnaireWeightsUpdateRequest request) {
        currentWeights.set(request);
        return currentWeights.get();
    }

    public QuestionnaireWeightsUpdateRequest getWeights() {
        return currentWeights.get();
    }
}
