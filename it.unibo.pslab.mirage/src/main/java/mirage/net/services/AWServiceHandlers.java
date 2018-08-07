package mirage.net.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import mirage.Environment;
import mirage.exceptions.ActionNotFoundException;
import mirage.exceptions.AugmentedEntityCreationException;
import mirage.exceptions.AugmentedEntityNotFoundException;
import mirage.exceptions.AugmentedEntityOutOfAWBoundsException;
import mirage.exceptions.PropertyNotFoundException;
import mirage.exceptions.RSMismatchException;
import mirage.exceptions.RegionNameAlreadyInAWException;
import mirage.exceptions.RegionOutOfAWBoundsException;
import mirage.heb.HologramEngineBridge;
import mirage.heb.events.HebEvent;
import mirage.net.utils.HttpStatus;
import mirage.ontology.AE;
import mirage.ontology.AW;
import mirage.ontology.Region;
import mirage.ontology.UserAvatar;
import mirage.ontology.ae.Extension;
import mirage.ontology.ae.Location;
import mirage.ontology.ae.Orientation;
import mirage.ontology.ae.extension.BasicExtension;
import mirage.ontology.ae.extension.SphericExtension;
import mirage.ontology.ae.location.CartesianLocation;
import mirage.ontology.ae.location.GpsLocation;
import mirage.ontology.ae.orientation.AngularOrientation;
import mirage.ontology.credentials.AgentCredentials;
import mirage.ontology.credentials.UserCredentials;
import mirage.ontology.factories.AEFactory;
import mirage.ontology.things.PhysicalThingInterface;
import mirage.ontology.user.Gaze3D;
import mirage.utils.C;

public class AWServiceHandlers {	
	
	protected static void handleTrackingRequest(ServerWebSocket ws) {
		String path = ws.path();
		String[] pathSequence = path.split("/");
		pathSequence = Arrays.stream(pathSequence).filter(s -> !s.equals("")).toArray(String[]::new);
	
		//If request's path is valid (referred to an entity or a region)
		if(checkPathForTracking(pathSequence)) {
			String awID = pathSequence[1];
			String sessionID = ws.headers().get("sessionID");
			if(agentAuthenticatedForTracking(awID, sessionID, ws)){
				AW aw = Environment.getAugmentedWorldRunningInstance(awID);
				
				if(aw == null | !aw.isActive()) {
					ws.reject();
				} else {
					if(pathSequence.length == 4) {
						if(pathSequence[2].equals("regions")) {
							handleTrackingRegion(ws, sessionID, pathSequence[3], aw);
						} else {
							handleTrackingEntity(ws, sessionID, pathSequence[3], aw);
						}
					} else {
						handleTrackingEntityProperty(ws, sessionID, pathSequence[3], pathSequence[5], aw);
					}
				}
			}
		} else {
			ws.reject();
		}
	}

	private static void handleTrackingEntity(ServerWebSocket ws, String sessionID, String id, AW aw) {
		try {
			AE entity = aw.entity(id);
			
			entity.addAEObserver(sessionID, ws.closeHandler((Void) -> {entity.removeObserver(sessionID);}));
			System.out.println("ADDED observer to entity " + id);
		} catch (AugmentedEntityNotFoundException e) {
			ws.reject();
		}
	}
	
	private static void handleTrackingEntityProperty(ServerWebSocket ws, String sessionID, String aeID, String propertyName, AW aw) {
		try {
			AE entity = aw.entity(aeID);
			entity.addAEPropertyObserver(sessionID, ws.closeHandler((Void) -> {entity.removeObserver(sessionID);}), propertyName);
			System.out.println("ADDED observer to entity " + aeID +" | to property "+ propertyName);
			
		} catch (AugmentedEntityNotFoundException | PropertyNotFoundException e) {
			ws.reject();
		}
	}

	private static void handleTrackingRegion(ServerWebSocket ws, String sessionID, String name, AW aw) {
		Region r = aw.region(name);
		
		if(r != null) {
			r.addObserver(sessionID, ws.closeHandler((Void) -> {r.removeObserver(sessionID);}));
			System.out.println("ADDED observer to region " + name);
		} else {
			ws.reject();
		}
	}

	protected static void handlePostAgentJoinAW(RoutingContext rc) {
		HttpServerResponse response = rc.response();
		
		JsonObject body = rc.getBodyAsJson();
		
		String awID = rc.request().getParam("awID");
		JsonObject credentials = body.getJsonObject("credentials");
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		String sessionID = Environment.getAugmentedWorldRunningInstance(awID).addAgent(new AgentCredentials(credentials.getString("username")));
		JsonObject responseBody = new JsonObject()
				.put("sessionID", sessionID);
		sendOKCREATED(response, responseBody);
	}
	
