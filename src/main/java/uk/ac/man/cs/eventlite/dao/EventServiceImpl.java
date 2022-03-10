package uk.ac.man.cs.eventlite.dao;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.man.cs.eventlite.entities.Event;

import java.util.Optional;

@Service
public class EventServiceImpl implements EventService {

	@Autowired
	private EventRepository eventRepository;

	@Override
	public long count() {
		return eventRepository.count();
	}
	
	@Override
	public Iterable<Event> findByNameLike(String name) {
		return eventRepository.findByNameLike(name);
	}

	@Override
	public Iterable<Event> findByNameContaining(String name) {
		return eventRepository.findByNameIgnoreCaseContainingOrderByDateAscNameAsc(name);
	}

	@Override
	public Optional<Event> findById(long id) {
		return eventRepository.findById(id);
	}

	@Override
	public Iterable<Event> findAll() {
		return eventRepository.findAllByOrderByDateAscTimeAsc();
	}

	@Override
	public Event save(Event event) {
		return eventRepository.save(event);
	}

	@Override
	public Optional<Event> findEventById(Long id) {
		return eventRepository.findById(id);
	}
	
	@Override
	public void delete(Event event) {
		eventRepository.delete(event);
	}

	@Override
	public void deleteAll() {
		eventRepository.deleteAll();
	}

	@Override
	public void deleteAll(Iterable<Event> event) {
		eventRepository.deleteAll(event);
	}

	@Override
	public void deleteById(long id) {
		eventRepository.deleteById(id);
	}
	
	@Override
	public void deleteAllById(Iterable<Long> ids) {
		eventRepository.deleteAllById(ids);
	}
	
}
