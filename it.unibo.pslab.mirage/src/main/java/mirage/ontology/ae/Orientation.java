package mirage.ontology.ae;

import mirage.ontology.interfaces.JsonFormattable;

public abstract class Orientation implements JsonFormattable {
	
	protected Type type;
	
	public Orientation(Type type) {
		this.type = type;
	}
	
	public Type type() {
		return type;
	}
	
	@Override
	public String toString() {
		return "orientation";
	}
	
	public enum Type {
		ANGULAR("3D/angular-orientation");
		
		private String rep;
		
		Type(String rep) {
			this.rep = rep;
		}
		
		@Override
		public String toString() {
			return rep;
		}
	}
}
