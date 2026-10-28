package com.matcher.matchmate_backend.service;

import com.matcher.matchmate_backend.dto.request.ListingCommentCreateRequest;
import com.matcher.matchmate_backend.dto.response.ListingCommentResponse;
import com.matcher.matchmate_backend.dto.response.ListingLikeStatusResponse;
import com.matcher.matchmate_backend.dto.response.ListingResponse;
import com.matcher.matchmate_backend.model.entity.AppUser;
import com.matcher.matchmate_backend.model.entity.Listing;
import com.matcher.matchmate_backend.model.entity.ListingComment;
import com.matcher.matchmate_backend.model.entity.ListingLike;
import com.matcher.matchmate_backend.repository.AppUserRepository;
import com.matcher.matchmate_backend.repository.ListingCommentRepository;
import com.matcher.matchmate_backend.repository.ListingLikeRepository;
import com.matcher.matchmate_backend.repository.ListingRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class ListingInteractionService {

    private final ListingLikeRepository likeRepository;
    private final ListingCommentRepository commentRepository;
    private final ListingRepository listingRepository;
    private final AppUserRepository userRepository;
    private final ListingService listingService;

    public ListingInteractionService(
            ListingLikeRepository likeRepository,
            ListingCommentRepository commentRepository,
            ListingRepository listingRepository,
            AppUserRepository userRepository,
            ListingService listingService
    ) {
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.listingService = listingService;
    }

    @Transactional
    public ListingLikeStatusResponse toggleLike(Long userId, Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));

        Optional<ListingLike> existing = likeRepository.findByUserIdAndListingId(userId, listingId);
        boolean isLiked;
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            isLiked = false;
        } else {
            likeRepository.save(ListingLike.builder()
                    .userId(userId)
                    .listingId(listingId)
                    .build());
            isLiked = true;
        }

        long count = likeRepository.countByListingId(listingId);
        return ListingLikeStatusResponse.builder()
                .listingId(listingId)
                .isLiked(isLiked)
                .likesCount(count)
                .build();
    }

    public ListingLikeStatusResponse getLikeStatus(Long userId, Long listingId) {
        if (!listingRepository.existsById(listingId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found");
        }
        boolean isLiked = likeRepository.existsByUserIdAndListingId(userId, listingId);
        long count = likeRepository.countByListingId(listingId);
        return ListingLikeStatusResponse.builder()
                .listingId(listingId)
                .isLiked(isLiked)
                .likesCount(count)
                .build();
    }

    public List<ListingResponse> getLikedListings(Long userId) {
        List<Listing> listings = likeRepository.findLikedListingsByUserId(userId);
        return listings.stream().map(listingService::toResponse).toList();
    }

    public List<ListingCommentResponse> getComments(Long listingId) {
        if (!listingRepository.existsById(listingId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found");
        }
        return commentRepository.findByListingIdOrderByCreatedAtAsc(listingId)
                .stream()
                .map(this::toCommentResponse)
                .toList();
    }

    @Transactional
    public ListingCommentResponse addComment(Long userId, Long listingId, ListingCommentCreateRequest request) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        AppUser author = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ListingComment comment = ListingComment.builder()
                .listing(listing)
                .user(author)
                .content(request.getContent().trim())
                .build();

        return toCommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        ListingComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        boolean isAuthor = comment.getUser().getId().equals(userId);
        boolean isHost = comment.getListing().getHostUser().getId().equals(userId);

        if (!isAuthor && !isHost) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this comment");
        }

        commentRepository.delete(comment);
    }

    private ListingCommentResponse toCommentResponse(ListingComment comment) {
        AppUser author = comment.getUser();
        return ListingCommentResponse.builder()
                .id(comment.getId())
                .listingId(comment.getListing().getId())
                .userId(author.getId())
                .authorName(author.getFullName())
                .authorAvatarUrl(author.getAvatarUrl())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
