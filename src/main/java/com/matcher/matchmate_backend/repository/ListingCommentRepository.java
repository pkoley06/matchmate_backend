package com.matcher.matchmate_backend.repository;

import com.matcher.matchmate_backend.model.entity.ListingComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingCommentRepository extends JpaRepository<ListingComment, Long> {

    List<ListingComment> findByListingIdOrderByCreatedAtAsc(Long listingId);

    long countByListingId(Long listingId);
}