	protected static void handlePostUserJoinAW(RoutingContext rc) {
		HttpServerResponse response = rc.response();
		
		//Body for eventuals user's parameters
		JsonObject body = rc.getBodyAsJson();
		
		String awID = rc.request().getParam("awID");
		JsonObject credentials = body.getJsonObject("credentials");
		JsonObject descriptionObj = body.getJsonObject("properties");

		if(badAwRequest(awID, response)) {
			return;
		} 
		
		//GENERATING SESSION ID FOR USER
		String sessionID = Environment.getAugmentedWorldRunningInstance(awID).addUser(new UserCredentials(credentials.getString("username")));

		//CREATING USER AVATAR
		if(descriptionObj == null) {
			sendError(HttpStatus.BAD_REQUEST, "BAD REQUEST: Errors in request body.", response);
			return;
		}

		UserAvatar ua = null;
		AW aw = Environment.getAugmentedWorldRunningInstance(awID);

		try {
			ua = AEFactory.createUserAvatar(descriptionObj);
		} catch (AugmentedEntityCreationException e) {
			log("Unable to create requested Augmented Entity");
			sendError(HttpStatus.BAD_REQUEST, "BAD REQUEST: Errors in  type or request body content.", response);
			return;
		}

		try {
			aw.addUserAvatar(sessionID, ua);
		} catch (RSMismatchException e) {
			log("Reference System inputs not valid");
			sendError(HttpStatus.BAD_REQUEST, "BAD REQUEST: Reference System not matching with AW.", response);
			return;
		} catch (AugmentedEntityOutOfAWBoundsException e) {
			log("Unable to create an Entity outside AW field");
			sendError(HttpStatus.BAD_REQUEST, "BAD REQUEST: Entity is out of AW's bounds.", response);
			return;
		}

		log("New UserAvatar created and added to the AW! <" + ua.getJSONRepresentation().encode() + ">");		
		
		JsonObject responseBody = new JsonObject()
				.put("sessionID", sessionID)
				.put("id", ua.id());
		sendOKCREATED(response, responseBody);		
		
	}
	
	protected static void handleDeleteQuitAW(RoutingContext rc) {
		HttpServerResponse response = rc.response();
		
		//Body for eventuals agent's parameters
		//JsonObject body = rc.getBodyAsJson();
		
		String awID = rc.request().getParam("awID");
		String sessionID = rc.request().getParam("sessionID");
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		Environment.getAugmentedWorldRunningInstance(awID).deleteAgent(sessionID);
		
		sendOKACCEPTED(response);
		
	}

	protected static void handleDeleteQuitUser(RoutingContext rc) {
		HttpServerResponse response = rc.response();
		
		//Body for eventuals user's parameters
		//JsonObject body = rc.getBodyAsJson();
		
		String awID = rc.request().getParam("awID");
		String sessionID = rc.request().getParam("sessionID");
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		try {
			Environment.getAugmentedWorldRunningInstance(awID).removeUserAvatar(sessionID);
			sendOKACCEPTED(response);
		} catch (AugmentedEntityNotFoundException e) {
			sendError(HttpStatus.NOT_FOUND, response);
		}
	}
	
	protected static void handleGetInfrastructureInfo(RoutingContext rc) {
		HttpServerResponse response = rc.response();
		
		JsonObject responseBody = new JsonObject();
		
		Collection<AW> localAws= Environment.getLocalAws().values();
		Collection<AW> remoteAws = Environment.getRemoteAws().values();
		
		JsonArray localsObj = new JsonArray();
		JsonArray remoteObj = new JsonArray();
		for (AW aw : remoteAws) {
			remoteObj.add(
					new JsonObject()
					.put("name", aw.name())
					.put("awID", aw.id())
					.put("active", aw.isActive())
					.put("Address", aw.address().getJSONRepresentation())
					);
		}
		
		for (AW aw : localAws) {
			localsObj.add(
					new JsonObject()
					.put("name", aw.name())
					.put("awID", Environment.generateAWIDFromName(aw.name()))
					.put("active", aw.isActive())
					);
		}
		
		responseBody.put("local AWs", localsObj).put("remote AWS", remoteObj);
		
		sendOK(responseBody, response);
	}
	
