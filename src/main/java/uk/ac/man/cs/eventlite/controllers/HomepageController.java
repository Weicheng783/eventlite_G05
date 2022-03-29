package uk.ac.man.cs.eventlite.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

import java.util.*;
import java.time.*;

@Controller
@RequestMapping(value = "/", produces = { MediaType.TEXT_HTML_VALUE })
public class HomepageController {

	private final static Logger log = LoggerFactory.getLogger(EventsController.class);

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;

	private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
		list.sort(Map.Entry.comparingByValue());

		Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}

		return result;
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String getNext3EventsAndTop3Venues(Model model) {
		ArrayList<Event> nextEvents = new ArrayList<Event>();
		Map<Venue, Integer> topVenues = new HashMap<>();

		LocalDate dateNow = LocalDate.now();
		LocalTime timeNow = LocalTime.now();

		for (Event event : eventService.findAllByOrderByDateAscNameAsc()) {
			if (event.getDate() == null) {
				continue;
			}
			if (dateNow.isBefore(event.getDate()) || (dateNow.isEqual(event.getDate()) && timeNow.isBefore(event.getTime()))) {
				nextEvents.add(event);
				topVenues.merge(event.getVenue(), 1, Integer::sum);
			}
		}

		topVenues = sortByValue(topVenues);
		ArrayList<Pair<Venue, Integer>> venueList = new ArrayList<>();
		int c = 0;
		for (Venue v : topVenues.keySet()) {
			venueList.add(Pair.of(v, topVenues.get(v)));
			c++;
			if (c == 3) {
				break;
			}
		}

		model.addAttribute("nextEvents", nextEvents);
		model.addAttribute("topVenues", venueList);

		return "/home";
	}

}
