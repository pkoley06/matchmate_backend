package com.matcher.matchmate_backend.dto.response;

import com.matcher.matchmate_backend.dto.request.QuestionnaireWeightsUpdateRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminQuestionnaireOverviewResponse {
    private List<QuestionnaireCategoryResponse> categories;
    private QuestionnaireWeightsUpdateRequest currentWeights;
}
