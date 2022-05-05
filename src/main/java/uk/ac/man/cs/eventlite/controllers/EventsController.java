package uk.ac.man.cs.eventlite.controllers;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;

import java.util.*;
import java.util.stream.Collectors;
import java.time.*;

@Controller
@RequestMapping(value = "/events", produces = { MediaType.TEXT_HTML_VALUE })
public class EventsController {

	private final static Logger log = LoggerFactory.getLogger(EventsController.class);

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;

	public Twitter getTwitterObject() {
		
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		.setOAuthConsumerKey("VSFHFyXd2LoeXcpCNjqlVDmrV")
	    .setOAuthConsumerSecret("hQdlQWtiOv3qxPEGBxmGkgWR9F4feYyoekaTFxizkizSauZZrN")
	    .setOAuthAccessToken("1509559619764559877-f1iI2Grdrqa1RrVgbkyA7Hci89l4W9")
	    .setOAuthAccessTokenSecret("qbM1YLv2rSIzRLTf30EhQnAWOqa7NCCIa3U5rwSCaGmUH");

		TwitterFactory tf = new TwitterFactory(cb.build());
	    Twitter twitter = tf.getInstance();
		
		return twitter;
	}

	@RequestMapping(value="/tweet" ,method=RequestMethod.GET)
	public String createTweet(@RequestParam("eventId") String eventId, @RequestParam("tweet") String tweet, 
			Model model, RedirectAttributes redirectAttrs) throws TwitterException {

		Twitter twitter = getTwitterObject();
	    try {
	    	Status status = twitter.updateStatus(tweet);
	    	redirectAttrs.addFlashAttribute("ok_message_Tweets", status.getText());
	    }catch(Exception e){
	    	redirectAttrs.addFlashAttribute("error_message", "The Tweet has NOT been posted. Your exception is: " + e.toString());
	    }

	    return "redirect:/events/"+ eventId;   
	}

	@ExceptionHandler(EventNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String eventNotFoundHandler(EventNotFoundException ex, Model model) {
		model.addAttribute("not_found_id", ex.getId());

		return "events/not_found";
	}
	
	@GetMapping("/{id}")
	public String getEvent(@PathVariable("id") long id, Model model) {
		Optional<Event> event = eventService.findById(id);
		if (event.isEmpty()) {
			log.info("Event not found");
			throw new EventNotFoundException(id);
		}

		log.info("Event found. redirecting...");
		model.addAttribute("event", event.get());
		return "events/event_details";
	}
	
	public void getTweets(Model model) {
		Twitter twitter = getTwitterObject();
		ResponseList<Status> tweetList;
		List<Status> tweetReadyList;

		try {
			tweetList = twitter.getUserTimeline();
			if(tweetList.size() <= 5) {
				tweetReadyList = tweetList.subList(0, tweetList.size());
			}else {
				tweetReadyList = tweetList.subList(0, 5);
			}
			model.addAttribute("tweets", tweetReadyList);
		} catch (TwitterException e) {
			e.printStackTrace();
		}
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
			else if (dateNow.isBefore(event.getDate()) || (dateNow.isEqual(event.getDate()) && timeNow.isBefore(event.getTime()))) {
				eventFuture.add(event);
			}
		}
		
		for (Event event : eventService.findAllByOrderByDateDescNameAsc()) {
			if (event.getDate() == null) {
				continue;
			}
			else if (dateNow.isAfter(event.getDate()) || (dateNow.isEqual(event.getDate()) && timeNow.isAfter(event.getTime()))) {
				eventPast.add(event);
			}
		}
		
		model.addAttribute("eventFuture", eventFuture);
		model.addAttribute("eventPast", eventPast);

		// model.addAttribute("events", eventService.findAll());
		
		// Twitter Getting Latest Five Status
		getTweets(model);

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
		
		Event eventUpdated;
		
		if (errors.hasErrors()) {
			model.addAttribute("event", event);
			model.addAttribute("venues", venueService.findAll());
			redirectAttrs.addFlashAttribute("error_message", "This event has not been updated correctly, please check carefully the fields and try it again.");
			Event event1 = new Event();
			Venue venue1 = new Venue();
			event1.setDate(LocalDateTime.now().plusDays(1).toLocalDate());
			event1.setDescription("some description...");
			event1.setId(10);
			event1.setName("Aevent");
			event1.setTime(LocalTime.MIDNIGHT);
			event1.setVenue(venue1);
			eventUpdated = event1;
		}else {
			eventUpdated = eventService.findEventById(id).get();
			eventUpdated.setName(event.getName());
			eventUpdated.setDate(event.getDate());
			eventUpdated.setTime(event.getTime());
			eventUpdated.setVenue(event.getVenue());

			eventService.save(eventUpdated);
			redirectAttrs.addFlashAttribute("ok_message", "The event has been updated.");
		}
		
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

	
//	  @RequestMapping("queryEventsInfo") 
//	  @ResponseBody
//	  public Map<String, Object> queryEventsInfo(HttpServletRequest request,HttpServletResponse response){ 
//	    Map<String, Object> map = new HashMap<String, Object>(); 
//	    try {
//	      @SuppressWarnings("rawtypes") 
//	      List eventList = (List) eventService.findAll();
//	      if(null != eventList && eventList.size() >0 ){ 
//	        map.put("event", eventList); 
//	      }
//	    } catch (Exception e) {
//	      e.printStackTrace(); 
//	    }
//	    return map; 
//	  }
}
