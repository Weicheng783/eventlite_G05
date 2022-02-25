package uk.ac.man.cs.eventlite.config.data;

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
@Profile("default")
public class InitialDataLoader {

	private final static Logger log = LoggerFactory.getLogger(InitialDataLoader.class);

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;

	@Bean
	CommandLineRunner initDatabase() {
		return args -> {
			if (venueService.count() > 0) {
				log.info("Database already populated with venues. Skipping venue initialization.");
			} else {
				// Build and save initial venues here.
				Venue testVenue = new Venue();
				testVenue.setName("Manchester Academy");
				testVenue.setCapacity(1000);
				venueService.save(testVenue);

				Venue testVenue2 = new Venue();
				testVenue2.setName("Engineering Building");
				testVenue2.setCapacity(5000);
				venueService.save(testVenue2);
			}

			if (eventService.count() > 0) {
				log.info("Database already populated with events. Skipping event initialization.");
			} else {
				// Build and save initial events here.
				Venue testVenue = new Venue();
				testVenue.setName("Manchester Academy");
				testVenue.setCapacity(1000);
				venueService.save(testVenue);

				Event event1 = new Event();
				event1.setName("Concert1");
				event1.setVenue(testVenue);
				eventService.save(event1);
				
				Event event2 = new Event();
				event2.setName("Concert2");
				event2.setVenue(testVenue);
				eventService.save(event2);
				
			}
		};
	}
}
