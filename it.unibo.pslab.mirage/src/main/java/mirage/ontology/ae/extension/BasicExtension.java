package mirage.ontology.ae.extension;

import io.vertx.core.json.JsonObject;
import mirage.ontology.ae.Extension;

public class BasicExtension extends Extension {

	private double radius;
	
	public BasicExtension(double radius) {
		super(Extension.ExtensionType.BASIC);
		
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
