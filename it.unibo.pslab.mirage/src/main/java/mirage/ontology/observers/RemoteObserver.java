package mirage.ontology.observers;

import io.vertx.core.json.JsonObject;

public interface RemoteObserver {
		public void notify(JsonObject newStatus);
}
