package uk.ac.man.cs.eventlite.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
public class Venue {

	@Id
	@GeneratedValue
	private long id;

	@Column(unique=true)
	@Size(max = 255, message="Venue name should be less than 256 characters")
	@NotEmpty(message = "Name of venue should not be empty")
	@Size(max=255, message="Venue name should be less than 256 characters")
	private String name;


	@NotEmpty(message = "Road name should not be empty")
	@Size(max=299, message = "Road name must be less than 300 characters")
	private String roadName;

	@NotEmpty(message = "Venue must have a postcode")
	@Size(max=7, message = "Post code too long")
	private String postcode;
	
	@NotNull(message= "Venue must have a capacity")
	@Min(value=1, message = "Capacity must be a positive integer")
	private int capacity;

	public Venue() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public String getRoadName() {
		return this.roadName;
	}

	public void setRoadName(String address) {
		this.roadName = address;
	}

	public String getPostcode() {
		return this.postcode;
	}

	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}
}
