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
				testVenue.setLatitude(53.4637002);
				testVenue.setLongitude(-2.2336457);
				testVenue.setRoadName("4 Oxford Road");
				testVenue.setPostcode("M13 9WJ");
				venueService.save(testVenue);

				Venue testVenue2 = new Venue();
				testVenue2.setName("Engineering Building");
				testVenue2.setCapacity(5000);
				testVenue2.setLatitude(53.4691693);
				testVenue2.setLongitude(-2.236098);
				testVenue2.setRoadName("5 Oxford Road");
				testVenue2.setPostcode("M13 9WJ");
				venueService.save(testVenue2);
			}

			if (eventService.count() > 0) {
				log.info("Database already populated with events. Skipping event initialization.");
			} else {
				// Build and save initial events here.
				Venue testVenue = new Venue();
				testVenue.setName("Manchester Academy 2");
				testVenue.setCapacity(1000);
				testVenue.setLatitude(53.4637002);
				testVenue.setLongitude(-2.2336457);
				testVenue.setRoadName("4 Oxford Road");
				testVenue.setPostcode("M13 9WJ");
				venueService.save(testVenue);

				Event event1 = new Event();
				event1.setName("Reading group");
				event1.setDate(LocalDate.now().plusDays(100));
				event1.setTime(LocalTime.now().plusHours(3));
				event1.setVenue(testVenue);
				eventService.save(event1);
				
				Event event2 = new Event();
				event2.setName("Music Concert");
				event2.setDate(LocalDate.now().plusDays(100));
				event2.setTime(LocalTime.now().plusHours(3));
				event2.setVenue(testVenue);
				eventService.save(event2);
				
				Event event3 = new Event();
				event3.setName("Careers fair");
				event3.setDate(LocalDate.now().plusDays(100));
				event3.setTime(LocalTime.now().plusHours(3));
				event3.setVenue(testVenue);
				eventService.save(event3);
			}
		};
	}
}
