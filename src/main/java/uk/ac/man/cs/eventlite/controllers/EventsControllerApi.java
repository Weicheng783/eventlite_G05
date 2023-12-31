package uk.ac.man.cs.eventlite.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import uk.ac.man.cs.eventlite.assemblers.EventModelAssembler;
import uk.ac.man.cs.eventlite.assemblers.VenueModelAssembler;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;
import uk.ac.man.cs.eventlite.exceptions.VenueNotFoundException;

import javax.validation.Valid;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/events", produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE})
public class EventsControllerApi {

    private static final String NOT_FOUND_MSG = "{ \"error\": \"%s\", \"id\": %d }";

    @Autowired EventService eventService;

    @Autowired EventModelAssembler eventAssembler;
    
    @Autowired
    private VenueModelAssembler venueAssembler;

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<?> eventNotFoundHandler(EventNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(String.format(NOT_FOUND_MSG, ex.getMessage(), ex.getId()));
    }


	@GetMapping("/{id}")
	public EntityModel<Event> getEvent(@PathVariable("id") long id) {
		if(!eventService.existsById(id)) {
            throw new EventNotFoundException(id);
        }
		Optional<Event> event = eventService.findById(id);
		try {
			return eventAssembler.toModel(event.get()).add(linkTo(methodOn(EventsControllerApi.class).getEventVenue(id)).withRel("venue"));
		}catch (Exception e) {
			return null;
		}
	}
	
	@GetMapping("/{id}/venue")
	public EntityModel<Venue> getEventVenue(@PathVariable("id") long id) {
		if(!eventService.existsById(id)) {
            throw new EventNotFoundException(id);
        }
		Optional<Event> e = eventService.findById(id);
		Venue venue = null;
		try {
			venue = e.get().getVenue();
		}catch(Exception ee) {
			venue = null;
		}
		return venueAssembler.toModel(venue);
	}
	
	
	@RequestMapping(value="/{id}", method=RequestMethod.DELETE)
	public ResponseEntity<?> deleteEvent(@PathVariable("id") long id){
		if (!eventService.existsById(id)) {
			throw new EventNotFoundException(id);
		}
		eventService.deleteById(id);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}

	@DeleteMapping
	public ResponseEntity<?> deleteAllEvents() {
		eventService.deleteAll();
		return ResponseEntity.noContent().build();
	}

    @GetMapping
    public CollectionModel<EntityModel<Event>> getAllEvents() {
        return eventAssembler.toCollectionModel(eventService.findAll())
                .add(linkTo(methodOn(EventsControllerApi.class).getAllEvents()).withSelfRel());
    }

//    @RequestMapping(name = "/search", method = RequestMethod.GET)
//    public CollectionModel<EntityModel<Event>> searchEventsByNameContaining(@RequestParam("name") String name) {
//        return eventAssembler.toCollectionModel(eventService.findByNameContaining(name))
//                .add(linkTo(methodOn(EventsControllerApi.class).searchEventsByNameContaining(name)).withSelfRel());
//    }

    @GetMapping("/new")
    public ResponseEntity<?> newEvent() {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createEvent(@RequestBody @Valid Event event, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.unprocessableEntity().build();
        }

        Event newEvent = eventService.save(event);
        EntityModel<Event> entity = null;
        if(eventAssembler.toModel(newEvent) != null) {
        	entity = eventAssembler.toModel(newEvent);
        }
        
        try {
        	return ResponseEntity.created(entity.getRequiredLink(IanaLinkRelations.SELF).toUri()).build();
        } catch (Exception e) {
        	return ResponseEntity.unprocessableEntity().build();
        }
    }
}
