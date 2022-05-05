package uk.ac.man.cs.eventlite.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.hamcrest.Matchers.endsWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.annotation.PathVariable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(SpringExtension.class)
@WebMvcTest(VenuesController.class)
@Import(Security.class)
public class VenuesControllerTest {

	private final static String BAD_ROLE = "USER";
	
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
	public void postVenues() throws Exception {
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());

		mvc.perform(post("/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "AAAVenue")
				.param("latitude", "0.0")
				.param("longitude", "0.0")
				.param("roadName", "100 Sackville Street")
				.param("postcode", "M1 3BB")
				.param("capacity", "1000")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
				.andExpect(view().name("redirect:/venues")).andExpect(model().hasNoErrors())
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeExists("ok_message"));

	   verify(venueService).save(arg.capture());
	   List<Venue> capturedParams = arg.getAllValues();
	   assertThat("100 Sackville Street", equalTo(capturedParams.get(0).getRoadName()));
	   assertThat("AAAVenue", equalTo(capturedParams.get(capturedParams.size()-1).getName()));
	}

	@Test
	public void getVenueNotFound() throws Exception {
		when(venueService.findById(1)).thenReturn(Optional.empty());
		
		mvc.perform(get("/venues/1").accept(MediaType.TEXT_HTML)).andExpect(status().isNotFound())
				.andExpect(view().name("venues/not_found")).andExpect(handler().methodName("getVenue"));
	}

	@Test
	public void getVenue() throws Exception {
		when(venueService.findById(1)).thenReturn(Optional.of(venue));

		mvc.perform(get("/venues/1").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("venues/venue_details")).andExpect(handler().methodName("getVenue"));

		verify(venue).getId();
	}
	
	@Test
	public void deleteVenueNotFound() throws Exception {
		when(venueService.existsById(1)).thenReturn(false);

		mvc.perform(delete("/venues/1").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML)
				.with(csrf())).andExpect(view().name("redirect:/venues"))
				.andExpect(handler().methodName("deleteVenue")).andExpect(flash().attributeExists("error_message"));

		verify(venueService, never()).deleteById(1);
	}
	
	@Test
	public void deleteVenueContainsAtLeastOneEvent() throws Exception {
		// Create a venue
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());

		mvc.perform(post("/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "AAAVenue")
				.param("latitude", "0.0")
				.param("longitude", "0.0")
				.param("roadName", "100 Sackville Street")
				.param("postcode", "M1 3BB")
				.param("capacity", "1000")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
				.andExpect(view().name("redirect:/venues")).andExpect(model().hasNoErrors())
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeExists("ok_message"));

	   verify(venueService).save(arg.capture());
	   List<Venue> capturedParams = arg.getAllValues();
	   assertThat("100 Sackville Street", equalTo(capturedParams.get(0).getRoadName()));
	   assertThat("AAAVenue", equalTo(capturedParams.get(capturedParams.size()-1).getName()));

		// Create one mock event which linked to the mock venue created above
		Event event = new Event();
		event.setDate(LocalDateTime.now().plusDays(1).toLocalDate());
		event.setDescription("some description...");
		event.setId(10);
		event.setName("Aevent");
		event.setTime(LocalTime.MIDNIGHT);
		event.setVenue(venue);
		eventService.save(event);

		verify(venueService, never()).deleteById(10);
	}
	
	@Test
	public void createVenueBadRole() throws Exception {
		mvc.perform(
				post("/venues").with(user("Rob").roles(BAD_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("template", "Howdy, %s!").accept(MediaType.TEXT_HTML).with(csrf()))
				.andExpect(status().isForbidden());

		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void deleteVenueContainsNoEvent() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);

		mvc.perform(delete("/venues/1").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML)
				.with(csrf())).andExpect(status().isFound()).andExpect(view().name("redirect:/venues"))
				.andExpect(handler().methodName("deleteVenue")).andExpect(flash().attributeExists("ok_message"));

		verify(venueService).deleteById(1);
	}
	
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

		@Test
		public void updateVenue() throws Exception {
			ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);

		    mvc.perform(put("/venues/update/1").with(user("Rob").roles(Security.ADMIN_ROLE))
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.param("name", "BABVenue")
					.param("latitude", "0.0")
					.param("longitude", "0.0")
					.param("roadName", "190 Sackville Street")
					.param("postcode", "M1 3BB")
					.param("capacity", "1000")
					.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
					.andExpect(view().name("redirect:/venues")).andExpect(model().hasNoErrors())
					.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeExists("ok_message"));

		   verify(venueService).save(arg.capture());
		   List<Venue> capturedParams = arg.getAllValues();
		   assertThat("190 Sackville Street", equalTo(capturedParams.get(0).getRoadName()));
		   assertThat("BABVenue", equalTo(capturedParams.get(capturedParams.size()-1).getName()));
		}


}