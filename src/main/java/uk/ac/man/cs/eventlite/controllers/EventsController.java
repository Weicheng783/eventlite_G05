package uk.ac.man.cs.eventlite.controllers;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import uk.ac.man.cs.eventlite.config.data.InitialDataLoader;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;

import java.util.Optional;
import java.util.ArrayList;
import java.time.*;

@Controller
@RequestMapping(value = "/events", produces = { MediaType.TEXT_HTML_VALUE })
public class EventsController {

	private final static Logger log = LoggerFactory.getLogger(EventsController.class);

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;

	@ExceptionHandler(EventNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String eventNotFoundHandler(EventNotFoundException ex, Model model) {
		model.addAttribute("not_found_id", ex.getId());

		return "events/not_found";
	}

	@GetMapping("/{id}")
	public String getEvent(@PathVariable("id") long id, Model model) {
		Optional<Event> event = eventService.findById(id);
		HttpHeaders headers = new HttpHeaders();
		if (event.isEmpty()) {
			log.info("Event not found");
			throw new EventNotFoundException(id);
		}

		log.info("Event found. redirecting...");
		model.addAttribute("event", event.get());
		return "events/event_details";
	}

	@GetMapping
	public String getAllEvents(Model model) {
		ArrayList<Event> eventFuture = new ArrayList<Event>();
		ArrayList<Event> eventPast = new ArrayList<Event>();

		LocalDate dateNow = LocalDate.now();
		LocalTime timeNow = LocalTime.now();

		for (Event event : eventService.findAllByOrderByDateAscNameAsc()) {
			if (event.getDate() == null) {
				continue;
			}
			if (dateNow.isBefore(event.getDate()) || (dateNow.isEqual(event.getDate()) && timeNow.isBefore(event.getTime()))) {
				eventFuture.add(event);
			} else {
				eventPast.add(event);
			}
		}
		
		model.addAttribute("eventFuture", eventFuture);
		model.addAttribute("eventPast", eventPast);

		// model.addAttribute("events", eventService.findAll());

		return "events/index";
	}
	
	@RequestMapping(value = "/update/{id}", method = RequestMethod.GET)
	public String getEventToUpdate(Model model, @PathVariable Long id) {
		Event event = eventService.findEventById(id).get();

		model.addAttribute("event", event);
		model.addAttribute("venues", venueService.findAll());
		
		return "events/update";
	}
	
	@RequestMapping(value = "/update/{id}", method = RequestMethod.PUT)
	public String updateEvent(@RequestBody @Valid @ModelAttribute ("event") Event event,
			BindingResult errors,@PathVariable Long id, Model model, RedirectAttributes redirectAttrs) {
		
		if (errors.hasErrors()) {
			model.addAttribute("event", event);
			model.addAttribute("venues", venueService.findAll());
			return "events/update";
		}
		
		Event eventUpdated = eventService.findEventById(id).get();
		eventUpdated.setName(event.getName());
		eventUpdated.setDate(event.getDate());
		eventUpdated.setTime(event.getTime());
		eventUpdated.setVenue(event.getVenue());

		eventService.save(eventUpdated);
		redirectAttrs.addFlashAttribute("ok_message", "The event has been updated.");
		
		return "redirect:/events";
	}
	
	@RequestMapping(value="/{id}" ,method=RequestMethod.DELETE)
	public String deleteEvent(@PathVariable("id") long id, RedirectAttributes redirectAttrs) {
		eventService.findById(id).orElseThrow(() -> new EventNotFoundException(id));
		eventService.deleteById(id);
		redirectAttrs.addFlashAttribute("ok_message", "Selected event deleted!");
		return "redirect:/events";
	}

	@DeleteMapping
	public String deleteAllEvents(RedirectAttributes redirectAttrs) {
		eventService.deleteAll();
		redirectAttrs.addFlashAttribute("ok_message", "All greetings deleted.");
		return "redirect:/events";
	}

	@RequestMapping(value = "/search", method = RequestMethod.GET)
	public String searchEventByNameContaining(@RequestParam("name") String name, Model model) {
		ArrayList<Event> eventFuture = new ArrayList<Event>();
		ArrayList<Event> eventPast = new ArrayList<Event>();
		
		LocalDate dateNow = LocalDate.now();
		LocalTime timeNow = LocalTime.now();
		
		for (Event event : eventService.findByNameIgnoreCaseContainingOrderByDateAscNameAsc(name)) {
			if (event.getDate() == null) {
				continue;
			}
			else if (dateNow.isBefore(event.getDate()) || (dateNow.isEqual(event.getDate()) && timeNow.isBefore(event.getTime()))) {
				eventFuture.add(event);
			}
		}
		
		for (Event event : eventService.findByNameIgnoreCaseContainingOrderByDateDescNameAsc(name)) {
			if (event.getDate() == null) {
				continue;
			}
			else if (dateNow.isAfter(event.getDate()) || (dateNow.isEqual(event.getDate()) && timeNow.isAfter(event.getTime()))) {
				eventPast.add(event);
			}
		}

		model.addAttribute("eventFuture", eventFuture);
		model.addAttribute("eventPast", eventPast);
		
		return "events/index";
	}

	@GetMapping("/new")
	public String newEvent(Model model) {
		if (!model.containsAttribute("event")) {
			model.addAttribute("event", new Event());
		}
		model.addAttribute("venues", venueService.findAll());
		return "events/new";
	}

	@PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String createEvent(@RequestBody @Valid @ModelAttribute Event event, BindingResult errors,
			Model model, RedirectAttributes redirectAttrs) {

		if (errors.hasErrors()) {
			model.addAttribute("event", event);
			model.addAttribute("venues", venueService.findAll());
			return "events/new";
		}

		eventService.save(event);
		redirectAttrs.addFlashAttribute("ok_message", "New event added.");

		return "redirect:/events";
	}

}
