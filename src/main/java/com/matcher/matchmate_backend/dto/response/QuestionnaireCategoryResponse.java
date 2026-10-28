package com.matcher.matchmate_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionnaireCategoryResponse {
    private String key;
    private String title;
    private String headline;
    private String description;
    private Double weight;
    private Boolean isDealbreaker;
    private List<QuestionnaireOptionDto> options;
}
