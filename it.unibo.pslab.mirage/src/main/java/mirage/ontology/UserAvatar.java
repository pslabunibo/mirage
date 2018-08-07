package mirage.ontology;


import io.vertx.core.json.JsonObject;
import mirage.ontology.user.Gaze3D;

public class UserAvatar extends AE {
	
	private Gaze3D gaze;
	
	public Gaze3D gaze() {
		return this.gaze;
	}
	
	public final void gaze(Gaze3D gaze) {
		this.gaze = gaze;
		
		notifyObservers("gaze", gaze.toString());
	}

	@Override
	public JsonObject getJSONRepresentation() {
		JsonObject rep = super.getJSONRepresentation();
		rep.put("gaze", gaze.getJSONRepresentation());
		
		return rep;
	}
	
}
