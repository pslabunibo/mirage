package mirage.heb;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import mirage.Environment;
import mirage.exceptions.AugmentedEntityNotFoundException;
import mirage.exceptions.HebNotActiveException;
import mirage.heb.events.HebEvent;
import mirage.heb.events.HebOutEvent;
import mirage.ontology.AE;
import mirage.utils.C;

/**
 * HEB: Hologram Engine Bridge
 */
public class HologramEngineBridge {
	
	private final Set<HologramEngine> registeredEngines;
	
	private static final HologramEngineBridge bridge = new HologramEngineBridge();
	
	private volatile boolean active = false;
	
	private volatile BlockingQueue<HebOutEvent> outboxEventsBulk;
	
	protected HologramEngineBridge() {
		registeredEngines = new HashSet<HologramEngine>();
		outboxEventsBulk = new ArrayBlockingQueue<>(C.Heb.BULK_SIZE);
	}
	
	/**
	 * 
	 * @return
	 */
	public static HologramEngineBridge instance() {
		return bridge;
	}
	
	/**
	 * Starts a TCP Server (in a dedicated thread) representing the HEB on the specified TCP port.
	 * @param port
	 */
	public static void start(final int port) {
		new Thread(bridge.new TCPServer(port)).start();
	}

	/**
	 * 
	 * @param eventType
	 * @param eventContent
	 */
	public void notifyEvent(final HebEvent eventType, final JsonObject eventContent) {
		broadcast(new HebOutEvent(eventType, eventContent));
	}
	
	private void broadcast(HebOutEvent event) {
		try {
			if(event.type().isReplaceable()) {
				
				outboxEventsBulk.stream()
				.filter(e -> e.type().isReplaceable() && e.type().equals(event.type()))
				.forEach(e -> {
					if(e.type().equals(HebEvent.UPDATE_HOLOGRAM_PROPERTY)) {
						JsonObject content = e.content();
						
						if(content.getString("entityId").equals(event.content().getString("entityId")) 
								&& content.getString("propertyId").equals(event.content().getString("propertyId"))){
							outboxEventsBulk.remove(e);
						}
					}
					
					//TODO: for other repeatable events...
				});
			}
			
			outboxEventsBulk.put(event);
			/*System.out.println("Added event to bulk: " + event.type().description() 
					+ "PROP: " + event.content().getString("propertyId") + " on " + event.content().getString("entityId")
					+ " - BULK size = " + outboxEventsBulk.size());*/	
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		registeredEngines.forEach(engine -> {
				engine.dispatch(event);
		});
	}
	
	public void onMessageReceived(String message) {
		//TODO: to be completed!
		System.out.println("Message received: " + message);
		
		/*JsonObject msgObj = new JsonObject(message);
		if(msgObj.getString("event").equals("rb\r\n")) {
			broadcast("messaggio in broadcast!");
		}*/
	}
	
	private void registerHologramEngine(HologramEngine he) throws HebNotActiveException {
		if(active) {
			registeredEngines.add(he);
		} else {
			throw new HebNotActiveException();
		}
	}
	
	private void log(String msg){
		LoggerFactory.getLogger(HologramEngineBridge.class).info("[" + HologramEngineBridge.class.getSimpleName() + "] " + msg);
	}
	
	/**
	 * 
	 */
	protected class TCPServer implements Runnable {
		private int port;
		
		public TCPServer(int port) {
			this.port = port;
		}
		
		public void run() {
	        final EventLoopGroup bossGroup = new NioEventLoopGroup();
	        final EventLoopGroup workerGroup = new NioEventLoopGroup();
	        
	        try {
	        	final ChannelFuture server = new ServerBootstrap()
	        			.group(bossGroup, workerGroup)
	   	             	.channel(NioServerSocketChannel.class)
	   	             	.childHandler(new TCPConnection())
	   	             	.option(ChannelOption.SO_BACKLOG, 128)
	   	             	.childOption(ChannelOption.SO_KEEPALIVE, true)
	   	             	.bind(port)
	   	             	.sync();
	        		            
	            log("Active.");
	            
	            active = true;
	            
	            server.channel().closeFuture().sync();
	        } catch (InterruptedException e) {
	        	log("An Error has been encountered during activation.");
				e.printStackTrace();
			} finally {
	            workerGroup.shutdownGracefully();
	            bossGroup.shutdownGracefully();
	        }
	    }
	}
	
	/**
	 * 
	 */
	protected class TCPConnection extends ChannelInitializer<SocketChannel>{
		@Override
		protected void initChannel(final SocketChannel ch) throws Exception {
			
			final BlockingQueue<HebOutEvent> queue = new ArrayBlockingQueue<>(outboxEventsBulk.size());
			outboxEventsBulk.forEach(event -> {
				if(event.type().equals(HebEvent.HOLOGRAM_CREATION)) {
					try {
						final AE entity = Environment.findAugmentedEntityById(event.content().getJsonObject("model").getString("id"));
						queue.add(new HebOutEvent(HebEvent.HOLOGRAM_CREATION, entity.getJSONRepresentation()));
					} catch (AugmentedEntityNotFoundException e) {
						queue.add(event);
					}
				} else {
					queue.add(event);
				}
			});
		
			final HologramEngine he = new HologramEngine(ch, queue);

			try {
				registerHologramEngine(he);
				log("New HologramEngine active on an host at " + he.hostIP());
			} catch (HebNotActiveException e) {
				log("Unable to register an Hologram Engine: the HEB is unavailable!");
			}
		}		
	}
}
