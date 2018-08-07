package mirage.utils;

/**
 * 
 */
public class Config {
	public static int WOAT_NODE_SERVICE_PORT = 8080;
	public static int WOAT_NODE_APPLICATION_PORT = 8081;
	
	public static boolean HOLOGRAM_ENGINE_BRIDGE_ENABLED = false;
	public static int HOLOGRAM_ENGINE_BRIDGE_PORT = 8888;
	
	public static String ENTITIES_PATH;
	
	/**
	 * 
	 */
	public static class Label {
		public static String MIRAGE_RUNTIME = "mirage-runtime";
		
		public static String WOAT_NODE = "woat-node";
		
		public static String WOAT_NODE_SERVICE_PORT = "service-port";
		public static String WOAT_NODE_APPLICATION_PORT = "application-port";
		
		public static String HEB = "heb";
		
		public static String AE_PACKAGE = "ae-src-path";
	}
}