	protected static void handleGetServiceInfo(RoutingContext rc) {
		HttpServerResponse response = rc.response();

		JsonObject responseBody = new JsonObject()
				.put("service-version", C.Service.VERSION);

		sendOK(responseBody, response);
	}
/*
	protected static void handleCreateAW(RoutingContext rc) {
		String name = rc.request().getParam("name");
		
		HttpServerResponse response = rc.response();
		JsonObject body = rc.getBodyAsJson();
		
		if (name != null) {
			if(checkRequestBody("createAw", body)) {
				try {
					AW aw = Environment.getInstance().getAW(name);
					//aw.start(name, body);
					sendOK(response);
					log("AW created successfully! " + aw.getJSONRepresentation().encode());
				} catch (AugmentedWorldAlreadyStartedException e) {
					sendError(HttpStatus.CONFLICT, "CONFLICT: An augmented world is already running on the infrastructure!", response);
				}
			} else {
				sendError(HttpStatus.BAD_REQUEST, "BAD REQUEST: Errors in request body.", response);
			}
			
		} else {
			sendError(HttpStatus.BAD_REQUEST, response);
		}
	}
	*/
	protected static void handleGetAWInfo(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		String sessionID = rc.request().getHeader("sessionID");

		HttpServerResponse response = rc.response();

		if(badAwRequest(awID, response)) {
			return;
		}
		
		if(agentAuthenticated(awID, sessionID, response)) {
			JsonObject runningAW = new JsonObject()
					.put("active-aw", Environment.getAugmentedWorldRunningInstance(awID).getJSONRepresentation());
			
	
			sendOK(runningAW, response);
		}
	}

	protected static void handleGetEntities(RoutingContext rc) {

		String awID = rc.request().getParam("awID");
		String sessionID = rc.request().getHeader("sessionID");
		
		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		if(agentAuthenticated(awID, sessionID, response)) {
			JsonArray entities = new JsonArray();
			AW aw = Environment.getAugmentedWorldRunningInstance(awID);
	
			aw.entities().forEach(ae -> {
				entities.add(ae.id());
			});
			
			aw.users().forEach(ua -> {
				entities.add(ua.id());
			});
			
			sendOK(new JsonObject().put("active-entities", entities), response);
		}
	}

	protected static void handleAddAugmentedEntity(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		String sessionID = rc.request().getHeader("sessionID");
		
		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}

