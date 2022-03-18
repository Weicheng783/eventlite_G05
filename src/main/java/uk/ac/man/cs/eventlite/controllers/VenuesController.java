package uk.ac.man.cs.eventlite.controllers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;

@Controller
@RequestMapping(value = "/venues", produces = { MediaType.TEXT_HTML_VALUE })
public class VenuesController {

	private final static Logger log = LoggerFactory.getLogger(VenuesController.class);

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;

	private long id;

	@ExceptionHandler(EventNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String eventNotFoundHandler(EventNotFoundException ex, Model model) {
		model.addAttribute("not_found_id", ex.getId());

		return "venues/not_found";
	}

	@GetMapping("/{id}")
	public String getVenue(@PathVariable("id") long id, Model model) {
		Optional<Venue> venue = venueService.findById(id);
		HttpHeaders headers = new HttpHeaders();
		if (venue.isEmpty()) {
			log.info("Event not found");
			throw new EventNotFoundException(id);
		}

		log.info("Venue found. redirecting...");
		model.addAttribute("venue", venue.get());
		return "venues/venue_details";
	}

	@GetMapping
	public String getAllVenues(Model model) {

		 model.addAttribute("venues", venueService.findAll());

		return "venues/index";
	}
		
	@GetMapping("/new")
	public String newVenue(Model model) {
		if (!model.containsAttribute("venue")) {
			model.addAttribute("venue", new Venue());
		}
		return "venues/new";
	}

	@PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String createVenue(@RequestBody @Valid @ModelAttribute Venue venue, BindingResult errors,
			Model model, RedirectAttributes redirectAttrs) {

		if (errors.hasErrors()) {
			model.addAttribute("venue", venue);
			return "events/new";
		}

		venueService.save(venue);
		redirectAttrs.addFlashAttribute("ok_message", "New venue added.");


		return "redirect:/venues";
	}
	
	@RequestMapping(value="/{id}" ,method=RequestMethod.DELETE)
	public String deleteVenue(@PathVariable("id") long id, RedirectAttributes redirectAttrs) {
		venueService.findById(id).orElseThrow(() -> new EventNotFoundException(id));
		venueService.deleteById(id);
		redirectAttrs.addFlashAttribute("ok_message", "Selected venue deleted!");
		return "redirect:/venues";
	}
}