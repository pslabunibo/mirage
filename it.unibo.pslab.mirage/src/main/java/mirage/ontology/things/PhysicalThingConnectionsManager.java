package mirage.ontology.things;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.json.JsonObject;
import mirage.ontology.AE;

public class PhysicalThingConnectionsManager {
	
	private static volatile Map<Integer, Map<String, PhysicalThingConnection>> connections = new HashMap<>();

	public static void activate(AE ae, JsonObject thingDescription) {
		
		String id = thingDescription.getString("id");
		String protocol = thingDescription.getString("protocol");
			
		switch(protocol) {
			case "tcp":
				String address = thingDescription.getString("address");
				int port = thingDescription.getInteger("port");
				
				Map<String, PhysicalThingConnection> table;
				
				if(!connections.containsKey(ae.hashCode())) {
					 table = new HashMap<>();
					
				} else {
					table = connections.get(ae.hashCode());
				}
				
				table.put(id, new PhysicalThingConnection((PhysicalThingInterface) ae, id, address, port));
				
				connections.put(ae.hashCode(), table);
				break;
			
			default:
				break;
		}
	}
	
	public static PhysicalThingConnection getPhysicalThingConnection(Integer hashCode, String id) {
		return connections.get(hashCode).get(id);
	}

	public static List<PhysicalThingConnection> getPhysicalThingConnections(int hashCode) {
		List<PhysicalThingConnection> list = new ArrayList<>();
		
		for(PhysicalThingConnection ptc : connections.get(hashCode).values()) {
			list.add(ptc);
		}
		
		return list;
	}
}
