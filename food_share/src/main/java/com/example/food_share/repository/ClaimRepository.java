package com.example.food_share.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.food_share.model.Claim;
import com.example.food_share.model.ClaimStatus;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
	boolean existsByListingIdAndStatus(Long listingId, ClaimStatus status);

	Optional<Claim> findByListingIdAndStatus(Long listingId, ClaimStatus status);

	List<Claim> findByStatusOrderByCollectedAtAsc(ClaimStatus status);

	@EntityGraph(attributePaths = "listing")
	List<Claim> findByNgoIdOrderByClaimedAtDesc(Long ngoId);
}