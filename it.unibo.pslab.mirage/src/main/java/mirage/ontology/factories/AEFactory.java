package mirage.ontology.factories;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import mirage.exceptions.AugmentedEntityCreationException;
import mirage.ontology.AE;
import mirage.ontology.UserAvatar;
import mirage.ontology.ae.Extension;
import mirage.ontology.ae.Location;
import mirage.ontology.ae.Orientation;
import mirage.ontology.ae.extension.BasicExtension;
import mirage.ontology.ae.extension.SphericExtension;
import mirage.ontology.ae.location.CartesianLocation;
import mirage.ontology.ae.location.GpsLocation;
import mirage.ontology.ae.orientation.AngularOrientation;
import mirage.ontology.annotations.ACTION;
import mirage.ontology.annotations.HOLOGRAM;
import mirage.ontology.annotations.PROPERTY;
import mirage.ontology.things.PhysicalThingConnectionsManager;
import mirage.ontology.user.Gaze3D;
import mirage.utils.Config;

/**
 * 
 */
public final class AEFactory {

	/**
	 * @param type
	 * @param descriptionObj
	 * @return
	 * @throws AugmentedEntityCreationException
	 */
	public static AE createAugmentedEntity(String type, JsonObject descriptionObj)
			throws AugmentedEntityCreationException {
		JsonObject modelObj = descriptionObj.getJsonObject("model");
		JsonObject propertiesObj = descriptionObj.getJsonObject("properties");
		JsonArray thingsArray = descriptionObj.getJsonArray("things");
		
		AE ae = null;
		
		try {
			Class<?> baseClass;
			
			if(type.equals("AE")) {
				baseClass = AE.class;
			} else {
				baseClass = Class.forName(Config.ENTITIES_PATH + "." + type);
			}

			Class<?> c = baseClass;
			
			final Annotation[] cAnn = c.getAnnotations();
			String hologramGeometry = "NULL";
			String hologramParent = "NULL";
			
			for(Annotation a : cAnn) {
				if(a instanceof HOLOGRAM) {
					hologramGeometry = ((HOLOGRAM) a).geometry();
					hologramParent = ((HOLOGRAM) a).parent();
				}
			}
			
			ae = (AE) c.newInstance();

			while (c != null) {
				try {
					setField(c, ae, "id", modelObj.getString("id"));
					setField(c, ae, "tag", modelObj.getString("tag"));
					setField(c, ae, "type", type);
					setField(c, ae, "hologramGeometry", hologramGeometry);
					setField(c, ae, "hologramParent", hologramParent);
					setField(c, ae, "location", decodeLocation(propertiesObj));
					setField(c, ae, "orientation", decodeOrientation(propertiesObj));
					setField(c, ae, "extension", decodeExtension(propertiesObj));					
					//setField(c, ae, "holograms", propertiesObj.getJsonArray("holograms"));					
					setField(c, ae, "things", thingsArray);
				} catch (NoSuchFieldException e) {
					//nothing to do, just ignore!
				}

				c = c.getSuperclass();
			}

			c = baseClass;

			for (Method m : c.getMethods()) {
				if (m.isAnnotationPresent(ACTION.class)) {
					List<String> paramsTypeList = new ArrayList<>();
					Arrays.asList(m.getParameterTypes()).forEach(t -> paramsTypeList.add(t.getName()));
					registerAction(c, ae, m.getName(), paramsTypeList.stream().toArray(String[]::new));
				}
			}

			while (c != null) {
				for (Field f : c.getDeclaredFields()) {
					f.setAccessible(true);
					if (f.isAnnotationPresent(PROPERTY.class)) {
						registerCustomProperty(c, ae, f.getName(), f.get(ae));
					}
				}

				c = c.getSuperclass();
			}
		} catch (NoClassDefFoundError | ClassNotFoundException | InstantiationException | IllegalAccessException | SecurityException
				| IllegalArgumentException e) {
			e.printStackTrace();
			throw new AugmentedEntityCreationException();
		}

		return ae;
	}
	
	public static UserAvatar createUserAvatar(JsonObject descriptionObj)
			throws AugmentedEntityCreationException {
		JsonObject modelObj = descriptionObj.getJsonObject("model");
		JsonObject propertiesObj = descriptionObj.getJsonObject("properties");
		//JsonArray thingsArray = descriptionObj.getJsonArray("things");
		
		UserAvatar userAvatar = null;
		
		try {
			Class<?> baseClass;
			
			baseClass = UserAvatar.class;

			Class<?> c = baseClass;
			
			final Annotation[] cAnn = c.getAnnotations();
			String hologramGeometry = "NULL";
			String hologramParent = "NULL";
			
			for(Annotation a : cAnn) {
				if(a instanceof HOLOGRAM) {
					hologramGeometry = ((HOLOGRAM) a).geometry();
					hologramParent = ((HOLOGRAM) a).parent();
				}
			}
			
			userAvatar = (UserAvatar) c.newInstance();

			while (c != null) {
				try {
					setField(c, userAvatar, "id", modelObj.getString("id"));
					setField(c, userAvatar, "tag", modelObj.getString("tag"));
					setField(c, userAvatar, "type", "UserAvatar");
					setField(c, userAvatar, "hologramGeometry", hologramGeometry);
					setField(c, userAvatar, "hologramParent", hologramParent);
					setField(c, userAvatar, "location", decodeLocation(propertiesObj));
					setField(c, userAvatar, "orientation", decodeOrientation(propertiesObj));
					setField(c, userAvatar, "extension", new BasicExtension(0));					
					//setField(c, userAvatar, "holograms", propertiesObj.getJsonArray("holograms"));					
					setField(c, userAvatar, "things", null);
				} catch (NoSuchFieldException e) {
					//nothing to do, just ignore!
				}

				c = c.getSuperclass();
			}

			c = baseClass;
			
			userAvatar.gaze(decodeGaze(propertiesObj));


		} catch (NoClassDefFoundError | InstantiationException | IllegalAccessException | SecurityException
				| IllegalArgumentException e) {
			e.printStackTrace();
			throw new AugmentedEntityCreationException();
		}

		return userAvatar;
	}

