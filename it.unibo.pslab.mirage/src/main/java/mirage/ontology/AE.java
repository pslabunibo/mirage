package mirage.ontology;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import mirage.Environment;
import mirage.exceptions.ActionNotFoundException;
import mirage.exceptions.PropertyNotFoundException;
import mirage.heb.HologramEngineBridge;
import mirage.heb.events.HebEvent;
import mirage.ontology.ae.Extension;
import mirage.ontology.ae.Location;
import mirage.ontology.ae.Orientation;
import mirage.ontology.annotations.PROPERTY;
import mirage.ontology.interfaces.JsonFormattable;
import mirage.ontology.observers.AEPropertyRemoteObserver;
import mirage.ontology.observers.AERemoteObserver;
import mirage.ontology.observers.RemoteObserver;
import mirage.utils.Config;

/**
 * 
 */
public class AE implements JsonFormattable{
		
	protected String id;
	protected String tag;
	protected String type;
	
	protected String hologramGeometry;
	protected String hologramParent;
	
	protected Location location;
	protected Orientation orientation;
	protected Extension extension;
	protected JsonArray things;
	
	private Map<String, Object> customProperties = new HashMap<>();
	private Map<String, String[]> actions = new HashMap<>();
	
	protected Map<String, AERemoteObserver> aeObservers = new HashMap<>();
	protected Map<String, AEPropertyRemoteObserver> aePropertyObservers = new HashMap<>();
	
	/*
	 * MODEL
	 */
	
	public final String id() {
		return this.id;
	}
	
	public final String tag() {
		return this.tag;
	}

	public final String type() {
		return this.type;
	}
	
	public final String hologramGeometry() {
		return this.hologramGeometry;
	}
	
	public final String hologramParent() {
		return this.hologramParent;
	}


	/*
	 * PROPERTIES
	 */
	
	//GETTERS
	
	public final Location location() {
		return this.location;
	}
	
	public final Orientation orientation() {
		return this.orientation;
	}
	
	public final Extension extension() {
		return this.extension;
	}
	
	public final JsonArray things() {
		return this.things;
	}
	
	public final Set<String> customProperties(){
		return customProperties.keySet();
	}
	
	public final Object customProperty(String name) throws PropertyNotFoundException{		
		if(!customProperties.containsKey(name)) {
			throw new PropertyNotFoundException(); 
		}
		
		return customProperties.get(name);
	}
	
	//SETTERS
	
	public final void location(Location location) {
		this.location = location;
		
		notifyObservers("location", location.getJSONRepresentation());
				
		for (Region region : Environment.getAugmentedWorldRunningInstance(this).regions()) {
			region.refreshStatus(this, false);
		}
		
		JsonObject messageContent = new JsonObject()
				.put("entityId", id)
				.put("propertyId", "location")
				.put("value", location.getJSONRepresentation());
		
		HologramEngineBridge heb = HologramEngineBridge.instance();
		heb.notifyEvent(HebEvent.UPDATE_HOLOGRAM_PROPERTY, messageContent);
	}

	public final void orientation(Orientation orientation) {
		this.orientation = orientation;
		
		notifyObservers("orientation", orientation.getJSONRepresentation());
		
		JsonObject messageContent = new JsonObject()
				.put("entityId", id)
				.put("propertyId", "orientation")
				.put("value", orientation.getJSONRepresentation());
		
		HologramEngineBridge heb = HologramEngineBridge.instance();
		heb.notifyEvent(HebEvent.UPDATE_HOLOGRAM_PROPERTY, messageContent);
	}

	public final void extension(Extension extension) {
		this.extension = extension;
		
		notifyObservers("extension", extension.getJSONRepresentation());
		
		JsonObject messageContent = new JsonObject()
				.put("entityId", id)
				.put("propertyId", "extension")
				.put("value", extension.getJSONRepresentation());
		
		HologramEngineBridge heb = HologramEngineBridge.instance();
		heb.notifyEvent(HebEvent.UPDATE_HOLOGRAM_PROPERTY, messageContent);
	}
	
	public final void things(JsonArray things) {
		this.things = things;
		
		notifyObservers("things", new JsonObject().put("things", things));
	}
	
	public final void customProperty(String name, Object newValue) throws PropertyNotFoundException{		
		if(!customProperties.containsKey(name)) {
			throw new PropertyNotFoundException();
		}
		
		customProperties.replace(name, newValue);
		
		try {
			Field f = getClass().getDeclaredField(name);
			f.setAccessible(true);
			if(f.isAnnotationPresent(PROPERTY.class) && !f.getAnnotation(PROPERTY.class).onUpdate().equals("")) {
				Method m = getClass().getDeclaredMethod(f.getAnnotation(PROPERTY.class).onUpdate());
				m.setAccessible(true);
				m.invoke(this);
			}
		} catch (NoSuchFieldException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
			e.printStackTrace();
		}
		
		String representation = "";

		try {
			representation = JsonObject.mapFrom(customProperties.get(name)).encodePrettily();
		} catch (IllegalArgumentException e) {
				representation = String.valueOf(customProperties.get(name));
		}
		
		notifyObservers(name, customProperties.get(name));
		
		JsonObject messageContent = new JsonObject()
				.put("entityId", id)
				.put("propertyId", name)
				.put("value", representation);
		
		HologramEngineBridge heb = HologramEngineBridge.instance();
		heb.notifyEvent(HebEvent.UPDATE_HOLOGRAM_PROPERTY, messageContent);
	}
	
