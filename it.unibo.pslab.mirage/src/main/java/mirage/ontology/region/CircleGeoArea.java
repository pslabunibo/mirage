package mirage.ontology.region;

import io.vertx.core.json.JsonObject;
import mirage.exceptions.RSMismatchException;
import mirage.ontology.ae.Extension;
import mirage.ontology.ae.Location;
import mirage.ontology.ae.extension.BasicExtension;
import mirage.ontology.ae.location.GpsLocation;

public class CircleGeoArea extends Area{
	
	private GpsLocation location;
	private BasicExtension extension;

	public CircleGeoArea(GpsLocation location, BasicExtension extension) {
		super();
		this.location = location;
		this.extension = extension;
	}

	@Override
	public boolean isPointIncluded(Location point) throws RSMismatchException {
		if(point instanceof GpsLocation) {
			return (this.distanceFromCenter(((GpsLocation)point).latitude(),((GpsLocation)point).longitude()))<this.extension.radius();
		} else {
			throw new RSMismatchException();
		}
	}
	
	private double distanceFromCenter(double x, double y) {
		return (Math.sqrt(Math.pow((x-this.location.latitude()), 2)+(Math.pow((y-this.location.longitude()),2))));
	}

	@Override
	public String toString() {
		return "Circle Area centered in ("+this.location.latitude()+","+this.location.longitude()+") with radius "+this.extension.radius();
	}

	@Override
	public JsonObject getJSONRepresentation() {
		JsonObject descriptionObject = new JsonObject();
		descriptionObject.put("type", "Circle Geographical Area");
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
