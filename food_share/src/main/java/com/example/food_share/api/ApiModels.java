package com.example.food_share.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.food_share.model.Claim;
import com.example.food_share.model.Donor;
import com.example.food_share.model.FoodListing;
import com.example.food_share.model.ListingStatus;
import com.example.food_share.model.Ngo;

public final class ApiModels {
	private ApiModels() {
	}

	@Schema(description = "Name used to register a donor or NGO")
	public record NameRequest(
			@Schema(description = "Organization name", example = "Campus Kitchen") @NotBlank String name) {
	}

	@Schema(description = "Food donation details. The safe-to-eat timestamp must be in the future.")
	public record CreateListingRequest(
			@Schema(description = "ID of the donor publishing this food", example = "1") @NotNull Long donorId,
			@Schema(description = "Short name for the food", example = "Vegetable curry") @NotBlank String name,
			@Schema(description = "Food category or preparation type", example = "Prepared meal") @NotBlank String foodType,
			@Schema(description = "Amount available", example = "12.5", minimum = "0.001") @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
			@Schema(description = "Unit used for the quantity; monthly totals keep units separate", example = "kg") @NotBlank String unit,
			@Schema(description = "Pickup area used to help nearby shelters find this food", example = "North Campus") @NotBlank @Size(max = 120) String pickupArea,
			@Schema(description = "Local date and time after which this food is unsafe", example = "2030-06-01T20:00:00") @NotNull @Future LocalDateTime safeUntil) {
	}

	@Schema(description = "NGO requesting the listing")
	public record CreateClaimRequest(
			@Schema(description = "ID of the NGO claiming the food", example = "1") @NotNull Long ngoId) {
	}

	public record DonorResponse(Long id, String name) {
		public static DonorResponse from(Donor donor) {
			return new DonorResponse(donor.getId(), donor.getName());
		}
	}

	public record NgoResponse(Long id, String name) {
		public static NgoResponse from(Ngo ngo) {
			return new NgoResponse(ngo.getId(), ngo.getName());
		}
	}

	public record ListingResponse(Long id, Long donorId, String name, String foodType,
			BigDecimal quantity, String unit, String pickupArea, LocalDateTime safeUntil, ListingStatus status) {
		public static ListingResponse from(FoodListing listing) {
			return new ListingResponse(listing.getId(), listing.getDonor().getId(), listing.getName(),
					listing.getFoodType(), listing.getQuantity(), listing.getUnit(), listing.getPickupArea(), listing.getSafeUntil(),
					listing.getStatus());
		}
	}

	public record ClaimResponse(Long id, Long listingId, Long ngoId, String status,
			LocalDateTime claimedAt, LocalDateTime collectedAt, String listingName,
			BigDecimal quantity, String unit, String pickupArea, LocalDateTime safeUntil) {
		public static ClaimResponse from(Claim claim) {
			return new ClaimResponse(claim.getId(), claim.getListing().getId(), claim.getNgo().getId(),
					claim.getStatus().name(), claim.getClaimedAt(), claim.getCollectedAt(),
					claim.getListing().getName(), claim.getListing().getQuantity(), claim.getListing().getUnit(),
					claim.getListing().getPickupArea(), claim.getListing().getSafeUntil());
		}
	}

	public record MonthlyDiversionResponse(String month, String unit, BigDecimal totalQuantity) {
	}

	public record ErrorResponse(String message) {
	}
}