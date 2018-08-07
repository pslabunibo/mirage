package mirage.ontology.ae.location;

import io.vertx.core.json.JsonObject;
import mirage.ontology.ae.Location;

public class GpsLocation extends Location {

	private double latitude, longitude, altitude;
	
	public GpsLocation(double latitude, double longitude, double altitude) {
		super(Location.LocationType.GPS);
		
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
	}
	
	public double latitude() {
		return latitude;
	}
	
	public double longitude() {
		return longitude;
	}
	
	public double altitude() {
		return altitude;
	}

	@Override
	public JsonObject getJSONRepresentation() {
		return new JsonObject()
			.put("type", type.toString())
			.put("latitude", latitude)
			.put("longitude", longitude)
			.put("altitude", altitude);
	}
}
