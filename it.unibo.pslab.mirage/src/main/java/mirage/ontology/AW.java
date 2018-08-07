package mirage.ontology;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import mirage.authentication.AgentsAuthenticator;
import mirage.authentication.Authenticator;
import mirage.authentication.UsersAuthenticator;
import mirage.exceptions.AugmentedEntityNotFoundException;
import mirage.exceptions.AugmentedEntityOutOfAWBoundsException;
import mirage.exceptions.AugmentedWorldAlreadyStartedException;
import mirage.exceptions.RSMismatchException;
import mirage.exceptions.RegionNameAlreadyInAWException;
import mirage.exceptions.RegionOutOfAWBoundsException;
import mirage.heb.HologramEngineBridge;
import mirage.heb.events.HebEvent;
import mirage.ontology.ae.Extension.ExtensionType;
import mirage.ontology.ae.location.CartesianLocation;
import mirage.ontology.ae.location.GpsLocation;
import mirage.ontology.aw.AwAddress;
import mirage.ontology.aw.AwEnvironment;
import mirage.ontology.aw.AwReferenceSystem;
import mirage.ontology.credentials.AgentCredentials;
import mirage.ontology.credentials.UserCredentials;
import mirage.ontology.factories.AreaFactory;
import mirage.ontology.interfaces.JsonFormattable;
import mirage.ontology.region.Area;

public class AW implements JsonFormattable{
	
	/*
	 * NON-STATIC MEMBERS
	 */
	
	private volatile boolean active;

	private String name;
	private String id;
	private JsonObject details;
	private Date startDateTime;
	private AwAddress address;
	private boolean local;
	
	private List<AE> entities;
	private Map<String, UserAvatar> users;
	private List<Region> regions;
	
	private AwEnvironment awEnvironment;
	private AwReferenceSystem awReferenceSystem;
	
	private Authenticator agentsAuthenticator;
	private Authenticator usersAuthenticator;
	
	private Area awArea;
	
	public AW() {
		this.active = false;
		
		this.entities = new ArrayList<>();
		this.users = new HashMap<>();
		this.regions = new ArrayList<>();

		this.agentsAuthenticator = new AgentsAuthenticator();
		this.usersAuthenticator = new UsersAuthenticator();
	}
	