	/*
	 * ACTIONS
	 */
	
	public final Map<String, String[]> actions(){
		return this.actions;
	}
	
	public final <T extends AE>void executeAction(T ae, String name, Object...params) throws ActionNotFoundException {
		if(!actions.keySet().contains(name)) {
			throw new ActionNotFoundException();
		}
		
		try {
			new Thread(() -> {
				try {
					Class<?> c = Class.forName(Config.ENTITIES_PATH + "." + type);
					
					if(params.length != 0) {
						String[] paramsTypeList = actions.get(name);
						Class<?>[] paramsClassList = new Class<?>[paramsTypeList.length];
						
						for(int i = 0; i < paramsTypeList.length; i++) {
							paramsClassList[i] = Class.forName(paramsTypeList[i]);
						}
						
						Method m = c.getMethod(name, paramsClassList);
						m.invoke(ae, params);
						
					} else {
						Method m = c.getMethod(name);
						m.invoke(ae);
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException | NoSuchMethodException | SecurityException e) {
					System.out.println("Error in invoke method!");
					e.printStackTrace();
				}
			}).start();
			
		} catch (IllegalArgumentException | SecurityException e) {
			e.printStackTrace();
		}
	}
	
	public void addAEObserver(String sessionID, ServerWebSocket ws) {
		this.aeObservers.put(sessionID, new AERemoteObserver(ws));
	}
	
	public void addAEPropertyObserver(String ip, ServerWebSocket ws, String propertyName) throws PropertyNotFoundException {
		if(propertyName.equals("location") ||
				propertyName.equals("orientation") ||
				propertyName.equals("extension") ||
				//propertyName.equals("holograms") ||
				propertyName.equals("things") ||
				customProperties.containsKey(propertyName)) {
			this.aePropertyObservers.put(ip, new AEPropertyRemoteObserver(ws, propertyName));
		} else {
			throw new PropertyNotFoundException();
		}
		
	}

	public void removeObserver(String sessionID) {
		this.aeObservers.remove(sessionID);
		this.aePropertyObservers.remove(sessionID);
	}

	
	/**
	 * @see mirage.ontology.interfaces.JsonFormattable#getJSONRepresentation()
	 */
	public JsonObject getJSONRepresentation() {
		JsonObject propertiesObject =  new JsonObject();
		
		propertiesObject.put("location", location.getJSONRepresentation());
		propertiesObject.put("orientation", orientation.getJSONRepresentation());
		propertiesObject.put("extension", extension.getJSONRepresentation());
		//propertiesObject.put("holograms", holograms);
		
		customProperties.entrySet().forEach(p -> {
			try {
				propertiesObject.put(p.getKey(), p.getValue());
			} catch(IllegalStateException e) {
				propertiesObject.put(p.getKey(), JsonObject.mapFrom(p.getValue()));
			}
		});
		
		JsonArray actionArray = new JsonArray();
		
		actions.keySet().forEach(action -> {
			JsonArray types = new JsonArray();
			
			for(int i = 0; i < actions.get(action).length; i++) {
				types.add(actions.get(action)[i]);
			}
			
			actionArray.add(new JsonObject()
					.put("name", action)
					.put("paramsTypeList", types));
		});
		
		
		return new JsonObject()
				.put("model", new JsonObject()
						.put("id", id)
						.put("tag", tag)
						.put("type", type)
						.put("hologram", new JsonObject()
											.put("geometry", hologramGeometry)
											.put("parent", hologramParent)
							)
					)
				.put("properties", propertiesObject)
				.put("things", things)
				.put("actions", actionArray);
	}
	
	protected static void log(AE instance, String msg) {
		System.out.println("[" + instance.id + ":"+ instance.getClass().getSimpleName() +"] " + msg);
	}
	
	protected void notifyObservers(String propertyName, Object newValue) {
		for (RemoteObserver observer : this.aeObservers.values()) {
			observer.notify(new JsonObject()
					.put("property", propertyName)
					.put("value", newValue));
		}
		
		for (RemoteObserver observer : this.aePropertyObservers.values()) {
			observer.notify(new JsonObject()
					.put("property", propertyName)
					.put("value", newValue));
		}
	}
}