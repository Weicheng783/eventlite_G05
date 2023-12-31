package uk.ac.man.cs.eventlite.dao;

import java.util.Optional;

import uk.ac.man.cs.eventlite.entities.Event;

import java.util.Optional;


public interface EventService {
	public boolean existsById(long id);

	public long count();
	
	public Iterable<Event> findByNameLike(String name);

	public Iterable<Event> findByNameIgnoreCaseContainingOrderByDateAscNameAsc(String name);
	
	public Iterable<Event> findByNameIgnoreCaseContainingOrderByDateDescNameAsc(String name);

	public Iterable<Event> findAll();
	
	public Iterable<Event> findAllByOrderByDateAscNameAsc();
	
	public Iterable<Event> findAllByOrderByDateDescNameAsc();

	public Optional<Event> findById(long id);

	public Event save(Event event);
	
	public Optional<Event> findEventById(Long id);

	public void delete(Event event);

	public void deleteAll();

	public void deleteAll(Iterable<Event> event);

	public void deleteById(long id);

	public void deleteAllById(Iterable<Long> ids);

}
