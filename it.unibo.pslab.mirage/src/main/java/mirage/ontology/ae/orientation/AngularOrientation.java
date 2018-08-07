package mirage.ontology.ae.orientation;

import io.vertx.core.json.JsonObject;
import mirage.ontology.ae.Orientation;

public class AngularOrientation extends Orientation {

	private double roll, pitch, yaw;
	
	public AngularOrientation(double roll, double pitch, double yaw) {
		super(Orientation.Type.ANGULAR);
		
		this.roll = roll;
		this.pitch = pitch;
		this.yaw = yaw;
	}
	
	public double roll() {
		return roll;
	}
	
	public double pitch() {
		return pitch;
	}
	
	public double yaw() {
		return yaw;
	}

	@Override
	public JsonObject getJSONRepresentation() {
		return new JsonObject()
			.put("type", type.toString())
			.put("roll", roll)
			.put("pitch", pitch)
			.put("yaw", yaw);
	}
}
