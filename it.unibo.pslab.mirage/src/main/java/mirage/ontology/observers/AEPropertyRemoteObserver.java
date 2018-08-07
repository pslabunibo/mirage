package mirage.ontology.observers;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;

public class AEPropertyRemoteObserver implements RemoteObserver{
	
	private ServerWebSocket ws;
	private String propertyName;

	public AEPropertyRemoteObserver(ServerWebSocket ws, String propertyName) {
		this.ws = ws;
		this.propertyName = propertyName;
	}

	@Override
	public void notify(JsonObject newStatus) {
		String propertyChanged = newStatus.getString("property");
		if(propertyChanged.equals(propertyName)) {
			ws.writeTextMessage(newStatus.encodePrettily());
		}
	}
	
}
