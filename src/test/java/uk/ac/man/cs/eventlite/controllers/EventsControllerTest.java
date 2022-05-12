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

import javax.print.attribute.standard.Media;

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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;

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
	
	@Mock
	private Model model;

	@MockBean
	private EventService eventService;

	@MockBean
	private VenueService venueService;
	
	@Test
	public void tweetTest() throws Exception{
		String tweetSent = new String(LocalDate.now().toString() + "//" + LocalTime.now().toString());
		when(eventService.existsById(1)).thenReturn(false);
		mvc.perform(get("/events/tweet").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML).param("eventId", "1").param("tweet", tweetSent)
				.with(csrf())).andExpect(status().isFound()).andExpect(view().name("redirect:/events/1"))
				.andExpect(handler().methodName("createTweet")).andExpect(flash().attributeExists("ok_message_Tweets"));

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
		// Tests posting an event with past date
		@Test
		public void postEventPastDate() throws Exception {
			mvc.perform(MockMvcRequestBuilders.post("/events").contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.with(user("Markel").roles(Security.ADMIN_ROLE))
					.param("name", "Event")
					.param("id", "10")
					.param("date", LocalDate.now().minusDays(3).toString())
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
		
		// Test posting an event all right
		@Test
		public void postingEventSuccessful() throws Exception {
			mvc.perform(MockMvcRequestBuilders.post("/events").contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.with(user("Markel").roles(Security.ADMIN_ROLE))
					.param("name", "Event")
					.param("id", "10")
					.param("date", LocalDateTime.now().plusDays(1).toString())
					.param("time", "00:00")
					.param("Venue_id", "10")
					.param("description", "This event is...")
					.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk());
		}
		
		@Test
		public void deleteAllEvents() throws Exception {
			mvc.perform(delete("/events").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML)
					.with(csrf())).andExpect(status().isFound()).andExpect(view().name("redirect:/events"))
					.andExpect(handler().methodName("deleteAllEvents")).andExpect(flash().attributeExists("ok_message"));

			verify(eventService).deleteAll();
		}
		
		@Test
		public void deleteEventFound() throws Exception {
			Optional<Event> testEvent = Optional.of(event);
			Long id = (long)1;
			
			when(eventService.findById(id)).thenReturn(testEvent);
			
			mvc.perform(delete("/events/1").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
			.andExpect(view().name("redirect:/events")).andExpect(handler().methodName("deleteEvent")).andExpect(flash().attributeExists("ok_message"));
			
		}


		@Test
		public void deleteEventNotFound() throws Exception {
			when(eventService.existsById(1)).thenReturn(false);

			mvc.perform(delete("/events/1").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML)
					.with(csrf())).andExpect(status().isNotFound()).andExpect(view().name("events/not_found"))
					.andExpect(handler().methodName("deleteEvent"));

			verify(eventService, never()).deleteById(1);
		}

		@Test
		public void searchEventByNameContaining() throws Exception {
			mvc.perform(get("/events/search?name=Test%20Event").accept(MediaType.TEXT_HTML)).andExpect(status().isOk()).andExpect(view().name("events/index"))
					.andExpect(handler().methodName("searchEventByNameContaining"))
					.andExpect(model().attributeExists("eventFuture"))
					.andExpect(model().attributeExists("eventPast"));
		}
		
		@Test
		public void updateEventNotSuccessful() throws Exception {

		    mvc.perform(put("/events/update/1").with(user("Rob").roles(Security.ADMIN_ROLE))
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.param("name", "BABEvent")
					.param("date", LocalDateTime.now().plusDays(1).toLocalDate().toString())
					.param("time", LocalTime.MIDNIGHT.toString())
					.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
					.andExpect(view().name("redirect:/events")).andExpect(model().hasNoErrors())
					.andExpect(handler().methodName("updateEvent")).andExpect(flash().attributeExists("error_message")).andReturn();

		}
		
		@Test
		public void getEventToUpdateTest() throws Exception{
			//			Long id = 1;
			Event event1 = new Event();
			Venue venue1 = new Venue();
			event1.setDate(LocalDateTime.now().plusDays(1).toLocalDate());
			event1.setDescription("some description...");
			event1.setId(1);
			event1.setName("Aevent");
			event1.setTime(LocalTime.MIDNIGHT);
			event1.setVenue(venue1);
			when(eventService.findById(1)).thenReturn(Optional.of(event1));
//			when(eventService.findEventById((long)1).get()).thenReturn(event1);
			
			
			
			mvc.perform(get("/events/update/1").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML))
					.andExpect(status().isOk())
					.andExpect(view().name("events/update")).andExpect(handler().methodName("getEventToUpdate"));
		}
		
		@Test
		public void getAllEvents() throws Exception {
			// TODO: Needs more work
		    EventsController eventcontrol = new EventsController();
//		    eventcontrol.createEvent(event, null, null, null)
		}
	
		@Test
		public void getEvent() throws Exception {
			Event event1 = new Event();
			Venue venue1 = new Venue();
			event1.setDate(LocalDateTime.now().plusDays(1).toLocalDate());
			event1.setDescription("some description...");
			event1.setId(1);
			event1.setName("Aevent");
			event1.setTime(LocalTime.MIDNIGHT);
			event1.setVenue(venue1);
			
			when(eventService.findById(1)).thenReturn(Optional.of(event1));

			mvc.perform(get("/events/1").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
					.andExpect(view().name("events/event_details")).andExpect(handler().methodName("getEvent"));
		}
}
