package mirage.ontology.aw;

public enum AwReferenceSystem {
	DEFAULT("default"),
	MARKER_BASED("marker-based");
	
	private String description;
	
	AwReferenceSystem(String description) {
		this.description = description;
	}
	
	@Override
	public String toString() {
		return this.description;
	}
}
