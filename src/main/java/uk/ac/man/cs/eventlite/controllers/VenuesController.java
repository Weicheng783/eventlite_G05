package uk.ac.man.cs.eventlite.controllers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;

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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;
import uk.ac.man.cs.eventlite.exceptions.VenueNotFoundException;

@Controller
@RequestMapping(value = "/venues", produces = { MediaType.TEXT_HTML_VALUE })
public class VenuesController {

	private final static Logger log = LoggerFactory.getLogger(VenuesController.class);

	private final String MAPBOX_ACCESS_TOKEN = "pk.eyJ1IjoiZGludWQxMSIsImEiOiJjbDE1Nzdib3QwaDJ6M2pzZ2p4bGdhZWo2In0.pNx1qRgo7vsmuoVt0R5-nQ";

	@Autowired
	private VenueService venueService;
	
	@Autowired
	private EventService eventService;
	
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
			log.info("Venue not found");
			throw new VenueNotFoundException(id);
		}

		log.info("Venue found. redirecting...");
		model.addAttribute("venue", venue.get());
		
		// 20220325 Venue search upcoming eligible Events.
		ArrayList<Event> eventFuture = new ArrayList<Event>();
		LocalDate dateNow = LocalDate.now();
		LocalTime timeNow = LocalTime.now();

		for (Event event : eventService.findAllByOrderByDateAscNameAsc()) {
			// If they are not equal OR event has no date, skip it.
			if ( (event.getVenue().getName().compareTo(venueService.findById(id).get().getName()) != 0)  || (event.getDate() == null) ) {
				continue;
			}
			else if (dateNow.isBefore(event.getDate()) || (dateNow.isEqual(event.getDate()) && timeNow.isBefore(event.getTime()))) {
				eventFuture.add(event);
			}
		}
		
		model.addAttribute("eventFuture", eventFuture);
		
		return "venues/venue_details";
	}
	
	@GetMapping
	public String getAllVenues(Model model) {
		model.addAttribute("venues", venueService.findAll());
		return "venues/index";
	}
	
	@RequestMapping(value = "/search", method = RequestMethod.GET)
	public String searchVenueByNameContaining(@RequestParam("name") String name, Model model) {
		model.addAttribute("venues", venueService.findByNameIgnoreCaseContainingOrderByNameAsc(name));
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
			System.out.println(errors.getAllErrors());
			return "venues/new";
		}

		MapboxGeocoding mapboxGeocoding = MapboxGeocoding.builder()
			.accessToken(MAPBOX_ACCESS_TOKEN)
			.query(venue.getRoadName() + " " + venue.getPostcode())
			.build();

		try {
			Thread.sleep(1000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		
		mapboxGeocoding.enqueueCall(new Callback<GeocodingResponse>() {
			@Override
			public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
			
				List<CarmenFeature> results = response.body().features();
			
				if (results.size() > 0) {
			
					// Log the first results Point.
					Point firstResultPoint = results.get(0).center();
					log.info("onResponse: " + firstResultPoint.toString());
					venue.setLatitude(firstResultPoint.latitude());
					venue.setLongitude(firstResultPoint.longitude());
					venueService.save(venue);
					redirectAttrs.addFlashAttribute("ok_message", "New venue added.");
			
				} else {
					// No result for your request were found.
					log.error("onResponse: No result found");
			
				}
			}
			
			@Override
			public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
				throwable.printStackTrace();
			}
		});
		
		return "redirect:/venues";
	}
	
	@RequestMapping(value="/{id}" ,method=RequestMethod.DELETE)
	public String deleteVenue(@PathVariable("id") long id, RedirectAttributes redirectAttrs) {

		// A venue cannot be deleted if it has one or more events.
		Iterable<Event> allEvents = eventService.findAll();
		for(Event t:allEvents) {
			if(t.getVenue().getName().compareTo(venueService.findById(id).get().getName()) == 0) {
				redirectAttrs.addFlashAttribute("ok_message", "Cannot delete this venue because it has at least one event contains.");
				System.out.println("Cannot delete this venue because it has at least one event contains.");
				return "redirect:/venues/" + id;
			}
		}

		venueService.findById(id).orElseThrow(() -> new VenueNotFoundException(id));
		venueService.deleteById(id);
		redirectAttrs.addFlashAttribute("ok_message", "Selected venue deleted!");
		return "redirect:/venues";
	}
	
	@RequestMapping(value = "/update/{id}", method = RequestMethod.GET)
	public String getVenueToUpdate(Model model, @PathVariable Long id) {
		Venue venue = venueService.findById(id).get();

		model.addAttribute("venue", venue);
		
		return "/venues/update";
	}
	
	@RequestMapping(value = "/update/{id}", method = RequestMethod.PUT)
	public String updateVenue(@RequestBody @Valid @ModelAttribute ("venue") Venue venue,
			BindingResult errors,@PathVariable Long id, Model model, RedirectAttributes redirectAttrs) {
		
		if (errors.hasErrors()) {
			model.addAttribute("venues", venue);
			return "/venues/update";
		}
		
		Venue venueUpdated = venueService.findById(id).get();
		venueUpdated.setName(venue.getName());
		venueUpdated.setCapacity(venue.getCapacity());
		venueUpdated.setRoadName(venue.getRoadName());
		venueUpdated.setPostcode(venue.getPostcode());
		// NOTE: Here the latitude and longitude CAN be updated manually
		// The redirect page also not refresh correctly if you add the Geocoding method here
		// We needn't confine them to the value that is found on Geocoding
		// Reference: Week 7 Lab manual - Warning 3: make sure that the postcode is used in the request so ambiguity is removed (ie there is one "Chapel Street" in almost all British towns).
		venueUpdated.setLatitude(venue.getLatitude());
		venueUpdated.setLongitude(venue.getLongitude());

		venueService.save(venueUpdated);
		redirectAttrs.addFlashAttribute("ok_message", "The venue has been updated.");
		return "redirect:/venues";
	}
}