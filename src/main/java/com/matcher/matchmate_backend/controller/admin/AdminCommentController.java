package com.matcher.matchmate_backend.controller.admin;

import com.matcher.matchmate_backend.dto.response.AdminCommentResponse;
import com.matcher.matchmate_backend.dto.response.PageResponse;
import com.matcher.matchmate_backend.service.AdminCommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Comment moderation log. Protected by the admin filter chain (ROLE_ADMIN). */
@RestController
@RequestMapping("/api/v1/admin/comments")
public class AdminCommentController {

    private final AdminCommentService adminCommentService;

    public AdminCommentController(AdminCommentService adminCommentService) {
        this.adminCommentService = adminCommentService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<AdminCommentResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long listingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminCommentService.list(search, listingId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminCommentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(adminCommentService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adminCommentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
