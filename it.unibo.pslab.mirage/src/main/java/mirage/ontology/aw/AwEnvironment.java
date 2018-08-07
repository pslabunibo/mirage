package mirage.ontology.aw;

public enum AwEnvironment {
	VR("vr"),
	AR("ar");
	
	private String description;
	
	AwEnvironment(String description) {
		this.description = description;
	}
	
	@Override
	public String toString() {
		return this.description;
	}
}
