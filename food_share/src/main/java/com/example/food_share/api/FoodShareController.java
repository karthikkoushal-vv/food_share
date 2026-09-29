package com.example.food_share.api;

import java.util.List;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.food_share.api.ApiModels.ClaimResponse;
import com.example.food_share.api.ApiModels.CreateClaimRequest;
import com.example.food_share.api.ApiModels.CreateListingRequest;
import com.example.food_share.api.ApiModels.DonorResponse;
import com.example.food_share.api.ApiModels.ListingResponse;
import com.example.food_share.api.ApiModels.MonthlyDiversionResponse;
import com.example.food_share.api.ApiModels.NameRequest;
import com.example.food_share.api.ApiModels.NgoResponse;
import com.example.food_share.model.Donor;
import com.example.food_share.model.Ngo;
import com.example.food_share.service.FoodShareService;

@RestController
@RequestMapping("/api")
@Tag(name = "FoodShare", description = "Food donation, collection, and impact reporting")
public class FoodShareController {
	private final FoodShareService service;

	public FoodShareController(FoodShareService service) {
		this.service = service;
	}

	@PostMapping("/donors")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register a donor", description = "Create the donor account that owns future food listings.")
	public DonorResponse createDonor(@Valid @RequestBody NameRequest request) {
		return DonorResponse.from(service.createDonor(request.name()));
	}

	@GetMapping("/donors")
	@Operation(summary = "List donors", description = "Return donor profiles that can publish food listings.")
	public List<DonorResponse> donors() {
		return service.getDonors().stream().map(DonorResponse::from).toList();
	}

	@GetMapping("/donors/{donorId}/listings")
	@Operation(summary = "View a donor's listings", description = "Return all listings created by a donor, including claimed, collected, and expired food.")
	public List<ListingResponse> donorListings(
			@Parameter(description = "ID of the donor", example = "1") @PathVariable Long donorId) {
		return service.getDonorListings(donorId).stream().map(ListingResponse::from).toList();
	}

	@PostMapping("/ngos")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register an NGO", description = "Create the NGO account that can claim available food listings.")
	public NgoResponse createNgo(@Valid @RequestBody NameRequest request) {
		return NgoResponse.from(service.createNgo(request.name()));
	}

	@GetMapping("/ngos")
	@Operation(summary = "List NGOs", description = "Return NGO profiles that can claim available food.")
	public List<NgoResponse> ngos() {
		return service.getNgos().stream().map(NgoResponse::from).toList();
	}

	@GetMapping("/ngos/{ngoId}/claims")
	@Operation(summary = "View an NGO's claims", description = "Return the selected NGO's claim history, including collection status.")
	public List<ClaimResponse> ngoClaims(
			@Parameter(description = "ID of the NGO", example = "1") @PathVariable Long ngoId) {
		return service.getNgoClaims(ngoId).stream().map(ClaimResponse::from).toList();
	}

	@PostMapping("/listings")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Publish surplus food", description = "Create a listing with quantity, unit, food type, donor, and a future safe-to-eat time.")
	public ListingResponse createListing(@Valid @RequestBody CreateListingRequest request) {
		return ListingResponse.from(service.createListing(request));
	}

	@GetMapping("/listings")
	@Operation(summary = "Browse available food", description = "Return listings that are available and have not reached their safe-to-eat time. Optionally filter by pickup-area text.")
	public List<ListingResponse> availableListings(
			@Parameter(description = "Optional pickup-area text, matched case-insensitively", example = "North Campus")
			@RequestParam(required = false) String area) {
		return service.getAvailableListings(area).stream().map(ListingResponse::from).toList();
	}

	@PostMapping("/listings/{listingId}/claims")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Claim a food listing", description = "An NGO claims an available listing. The request is rejected with HTTP 409 if it is expired or already claimed.")
	public ClaimResponse claimListing(
			@Parameter(description = "ID of the food listing", example = "1") @PathVariable Long listingId,
			@Valid @RequestBody CreateClaimRequest request) {
		return ClaimResponse.from(service.claimListing(listingId, request.ngoId()));
	}

	@PatchMapping("/claims/{claimId}/collected")
	@Operation(summary = "Mark food as collected", description = "Confirm pickup for a claim. A claim can be collected only once and before the listing expires.")
	public ClaimResponse markCollected(
			@Parameter(description = "ID of the claim to complete", example = "1") @PathVariable Long claimId) {
		return ClaimResponse.from(service.markCollected(claimId));
	}

	@GetMapping("/statistics/diverted/monthly")
	@Operation(summary = "View monthly diverted food", description = "Sum collected food quantities by calendar month and unit. Different units are reported separately.")
	public List<MonthlyDiversionResponse> monthlyDiversion() {
		return service.monthlyDiversion();
	}
}