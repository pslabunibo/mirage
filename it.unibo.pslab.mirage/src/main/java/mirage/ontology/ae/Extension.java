package mirage.ontology.ae;

import mirage.ontology.interfaces.JsonFormattable;

public abstract class Extension implements JsonFormattable {

protected ExtensionType type;
	
	public Extension(ExtensionType type) {
		this.type = type;
	}
	
	public ExtensionType type() {
		return type;
	}
	
	@Override
	public String toString() {
		return "extension";
	}
	
	public enum ExtensionType {
		BASIC("basic-extension"),
		SPHERIC("spheric-extension"),
		POLYGONAL("polygonal-extension");
		
		private String rep;
		
		ExtensionType(String rep) {
			this.rep = rep;
		}
		
		@Override
		public String toString() {
			return rep;
		}
	}

}
