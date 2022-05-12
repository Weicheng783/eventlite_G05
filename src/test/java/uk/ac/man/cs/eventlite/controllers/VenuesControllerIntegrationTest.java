package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import uk.ac.man.cs.eventlite.EventLite;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventLite.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureWebTestClient(timeout = "100000")//10 seconds
@ActiveProfiles("test")
public class VenuesControllerIntegrationTest extends AbstractTransactionalJUnit4SpringContextTests {

	@LocalServerPort
	private int port;
	private static Pattern CSRF = Pattern.compile("(?s).*name=\"_csrf\".*?value=\"([^\"]+).*");
	private static String CSRF_HEADER = "X-CSRF-TOKEN";
	private WebTestClient client;
	private static String SESSION_KEY = "JSESSIONID";

	@BeforeEach
	public void setup() {
		client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
	}

	@Test
	public void testGetAllVenues() {
		client.get().uri("/venues").accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk();
	}

	
	@Test
	public void testGetVenue() {
		client.get().uri("/venues/1").accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk();
		}
	
	@Test
	public void testGetVenueNotFound() {
		client.get().uri("/venues/99").accept(MediaType.TEXT_HTML).exchange().expectStatus().isNotFound().expectHeader()
				.contentTypeCompatibleWith(MediaType.TEXT_HTML).expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("99"));
				});
	}
	
	@Test
	public void postVenueNoUser() {
		int currentRows = countRowsInTable("venue");
		// We don't set the session ID, so have no credentials.
		// This should redirect to the sign-in page.
		client.post().uri("/venues").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue("New venue").exchange().expectStatus().isFound().expectHeader()
				.value("Location", endsWith("/sign-in"));

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("venue")));
	}
	
	@Test public void postVenueWithUser() {
		int currentRows = countRowsInTable("venue");
		String[] tokens = login();

		// Attempt to POST a valid greeting.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("template", "Howdy, %s!");

		// The session ID cookie holds our login credentials.
		client.post().uri("/venues").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isOk().expectHeader().value("Location", endsWith("/venues"));

		// Check one row is added to the database.
		assertThat(currentRows + 1, equalTo(countRowsInTable("venue")));
		
		}


private String[] login() {
	String[] tokens = new String[2];

	// Although this doesn't POST the log in form it effectively logs us in.
	// If we provide the correct credentials here, we get a session ID back which
	// keeps us logged in.
	EntityExchangeResult<String> result = client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get()
			.uri("/").accept(MediaType.TEXT_HTML).exchange().expectBody(String.class).returnResult();
	tokens[0] = getCsrfToken(result.getResponseBody());
	tokens[1] = result.getResponseCookies().getFirst(SESSION_KEY).getValue();

	return tokens;
}


private String getCsrfToken(String body) {
	Matcher matcher = CSRF.matcher(body);

	// matcher.matches() must be called; might as well assert something as well...
	assertThat(matcher.matches(), equalTo(true));

	return matcher.group(1);
}





	


}