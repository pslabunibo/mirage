package unused;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCPClient {

	private String host;
	private int port;

	private Socket socket;
	private volatile AtomicBoolean isFinished = new AtomicBoolean();

	public TCPClient(final String host, final int port) {
		this.host = host;
		this.port = port;
	}

	public void connect() throws UnknownHostException, IOException {
		
		socket = new Socket(host, port);
		
		final Thread inThread = new Thread() {
			@Override
			public void run() {
				// Use a Scanner to read from the remote server

				Scanner in = null;
				try {
					in = new Scanner(socket.getInputStream());
					String line = in.nextLine();
					while (!isFinished.get()) {
						System.out.println(line);
						line = in.nextLine();
					}
				} catch (Exception e) {
//					e.printStackTrace();
				} finally {
					if (in != null) {
						in.close();
					}
				}
			};
		};
		
		inThread.start();
	}

	public void disconnect() {
		try {
			if(socket != null) {
				socket.close();
			}
		} catch (IOException e) {
		}
	}

	public boolean isConnected() {
		if(socket != null) {
			return socket.isConnected() && !socket.isClosed();
		}
		
		return false;
		
	}

	public synchronized void send(String message) {
		try {
			socket.getOutputStream().write(message.getBytes());
			socket.getOutputStream().flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}