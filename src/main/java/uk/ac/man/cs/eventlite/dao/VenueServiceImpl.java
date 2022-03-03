package uk.ac.man.cs.eventlite.dao;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.dao.VenueRepository;

@Service
@Transactional
public class VenueServiceImpl implements VenueService {

	private final static Logger log = LoggerFactory.getLogger(VenueServiceImpl.class);

	private final static String DATA = "data/venues.json";

	@Autowired
	private VenueRepository venueRepository;

	@Override
	public long count() {
		return venueRepository.count();
	}

	@Override
	public Iterable<Venue> findAll() {
		return venueRepository.findAll();
	}

	@Override
	public Venue save(Venue venue) {
		return venueRepository.save(venue);
	}

}
