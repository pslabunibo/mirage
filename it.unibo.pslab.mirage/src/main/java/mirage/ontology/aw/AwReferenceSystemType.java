package mirage.ontology.aw;

public enum AwReferenceSystemType {
	GEOGRAPHICAL("geographical"),
	CARTESIAN("cartesian");
	
	private String description;
	
	AwReferenceSystemType(String description) {
		this.description = description;
	}
	
	@Override
	public String toString() {
		return this.description;
	}
}