		if(agentAuthenticated(awID, sessionID, response)) {
			JsonObject descriptionObj = rc.getBodyAsJson();
			
			if(descriptionObj == null) {
				sendError(HttpStatus.BAD_REQUEST, "BAD REQUEST: Errors in request body.", response);
				return;
			}
	
			AE ae = null;
			AW aw = Environment.getAugmentedWorldRunningInstance(awID);
	
			try {
				ae = AEFactory.createAugmentedEntity(descriptionObj.getJsonObject("model").getString("type"), descriptionObj);
			} catch (AugmentedEntityCreationException e) {
				log("Unable to create requested Augmented Entity");
				sendError(HttpStatus.BAD_REQUEST, "BAD REQUEST: Errors in  type or request body content.", response);
				return;
			}
	
			try {
				aw.addAugmentedEntity(ae);
			} catch (RSMismatchException e) {
				log("Reference System inputs not valid");
				sendError(HttpStatus.BAD_REQUEST, "BAD REQUEST: Reference System not matching with AW.", response);
				return;
			} catch (AugmentedEntityOutOfAWBoundsException e) {
				log("Unable to create an Entity outside AW field");
				sendError(HttpStatus.BAD_REQUEST, "BAD REQUEST: Entity is out of AW's bounds.", response);
				return;
			}
	
			sendOK(new JsonObject().put("id", ae.id()), response);
	
			log("New AE created and added to the AW! <" + ae.getJSONRepresentation().encode() + ">");
			
			if(ae instanceof PhysicalThingInterface && !ae.things().isEmpty()) {
				for(Object thing : ae.things()) {
					AEFactory.linkToPhysicalThing(ae, (JsonObject) thing);
				}
			}
		}
	}

	protected static void handleGetEntityByID(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		String sessionID = rc.request().getHeader("sessionID");
		
		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		if(agentAuthenticated(awID, sessionID, response)) {

			String entityID = rc.request().getParam("entityID");
	
			if (entityID == null) {
				sendError(HttpStatus.BAD_REQUEST, response);
				return;
			}	
			
			AW aw = Environment.getAugmentedWorldRunningInstance(awID);
			
			try {
				AE ae = aw.entity(entityID);
				sendOK(ae.getJSONRepresentation(), response);
			} catch (AugmentedEntityNotFoundException e) {
				try {
					UserAvatar ua = aw.user(entityID);
					sendOK(ua.getJSONRepresentation(), response);
				} catch (AugmentedEntityNotFoundException e1) {
					sendError(HttpStatus.NOT_FOUND, response);
				}
			}
		}
	}

	protected static void handleRemoveEntityByID(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		String sessionID = rc.request().getHeader("sessionID");
		
		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		if(agentAuthenticated(awID, sessionID, response)) {

			String entityID = rc.request().getParam("entityID");
	
			if (entityID == null) {
				sendError(HttpStatus.BAD_REQUEST, response);
				return;
			}
			
			AW aw = Environment.getAugmentedWorldRunningInstance(awID);
	
			try {
				aw.removeAugmentedEntity(entityID);
				sendOK(response);
			} catch (AugmentedEntityNotFoundException e) {
				sendError(HttpStatus.NOT_FOUND, response);
			}
		}
	}
	
	protected static void handleDefineRegion(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		String sessionID = rc.request().getHeader("sessionID");
		
		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		if(agentAuthenticated(awID, sessionID, response)) {

			AW aw = Environment.getAugmentedWorldRunningInstance(awID);
	
			JsonObject descriptionObj = rc.getBodyAsJson();
			String name = descriptionObj.getString("name");
			JsonObject extensionObj = descriptionObj.getJsonObject("extension");
			try {
				String regionID = aw.addRegion(name, extensionObj);
				if(regionID != null) {
					JsonObject responseBody = new JsonObject()
							.put("id", regionID);
					sendOKCREATED(response, responseBody);
					
					log("New Region defined for the AW! <" + regionID + ">");
					
				} else {
					sendError(HttpStatus.BAD_REQUEST, "BAD REQUEST: Extension type not supported", response);
				}
			} catch (RSMismatchException e) {
				sendError(HttpStatus.BAD_REQUEST,"BAD REQUEST: Reference System is not matching with AW", response);
			} catch (RegionOutOfAWBoundsException e) {
				sendError(HttpStatus.BAD_REQUEST,"BAD REQUEST: Region is out of AW's bounds", response);
			} catch (RegionNameAlreadyInAWException e) {
				sendError(HttpStatus.BAD_REQUEST,"BAD REQUEST: Region name already exists", response);
			}
		}
	}
	
	protected static void handleGetRegionsDetails(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		String sessionID = rc.request().getHeader("sessionID");
		
		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		if(agentAuthenticated(awID, sessionID, response)) {
			
			AW aw = Environment.getAugmentedWorldRunningInstance(awID);
	
			JsonObject descriptionObj = new JsonObject();
			JsonArray regionsArray = new JsonArray();
			
			for (Region r : aw.regions()) {
				regionsArray.add(new JsonObject().put("name", r.name()).put("id", r.id()));
			}
			
			descriptionObj.put("regions", regionsArray);
	
			sendOK(descriptionObj, response);
		}
		
	}
	
	protected static void handleGetRegion(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		String sessionID = rc.request().getHeader("sessionID");

		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		if(agentAuthenticated(awID, sessionID, response)) {
		
			String regionID = rc.request().getParam("regionID");
	
			if (regionID == null) {
				sendError(HttpStatus.BAD_REQUEST, response);
				return;
			}
			
			AW aw = Environment.getAugmentedWorldRunningInstance(awID);
	
			Region region = aw.region(regionID);
	
			if(region != null) {
				sendOK(region.getJSONRepresentation(), response);
			} else {
				sendError(HttpStatus.NOT_FOUND, response);
			}
		}
	}

	protected static void handleGetEntityModel(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		String sessionID = rc.request().getHeader("sessionID");
		
		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		if(agentAuthenticated(awID, sessionID, response)) {

			String entityID = rc.request().getParam("entityID");
	
			if (entityID == null) {
				sendError(HttpStatus.BAD_REQUEST, response);
				return;
			}
			
			try {
				AW aw = Environment.getAugmentedWorldRunningInstance(awID);
	
				AE ae = aw.entity(entityID);
	
				JsonObject resBody = new JsonObject()
					.put("id", ae.id())
					.put("tag", ae.tag())
					.put("type", ae.type());
	
				sendOK(resBody, response);
			} catch (AugmentedEntityNotFoundException e) {
				sendError(HttpStatus.NOT_FOUND, response);
			}
		}
	}

	protected static void handleGetEntityModelElement(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		String sessionID = rc.request().getHeader("sessionID");
		
		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		if(agentAuthenticated(awID, sessionID, response)) {

			String entityID = rc.request().getParam("entityID");
			String element = rc.request().getParam("modelElement");
	
			if (entityID == null) {
				sendError(HttpStatus.BAD_REQUEST, response);
				return;
			}
			
			try {
				AW aw = Environment.getAugmentedWorldRunningInstance(awID);
	
				AE ae = aw.entity(entityID);
	
				switch(element) {
					case "id":
						sendOK("\"" + ae.id() + "\"", response);
						break;
						
					case "tag":
						sendOK("\"" + ae.tag() + "\"", response);
						break;
					
					case "type":
						sendOK("\"" + ae.type() + "\"", response);
						break;
					
					default:
						sendError(400, response);
						break;
				}
			} catch (AugmentedEntityNotFoundException e) {
				sendError(HttpStatus.NOT_FOUND, response);
			}
		}
	}

	protected static void handleGetEntityProperties(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		String sessionID = rc.request().getHeader("sessionID");

		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		if(agentAuthenticated(awID, sessionID, response)) {
		
			String entityID = rc.request().getParam("entityID");
	
			if (entityID == null) {
				sendError(HttpStatus.BAD_REQUEST, response);
				return;
			}
			
			try {
				AW aw = Environment.getAugmentedWorldRunningInstance(awID);
	
				AE ae = aw.entity(entityID);
	
				JsonObject resBody = new JsonObject()
						.put("location", ae.location().getJSONRepresentation())
						.put("orientation", ae.orientation().getJSONRepresentation())
						.put("extension", ae.extension().getJSONRepresentation());
						//.put("holograms", ae.holograms());
				
				if(ae instanceof UserAvatar) {
					resBody.put("gaze", ((UserAvatar) ae).gaze().getJSONRepresentation());
				}
	
				for (String p : ae.customProperties()) {
					try {
						try {
							resBody.put(p, ae.customProperty(p));
						} catch (IllegalStateException e) {
							resBody.put(p, JsonObject.mapFrom(ae.customProperty(p)));
						}
					} catch (PropertyNotFoundException e) {
						e.printStackTrace();
					}
				}
				
				sendOK(resBody, response);
			} catch (AugmentedEntityNotFoundException e) {
				sendError(HttpStatus.NOT_FOUND, response);
				return;
			}
		}
	}

	protected static void handleGetEntityProperty(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		String sessionID = rc.request().getHeader("sessionID");
		
		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		if(agentAuthenticated(awID, sessionID, response)) {
			
			String entityID = rc.request().getParam("entityID");
			String property = rc.request().getParam("property");
	
			if (entityID == null || property == null) {
				sendError(HttpStatus.BAD_REQUEST, response);
				return;
			}
			
			try {
				AW aw = Environment.getAugmentedWorldRunningInstance(awID);
	
				AE ae = aw.entity(entityID);
	
				if (property.equals("location")) {
					sendOK(ae.location().getJSONRepresentation(), response);
					return;
				}
	
				if (property.equals("orientation")) {
					sendOK(ae.orientation().getJSONRepresentation(), response);
					return;
				}
	
				if (property.equals("extension")) {
					sendOK(ae.extension().getJSONRepresentation(), response);
					return;
				}
	
				JsonObject resObj = new JsonObject();
	
				try {
					log(JsonObject.mapFrom(ae.customProperty(property)).encodePrettily());
					resObj.put("value", JsonObject.mapFrom(ae.customProperty(property)).encodePrettily());
				} catch (IllegalArgumentException e) {
					if (ae.customProperty(property).getClass().equals(String.class)) {
						resObj = new JsonObject().put("value", "\"" + String.valueOf(ae.customProperty(property)) + "\"");
					} else {
						resObj = new JsonObject().put("value", String.valueOf(ae.customProperty(property)));
					}
				}
	
				sendOK(resObj, response);
			} catch (AugmentedEntityNotFoundException | PropertyNotFoundException e) {
				sendError(HttpStatus.NOT_FOUND, response);
			}
		}
	}

	protected static void handleSetEntityProperty(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		String sessionID = rc.request().getHeader("sessionID");
		
		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		if(agentAuthenticated(awID, sessionID, response)) {
		
			String entityID = rc.request().getParam("entityID");
			String property = rc.request().getParam("property");
	
			if (entityID == null || property == null) {
				sendError(HttpStatus.BAD_REQUEST, response);
				return;
			}
			
			try {
				AW aw = Environment.getAugmentedWorldRunningInstance(awID);
	
				AE ae = aw.entity(entityID);
				
				if(!(ae instanceof UserAvatar)) {
	
					if (property.equals("location")) {
						JsonObject details = rc.getBodyAsJson();
						
						Location location = null;
		
						if (details.getString("type").equals(Location.LocationType.CARTESIAN.toString())) {
							location = new CartesianLocation(details.getDouble("x"), details.getDouble("y"), details.getDouble("z"));
						} else if (details.getString("type").equals(Location.LocationType.GPS.toString())) {
							location = new GpsLocation(details.getDouble("latitude"), details.getDouble("longitude"), details.getDouble("altitude"));
						}
		
						if (location != null) {
							ae.location(location);
						}
		
						log("Location updated on " + ae.id());
						sendOK(response);
						return;
					}
		
					if (property.equals("orientation")) {
						JsonObject details = rc.getBodyAsJson();
						
						Orientation orientation = null;
		
						if (details.getString("type").equals(Orientation.Type.ANGULAR.toString())) {
							orientation = new AngularOrientation(details.getDouble("roll"), details.getDouble("pitch"),
									details.getDouble("yaw"));
						}
		
						if (orientation != null) {
							ae.orientation(orientation);
						}
		
						log("Orientation updated on " + ae.id());
						sendOK(response);
						return;
					}
		
					if (property.equals("extension")) {
						JsonObject details = rc.getBodyAsJson();
						
						Extension extension = null;
						
						if (details.getString("type").equals(Extension.ExtensionType.BASIC.toString())) {
							extension = new BasicExtension(details.getDouble("radius"));
						}
						
						if (details.getString("type").equals(Extension.ExtensionType.SPHERIC.toString())) {
							extension = new SphericExtension(details.getDouble("radius"));
						}
						
						if (extension != null) {
							ae.extension(extension);
						}
		
						log("Orientation updated on " + ae.id());
						
						sendOK(response);
						return;
					}
					
					if(ae.customProperties().contains(property)) {
						String details = rc.getBodyAsString();		
						try {
							//TODO: considerare i valori non string...
							ae.customProperty(property, details);
						} catch (PropertyNotFoundException e) {
							e.printStackTrace();
						} 
						
						sendOK(response);
						return;
					}
		
					log("error!");
					
					//TODO: to be completed. custom properties?
				} else {
					sendError(HttpStatus.UNAUTHORIZED, response);
				}
			} catch (AugmentedEntityNotFoundException e) {
				log("Entity or Property not found!");
				sendError(HttpStatus.NOT_FOUND, response);
			}
		}
	}

	protected static void handleDoActionOnEntityHologram(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		String sessionID = rc.request().getHeader("sessionID");
		
		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		if(agentAuthenticated(awID, sessionID, response)) {
			
			String entityID = rc.request().getParam("entityID");
			String hologramID = rc.request().getParam("hologramID");
			String action = rc.request().getParam("action");
	
			if (entityID == null || hologramID == null || action == null) {
				sendError(HttpStatus.BAD_REQUEST, response);
			} else {
				JsonObject paramsObj = new JsonObject();
	
				try {
					paramsObj = rc.getBodyAsJson();
				} catch (DecodeException de) {	}
				
				JsonObject messageContent = new JsonObject()
						.put("entityId", entityID)
						.put("hologramId", hologramID)
						.put("actionId", action)
						.put("params", paramsObj);
				
				HologramEngineBridge heb = HologramEngineBridge.instance();
				heb.notifyEvent(HebEvent.EXECUTE_ACTION_ON_HOLOGRAM, messageContent);
	
				sendOK(response);
			}
		}
	}

	protected static void handleGetEntityActions(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		String sessionID = rc.request().getHeader("sessionID");

		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		if(agentAuthenticated(awID, sessionID, response)) {

			String entityID = rc.request().getParam("entityID");
	
			if (entityID == null) {
				sendError(HttpStatus.BAD_REQUEST, response);
				return;
			}
			
			try {
				AW aw = Environment.getAugmentedWorldRunningInstance(awID);
	
				AE ae = aw.entity(entityID);
	
				JsonArray actionsList = new JsonArray();
	
				for (String action : ae.actions().keySet()) {
					JsonArray types = new JsonArray();
	
					for (int i = 0; i < ae.actions().get(action).length; i++) {
						types.add(ae.actions().get(action)[i]);
					}
	
					actionsList.add(new JsonObject().put("name", action).put("paramsTypeList", types));
				}
	
				sendOK(actionsList.encodePrettily(), response);
	
			} catch (AugmentedEntityNotFoundException e) {
				sendError(HttpStatus.NOT_FOUND, response);
			}
		}
	}

	protected static void handleDoActionOnEntity(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		
		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		String entityID = rc.request().getParam("entityID");
		String action = rc.request().getParam("action");

		if (entityID == null || action == null) {
			sendError(HttpStatus.BAD_REQUEST, response);
		} else {
			try {
				List<Object> paramsValues = new ArrayList<>();

				try {
					JsonObject paramsObj = rc.getBodyAsJson();

					if (paramsObj != null) {
						JsonArray params = paramsObj.getJsonArray("params");

						if (params != null) {
							params.forEach(p -> paramsValues.add(p));
						}
					}
				} catch (DecodeException de) {
					//Action with no parameters. Nothing to do!
				}

				AW aw = Environment.getAugmentedWorldRunningInstance(awID);

				AE ae = aw.entity(entityID);

				if (paramsValues.size() > 0) {
					ae.executeAction(ae, action, paramsValues.toArray());
				} else {
					ae.executeAction(ae, action);
				}

				sendOKACCEPTED(response);
			} catch (AugmentedEntityNotFoundException | ActionNotFoundException e) {
				sendError(HttpStatus.NOT_FOUND, response);
			}
		}
	}
	
	protected static void handleUserUpdateGaze(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		String sessionID = rc.request().getHeader("sessionID");
		
		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		if(userAuthenticated(awID, sessionID, response)) {
			
			JsonObject details = rc.getBodyAsJson();
			String userId = Environment.getAugmentedWorldRunningInstance(awID).getUserIDFromSessionId(sessionID);
			Gaze3D newGaze = new Gaze3D(
					details.getDouble("x"),
					details.getDouble("y"),
					details.getDouble("z")
					);
			
			
			try {
				Environment.getAugmentedWorldRunningInstance(awID).user(userId).gaze(newGaze);
				sendOKACCEPTED(response);
			} catch (AugmentedEntityNotFoundException e) {
				sendError(HttpStatus.NOT_FOUND, response);
			}
			
		}
	}
	
	protected static void handleUserUpdateLocation(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		String sessionID = rc.request().getHeader("sessionID");
		
		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		if(userAuthenticated(awID, sessionID, response)) {
			

			JsonObject details = rc.getBodyAsJson();
			String userId = Environment.getAugmentedWorldRunningInstance(awID).getUserIDFromSessionId(sessionID);
			
			Location location = null;

			if (details.getString("type").equals(Location.LocationType.CARTESIAN.toString())) {
				location = new CartesianLocation(details.getDouble("x"), details.getDouble("y"),
						details.getDouble("z"));
			} else if (details.getString("type").equals(Location.LocationType.GPS.toString())) {
				location = new GpsLocation(details.getDouble("latitude"), details.getDouble("longitude"),
						details.getDouble("altitude"));
			}

			if (location != null) {
				try {
					Environment.getAugmentedWorldRunningInstance(awID).user(userId).location(location);
				} catch (AugmentedEntityNotFoundException e) {
					sendError(HttpStatus.NOT_FOUND, response);
				}
			}

			log("Location updated on " + userId);
			sendOKACCEPTED(response);
			return;

			
		}
	}
	
	
