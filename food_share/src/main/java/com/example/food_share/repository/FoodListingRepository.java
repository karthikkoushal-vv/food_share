package com.example.food_share.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.food_share.model.FoodListing;
import com.example.food_share.model.ListingStatus;

public interface FoodListingRepository extends JpaRepository<FoodListing, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select listing from FoodListing listing where listing.id = :id")
	Optional<FoodListing> findByIdForUpdate(@Param("id") Long id);

	List<FoodListing> findByStatusAndSafeUntilAfterOrderBySafeUntilAsc(ListingStatus status,
			LocalDateTime now);

	List<FoodListing> findByDonorIdOrderByCreatedAtDesc(Long donorId);

	List<FoodListing> findByStatusInAndSafeUntilLessThanEqual(Collection<ListingStatus> statuses,
			LocalDateTime now);
}