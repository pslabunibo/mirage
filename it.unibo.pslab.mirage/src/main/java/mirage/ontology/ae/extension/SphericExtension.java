package mirage.ontology.ae.extension;

import io.vertx.core.json.JsonObject;
import mirage.ontology.ae.Extension;

public class SphericExtension extends Extension {

	private double radius;
	
	public SphericExtension(double radius) {
		super(Extension.ExtensionType.SPHERIC);
		
		this.radius = radius;
	}
	
	public double radius() {
		return this.radius;
	}

	@Override
	public JsonObject getJSONRepresentation() {
		return new JsonObject()
			.put("type", type.toString())
			.put("radius", radius);
	}
}
