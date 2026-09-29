package com.example.food_share.api;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@Configuration
@OpenAPIDefinition(info = @Info(
		title = "FoodShare Surplus Food Donation API",
		version = "1.0.0",
		description = "Register donors and NGOs, publish surplus food, claim and collect listings, and review monthly diverted quantities. Listings include an expiry time and can have only one active claim."))
public class OpenApiConfig {
}