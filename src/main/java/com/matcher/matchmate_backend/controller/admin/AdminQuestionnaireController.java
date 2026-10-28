package com.matcher.matchmate_backend.controller.admin;

import com.matcher.matchmate_backend.dto.request.QuestionnaireWeightsUpdateRequest;
import com.matcher.matchmate_backend.dto.response.AdminQuestionnaireOverviewResponse;
import com.matcher.matchmate_backend.service.AdminQuestionnaireService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/questionnaire")
public class AdminQuestionnaireController {

    private final AdminQuestionnaireService adminQuestionnaireService;

    public AdminQuestionnaireController(AdminQuestionnaireService adminQuestionnaireService) {
        this.adminQuestionnaireService = adminQuestionnaireService;
    }

    @GetMapping
    public ResponseEntity<AdminQuestionnaireOverviewResponse> getOverview() {
        return ResponseEntity.ok(adminQuestionnaireService.getOverview());
    }

    @PutMapping("/weights")
    public ResponseEntity<QuestionnaireWeightsUpdateRequest> updateWeights(
            @Valid @RequestBody QuestionnaireWeightsUpdateRequest request
    ) {
        return ResponseEntity.ok(adminQuestionnaireService.updateWeights(request));
    }
}