	/**
	 * 
	 * @param name
	 * @param details
	 * @throws AugmentedWorldAlreadyStartedException
	 */
	public void start(String name, String id, Area awArea, AwEnvironment environment, AwReferenceSystem refSys, boolean local, AwAddress awAddress) throws AugmentedWorldAlreadyStartedException{
		if(!active) {
			this.active = true;
			
			this.name = name;
			this.id = id;
			this.awArea = awArea;
			this.startDateTime = new Date();
			this.local = local;
			this.address = awAddress;
			
			this.awEnvironment = environment;
			this.awReferenceSystem = refSys;
							
			HologramEngineBridge heb = HologramEngineBridge.instance();
			
			JsonObject eventContent = new JsonObject()
					.put("name", name);
			
			if(details != null) {
				eventContent.mergeIn(details, true);
			}
			
			heb.notifyEvent(HebEvent.AW_INSTANTIATION, eventContent);
			
		} else {
			throw new AugmentedWorldAlreadyStartedException();
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isActive() {
		return this.active;
	}
	
	/**
	 * 
	 * @return
	 */
	public String name() {
		return this.name;
	}
	
	/**
	 * 
	 */
	public String id() {
		return this.id;
	}
	
	/**
	 * 
	 * @return
	 */
	public AwAddress address() {
		return address;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isLocal() {
		return local;
	}

	/**
	 * 
	 * @return
	 */
	public List<AE> entities() {
		return this.entities;
	}
	
	
	/**
	 * 
	 * @return
	 */
	public List<UserAvatar> users() {
		return new ArrayList<>(this.users.values());
	}
	
	/**
	 * 
	 * @return
	 */
	public List<Region> regions() {
		return regions;
	}
	
	/**
	 * 
	 * @return users of the AW
	 */
	public List<AgentCredentials> agents(){
		List<AgentCredentials> res = new ArrayList<>();
		
		for (int i = 0; i < agentsAuthenticator.getAuthenticatedCredentials().size(); i++) {
			res.add(parseJsonToAgentCredentials(agentsAuthenticator.getAuthenticatedCredentials().getJsonObject(i)));
		}
		
		return res;
	}

	/**
	 * 
	 * @param id
	 * @return
	 * @throws AugmentedEntityNotFoundException
	 */
	public AE entity(String id) throws AugmentedEntityNotFoundException{
		
		for(AE ae : entities) {
			if(ae.id().equals(id)) {
				return ae;
			}
		}
		return user(id);
		
	}
	
	public UserAvatar user(String id) throws AugmentedEntityNotFoundException {
		for(UserAvatar ua : users.values()) {
			if(ua.id().equals(id)) {
				return ua;
			}
		}
		
		throw new AugmentedEntityNotFoundException();
	}
	
	/**
	 * 
	 * @param ID
	 * @return
	 */
	public Region region(String ID) {
		Optional<Region> r = this.regions.stream()
				.filter(region -> region.id().equals(ID))
				.findFirst();
		if(r.isPresent()) {
			return r.get();
		}
		
		return null;
	}

	/**
	 * Adds a new Augmented Entity to the current instance of the AW.
	 * @param ae The entity to be added.
	 * @throws RSMismatchException 
	 * @throws AugmentedEntityOutOfAWBoundsException 
	 */
	public void addAugmentedEntity(AE ae) throws RSMismatchException, AugmentedEntityOutOfAWBoundsException {
		
		if(awArea.isPointIncluded(ae.location())) {
			entities.add(ae);
			for (Region region : regions) {
				region.refreshStatus(ae, false);
			}
		} else {				
			throw new AugmentedEntityOutOfAWBoundsException();
		}
		
		HologramEngineBridge heb = HologramEngineBridge.instance();
		heb.notifyEvent(HebEvent.HOLOGRAM_CREATION, ae.getJSONRepresentation());
	}
	
	/**
	 * Adds a new User Avatar to the current instance of the AW.
	 * @param ua The avatar to be added.
	 * @throws RSMismatchException 
	 * @throws AugmentedEntityOutOfAWBoundsException 
	 */
	public void addUserAvatar(String sessionID, UserAvatar ua) throws RSMismatchException, AugmentedEntityOutOfAWBoundsException {
		
		if(awArea.isPointIncluded(ua.location())) {
			users.put(sessionID, ua);
			for (Region region : regions) {
				region.refreshStatus(ua, false);
			}
		} else {				
			throw new AugmentedEntityOutOfAWBoundsException();
		}
		
		HologramEngineBridge heb = HologramEngineBridge.instance();
		heb.notifyEvent(HebEvent.HOLOGRAM_CREATION, ua.getJSONRepresentation());
	}
	
	/**
	 * Adds a new Region to the AW
	 * @param name
	 * @param details
	 * @throws RSMismatchException
	 * @throws RegionNameAlreadyInAWException 
	 * @throws RegionOutOfAWBoundsException 
	 */
	public String addRegion(String name, JsonObject details) throws RSMismatchException, RegionNameAlreadyInAWException, RegionOutOfAWBoundsException {
		if(region(generateRegionIDFromName(name)) != null) {
			throw new RegionNameAlreadyInAWException();
		}
		switch(details.getString("type")) {
			case "circle":
				Double[] values = {
						details.getDouble("x"),
						details.getDouble("y"),
						details.getDouble("z"),
						details.getDouble("r")
				};
				boolean regionExtOk = false;
				switch(awArea.getLocation().type()) {
					case CARTESIAN:
						if(awArea.isPointIncluded(new CartesianLocation(values[0], values[1], values[2]))){
							regionExtOk = true;
						}
						break;
					case GPS:
						if(awArea.isPointIncluded(new GpsLocation(values[0], values[1], values[2]))){
							regionExtOk = true;
						}
						break;
				}
				if(regionExtOk) {
					Area regionArea = AreaFactory.getInstance().generateArea(awArea.getLocation().type(), ExtensionType.BASIC, values);
					String id = generateRegionIDFromName(name);
					this.regions.add(new Region(name, id, regionArea, this));
					return id;
				} else {
					throw new RegionOutOfAWBoundsException();
				}
				
				
			default:
				return null;
				
		}
		
		
	}

	/**
	 * Adds an agent to the AW
	 * @param id
	 */
	public String addAgent(AgentCredentials creds) {
		String sessionID = agentsAuthenticator.newAuthentication(new JsonObject().put("username", creds.getUsername()));
		return sessionID;
	}
	
	/**
	 * Remove an agent to authenticated agents
	 * @param sessionID
	 */
	public void deleteAgent(String sessionID) {
		agentsAuthenticator.removeAuthentication(sessionID);
	}
	
	/**
	 * 
	 * @param sessionID
	 * @return true if the agent is joined, false otherwise
	 */
	public boolean isAgentJoined(String sessionID) {
		return agentsAuthenticator.isAuthenticated(sessionID);
	}
	
	/**
	 * Adds a user to the AW
	 * @param id
	 */
	public String addUser(UserCredentials creds) {
		String sessionID = usersAuthenticator.newAuthentication(new JsonObject().put("username", creds.getUsername()));
		return sessionID;
	}
	
	/**
	 * 
	 * @param sessionID
	 * @return true if the agent is joined, false otherwise
	 */
	public boolean isUserJoined(String sessionID) {
		return usersAuthenticator.isAuthenticated(sessionID);
	}

	/**
	 * Removes a previously added Augmented Entity from the current instance of the AW.
	 * @param id The identifier of the entity to be removed.
	 */
	public void removeAugmentedEntity(String id) throws AugmentedEntityNotFoundException{
		
		for (int i = 0; i < entities.size(); i++) {
			if(entities.get(i).id().equals(id)) {
				AE ae = entities.get(i);
				entities.remove(entities.get(i));
				for (Region region : regions) {
					region.refreshStatus(ae, true);
				}
				

				/*
				 * TODO: manage hologram dispose action
				 * HologramEngineBridge heb = HologramEngineBridge.instance();
				 * heb.notifyEvent(HebEvent.HOLOGRAM_DISPOSING, ...);
				 */
				return;
			}
		}
		
		throw new AugmentedEntityNotFoundException();
	}
	
	/**
	 * Remove an agent to authenticated agents
	 * @param sessionID
	 * @throws AugmentedEntityNotFoundException 
	 */
	public void removeUserAvatar(String sessionID) throws AugmentedEntityNotFoundException {
		usersAuthenticator.removeAuthentication(sessionID);
		
		UserAvatar ua = users.get(sessionID);
		users.remove(sessionID);
		if(ua != null) {
			for (Region region : regions) {
				region.refreshStatus(ua, true);
			}
			
			/*
			 * TODO: manage hologram dispose action
			 * HologramEngineBridge heb = HologramEngineBridge.instance();
			 * heb.notifyEvent(HebEvent.HOLOGRAM_DISPOSING, ...);
			 */
			
			return;
		}
		
		throw new AugmentedEntityNotFoundException();
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public boolean checkEntityAvailability(String id) {
		for(AE ae : entities) {
			if(ae.id().equals(id)) {
				return true;
			}
		}
		
		return false;
	}
	
	public String getUserIDFromSessionId(String sessionId) {
		return users.get(sessionId).id();
	}
	
	public void moveToAddress(AwAddress newAddress) {
		//Da implementare spostamento ad altro indirizzo
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AW other = (AW) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public JsonObject getJSONRepresentation() {
		JsonArray entities = new JsonArray();
		for (AE ae : this.entities) {
			entities.add(new JsonObject().put("id", ae.id()).put("type", ae.type()));
		}
		JsonArray regions = new JsonArray();
		for (Region region : this.regions) {
			regions.add(new JsonObject().put("id", region.id()).put("name", region.name()));
		}
		return new JsonObject()
			.put("name", name)
			.put("id", this.id)
			.put("startDate", new SimpleDateFormat("yyyy-MM-dd").format(startDateTime))
			.put("startTime", new SimpleDateFormat("hh:mm:ss").format(startDateTime))
			.put("entities", entities)
			.put("regions", regions)
			.put("n. agents", this.agentsAuthenticator.getAuthenticatedCredentials().size())
			.put("area", awArea.getJSONRepresentation())
			.put("environment", this.awEnvironment.toString())
			.put("reference-system", this.awReferenceSystem.toString())
			.put("details", details);
	}

	private String generateRegionIDFromName(String name) {
		return name;//.hashCode()+"";
	}
	
	private AgentCredentials parseJsonToAgentCredentials(JsonObject json) {
		return new AgentCredentials(json.getString("username"));
	}
}