	/**
	 * 
	 * @param ae
	 * @param thingDescription
	 */
	public static void linkToPhysicalThing(AE ae, JsonObject thingDescription) {
		PhysicalThingConnectionsManager.activate(ae, thingDescription);
	}
	
	/*
	 * Private methods
	 */
	
	private static void setField(Class<?> c, Object instance, String field, Object value)
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field idField = c.getDeclaredField(field);
		idField.setAccessible(true);
		idField.set(instance, value);
	}
	
	@SuppressWarnings("unchecked")
	private static void registerAction(final Class<?> c, Object instance, String name, String...paramsTypeList) {
		Class<?> c1 = c;		
		Field actionsField = null;
		boolean found = false;
		
		while(!found && c1 != null) {
			try {
				actionsField = c1.getDeclaredField("actions");
				actionsField.setAccessible(true);
				found = true;
			} catch (NoSuchFieldException | SecurityException e) {
				c1 = c1.getSuperclass();
			}
		}
		
		if(found) {
			try {
				HashMap<String, String[]> actions = (HashMap<String, String[]>) actionsField.get(instance);
				actions.put(name, paramsTypeList);
			} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void registerCustomProperty(final Class<?> c, Object instance, String name, Object value) {
		Class<?> c1 = c;		
		Field cpField = null;
		boolean found = false;
		
		while(!found && c1 != null) {
			try {
				cpField = c1.getDeclaredField("customProperties");
				cpField.setAccessible(true);
				found = true;
			} catch (NoSuchFieldException | SecurityException e) {
				c1 = c1.getSuperclass();
			}
		}
		
		if(found) {
			try {
				HashMap<String, Object> customProperties = (HashMap<String, Object>) cpField.get(instance);
				
				List<String> defaultProperties = new ArrayList<>(
						Arrays.asList("location", "orientation", "extension")//, "holograms")
				);
				
				if(defaultProperties.contains(name) || customProperties.containsKey(name)) {
					return; //throw new PropertyAlreadyAddedException();
				}
				
				customProperties.put(name, value);
				
			} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}	
		}
	}
	
	private static Location decodeLocation(JsonObject propertiesObj) {
		JsonObject locationObj = propertiesObj.getJsonObject("location");
		
		Location location = null;

		if (locationObj != null) {
			if (locationObj.getString("type").equals(Location.LocationType.CARTESIAN.toString())) {
				return new CartesianLocation(
						locationObj.getDouble("x"),
						locationObj.getDouble("y"),
						locationObj.getDouble("z"));
			} 
			
			if (locationObj.getString("type").equals(Location.LocationType.GPS.toString())) {
				return new GpsLocation(
						locationObj.getDouble("latitude"),
						locationObj.getDouble("longitude"),
						locationObj.getDouble("altitude"));
			}
		}
		
		return location;
	}
	
	private static Orientation decodeOrientation(JsonObject propertiesObj) {
		JsonObject orientationObj = propertiesObj.getJsonObject("orientation");
		
		Orientation orientation = null;

		if (orientationObj != null) {
			if (orientationObj.getString("type").equals(Orientation.Type.ANGULAR.toString())) {
				return new AngularOrientation(
						orientationObj.getDouble("roll"),
						orientationObj.getDouble("pitch"),
						orientationObj.getDouble("yaw"));
			}
		}
		
		return orientation;
	}
	
	private static Extension decodeExtension(JsonObject propertiesObj) {
		JsonObject extensionObj = propertiesObj.getJsonObject("extension");
		
		Extension extension = null;

		if (extensionObj != null) {
			if (extensionObj.getString("type").equals(Extension.ExtensionType.BASIC.toString())) {
				return new BasicExtension(
						extensionObj.getDouble("radius"));
			}
			
			if (extensionObj.getString("type").equals(Extension.ExtensionType.SPHERIC.toString())) {
				return new SphericExtension(
						extensionObj.getDouble("radius"));
			}
		}
		
		return extension;
	}

	private static Gaze3D decodeGaze(JsonObject propertiesObj) {
		JsonObject gazeObj = propertiesObj.getJsonObject("gaze");

		if (gazeObj != null) {
			return new Gaze3D(
					gazeObj.getDouble("x"),
					gazeObj.getDouble("y"),
					gazeObj.getDouble("z")
					);		
		}
		
		return null;
	}
}
