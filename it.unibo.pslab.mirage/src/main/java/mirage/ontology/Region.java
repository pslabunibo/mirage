package mirage.ontology;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import mirage.exceptions.RSMismatchException;
import mirage.ontology.interfaces.JsonFormattable;
import mirage.ontology.observers.RemoteObserver;
import mirage.ontology.region.Area;

public class Region implements JsonFormattable {
	
	private String name;
	private String id;
	private Area extension;
	
	private HashMap<String, AE> entitiesInRegion;
	
	private HashMap<String, RegionRemoteObserver> observers;
	
	public Region(String name, String id, Area extension, AW aw) {
		super();
		this.name = name;
		this.id = id;
		this.extension = extension;
		
		this.observers = new HashMap<>();
		
		this.entitiesInRegion = new HashMap<>();
		
		for (AE entity : this.getAEsInside(aw.entities())) {
			this.entitiesInRegion.put(entity.id(), entity);
		}
	}
	
	public String name() {
		return this.name;
	}
	
	public String id() {
		return id;
	}

	public void refreshStatus(AE entity, boolean deletedEntity) {
		int prevSize = entitiesInRegion.size();
		
		if(!deletedEntity) {
			if(this.isAEInRegion(entity)) {
				this.entitiesInRegion.put(entity.id(), entity);
			} else {
				this.entitiesInRegion.remove(entity.id());
			}
		} else {
			this.entitiesInRegion.remove(entity.id());
		}
		
		if(prevSize != entitiesInRegion.size()) {
			JsonObject propertiesObject = new JsonObject();
			propertiesObject.put("name", this.name);
			propertiesObject.put("extension", this.extension.getJSONRepresentation());
			propertiesObject.put("id", id);
			JsonArray entitiesArray = new JsonArray();
			for (AE ae : this.entitiesInRegion.values()) {
				entitiesArray.add(new JsonObject()
						.put("id", ae.id())
						.put("type", ae.type()));
			}
			propertiesObject.put("entities", entitiesArray);
			
			for (RegionRemoteObserver observer : observers.values()) {
				observer.notify(propertiesObject);
			}
		}
	}

	public void addObserver(String sessionID, ServerWebSocket ws) {
		this.observers.put(sessionID, new RegionRemoteObserver(ws));
	}

	public void removeObserver(String sessionID) {
		this.observers.remove(sessionID);
	}

	@Override
	public JsonObject getJSONRepresentation() {
		JsonObject propertiesObject = new JsonObject();
		propertiesObject.put("name", this.name);
		propertiesObject.put("extension", this.extension.getJSONRepresentation());
		JsonArray entitiesArray = new JsonArray();
		for (AE ae : this.entitiesInRegion.values()) {
			entitiesArray.add(new JsonObject()
					.put("id", ae.id())
					.put("type", ae.type()));
		}
		propertiesObject.put("entities", entitiesArray);
		return propertiesObject;
	}
	
	private List<AE> getAEsInside(List<AE> entities){
		List<AE> entitiesInside = entities.stream()
				.filter((AE ae) -> isAEInRegion(ae)).collect(Collectors.toList());
		return entitiesInside;
	}


	private boolean isAEInRegion(AE entity) {
		try {
			return this.extension.isPointIncluded(entity.location());
		} catch (RSMismatchException e) {
			e.printStackTrace();
			return false;
		}
	}


	class RegionRemoteObserver implements RemoteObserver{
		
		private ServerWebSocket ws;

		public RegionRemoteObserver(ServerWebSocket ws) {
			this.ws = ws;
		}

		@Override
		public void notify(JsonObject newStatus) {
			ws.writeTextMessage(newStatus.encodePrettily());
		}
		
	}

}