/*	
	protected static void handleMessageQueueStatus(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		
		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		String queueId = rc.request().getParam("queueID");

		if (queueId == null) {
			sendError(400, response);
		} else {
			JsonArray messages = new JsonArray();
			
			List<Message> messageList = QueuesManager.getMessageListByQueue(queueId);
			
			messageList.forEach(m -> {
				messages.add(new JsonObject()
						.put("timestamp", m.timestamp())
						.put("type", m.type())
						.put("content", m.content()));
			});
			
			response.putHeader("content-type", "application/json").end(messages.encodePrettily());
		}
	}
	
	protected static void handlePopMessageFromQueue(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		
		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		String queueId = rc.request().getParam("queueID");

		if (queueId == null) {
			sendError(400, response);
		} else {
			Message m = QueuesManager.consumeMessage(queueId);
			
			if(m != null) {
				JsonObject messageObj = new JsonObject()
						.put("timestamp", m.timestamp())
						.put("type", m.type())
						.put("content", m.content());
				
				response.putHeader("content-type", "application/json").end(messageObj.encodePrettily());
			} else {
				sendError(400, response);
			}
		}
	}
	
	protected static void handlePushMessageToQueue(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		
		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		String queueId = rc.request().getParam("queueID");

		if (queueId == null) {
			sendError(400, response);
		} else {
			JsonObject messageObj = rc.getBodyAsJson();
			
			QueuesManager.storeMessage(queueId, 
					new Message(messageObj.getString("type"), messageObj.getJsonObject("content")));
			
			response.end();
		}
	}
	
	public static void handleAgentRegisterPolicy(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		
		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		String agentName = rc.request().getParam("agentName");
		
		if(agentName == null) {
			sendError(400, response);
			return;
		}
		
		JsonObject policy = rc.getBodyAsJson();
		
		if(policy != null) {
			if(agentName.equals(CollisionDetectorAgent.class.getSimpleName())) {
				JsonObject responseObj;
				try {
					responseObj = CollisionDetectorAgent.instance().registerNewPolicy(policy);
				} catch (PolicyMalformedException e) {
					sendError(400, response);
					return;
				}
				response.putHeader("content-type", "application/json").end(responseObj.encodePrettily());
				return;
			}
			
			sendError(400, response);
		} else {
			sendError(400, response);
		}
	}
	
	public static void handleAgentRemovePolicy(RoutingContext rc) {
		String awID = rc.request().getParam("awID");
		
		HttpServerResponse response = rc.response();
		
		if(badAwRequest(awID, response)) {
			return;
		}
		
		String agentName = rc.request().getParam("agentName");
		String policyId = rc.request().getParam("policyID");
		
		if(agentName == null) {
			sendError(400, response);
			return;
		}
		
		if(policyId != null) {
			if(agentName.equals(CollisionDetectorAgent.class.getSimpleName())) {
				CollisionDetectorAgent.instance().removePolicy(policyId);
				response.end();
				return;
			}
			
			sendError(400, response);
		} else {
			sendError(400, response);
		}
	}
	
	/*
	 * PRIVATE MEMBERS
	 */
	
