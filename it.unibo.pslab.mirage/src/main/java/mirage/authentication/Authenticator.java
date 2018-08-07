package mirage.authentication;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface Authenticator {

	public boolean isAuthenticated(String sessionID);
	
	public String newAuthentication(JsonObject credentials);
	
	public void removeAuthentication(String sessionID);
	
	public void cleanAuthentications();
	
	public JsonArray getAuthenticatedCredentials();
		
}
