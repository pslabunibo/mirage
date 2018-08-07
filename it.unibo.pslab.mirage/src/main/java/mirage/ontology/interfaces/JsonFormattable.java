package mirage.ontology.interfaces;

import io.vertx.core.json.JsonObject;

public interface JsonFormattable {

	/**
	 * Provides the JsonRepresentation of the Object.
	 * @return The JsonObject with the encoded representation.
	 */
	public JsonObject getJSONRepresentation();
}