private static boolean checkPathForTracking(String[] pathSequence) {
		if(pathSequence.length == 4 && 
				(pathSequence[2].equals("regions") || pathSequence[2].equals("entities"))) {
			return true;
		} else if(pathSequence.length == 6 && 
				 pathSequence[2].equals("entities") && 
				 pathSequence[4].equals("properties")) {
			return true;
		}
		return false;
	}
	
	private static boolean badAwRequest(String awID, HttpServerResponse response) {
		if (awID == null) {
			sendError(HttpStatus.BAD_REQUEST, response);
			return true;
		}
		AW aw = Environment.getAugmentedWorldRunningInstance(awID);

		if(aw == null || !aw.isActive()) {
			sendError(HttpStatus.NOT_FOUND, response);
			return true;
		}
		
		return false;
	}
	
	private static boolean agentAuthenticated(String awID, String sessionID, HttpServerResponse response) {
		if (awID == null) {
			sendError(HttpStatus.BAD_REQUEST, response);
			return false;
		}
		AW aw = Environment.getAugmentedWorldRunningInstance(awID);

		if(aw == null || !aw.isActive()) {
			sendError(HttpStatus.NOT_FOUND, response);
			return false;
		}
		
		if(aw.isAgentJoined(sessionID)) {
			return true;
		}
		
		sendError(HttpStatus.UNAUTHORIZED, response);
		return false;
	}
	
	private static boolean userAuthenticated(String awID, String sessionID, HttpServerResponse response) {
		if (awID == null) {
			sendError(HttpStatus.BAD_REQUEST, response);
			return false;
		}
		AW aw = Environment.getAugmentedWorldRunningInstance(awID);

		if(aw == null || !aw.isActive()) {
			sendError(HttpStatus.NOT_FOUND, response);
			return false;
		}
		
		if(aw.isUserJoined(sessionID)) {
			return true;
		}
		
		sendError(HttpStatus.UNAUTHORIZED, response);
		return false;
	}
	
	private static boolean agentAuthenticatedForTracking(String awID, String sessionID, ServerWebSocket ws) {
		if (awID == null) {
			ws.reject();
			return false;
		}
		AW aw = Environment.getAugmentedWorldRunningInstance(awID);

		if(aw == null || !aw.isActive()) {
			ws.reject();
			return false;
		}
		
		if(aw.isAgentJoined(sessionID)) {
			return true;
		}
		
		ws.reject();
		return false;
	}

	private static void sendOKCREATED(HttpServerResponse response, JsonObject responseBody) {
		response
			.setStatusCode(HttpStatus.CREATED)
			//.putHeader("content-type", "application/json")
			.end(responseBody.encodePrettily());
	}
	
	private static void sendOKACCEPTED(HttpServerResponse response) {
		response
			.setStatusCode(HttpStatus.ACCEPTED)
			//.putHeader("content-type", "application/json")
			.end();
	}
	
	private static void sendOK(HttpServerResponse response) {
		response
			.setStatusCode(HttpStatus.OK)
			.end();
	}
	
	private static void sendOK(String responseBody, HttpServerResponse response) {
		response
		.setStatusCode(HttpStatus.OK)
		.putHeader("content-type", "application/json")
		.end(responseBody);
	}
	
	private static void sendOK(JsonObject responseBody, HttpServerResponse response) {
		response
			.setStatusCode(HttpStatus.OK)
			.putHeader("content-type", "application/json")
			.end(responseBody.encodePrettily());
	}
	
	private static void sendError(int statusCode, HttpServerResponse response) {
		sendError(statusCode, null, response);
	}

	private static void sendError(int statusCode, String statusMessage, HttpServerResponse response) {
		response.setStatusCode(statusCode);
		
		if(statusMessage != null){
			response.setStatusMessage(statusMessage);
		}
		
		response.end();
	}

	private static void log(String msg) {
		LoggerFactory.getLogger(AWServiceHandlers.class).info("[" + AWService.class.getSimpleName() + "] " + msg);
	}
}
