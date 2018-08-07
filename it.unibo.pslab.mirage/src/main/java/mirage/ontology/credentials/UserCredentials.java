package mirage.ontology.credentials;

public class UserCredentials {

	private String username;

	public UserCredentials(String username) {
		super();
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	@Override
	public boolean equals(Object obj) {
		return username.equals(((UserCredentials)obj).username);
	}
}
