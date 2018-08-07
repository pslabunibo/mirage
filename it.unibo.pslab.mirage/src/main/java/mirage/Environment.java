package mirage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import mirage.exceptions.AugmentedEntityNotFoundException;
import mirage.exceptions.AugmentedWorldAlreadyStartedException;
import mirage.exceptions.EnvironmentNotReadyException;
import mirage.heb.HologramEngineBridge;
import mirage.net.services.AWService;
import mirage.ontology.AE;
import mirage.ontology.AW;
import mirage.ontology.ae.Extension.ExtensionType;
import mirage.ontology.ae.Location.LocationType;
import mirage.ontology.aw.AwAddress;
import mirage.ontology.aw.AwEnvironment;
import mirage.ontology.aw.AwReferenceSystem;
import mirage.ontology.aw.AwReferenceSystemType;
import mirage.ontology.factories.AreaFactory;
import mirage.ontology.region.Area;
import mirage.utils.Config;

/**
 * 
 */
public class Environment {
	
	private static Environment instance = new Environment();
	
	private Map<String,AW> localAws = new HashMap<>();
	private Map<String,AW> remoteAws = new HashMap<>();
	private boolean deployed = false;
		
	/**
	 * 
	 * @param configFile
	 */
	public static void deployRuntime(String configFile) {
		instance.configEnvironment(configFile);		
		
		instance.startAWService();
		instance.startHologramEngineBridge();
		
		instance.deployed = true;
	}	
	
	/**
	 * 
	 * @param name
	 * @param environment
	 * @param referenceSystem
	 * @param referenceSystemType
	 * @param extensionType
	 * @param values
	 * @param local
	 * @param awAddress
	 * @throws EnvironmentNotReadyException
	 * @throws AugmentedWorldAlreadyStartedException
	 */
	public static void startNewAugmentedWorldInstance(final String name, final AwEnvironment environment, final AwReferenceSystem referenceSystem, final AwReferenceSystemType referenceSystemType, final ExtensionType extensionType, final Double[] values, final boolean local, final AwAddress awAddress)
			throws EnvironmentNotReadyException, AugmentedWorldAlreadyStartedException {
		instance.runAW(name, environment, referenceSystem, referenceSystemType, extensionType, values, local, awAddress);
	}
	
	/**
	 * 
	 * @param awID
	 * @return
	 */
	public static AW getAugmentedWorldRunningInstance(String awID) {
		AW aw = instance.localAws.get(awID);
		if(aw == null) {
			return instance.remoteAws.get(awID);
		}
		
		return aw;
	}
	
	public static AW getAugmentedWorldRunningInstance(AE ae) {
		AW aw = null;
		
		Collection<AW> aws = instance.localAws.values();
		aws.addAll(instance.remoteAws.values());
		
		for(AW w : aws) {
			List<AE> entities = w.entities();
			for(AE e : entities) {
				if(e.id().equals(ae.id())) {
					aw = w;
				}
			}
		}

		return aw;
	}
	
	public static AE findAugmentedEntityById(final String id) throws AugmentedEntityNotFoundException {
		Collection<AW> aws = instance.localAws.values();
		aws.addAll(instance.remoteAws.values());
		
		for(AW w : aws) {
			List<AE> entities = w.entities();
			for(AE e : entities) {
				if(e.id().equals(id)) {
					return e;
				}
			}
		}
		
		throw new AugmentedEntityNotFoundException();
	}
	
	private void runAW(String name, AwEnvironment environment, AwReferenceSystem referenceSystem, AwReferenceSystemType referenceSystemType, ExtensionType extensionType, Double[] values, boolean local, AwAddress awAddress) throws EnvironmentNotReadyException, AugmentedWorldAlreadyStartedException {
		if(this.deployed) {
			String awID = generateAWIDFromName(name);
			if(!this.localAws.containsKey(awID)) {	
					if(local) {
						runLocalAW(name, environment, referenceSystem, referenceSystemType, extensionType, values, null);
					} else {
						//TODO: runRemoteAW con ip e porta
					}
					
			} else {
				throw new AugmentedWorldAlreadyStartedException();
			}
		}
	}
	
