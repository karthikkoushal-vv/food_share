package com.example.food_share.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Claim {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "listing_id", nullable = false)
	private FoodListing listing;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ngo_id", nullable = false)
	private Ngo ngo;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private ClaimStatus status;

	@Column(nullable = false)
	private LocalDateTime claimedAt;

	private LocalDateTime collectedAt;

	protected Claim() {
	}

	public Claim(FoodListing listing, Ngo ngo) {
		this.listing = listing;
		this.ngo = ngo;
		this.status = ClaimStatus.CLAIMED;
		this.claimedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public FoodListing getListing() {
		return listing;
	}

	public Ngo getNgo() {
		return ngo;
	}

	public ClaimStatus getStatus() {
		return status;
	}

	public LocalDateTime getClaimedAt() {
		return claimedAt;
	}

	public LocalDateTime getCollectedAt() {
		return collectedAt;
	}

	public void setStatus(ClaimStatus status) {
		this.status = status;
	}

	public void setCollectedAt(LocalDateTime collectedAt) {
		this.collectedAt = collectedAt;
	}
}