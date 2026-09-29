package com.example.food_share.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ListingExpiryScheduler {
	private final FoodShareService service;

	public ListingExpiryScheduler(FoodShareService service) {
		this.service = service;
	}

	@Scheduled(fixedDelay = 60_000, initialDelay = 1_000)
	public void expireListings() {
		service.expireListings();
	}
}