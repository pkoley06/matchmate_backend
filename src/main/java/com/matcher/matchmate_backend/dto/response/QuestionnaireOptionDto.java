package com.matcher.matchmate_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionnaireOptionDto {
    private String value;
    private String label;
    private String description;
}
