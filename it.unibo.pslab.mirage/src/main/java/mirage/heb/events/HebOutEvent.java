package mirage.heb.events;

import io.vertx.core.json.JsonObject;

public class HebOutEvent {
	
	private final HebEvent eventType;
	private final JsonObject eventContent;

	public HebOutEvent(final HebEvent eventType, final JsonObject eventContent) {
		this.eventType = eventType;
		this.eventContent = eventContent;
	}
	
	public HebEvent type() {
		return this.eventType;
	}
	
	public JsonObject content() {
		return this.eventContent;
	}
}
