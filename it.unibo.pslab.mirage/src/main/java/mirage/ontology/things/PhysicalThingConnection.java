package mirage.ontology.things;

import java.io.IOException;
import java.net.Socket;

import io.vertx.core.json.JsonObject;
import mirage.ontology.AE;

public class PhysicalThingConnection {
	
	private Socket socket;

	private String id;
	private PhysicalThingInterface pt;

	private String address;
	private int port;
	
	private volatile boolean stop;
	
	public PhysicalThingConnection(PhysicalThingInterface pt, String id, String address, int port) {
		this.pt = pt;
		this.id = id;
		this.address = address;
		this.port = port;
		
		this.stop = false;
		
		new Thread(() -> {
			connect();
		}).start();
	}
	
	private void connect() {
			tryConnect();
			
			JsonObject message = new JsonObject()
					.put("message", "connection-request")
					.put("content", new JsonObject()
							.put("aeId", ((AE)pt).id()));
			
			try {
				socket.getOutputStream().write(message.encode().getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			pt.onConnected(id);
			stop = false;
			startListening();
	}
	
	private void tryConnect() {
		try {
			socket = new Socket(address, port);
		} catch (IOException e) {
			pt.onDisconnected(id);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			
			tryConnect();
		}
	}
	
	boolean wanttoreconnect = false;
	
	private void startListening() {
		while(!stop) {
			try {
				byte[] buffer = new byte[1024];
				
				int bytesReaded = socket.getInputStream().read(buffer);
				
				if(bytesReaded == -1) {
					stop = true;
					wanttoreconnect = true;
				} else {
					
					pt.onMessageReceived(id, new JsonObject().put("message", new String(buffer).substring(0, bytesReaded)));
				}
				
			} catch (IOException e) {
			}
		}
		
		if(wanttoreconnect) {
			connect();
			wanttoreconnect = false;
		}
	}
	
	public synchronized void sendMessage(String message) {
		try {
			socket.getOutputStream().write(message.getBytes());
			socket.getOutputStream().flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void terminate() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