	public static String generateAWIDFromName(String name) {
		return name;
	}


	public static Map<String, AW> getLocalAws() {
		return instance.localAws;
	}


	public static Map<String, AW> getRemoteAws() {
		return instance.remoteAws;
	}

	private void runLocalAW(String name, AwEnvironment environment, AwReferenceSystem referenceSystem, AwReferenceSystemType referenceSystemType, ExtensionType extensionType, Double[] values, String markersDb) throws EnvironmentNotReadyException, AugmentedWorldAlreadyStartedException {
		if(this.deployed) {
			AW aw = new AW();
			
			String awID = generateAWIDFromName(name);
			
			switch(extensionType) {
				case BASIC:
					
					Area extension = null;
					
					if(referenceSystemType == AwReferenceSystemType.GEOGRAPHICAL) {
						extension = AreaFactory.getInstance().generateArea(LocationType.GPS, ExtensionType.BASIC, values);
					} else {
						extension = AreaFactory.getInstance().generateArea(LocationType.CARTESIAN, ExtensionType.BASIC, values);
					}
					
					try {
						aw.start(name, generateAWIDFromName(name), extension,environment, referenceSystem, true, null);
						this.localAws.put(awID,aw);
					} catch (AugmentedWorldAlreadyStartedException e) {
						e.printStackTrace();
					}
					break;
					
			case POLYGONAL:
				break;
			case SPHERIC:
				break;
			default:
				break;
			}
			
			log("New Augmented World is running as local AW (name = " + name + ", id = " + awID + ")");
		} else {
			throw new AugmentedWorldAlreadyStartedException();
		}
	}

	private void configEnvironment(String configFile) {
		//TODO: to be completed
		
		try {
			String configFileContent = new String(Files.readAllBytes(Paths.get(configFile)));
			JsonObject configObj = new JsonObject(configFileContent);
			
			JsonObject mirageRuntimeObj = configObj.getJsonObject(Config.Label.MIRAGE_RUNTIME);
			
			/*
			 * WOAT NODE OBJ
			 */
			JsonObject woatNodeObj = mirageRuntimeObj.getJsonObject(Config.Label.WOAT_NODE);
			
			if(woatNodeObj != null) {
				Config.WOAT_NODE_SERVICE_PORT = woatNodeObj.getInteger(Config.Label.WOAT_NODE_SERVICE_PORT);
				Config.WOAT_NODE_APPLICATION_PORT = woatNodeObj.getInteger(Config.Label.WOAT_NODE_APPLICATION_PORT);
			} else {
				System.out.println("Configuration file does not have a definition for WOAT NODE settings.");
			}
				
			/*
			 * REMOTE UNITY ENGINE OBJ
			 */
			JsonObject hebObj = mirageRuntimeObj.getJsonObject(Config.Label.HEB);
			
			if(hebObj != null && hebObj.getBoolean("enabled") != null) {
				if(hebObj.getBoolean("enabled")) {
					Config.HOLOGRAM_ENGINE_BRIDGE_ENABLED = true;
					Config.HOLOGRAM_ENGINE_BRIDGE_PORT = hebObj.getInteger("port");
				} else {
					Config.HOLOGRAM_ENGINE_BRIDGE_ENABLED = false;
				}
			} else {
				System.out.println("Configuration file does not have a definition for HEB settings.");
			}
	
			/*
			 * AE SRC PATH
			 */
			Config.ENTITIES_PATH = mirageRuntimeObj.getString(Config.Label.AE_PACKAGE);			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void startAWService() {
		try {
			Vertx vertx = Vertx.vertx();
			vertx.deployVerticle(new AWService());
		} catch (Exception ex){
			ex.printStackTrace();
		}
	}
	
	private void startHologramEngineBridge() {
		if(Config.HOLOGRAM_ENGINE_BRIDGE_ENABLED) {
			HologramEngineBridge.start(Config.HOLOGRAM_ENGINE_BRIDGE_PORT);
		}
	}
		
	private static void log(String msg){
		LoggerFactory.getLogger(Environment.class).info("[" + Environment.class.getSimpleName() + "] " + msg);
	}
}
