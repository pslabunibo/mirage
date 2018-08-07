package mirage.ontology.region;

import io.vertx.core.json.JsonObject;
import mirage.exceptions.RSMismatchException;
import mirage.ontology.ae.Extension;
import mirage.ontology.ae.Location;
import mirage.ontology.ae.extension.BasicExtension;
import mirage.ontology.ae.location.CartesianLocation;

public class CircleCartesianArea extends Area {

	private CartesianLocation location;
	private BasicExtension extension;
	

	public CircleCartesianArea(CartesianLocation location, BasicExtension extension) {
		super();
		this.location = location;
		this.extension = extension;
	}

	@Override
	public boolean isPointIncluded(Location point) throws RSMismatchException {
		if(point instanceof CartesianLocation) {
			return (this.distanceFromCenter(((CartesianLocation)point).x(),((CartesianLocation)point).z())) < this.extension.radius();
		} else {
			throw new RSMismatchException();
		}
	}
	
	private double distanceFromCenter(double x, double z) {
		return (Math.sqrt(Math.pow((x-this.location.x()), 2)+(Math.pow((z-this.location.z()),2))));
	}

	@Override
	public String toString() {
		return "Circle Area centered in ("+this.location.x()+","+this.location.z()+") with radius "+this.extension.radius();
	}

	@Override
	public JsonObject getJSONRepresentation() {
		JsonObject descriptionObject = new JsonObject();
		descriptionObject.put("type", "Circle Cartesian Area");
		descriptionObject.put("location", this.location.getJSONRepresentation());
		descriptionObject.put("extension", this.extension.getJSONRepresentation());
		return descriptionObject;
	}

	@Override
	public Location getLocation() {
		return this.location;
	}

	@Override
	public Extension getExtension() {
		return this.extension;
	}


}
