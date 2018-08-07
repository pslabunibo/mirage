package mirage.ontology.observers;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;

public class AERemoteObserver implements RemoteObserver{

	private ServerWebSocket ws;

	public AERemoteObserver(ServerWebSocket ws) {
		this.ws = ws;
	}

	@Override
	public void notify(JsonObject newStatus) {
		ws.writeTextMessage(newStatus.encodePrettily());
	}
	
}
