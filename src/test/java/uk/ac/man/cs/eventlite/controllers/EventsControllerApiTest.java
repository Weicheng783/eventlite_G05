package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;


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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindingResult;

import uk.ac.man.cs.eventlite.assemblers.EventModelAssembler;
import uk.ac.man.cs.eventlite.assemblers.VenueModelAssembler;
import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;
import uk.ac.man.cs.eventlite.exceptions.VenueNotFoundException;

import static org.mockito.Mockito.never;

@ExtendWith(SpringExtension.class)
@WebMvcTest(EventsControllerApi.class)
@Import({ Security.class, EventModelAssembler.class })
public class EventsControllerApiTest {

	@Autowired
	private MockMvc mvc;
	
	@Mock
	private Event event;
	
	@Mock
	private BindingResult br;

	@MockBean
	private EventService eventService;
	
	@MockBean
	private VenueService venueService;

	@MockBean
    private VenueModelAssembler venueAssembler;
	
	@Mock
    private EventModelAssembler eventAssembler;

	@Test
	public void newEvent() throws Exception {
		mvc.perform(get("/api/events/new").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotAcceptable())
		.andExpect(handler().methodName("newEvent"));
	}

	
	@Test
	public void createEventNotAccept() throws Exception {
		EventsControllerApi vapi = new EventsControllerApi();
		vapi.eventService = eventService;
		vapi.eventAssembler = eventAssembler;
		vapi.createEvent(event, br);
	}
	
	@Test
	public void createEventNotSuccess() throws Exception {
		when(br.hasErrors()).thenReturn(true);
		EventsControllerApi vapi = new EventsControllerApi();
		vapi.eventService = eventService;
		vapi.eventAssembler = eventAssembler;

		ResponseEntity<?> e = vapi.createEvent(event, br);
		e.equals(ResponseEntity.unprocessableEntity().build());
	}
	
	@Test
	public void getEventSuccess() throws Exception {
		when(eventService.existsById(99)).thenReturn(true);
		mvc.perform(get("/api/events/99").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getEvent"));
	}
	
	@Test
	public void getEventVenueSuccess() throws Exception {
		when(eventService.existsById(99)).thenReturn(true);
		mvc.perform(get("/api/events/99/venue").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getEventVenue"));
	}
	
	@Test
	public void getVenueNoEvent() throws Exception{
		when(eventService.existsById(99)).thenReturn(false);
		mvc.perform(get("/api/events/99/venue").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
		.andExpect(jsonPath("$.error", containsString("event 99"))).andExpect(jsonPath("$.id", equalTo(99)))
		.andExpect(handler().methodName("getEventVenue"));
	}
	
	@Test
	public void deleteEventNotExists() throws Exception {
		when(eventService.existsById(0)).thenReturn(false);
		EventsControllerApi vapi = new EventsControllerApi();
		vapi.eventService = eventService;
		EventNotFoundException es = new EventNotFoundException(0);
		try {
			vapi.deleteEvent(0);
		} catch (Exception e) {e.equals(es);}
	}
	
	@Test
	public void deleteVenueSuccess() throws Exception {
		when(eventService.existsById(0)).thenReturn(true);
		EventsControllerApi vapi = new EventsControllerApi();
		vapi.eventService = eventService;
		vapi.deleteEvent(0);
	}
	
	@Test
	public void getIndexWhenNoEvents() throws Exception {
		when(eventService.findAll()).thenReturn(Collections.<Event>emptyList());

		mvc.perform(get("/api/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getAllEvents")).andExpect(jsonPath("$.length()", equalTo(1)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/events")));

		verify(eventService).findAll();
	}

	@Test
	public void getIndexWithEvents() throws Exception {
		Event e = new Event();
		e.setId(0);
		e.setName("Event");
		e.setDate(LocalDate.now());
		e.setTime(LocalTime.now());
		e.setVenue(new Venue());
		when(eventService.findAll()).thenReturn(Collections.<Event>singletonList(e));

		mvc.perform(get("/api/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getAllEvents")).andExpect(jsonPath("$.length()", equalTo(2)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/events")))
				.andExpect(jsonPath("$._embedded.events.length()", equalTo(1)));

		verify(eventService).findAll();
	}

	@Test
	public void getEventNotFound() throws Exception {
		mvc.perform(get("/api/events/99").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("event 99"))).andExpect(jsonPath("$.id", equalTo(99)))
				.andExpect(handler().methodName("getEvent"));
	}

	
	@Test
	public void deleteAllEvents() throws Exception {
		mvc.perform(delete("/api/events").with(user("Rob").roles(Security.ADMIN_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent()).andExpect(content().string(""))
				.andExpect(handler().methodName("deleteAllEvents"));

		verify(eventService).deleteAll();
	}


}
