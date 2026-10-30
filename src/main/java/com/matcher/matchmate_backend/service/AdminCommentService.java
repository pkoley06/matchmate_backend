package com.matcher.matchmate_backend.service;

import com.matcher.matchmate_backend.dto.response.AdminCommentResponse;
import com.matcher.matchmate_backend.dto.response.PageResponse;
import com.matcher.matchmate_backend.model.entity.AppUser;
import com.matcher.matchmate_backend.model.entity.Listing;
import com.matcher.matchmate_backend.model.entity.ListingComment;
import com.matcher.matchmate_backend.repository.ListingCommentLikeRepository;
import com.matcher.matchmate_backend.repository.ListingCommentRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Admin console: inspect the room-comment log and remove anything flagged. */
@Service
public class AdminCommentService {

    private final ListingCommentRepository commentRepository;
    private final ListingCommentLikeRepository commentLikeRepository;
    private final ListingInteractionService interactionService;

    public AdminCommentService(
            ListingCommentRepository commentRepository,
            ListingCommentLikeRepository commentLikeRepository,
            ListingInteractionService interactionService
    ) {
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.interactionService = interactionService;
    }

    @Transactional
    public PageResponse<AdminCommentResponse> list(String search, Long listingId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.clamp(size, 1, 100);
        String query = StringUtils.hasText(search) ? search.trim() : null;

        Page<ListingComment> result = commentRepository.searchForModeration(query, listingId, PageRequest.of(safePage, safeSize));

        List<Long> ids = result.getContent().stream().map(ListingComment::getId).toList();
        Map<Long, Long> likeCounts = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : commentLikeRepository.countByCommentIds(ids)) {
                likeCounts.put((Long) row[0], (Long) row[1]);
            }
        }
        return PageResponse.of(result, c -> toResponse(c, likeCounts.getOrDefault(c.getId(), 0L)));
    }

    @Transactional
    public AdminCommentResponse getById(Long id) {
        ListingComment comment = commentRepository.findWithRelations(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        return toResponse(comment, commentLikeRepository.countByCommentId(id));
    }

    /** Deletes and broadcasts the removal to anyone viewing the room thread. */
    public void delete(Long id) {
        interactionService.deleteCommentAsModerator(id);
    }

    private AdminCommentResponse toResponse(ListingComment comment, long likesCount) {
        AppUser author = comment.getUser();
        Listing listing = comment.getListing();
        return AdminCommentResponse.builder()
                .id(comment.getId())
                .listingId(listing.getId())
                .listingTitle(listing.getTitle())
                .listingLocality(listing.getLocalityName())
                .listingHostUserId(listing.getHostUser() != null ? listing.getHostUser().getId() : null)
                .authorUserId(author.getId())
                .authorName(author.getFullName())
                .authorAvatarUrl(author.getAvatarUrl())
                .authorSuspended(author.getIsSuspended())
                .content(comment.getContent())
                .likesCount(likesCount)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
