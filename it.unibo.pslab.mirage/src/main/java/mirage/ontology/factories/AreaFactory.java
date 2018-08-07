package mirage.ontology.factories;

import mirage.ontology.ae.Extension;
import mirage.ontology.ae.Location;
import mirage.ontology.ae.Extension.ExtensionType;
import mirage.ontology.ae.Location.LocationType;
import mirage.ontology.ae.extension.BasicExtension;
import mirage.ontology.ae.location.CartesianLocation;
import mirage.ontology.ae.location.GpsLocation;
import mirage.ontology.region.Area;
import mirage.ontology.region.CircleCartesianArea;
import mirage.ontology.region.CircleGeoArea;

public class AreaFactory {
	private static AreaFactory instance = new AreaFactory();
	
	public static AreaFactory getInstance(){
		return instance;
	}
	
	public Area generateArea(Location.LocationType locType, Extension.ExtensionType extType, Double[] values) {
		if(extType == ExtensionType.BASIC) {
			if(locType == LocationType.CARTESIAN) {
				//Nell'array vi sono in ordine: x, y, z, radius
				return new CircleCartesianArea(new CartesianLocation(values[0], values[1], values[2]), new BasicExtension(values[3]));
			} else if(locType == LocationType.GPS) {
				return new CircleGeoArea(new GpsLocation(values[0], values[1], values[2]), new BasicExtension(values[3]));
			}
		} else if(extType == ExtensionType.SPHERIC) {
			//TODO: sferico
		} else if(extType == ExtensionType.POLYGONAL) {
			//TODO: poligonale
		}
		return null;
	}
	
}
