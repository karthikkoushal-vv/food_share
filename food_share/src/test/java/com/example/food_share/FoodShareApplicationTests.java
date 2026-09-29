package com.example.food_share;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.example.food_share.service.FoodShareService;

@SpringBootTest
@AutoConfigureMockMvc
class FoodShareApplicationTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DonorRepository donorRepository;

	@Autowired
	private NgoRepository ngoRepository;

	@Autowired
	private FoodListingRepository listingRepository;

	@Autowired
	private ClaimRepository claimRepository;

	@Autowired
	private FoodShareService foodShareService;

	@BeforeEach
	void clearDatabase() {
		jdbcTemplate.execute("DELETE FROM claim");
		jdbcTemplate.execute("DELETE FROM food_listing");
		jdbcTemplate.execute("DELETE FROM donor");
		jdbcTemplate.execute("DELETE FROM ngo");
	}

	@Test
	void donorCanListFoodAndNgoCanClaimAndCollectIt() throws Exception {
		long donorId = createNamedRecord("donors", "Campus Kitchen");
		long ngoId = createNamedRecord("ngos", "Community Table");
		mockMvc.perform(get("/api/donors"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Campus Kitchen"));
		mockMvc.perform(get("/api/ngos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Community Table"));
		String safeUntil = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		String listing = mockMvc.perform(post("/api/listings").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"donorId":%d,"name":"Vegetable curry","foodType":"Prepared meal","quantity":12.5,"unit":"kg","pickupArea":"North Campus","safeUntil":"%s"}
						""".formatted(donorId, safeUntil)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("AVAILABLE"))
				.andExpect(jsonPath("$.pickupArea").value("North Campus"))
				.andReturn().getResponse().getContentAsString();
		long listingId = readId(listing);
		mockMvc.perform(get("/api/listings").param("area", "north"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(listingId));
		mockMvc.perform(get("/api/listings").param("area", "South Campus"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(get("/api/donors/{id}/listings", donorId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(listingId));

		String claim = mockMvc.perform(post("/api/listings/{id}/claims", listingId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"ngoId\":%d}".formatted(ngoId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("CLAIMED"))
				.andReturn().getResponse().getContentAsString();
		long claimId = readId(claim);
		mockMvc.perform(get("/api/ngos/{id}/claims", ngoId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(claimId))
				.andExpect(jsonPath("$[0].pickupArea").value("North Campus"));

		mockMvc.perform(post("/api/listings/{id}/claims", listingId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"ngoId\":%d}".formatted(ngoId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Listing is not available to claim"));

		mockMvc.perform(patch("/api/claims/{id}/collected", claimId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COLLECTED"));
		mockMvc.perform(patch("/api/claims/{id}/collected", claimId))
				.andExpect(status().isConflict());
		mockMvc.perform(get("/api/statistics/diverted/monthly"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].unit").value("kg"))
				.andExpect(jsonPath("$[0].totalQuantity").value(12.5));
	}

	@Test
	void invalidListingInputReturnsReadableValidationError() throws Exception {
		mockMvc.perform(post("/api/listings").contentType(MediaType.APPLICATION_JSON)
				.content("{\"donorId\":1,\"name\":\"\",\"foodType\":\"meal\",\"quantity\":0,\"unit\":\"kg\",\"pickupArea\":\"North Campus\",\"safeUntil\":\"2000-01-01T00:00:00\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void rootShowsHomepageAndSwaggerRemainsSeparate() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("index.html"));
		mockMvc.perform(get("/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Good food finds a table.")))
				.andExpect(content().string(containsString("href=\"/swagger-ui/index.html\"")));
		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk());
	}

	@Test
	void expiryMarksAvailableAndClaimedListingsAndClaimsExpired() {
		Donor donor = donorRepository.save(new Donor("Campus Kitchen"));
		Ngo ngo = ngoRepository.save(new Ngo("Community Table"));
		FoodListing available = listingRepository.save(new FoodListing(donor, "Bread", "Bakery",
				BigDecimal.ONE, "kg", LocalDateTime.now().minusMinutes(1)));
		FoodListing claimed = new FoodListing(donor, "Soup", "Prepared meal", BigDecimal.TEN,
				"litre", LocalDateTime.now().minusMinutes(1));
		claimed.setStatus(ListingStatus.CLAIMED);
		claimed = listingRepository.save(claimed);
		Claim claim = claimRepository.save(new Claim(claimed, ngo));

		foodShareService.expireListings();

		assertEquals(ListingStatus.EXPIRED,
				listingRepository.findById(available.getId()).orElseThrow().getStatus());
		assertEquals(ListingStatus.EXPIRED,
				listingRepository.findById(claimed.getId()).orElseThrow().getStatus());
		assertEquals(ClaimStatus.EXPIRED, claimRepository.findById(claim.getId()).orElseThrow().getStatus());
	}

	private long createNamedRecord(String resource, String name) throws Exception {
		String response = mockMvc.perform(post("/api/" + resource).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"%s\"}".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return readId(response);
	}

	private long readId(String response) {
		return Long.parseLong(response.replaceFirst("^.*?\\\"id\\\":(\\d+).*$", "$1"));
	}

}
