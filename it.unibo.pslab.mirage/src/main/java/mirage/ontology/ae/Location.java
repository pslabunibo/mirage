package mirage.ontology.ae;

import mirage.ontology.interfaces.JsonFormattable;

public abstract class Location implements JsonFormattable {
	
	protected LocationType type;
	
	public Location(LocationType type) {
		this.type = type;
	}
	
	public LocationType type() {
		return type;
	}
	
	@Override
	public String toString() {
		return "location";
	}
	
	public enum LocationType {
		CARTESIAN("3D/cartesian-location"),
		GPS("gps-location");
		
		private String rep;
		
		LocationType(String rep) {
			this.rep = rep;
		}
		
		@Override
		public String toString() {
			return rep;
		}
	}
}
