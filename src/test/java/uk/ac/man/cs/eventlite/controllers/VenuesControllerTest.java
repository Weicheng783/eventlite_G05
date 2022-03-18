package uk.ac.man.cs.eventlite.controllers;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.never;
import static org.hamcrest.Matchers.endsWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@ExtendWith(SpringExtension.class)
@WebMvcTest(VenuesController.class)
@Import(Security.class)
public class VenuesControllerTest {

	@Autowired
	private MockMvc mvc;

	@Mock
	private Event event;

	@Mock
	private Venue venue;

	@MockBean
	private EventService eventService;

	@MockBean
	private VenueService venueService;
	

	// Tests if users not logged in can update/add an venue
	@Test
	public void postVenueNoAuth() throws Exception {
		mvc.perform(MockMvcRequestBuilders.post("/venues").contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("name", "Venue")
			.param("capacity", "1000")
			.param("roadName","road name")
			.param("postcode", "m1 aaa")
			.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
			.andExpect(header().string("Location", endsWith("/sign-in")));

		verify(venueService, never()).save(venue);
	}
	
	// Tests if venues can be posted without csrf
	@Test
	public void postVenueNoCsrf() throws Exception {
		mvc.perform(MockMvcRequestBuilders.post("/venues").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue")
				.param("capacity", "1000")
				.param("roadName","road name")
				.param("postcode", "m1 aaa")
				.accept(MediaType.TEXT_HTML))
			.andExpect(status().isForbidden());

		verify(venueService, never()).save(venue);
	}
	
	// Tests posting a venue with a name longer than 256 characters
	@Test
	public void postLongVenueName() throws Exception {
		mvc.perform(MockMvcRequestBuilders.post("/venues").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.with(user("Markel").roles(Security.ADMIN_ROLE))
				.param("name", "Nam quis nulla. Integer malesuada. In in enim a arcu imperdiet malesuada. Sed vel lectus. Donec odio urna, tempus molestie, porttitor ut, iaculis quis, sem. Phasellus rhoncus. Aenean id metus id velit ullamcorper pulvinar. Vestibulum fermentum tortor id mi.")
				.param("capacity", "1000")
				.param("roadName","road name")
				.param("postcode", "m1 aaa")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrors("venue", "name"));


		verify(venueService, never()).save(venue);
	}
	
	// Tests posting a venue with no name 
		@Test
		public void postNoVenueName() throws Exception {
			mvc.perform(MockMvcRequestBuilders.post("/venues").contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.with(user("Markel").roles(Security.ADMIN_ROLE))
					.param("name", "")
					.param("capacity", "1000")
					.param("roadName","road name")
					.param("postcode", "m1 aaa")
					.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
					.andExpect(model().attributeHasFieldErrors("venue", "name"));


			verify(venueService, never()).save(venue);
		}
		
		// Tests posting a venue with long address
			@Test
			public void postLongRoadNameVenue() throws Exception {
				mvc.perform(MockMvcRequestBuilders.post("/venues").contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.with(user("Markel").roles(Security.ADMIN_ROLE))
						.param("name", "Name")
						.param("capacity", "1000")
						.param("roadName","Nam quis nulla. Integer malesuada. In in enim a arcu imperdiet malesuada. Sed vel lectus. Donec odio urna, tempus molestie, porttitor ut, iaculis quis, sem. Phasellus rhoncus. Aenean id metus id velit ullamcorper pulvinar. Vestibulum fermentum tortor id mi.Nam quis nulla. Integer malesuada. In in enim a arcu imperdiet malesuada. Sed vel lectus. Donec odio urna, tempus molestie, porttitor ut, iaculis quis, sem. Phasellus rhoncus. Aenean id metus id velit ullamcorper pulvinar. Vestibulum fermentum tortor id mi.")
						.param("postcode", "m1 aaa")
						.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
						.andExpect(model().attributeHasFieldErrors("venue", "roadName"));


				verify(venueService, never()).save(venue);
			}
		// Tests posting a venue with negative capacity
		@Test
		public void postInvalidCapVenue() throws Exception {
			mvc.perform(MockMvcRequestBuilders.post("/venues").contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.with(user("Markel").roles(Security.ADMIN_ROLE))
					.param("name", "Name")
					.param("capacity", "-1000")
					.param("roadName","road name")
					.param("postcode", "m1 aaa")
					.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
					.andExpect(model().attributeHasFieldErrors("venue", "capacity"));


			verify(venueService, never()).save(venue);
		}


}
