package mirage.ontology.ae.location;

import io.vertx.core.json.JsonObject;
import mirage.ontology.ae.Location;

public class CartesianLocation extends Location {

	private double x, y, z;
	
	public CartesianLocation(double x, double y, double z) {
		super(Location.LocationType.CARTESIAN);
		
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public double x() {
		return x;
	}
	
	public double y() {
		return y;
	}
	
	public double z() {
		return z;
	}

	@Override
	public JsonObject getJSONRepresentation() {
		return new JsonObject()
			.put("type", type.toString())
			.put("x", x)
			.put("y", y)
			.put("z", z);
	}
}
