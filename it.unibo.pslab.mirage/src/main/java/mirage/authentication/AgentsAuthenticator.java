package mirage.authentication;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import mirage.ontology.Agent;
import mirage.ontology.credentials.AgentCredentials;

public class AgentsAuthenticator implements Authenticator {
	
	private Map<String, Agent> agents;
	
	public AgentsAuthenticator() {
		this.agents = new HashMap<>();
	}

	@Override
	public boolean isAuthenticated(String sessionID) {
		return (agents.containsKey(sessionID));
	}

	@Override
	public synchronized String newAuthentication(JsonObject credentials) {
		String username = credentials.getString("username");
		String sessionID;
		do {
			sessionID = UUID.randomUUID().toString();
		} while(agents.containsKey(sessionID));
		
		agents.put(sessionID, new Agent(new AgentCredentials(username), sessionID));
		return sessionID;
	}

	@Override
	public synchronized void removeAuthentication(String sessionID) {
		agents.remove(sessionID);
	}

	@Override
	public synchronized void cleanAuthentications() {
		agents.clear();
	}

	@Override
	public JsonArray getAuthenticatedCredentials() {
		JsonArray res = new JsonArray();
		agents.values().stream()
				.map(agent -> {return agent.getCredentials();})
				.map(cred -> {return parseCredentialsToJson(cred);})
				.forEach(jsonCred -> {
					res.add(jsonCred);
				});;
				
		return res;
	}
	
	private static JsonObject parseCredentialsToJson(AgentCredentials agentCredentials) {
		return new JsonObject().put("username", agentCredentials.getUsername());
	}

}
