package mirage.ontology.aw;

import java.net.InetAddress;

import io.vertx.core.json.JsonObject;
import mirage.ontology.interfaces.JsonFormattable;

/**
 * Models the address of a remote AW, storing IP and PORT
 * @author marco
 *
 */
public class AwAddress implements JsonFormattable{
	
	InetAddress ip;
	int port;
	
	public AwAddress(InetAddress ip, int port) {
		super();
		this.ip = ip;
		this.port = port;
	}

	public InetAddress getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}

	@Override
	public JsonObject getJSONRepresentation() {
		return new JsonObject()
				.put("ip", this.ip.getHostAddress())
				.put("port", this.port);
	}
	
	
}
