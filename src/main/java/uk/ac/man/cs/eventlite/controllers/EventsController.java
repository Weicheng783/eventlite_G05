package uk.ac.man.cs.eventlite.controllers;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;

@Controller
@RequestMapping(value = "/events", produces = { MediaType.TEXT_HTML_VALUE })
public class EventsController {

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
		throw new EventNotFoundException(id);
	}

	@GetMapping
	public String getAllEvents(Model model) {

		model.addAttribute("events", eventService.findAll());

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
	


}
