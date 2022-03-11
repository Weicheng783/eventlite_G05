package uk.ac.man.cs.eventlite.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
@WebMvcTest(EventsController.class)
@Import(Security.class)
public class EventsControllerTest {

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

	@Test
	public void getIndexWhenNoEvents() throws Exception {
		when(eventService.findAll()).thenReturn(Collections.<Event>emptyList());
		when(venueService.findAll()).thenReturn(Collections.<Venue>emptyList());

		mvc.perform(get("/events").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("events/index")).andExpect(handler().methodName("getAllEvents"));

		verify(eventService).findAll();
		// verify(venueService).findAll();
		verifyNoInteractions(event);
		verifyNoInteractions(venue);
	}

	@Test
	public void getIndexWithEvents() throws Exception {
		when(venue.getName()).thenReturn("Kilburn Building");
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));

		when(event.getVenue()).thenReturn(venue);
		when(eventService.findAll()).thenReturn(Collections.<Event>singletonList(event));

		mvc.perform(get("/events").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("events/index")).andExpect(handler().methodName("getAllEvents"));

		verify(eventService).findAll();
		// verify(venueService).findAll();
	}

	@Test
	public void getEventNotFound() throws Exception {
		mvc.perform(get("/events/99").accept(MediaType.TEXT_HTML)).andExpect(status().isNotFound())
				.andExpect(view().name("events/not_found")).andExpect(handler().methodName("getEvent"));
	}
	

	// Tests if users not logged in can update/add an event
	@Test
	public void postEventNoAuth() throws Exception {
		mvc.perform(MockMvcRequestBuilders.post("/events").contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("name", "Event")
			.param("id", "10")
			.param("date", LocalDate.now().toString())
			.param("time", LocalTime.now().toString())
			.param("Venue_id", "10")
			.param("description", "This event is...")
			.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
			.andExpect(header().string("Location", endsWith("/sign-in")));

		verify(eventService, never()).save(event);
	}
	
	// Tests if events can be posted without csrf
	@Test
	public void postEventNoCsrf() throws Exception {
		mvc.perform(MockMvcRequestBuilders.post("/events").contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.with(user("Markel").roles(Security.ADMIN_ROLE))
			.param("name", "Event")
			.param("id", "10")
			.param("date", LocalDate.now().toString())
			.param("time", LocalTime.now().toString())
			.param("Venue_id", "10")
			.param("description", "This event is...")
			.accept(MediaType.TEXT_HTML))
			.andExpect(status().isForbidden());

		verify(eventService, never()).save(event);
	}
	
	// Tests posting an event with a name longer than 256 characters
	@Test
	public void postLongEventName() throws Exception {
		mvc.perform(MockMvcRequestBuilders.post("/events").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.with(user("Markel").roles(Security.ADMIN_ROLE))
				.param("name", "Nam quis nulla. Integer malesuada. In in enim a arcu imperdiet malesuada. Sed vel lectus. Donec odio urna, tempus molestie, porttitor ut, iaculis quis, sem. Phasellus rhoncus. Aenean id metus id velit ullamcorper pulvinar. Vestibulum fermentum tortor id mi.")
				.param("id", "10")
				.param("date", LocalDate.now().toString())
				.param("time", LocalTime.now().toString())
				.param("Venue_id", "10")
				.param("description", "This event is...")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrors("event", "name"));


		verify(eventService, never()).save(event);
	}
	
	// Tests posting an event with no name 
		@Test
		public void postNoEventName() throws Exception {
			mvc.perform(MockMvcRequestBuilders.post("/events").contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.with(user("Markel").roles(Security.ADMIN_ROLE))
					.param("name", "")
					.param("id", "10")
					.param("date", LocalDate.now().toString())
					.param("time", LocalTime.now().toString())
					.param("Venue_id", "10")
					.param("description", "This event is...")
					.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
					.andExpect(model().attributeHasFieldErrors("event", "name"));


			verify(eventService, never()).save(event);
		}
		
		// Tests posting an event with no date 
			@Test
			public void postNoEventDate() throws Exception {
				mvc.perform(MockMvcRequestBuilders.post("/events").contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.with(user("Markel").roles(Security.ADMIN_ROLE))
						.param("name", "Event")
						.param("id", "10")
						.param("date", "")
						.param("time", LocalTime.now().toString())
						.param("Venue_id", "10")
						.param("description", "This event is...")
						.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
						.andExpect(model().attributeHasFieldErrors("event", "date"));


				verify(eventService, never()).save(event);
			}
		// Tests posting an event with no venue
		@Test
		public void postNoEventVenue() throws Exception {
			mvc.perform(MockMvcRequestBuilders.post("/events").contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.with(user("Markel").roles(Security.ADMIN_ROLE))
					.param("name", "Event")
					.param("id", "10")
					.param("date", LocalDate.now().toString())
					.param("time", LocalTime.now().toString())
					.param("Venue_id", "")
					.param("description", "This event is...")
					.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
					.andExpect(model().attributeHasFieldErrors("event", "date"));


			verify(eventService, never()).save(event);
		}
	// Tests posting an event with a description longer than 500 characters
		@Test
		public void postLongEventDescription() throws Exception {
			mvc.perform(MockMvcRequestBuilders.post("/events").contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.with(user("Markel").roles(Security.ADMIN_ROLE))
					.param("name", "Event")
					.param("id", "10")
					.param("date", LocalDate.now().toString())
					.param("time", LocalTime.now().toString())
					.param("Venue_id", "10")
					.param("description", "Nam quis nulla. Integer malesuada. In in enim a arcu imperdiet malesuada. Sed vel lectus. Donec odio urna, tempus molestie, porttitor ut, iaculis quis, sem. Phasellus rhoncus. Aenean id metus id velit ullamcorper pulvinar. Vestibulum fermentum tortor id mi. Pellentesque ipsum. Nulla non arcu lacinia neque faucibus fringilla. Nulla non lectus sed nisl molestie malesuada. Proin in tellus sit amet nibh dignissim sagittis. Vivamus luctus egestas leo. Maecenas sollicitudin. Nullam rhoncus aliquam metu")
					.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
					.andExpect(model().attributeHasFieldErrors("event", "description"));


			verify(eventService, never()).save(event);
		}
}
