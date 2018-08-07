package mirage.heb;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.SocketChannel;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;

import mirage.heb.events.HebOutEvent;

public class HologramEngine {
	private SocketChannel channel;
	
	private volatile BlockingQueue<HebOutEvent> outboxEventsQueue;
	
	public HologramEngine(final SocketChannel channel, final BlockingQueue<HebOutEvent> pastEvents) {
		this.channel = channel;
		this.outboxEventsQueue = new ArrayBlockingQueue<>(pastEvents.size());
		
		initChannel();
		
		pastEvents.stream()
			.forEach(event -> outboxEventsQueue.add(event));
		
		log("Hologram engine created with " + outboxEventsQueue.size() + " of " + pastEvents.size() + " past elements!");
	}
	
	private void initChannel() {
		channel.pipeline().addLast(new ChannelInHandler(), new ChannelOutHandler());
	}
	
	public void dispatch(final HebOutEvent event) {
		try {
			outboxEventsQueue.put(event);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isConnected() {
		return channel.isActive();
	}
	
	public String hostIP() {
		return channel.remoteAddress().getAddress().getHostAddress();
	}
	
	private void onMessageReceived(String event) {
		HologramEngineBridge.instance().onMessageReceived(new JsonObject()
				.put("id", channel.id().asLongText())
				.put("host", hostIP())
				.put("event", new JsonObject(event)).encodePrettily());
	}
	
	private void log(String msg){
		LoggerFactory.getLogger(HologramEngine.class).info("[" + HologramEngine.class.getSimpleName() + " @ " + hostIP() + "] " + msg);
	}
	
	/**
	 * 
	 */
	private class ChannelInHandler extends ChannelInboundHandlerAdapter {
		
		private EventsDispatcher eventDispatcher;
		
		@Override
		public void channelActive(ChannelHandlerContext ctx) {
			eventDispatcher = new EventsDispatcher(outboxEventsQueue);
			new Thread(eventDispatcher).start();
			log("EventDispatcher Started & Channel Active.");
		}
		
		@Override
		public void channelInactive(ChannelHandlerContext ctx) {
			eventDispatcher.terminate();
			log("EventDispatcher Terminated & Channel Inactive.");
		}
		
	    @Override
	    public void channelRead(ChannelHandlerContext ctx, Object msg) {	    	
	    	String message = ((ByteBuf) msg).toString(io.netty.util.CharsetUtil.US_ASCII);
	    	onMessageReceived(message);
	    }
	    
	    @Override
	    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
	        log(cause.getMessage());
	        ctx.close();
	    }
	}
	
	/**
	 * 
	 */
	private class ChannelOutHandler extends ChannelOutboundHandlerAdapter {
		
		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			String message = msg.toString();
			
			ctx.write(Unpooled.buffer()
					.writeInt(message.length())
					.writeBytes(message.getBytes()), promise);
		}
		
		@Override
		public void flush(ChannelHandlerContext ctx) throws Exception {
			//Not used!
			ctx.flush();
		}
	}

	/**
	 * 
	 */
	private class EventsDispatcher implements Runnable{
		
		private volatile boolean stop = false;
		private final BlockingQueue<HebOutEvent> queue;
		
		public EventsDispatcher(BlockingQueue<HebOutEvent> queue) {
			this.queue = queue;
		}
		
		@Override
		public void run() {
			log("EventDispatcher running!");
			while(!stop) {
				try {
					log("EventDispatcher waiting for a message to be dispatched...");
					sendEvent(queue.take());
					log("event sended...");
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		private void sendEvent(HebOutEvent event) {
			JsonObject msgObj = new JsonObject()
					.put("message", event.type().description())
					.put("content", event.content());
			
			channel.writeAndFlush(msgObj.encode());
			
			log("Event Sended: " + event.type().description());
		}
		
		public void terminate() {
			stop = true;
		}	
	}
}
