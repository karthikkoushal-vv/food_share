package com.example.food_share.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

@Entity
public class FoodListing {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "donor_id", nullable = false)
	private Donor donor;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String foodType;

	@Column(nullable = false, precision = 12, scale = 3)
	private BigDecimal quantity;

	@Column(nullable = false)
	private String unit;

	@Column(length = 120)
	private String pickupArea;

	@Column(nullable = false)
	private LocalDateTime safeUntil;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private ListingStatus status;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	protected FoodListing() {
	}

	public FoodListing(Donor donor, String name, String foodType, BigDecimal quantity, String unit,
			LocalDateTime safeUntil) {
		this(donor, name, foodType, quantity, unit, null, safeUntil);
	}

	public FoodListing(Donor donor, String name, String foodType, BigDecimal quantity, String unit,
			String pickupArea, LocalDateTime safeUntil) {
		this.donor = donor;
		this.name = name;
		this.foodType = foodType;
		this.quantity = quantity;
		this.unit = unit;
		this.pickupArea = pickupArea;
		this.safeUntil = safeUntil;
		this.status = ListingStatus.AVAILABLE;
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Donor getDonor() {
		return donor;
	}

	public String getName() {
		return name;
	}

	public String getFoodType() {
		return foodType;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public String getUnit() {
		return unit;
	}

	public String getPickupArea() {
		return pickupArea;
	}

	public LocalDateTime getSafeUntil() {
		return safeUntil;
	}

	public ListingStatus getStatus() {
		return status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setStatus(ListingStatus status) {
		this.status = status;
	}
}