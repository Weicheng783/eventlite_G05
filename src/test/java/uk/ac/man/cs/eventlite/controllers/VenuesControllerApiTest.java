package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.Mock;
import org.mockito.internal.junit.JUnitRule;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;

import uk.ac.man.cs.eventlite.assemblers.EventModelAssembler;
import uk.ac.man.cs.eventlite.assemblers.VenueModelAssembler;
import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.exceptions.VenueNotFoundException;

import static org.mockito.Mockito.never;

@ExtendWith(SpringExtension.class)
@WebMvcTest(VenuesControllerApi.class)
@Import({ Security.class, VenueModelAssembler.class })
public class VenuesControllerApiTest {

	private final static String BAD_ROLE = "USER";
	
	@Autowired
	private MockMvc mvc;
	
	@Mock
	private Venue venue;
	
	@Mock
	private BindingResult br;
	
	@MockBean
	private EntityModel<Venue> entity;

	@MockBean
	private VenueService venueService;
	
	@MockBean
	private EventService eventService;

	@MockBean
    private EventModelAssembler eventAssembler;
	
	@Mock
    private VenueModelAssembler venueAssembler;

	
//	@Mock
//	private VenuesControllerApi vcapi;
	
	@Test
	public void getIndexWhenNoVenues() throws Exception {
		
		when(venueService.findAll()).thenReturn(Collections.<Venue> emptyList());

		mvc.perform(get("/api/venues").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
			.andExpect(handler().methodName("getAllVenues")).andExpect(jsonPath("$.length()", equalTo(1)))
			.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues")));

		verify(venueService).findAll();
	}

	@Test
	public void getVenueNotFound() throws Exception {
		mvc.perform(get("/api/venues/99").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("venue 99"))).andExpect(jsonPath("$.id", equalTo(99)))
				.andExpect(handler().methodName("getVenue"));
	}
	
	@Test
	public void getVenueNext3EventsNotFound() throws Exception {
		mvc.perform(get("/api/venues/99/next3events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("venue 99"))).andExpect(jsonPath("$.id", equalTo(99)))
				.andExpect(handler().methodName("getVenueNext3Events"));
	}
	
	@Test
	public void getVenueEventsNotFound() throws Exception {
		mvc.perform(get("/api/venues/99/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("venue 99"))).andExpect(jsonPath("$.id", equalTo(99)))
				.andExpect(handler().methodName("getVenueEvents"));
	}
	
	@Test
	public void getVenueSuccess() throws Exception {
		when(venueService.existsById(99)).thenReturn(true);
		mvc.perform(get("/api/venues/99").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getVenue"));
	}
	
	@Test
	public void getVenueEventsSuccess() throws Exception {
		when(venueService.existsById(99)).thenReturn(true);
		mvc.perform(get("/api/venues/99/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getVenueEvents"));
	}
	
	@Test
	public void getVenueNext3EventsSuccess() throws Exception {
		when(venueService.existsById(99)).thenReturn(true);
		mvc.perform(get("/api/venues/99/next3events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getVenueNext3Events"));
	}
	
	@Test
	public void newVenue() throws Exception {
		mvc.perform(get("/api/venues/new").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotAcceptable())
		.andExpect(handler().methodName("newVenue"));
	}
	
	@Test
	public void createVenueNotAccept() throws Exception {
		VenuesControllerApi vapi = new VenuesControllerApi();
		vapi.venueService = venueService;
		vapi.venueAssembler = venueAssembler;
		vapi.createVenue(venue, br);
	}
	
	@Test
	public void createVenueNotSuccess() throws Exception {
		when(br.hasErrors()).thenReturn(true);
		VenuesControllerApi vapi = new VenuesControllerApi();
		vapi.venueService = venueService;
		vapi.venueAssembler = venueAssembler;

		ResponseEntity<?> e = vapi.createVenue(venue, br);
		e.equals(ResponseEntity.unprocessableEntity().build());
	}
	
	@Test
	public void deleteVenueContainsNoEvent() throws Exception {
		when(venueService.existsById(0)).thenReturn(true);
		VenuesControllerApi vapi = new VenuesControllerApi();
		vapi.venueService = venueService;
		vapi.deleteVenue(0);
	}
	
	@Test
	public void deleteVenueNotExists() throws Exception {
		when(venueService.existsById(0)).thenReturn(false);
		VenuesControllerApi vapi = new VenuesControllerApi();
		vapi.venueService = venueService;
		VenueNotFoundException es = new VenueNotFoundException(0);
		try {
			vapi.deleteVenue(0);
		} catch (Exception e) {e.equals(es);}
	}
}