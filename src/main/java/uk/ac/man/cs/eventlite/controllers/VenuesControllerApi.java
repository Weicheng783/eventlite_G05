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
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;
import uk.ac.man.cs.eventlite.exceptions.VenueNotFoundException;

import javax.validation.Valid;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/api/venues", produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE})
public class VenuesControllerApi {

    private static final String NOT_FOUND_MSG = "{ \"error\": \"%s\", \"id\": %d }";

    @Autowired
    private VenueService venueService;

    @Autowired
    private VenueModelAssembler venueAssembler;

    @ExceptionHandler(VenueNotFoundException.class)
    public ResponseEntity<?> venueNotFoundHandler(VenueNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(String.format(NOT_FOUND_MSG, ex.getMessage(), ex.getId()));
    }

	@GetMapping("/{id}")
	public EntityModel<Event> getVenue(@PathVariable("id") long id) {
		throw new VenueNotFoundException(id);
	}

    @GetMapping
    public CollectionModel<EntityModel<Venue>> getAllVenues() {
        return venueAssembler.toCollectionModel(venueService.findAll())
                .add(linkTo(methodOn(VenuesControllerApi.class).getAllVenues()).withSelfRel());
    }

//    @RequestMapping(name = "/search", method = RequestMethod.GET)
//    public CollectionModel<EntityModel<Event>> searchEventsByNameContaining(@RequestParam("name") String name) {
//        return eventAssembler.toCollectionModel(eventService.findByNameContaining(name))
//                .add(linkTo(methodOn(EventsControllerApi.class).searchEventsByNameContaining(name)).withSelfRel());
//    }

    @GetMapping("/new")
    public ResponseEntity<?> newVenue() {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createVenue(@RequestBody @Valid Venue venue, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.unprocessableEntity().build();
        }

        Venue newVenue = venueService.save(venue);
        EntityModel<Venue> entity = venueAssembler.toModel(newVenue);

        return ResponseEntity.created(entity.getRequiredLink(IanaLinkRelations.SELF).toUri()).build();
    }
    
	@RequestMapping(value="/{id}", method=RequestMethod.DELETE)
	public ResponseEntity<?> deleteVenue(@PathVariable("id") long id){
		if (!venueService.existsById(id)) {
			throw new VenueNotFoundException(id);
		}

		venueService.findById(id).orElseThrow();
		venueService.deleteById(id);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}
}
