package unused;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import it.unibo.disi.pslab.aw.Config;
import it.unibo.disi.pslab.aw.ontology.AE;

public class UnityEngine {
	private static TCPClient tcpClient;
	private static Vertx vertx;
	private static String serviceId;
	
	private static ExecutorService threadPool;
	
	private static String awName;
	private static JsonObject awDetails;
	
	/**
	 * 
	 * @param v
	 */
	public static void init(String id, Vertx v) {
		serviceId = id;
		vertx = v;
		
		threadPool = Executors.newFixedThreadPool(1);//Runtime.getRuntime().availableProcessors() + 1);
		
		tcpClient = new TCPClient(Config.UNITY_ENGINE_ADDRESS, Config.HOLOGRAM_ENGINE_BRIDGE_PORT);
	}
	
	public static boolean isEnabled() {
		return Config.HOLOGRAM_ENGINE_BRIDGE_ENABLED;
	}
	
	/**
	 * 
	 * @param id
	 */
	public static void istantiateAugmentWorld(String name, JsonObject details) {
		awName = name;
		awDetails = details;
		
		try {
			tcpClient.connect();
			
			while(!tcpClient.isConnected());
			
			JsonObject messageContent = new JsonObject()
					.put("name", name);
			
			messageContent.mergeIn(details, true);
			
			send("create-augmented-world", messageContent, Config.UNITY_ENGINE_SEND_ATTEMPTS);
		} catch (UnknownHostException e) {
			log("Unable to connect (unknown host)!");
		} catch (IOException e) {
			log("Unable to connect (server unreachable)!");
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public static boolean isConnected() {
		if(tcpClient != null) {
			return tcpClient.isConnected();
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param ae
	 */
	public static void createHologram(AE ae) {
		send("new-hologram", ae.getJSONRepresentation(), Config.UNITY_ENGINE_SEND_ATTEMPTS);
	}
	
	/**
	 * 
	 * @param id
	 */
	public static void removeHologram(String id) {
		//TODO: to be implemented!
	}
	
	/**
	 * 
	 * @param entityId
	 * @param propertyId
	 * @param details
	 */
	public static void updateHologramProperty(String entityId, String propertyId, JsonObject value) {		
		JsonObject messageContent = new JsonObject()
				.put("entityId", entityId)
				.put("propertyId", propertyId)
				.put("value", value);
		
		send("update-hologram-property", messageContent, Config.UNITY_ENGINE_SEND_ATTEMPTS);
	}
	
	public static void updateHologramProperty(String entityId, String propertyId, String value) {		
		JsonObject messageContent = new JsonObject()
				.put("entityId", entityId)
				.put("propertyId", propertyId)
				.put("value", value);
		
		send("update-hologram-property", messageContent, Config.UNITY_ENGINE_SEND_ATTEMPTS);
	}
	
	/**
	 * 
	 * @param entityId
	 * @param hologramId
	 * @param actionId
	 * @param params
	 */
	public static void executeHologramAction(String entityId, String hologramId, String actionId, JsonObject params) {
		JsonObject messageContent = new JsonObject()
				.put("entityId", entityId)
				.put("hologramId", hologramId)
				.put("actionId", actionId)
				.put("params", params);
				
		send("execute-action-on-hologram", messageContent, Config.UNITY_ENGINE_SEND_ATTEMPTS);
	}
	
	/*
	 * -------------------------------------------------------------------
	 * -------------------- internal utils -------------------------------
	 * -------------------------------------------------------------------
	 */
	
	private static synchronized void send(String message, JsonObject content, int attemps) {
		if(attemps == 0) {
			return;
		}
		
		threadPool.execute(() -> {			
			if(tcpClient.isConnected()) {
				JsonObject msg = new JsonObject()
						.put("message", message)
						.put("content", content);
				
				tcpClient.send(msg.encode());
			} else {
				log("Send message failed! I will retry...");
				vertx.setTimer(1000, handler -> {
					istantiateAugmentWorld(awName, awDetails);
					send(message, content, attemps - 1);
				});
			}
		});
	}
	
	private static void log(String msg){
		System.out.println("[" + UnityEngine.class.getSimpleName() + "] " + msg);
		//LoggerFactory.getLogger(UnityEngine.class).info("[" + UnityEngine.class.getSimpleName() + "] " + msg);
	}
}
