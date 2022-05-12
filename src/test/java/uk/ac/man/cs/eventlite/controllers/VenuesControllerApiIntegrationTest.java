package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.web.reactive.server.WebTestClient;

import uk.ac.man.cs.eventlite.EventLite;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventLite.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class VenuesControllerApiIntegrationTest extends AbstractTransactionalJUnit4SpringContextTests {

	@LocalServerPort
	private int port;
	
	private int currentRows;
	
	private String validVenue = "{ \"name\": \"House\", \"roadName\": \"20 Cawdor Rd\", \"postcode\": \"M14 6LQ\", \"capacity\": 70 }";

	private WebTestClient client;

	@BeforeEach
	public void setup() {
		currentRows = countRowsInTable("venue");
		client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port + "/api").build();
	}

	@Test
	public void testGetAllVenues() {
		client.get().uri("/venues").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectHeader()
				.contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$._links.self.href")
				.value(endsWith("/api/venues")).jsonPath("$._embedded.venues.length()").value(equalTo(2));
	}

	@Test
	public void getVenueNotFound() {
		client.get().uri("/venues/99").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNotFound()
				.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$.error")
				.value(containsString("venue 99")).jsonPath("$.id").isEqualTo(99);
	}
	
	@Test
	public void getVenueFound() {
		client.get().uri("/venues/1").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectHeader()
			.contentType(MediaType.APPLICATION_JSON).expectBody()
			.jsonPath("$.name").isEqualTo("Kilburn Building")
			.jsonPath("$.roadName").isEqualTo("Kilburn Building University of Manchester, Oxford Rd")
			.jsonPath("$.postcode").isEqualTo("M13 9PL")
			.jsonPath("$.capacity").isEqualTo("1000")
			.jsonPath("$._links.self.href").value(endsWith("/api/venues/1"));
	}
	
	@Test
	public void getVenueEventsNotFound() {
		client.get().uri("/venues/99/events").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNotFound()
				.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$.error")
				.value(containsString("venue 99")).jsonPath("$.id").isEqualTo(99);
	}
	
	@Test
	public void getVenueEventsFound() {
		client.get().uri("/venues/1/next3events").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectHeader()
			.contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$._embedded.events.length()").isEqualTo(3);
	}
	
	@Test
	public void getVenueNext3EventsNotFound() {
		client.get().uri("/venues/99/next3events").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNotFound()
				.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$.error")
				.value(containsString("venue 99")).jsonPath("$.id").isEqualTo(99);
	}
	
	@Test
	public void getVenueNext3EventsFound() {
		client.get().uri("/venues/1/events").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectHeader()
			.contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$._embedded.events.length()").isEqualTo(3);
	}
	
	@Test
	public void postVenueNoUser() {
		// Attempt to POST a valid venue.
		client.post().uri("/venues").accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue(validVenue).exchange().expectStatus().isUnauthorized();

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("venue")));
	}
	
	@Test
	public void postVenueBadUser() {
		// Attempt to POST a valid venue.
		client.mutate().filter(basicAuthentication("Bad", "Person")).build().post().uri("/venues")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue(validVenue).exchange().expectStatus().isUnauthorized();

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("venue")));
	}
	
	@Test
	public void postVenueNoData() {
		// Attempt to POST a empty venue.
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().post().uri("/venues")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.exchange().expectStatus().isBadRequest();

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("venue")));
	}
	
	@Test
	public void postVenueBadData() {
		// Attempt to POST a empty venue.
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().post().uri("/venues")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{ \"name\": \"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"roadName\": \"20 Cawdor Rd\", \"postcode\": \"M14 6LQ\", \"capacity\": 70 }")
				.exchange().expectStatus().isEqualTo(422);

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("venue")));
	}
	
	@Test
	@DirtiesContext
	public void postVenueWithUser() {
		// Attempt to POST a valid greeting.
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().post().uri("/venues")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue(validVenue).exchange().expectStatus().isCreated().expectHeader()
				.value("Location", containsString("/api/venues")).expectBody().isEmpty();

		// Check one row is added to the database.
		assertThat(currentRows + 1, equalTo(countRowsInTable("venue")));
	}
	 
}