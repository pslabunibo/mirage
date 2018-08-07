package mirage.ontology.region;

import mirage.exceptions.RSMismatchException;
import mirage.ontology.ae.Extension;
import mirage.ontology.ae.Location;
import mirage.ontology.interfaces.JsonFormattable;

public abstract class Area implements JsonFormattable{
	
	public abstract boolean isPointIncluded(Location point) throws RSMismatchException;
	
	public abstract Location getLocation();
	
	public abstract Extension getExtension();
	
}
