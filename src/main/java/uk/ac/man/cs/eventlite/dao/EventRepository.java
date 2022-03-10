package uk.ac.man.cs.eventlite.dao;

import org.springframework.data.repository.CrudRepository;

import uk.ac.man.cs.eventlite.entities.Event;

public interface EventRepository extends CrudRepository<Event, Long> {
	
	public Iterable<Event> findByNameIgnoreCaseContainingOrderByDateDescNameAsc(String name);
	
	public Iterable<Event> findByNameLike(String name);

	public Iterable<Event> findAllByOrderByDateAscTimeAsc();
	
	public Iterable<Event> findAllByOrderByDateAscNameAsc();
	
	public Iterable<Event> findAllByOrderByDateDescNameAsc();

	public long count();

}