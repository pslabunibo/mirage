package mirage.ontology.things;

import java.util.List;

import io.vertx.core.json.JsonObject;

public interface PhysicalThingInterface {
	void onConnected(String id);
	void onDisconnected(String id);
	
	void onMessageReceived(String id, JsonObject msg);
	
	default PhysicalThingConnection link(String id) {
		return PhysicalThingConnectionsManager.getPhysicalThingConnection(hashCode(), id);
	}
	
	default List<PhysicalThingConnection> links() {
		return PhysicalThingConnectionsManager.getPhysicalThingConnections(hashCode());
	}
}
