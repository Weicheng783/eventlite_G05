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
public class EventsControllerApiIntegrationTest extends AbstractTransactionalJUnit4SpringContextTests {

	@LocalServerPort
	private int port;
	
	private int currentRows;

	private String validEvent = "{ \"name\": \"Test Event 4\", \"date\": \"2200-12-31\", \"time\": \"12:00:00\", \"venue\": {\"id\":\"1\"} }";
	
	private WebTestClient client;

	@BeforeEach
	public void setup() {
		currentRows = countRowsInTable("events");
		client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port + "/api").build();
	}

	@Test
	public void testGetAllEvents() {
		client.get().uri("/events").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectHeader()
				.contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$._links.self.href")
				.value(endsWith("/api/events")).jsonPath("$._embedded.events.length()").value(equalTo(3));
	}

	@Test
	public void getEventFound() {
		client.get().uri("/events/3").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectHeader()
		.contentType(MediaType.APPLICATION_JSON).expectBody()
		.jsonPath("$.name").isEqualTo("Test Event 1")
		.jsonPath("$._links.self.href").value(endsWith("/api/events/3"));
	}
	
	@Test
	public void getEventNotFound() {
		client.get().uri("/events/99").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNotFound()
				.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$.error")
				.value(containsString("event 99")).jsonPath("$.id").isEqualTo(99);
	}
	
	@Test
	public void getEventVenue() {
		client.get().uri("/events/3/venue").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectHeader()
		.contentType(MediaType.APPLICATION_JSON).expectBody()
		.jsonPath("$.name").isEqualTo("Kilburn Building")
		.jsonPath("$._links.self.href").value(endsWith("/api/venues/1"));
	}
	
	@Test
	public void deleteEventNoUser() {
		int currentRows = countRowsInTable("events");

		client.delete().uri("/api/events/1").accept(MediaType.APPLICATION_JSON).exchange().expectStatus()
				.isUnauthorized();

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}

	@Test
	public void deleteEventBadUser() {
		int currentRows = countRowsInTable("events");

		client.mutate().filter(basicAuthentication("Bad", "Person")).build().delete().uri("/api/events/1")
				.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isUnauthorized();

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}

	@Test
	public void deleteAllEventsBadUser() {
		int currentRows = countRowsInTable("events");

		client.mutate().filter(basicAuthentication("Bad", "Person")).build().delete().uri("/api/events")
				.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isUnauthorized();

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}
	
	@Test
	public void postEventBadUser() {
		// Attempt to POST a valid event.
		client.mutate().filter(basicAuthentication("Bad", "Person")).build().post().uri("/events")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue(validEvent).exchange().expectStatus().isUnauthorized();

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}
	
	@Test
	public void postEventNoData() {
		// Attempt to POST a empty venue.
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().post().uri("/events")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.exchange().expectStatus().isBadRequest();

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}
	
	@Test
	public void postEventBadData() {
		// Attempt to POST a invalid name for event.
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().post().uri("/events")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{ \"name\": \"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"date\": \"2200-12-31\", \"time\": \"12:00:00\", \"venue\": {\"id\":\"1\"} }")
				.exchange().expectStatus().isEqualTo(422);

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}
	
	@Test
	@DirtiesContext
	public void postEventWithUser() {
		// Attempt to POST a valid greeting.
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().post().uri("/events")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue(validEvent).exchange().expectStatus().isCreated().expectHeader()
				.value("Location", containsString("/api/events")).expectBody().isEmpty();

		// Check one row is added to the database.
		assertThat(currentRows + 1, equalTo(countRowsInTable("events")));
	}

}
