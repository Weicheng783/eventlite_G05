package uk.ac.man.cs.eventlite.dao;

import uk.ac.man.cs.eventlite.entities.Event;

import java.util.Optional;


public interface EventService {

	public long count();
	
	public Iterable<Event> findByNameLike(String name);

	public Iterable<Event> findByNameContaining(String name);

	public Iterable<Event> findAll();

	public Optional<Event> findById(long id);

	public Event save(Event event);
}
