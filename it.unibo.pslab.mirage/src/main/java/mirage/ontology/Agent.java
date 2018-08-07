package mirage.ontology;

import io.vertx.core.json.JsonObject;
import mirage.ontology.credentials.AgentCredentials;
import mirage.ontology.interfaces.JsonFormattable;

public class Agent implements JsonFormattable{
	
	private AgentCredentials credentials;
	private String sessionID;

	public Agent(AgentCredentials credentials, String sessionID){
		super();
		this.credentials = credentials;
		this.sessionID = sessionID;
	}

	public AgentCredentials getCredentials() {
		return credentials;
	}

	public String getSessionID() {
		return sessionID;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Agent other = (Agent) obj;
		if (!credentials.getUsername().equals(other.credentials.getUsername()) || !sessionID.equals(other.sessionID))
			return false;
		return true;
	}

	@Override
	public JsonObject getJSONRepresentation() {
		return new JsonObject().put("credentials", new JsonObject().put("username", credentials.getUsername()));
	}
}
