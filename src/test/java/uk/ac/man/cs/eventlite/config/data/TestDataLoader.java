package uk.ac.man.cs.eventlite.config.data;

import java.time.LocalDate;
import java.time.LocalTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@Configuration
@Profile("test")
public class TestDataLoader {

	private final static Logger log = LoggerFactory.getLogger(TestDataLoader.class);

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;

	@Bean
	CommandLineRunner initDatabase() {
		return args -> {
			// Build and save test events and venues here.
			// The test database is configured to reside in memory, so must be initialized
			// every time.
			Venue venue = new Venue();
			venue.setName("Kilburn Building");
			venue.setRoadName("Kilburn Building University of Manchester, Oxford Rd");
			venue.setPostcode("M13 9PL");
			venue.setCapacity(1000);
			venueService.save(venue);

			Event event1 = new Event();
			event1.setName("Test Event 1");
			event1.setDate(LocalDate.now());
			event1.setTime(LocalTime.now());
			event1.setVenue(venue);
			eventService.save(event1);

			Event event2 = new Event();
			event2.setName("Test Event 1");
			event2.setDate(LocalDate.now());
			event2.setTime(LocalTime.now());
			event2.setVenue(venue);
			eventService.save(event2);

			Event event3 = new Event();
			event3.setName("Test Event 1");
			event3.setDate(LocalDate.now());
			event3.setTime(LocalTime.now());
			event3.setVenue(venue);
			eventService.save(event3);
		};
	}
}
