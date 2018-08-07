package mirage.ontology.credentials;

public class AgentCredentials {

	private String username;

	public AgentCredentials(String username) {
		super();
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	@Override
	public boolean equals(Object obj) {
		return username.equals(((AgentCredentials)obj).username);
	}
}
