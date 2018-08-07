package mirage.authentication;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import mirage.ontology.User;
import mirage.ontology.credentials.UserCredentials;

public class UsersAuthenticator implements Authenticator {

private Map<String, User> users;
	
	public UsersAuthenticator() {
		this.users = new HashMap<>();
	}

	@Override
	public boolean isAuthenticated(String sessionID) {
		return (users.containsKey(sessionID));
	}

	@Override
	public synchronized String newAuthentication(JsonObject credentials) {
		String username = credentials.getString("username");
		String sessionID;
		do {
			sessionID = UUID.randomUUID().toString();
		} while(users.containsKey(sessionID));
		
		users.put(sessionID, new User(new UserCredentials(username), sessionID));
		return sessionID;
	}

	@Override
	public synchronized void removeAuthentication(String sessionID) {
		users.remove(sessionID);
	}

	@Override
	public synchronized void cleanAuthentications() {
		users.clear();
	}

	@Override
	public JsonArray getAuthenticatedCredentials() {
		JsonArray res = new JsonArray();
		users.values().stream()
				.map(user -> {return user.getCredentials();})
				.map(cred -> {return parseCredentialsToJson(cred);})
				.forEach(jsonCred -> {
					res.add(jsonCred);
				});;
				
		return res;
	}
	
	private static JsonObject parseCredentialsToJson(UserCredentials userCredentials) {
		return new JsonObject().put("username", userCredentials.getUsername());
	}
}
