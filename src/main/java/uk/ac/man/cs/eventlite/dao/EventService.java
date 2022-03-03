package uk.ac.man.cs.eventlite.dao;

import java.util.Optional;

import uk.ac.man.cs.eventlite.entities.Event;

public interface EventService {

	public long count();

	public Iterable<Event> findAll();

	public Event save(Event event);

	public void delete(Event event);

	public void deleteAll();

	public void deleteAll(Iterable<Event> event);

	public void deleteById(long id);

	public void deleteAllById(Iterable<Long> ids);

	public Optional<Event> findById(long id);

}
