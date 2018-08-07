package mirage.ontology.user;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import io.vertx.core.json.JsonObject;
import mirage.ontology.interfaces.JsonFormattable;

public class Gaze3D extends Vector3D implements JsonFormattable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Gaze3D(double x, double y, double z) {
		super(x, y, z);
	}

	@Override
	public JsonObject getJSONRepresentation() {
		return new JsonObject()
				.put("x", this.getX())
				.put("y", this.getY())
				.put("z", this.getZ());
	}

}
