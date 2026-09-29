package com.example.food_share.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.food_share.api.ApiModels.CreateListingRequest;
import com.example.food_share.api.ApiModels.MonthlyDiversionResponse;
import com.example.food_share.model.Claim;
import com.example.food_share.model.ClaimStatus;
import com.example.food_share.model.Donor;
import com.example.food_share.model.FoodListing;
import com.example.food_share.model.ListingStatus;
import com.example.food_share.model.Ngo;
import com.example.food_share.repository.ClaimRepository;
import com.example.food_share.repository.DonorRepository;
import com.example.food_share.repository.FoodListingRepository;
import com.example.food_share.repository.NgoRepository;

@Service
public class FoodShareService {
	private final DonorRepository donorRepository;
	private final NgoRepository ngoRepository;
	private final FoodListingRepository listingRepository;
	private final ClaimRepository claimRepository;

	public FoodShareService(DonorRepository donorRepository, NgoRepository ngoRepository,
			FoodListingRepository listingRepository, ClaimRepository claimRepository) {
		this.donorRepository = donorRepository;
		this.ngoRepository = ngoRepository;
		this.listingRepository = listingRepository;
		this.claimRepository = claimRepository;
	}

	@Transactional
	public Donor createDonor(String name) {
		return donorRepository.save(new Donor(name.trim()));
	}

	@Transactional
	public Ngo createNgo(String name) {
		return ngoRepository.save(new Ngo(name.trim()));
	}

	@Transactional(readOnly = true)
	public List<Donor> getDonors() {
		return donorRepository.findAll();
	}

	@Transactional(readOnly = true)
	public List<Ngo> getNgos() {
		return ngoRepository.findAll();
	}

	@Transactional
	public FoodListing createListing(CreateListingRequest request) {
		Donor donor = donorRepository.findById(request.donorId())
				.orElseThrow(() -> notFound("Donor", request.donorId()));
		return listingRepository.save(new FoodListing(donor, request.name().trim(), request.foodType().trim(),
				request.quantity(), request.unit().trim(), request.pickupArea().trim(), request.safeUntil()));
	}

	@Transactional
	public List<FoodListing> getAvailableListings(String area) {
		expireListings();
		List<FoodListing> available = listingRepository.findByStatusAndSafeUntilAfterOrderBySafeUntilAsc(
				ListingStatus.AVAILABLE, LocalDateTime.now());
		if (area == null || area.isBlank()) {
			return available;
		}
		String normalizedArea = area.trim().toLowerCase(Locale.ROOT);
		return available.stream()
				.filter(listing -> listing.getPickupArea() != null
						&& listing.getPickupArea().toLowerCase(Locale.ROOT).contains(normalizedArea))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<FoodListing> getDonorListings(Long donorId) {
		donorRepository.findById(donorId).orElseThrow(() -> notFound("Donor", donorId));
		return listingRepository.findByDonorIdOrderByCreatedAtDesc(donorId);
	}

	@Transactional(readOnly = true)
	public List<Claim> getNgoClaims(Long ngoId) {
		ngoRepository.findById(ngoId).orElseThrow(() -> notFound("NGO", ngoId));
		return claimRepository.findByNgoIdOrderByClaimedAtDesc(ngoId);
	}

	@Transactional
	public Claim claimListing(Long listingId, Long ngoId) {
		FoodListing listing = listingRepository.findByIdForUpdate(listingId)
				.orElseThrow(() -> notFound("Listing", listingId));
		if (!listing.getSafeUntil().isAfter(LocalDateTime.now())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Listing has passed its safe-to-eat time");
		}
		if (listing.getStatus() != ListingStatus.AVAILABLE
				|| claimRepository.existsByListingIdAndStatus(listingId, ClaimStatus.CLAIMED)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Listing is not available to claim");
		}
		Ngo ngo = ngoRepository.findById(ngoId).orElseThrow(() -> notFound("NGO", ngoId));
		listing.setStatus(ListingStatus.CLAIMED);
		return claimRepository.save(new Claim(listing, ngo));
	}

	@Transactional
	public Claim markCollected(Long claimId) {
		Claim claim = claimRepository.findById(claimId)
				.orElseThrow(() -> notFound("Claim", claimId));
		FoodListing listing = listingRepository.findByIdForUpdate(claim.getListing().getId())
				.orElseThrow(() -> notFound("Listing", claim.getListing().getId()));
		if (claim.getStatus() != ClaimStatus.CLAIMED || listing.getStatus() != ListingStatus.CLAIMED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Claim is not awaiting collection");
		}
		if (!listing.getSafeUntil().isAfter(LocalDateTime.now())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Listing has passed its safe-to-eat time");
		}
		LocalDateTime collectedAt = LocalDateTime.now();
		claim.setStatus(ClaimStatus.COLLECTED);
		claim.setCollectedAt(collectedAt);
		listing.setStatus(ListingStatus.COLLECTED);
		return claim;
	}

	@Transactional(readOnly = true)
	public List<MonthlyDiversionResponse> monthlyDiversion() {
		Map<String, BigDecimal> totals = new TreeMap<>();
		for (Claim claim : claimRepository.findByStatusOrderByCollectedAtAsc(ClaimStatus.COLLECTED)) {
			FoodListing listing = claim.getListing();
			String key = YearMonth.from(claim.getCollectedAt()) + "|" + listing.getUnit();
			totals.merge(key, listing.getQuantity(), BigDecimal::add);
		}
		return totals.entrySet().stream().map(entry -> {
			String[] parts = entry.getKey().split("\\|", 2);
			return new MonthlyDiversionResponse(parts[0], parts[1], entry.getValue());
		}).toList();
	}

	@Transactional
	public void expireListings() {
		List<FoodListing> expired = listingRepository.findByStatusInAndSafeUntilLessThanEqual(
				List.of(ListingStatus.AVAILABLE, ListingStatus.CLAIMED), LocalDateTime.now());
		for (FoodListing listing : expired) {
			listing.setStatus(ListingStatus.EXPIRED);
			claimRepository.findByListingIdAndStatus(listing.getId(), ClaimStatus.CLAIMED)
					.ifPresent(claim -> claim.setStatus(ClaimStatus.EXPIRED));
		}
	}

	private ResponseStatusException notFound(String type, Long id) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " not found: " + id);
	}
}