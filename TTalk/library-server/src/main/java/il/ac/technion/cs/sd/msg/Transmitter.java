package il.ac.technion.cs.sd.msg;

import java.util.concurrent.BlockingQueue;
import java.util.function.BiConsumer;

public class Transmitter<Message> extends Thread {
	private final BlockingQueue<Envelope<Message>> queue;
	private final BiConsumer<String, Message> consumer; // (client, payload)
	private boolean isActive = true;
	
	/**
	 * Create a Transmitter, for sending each message in the queue, by order of insertion.
	 * 
	 * @param q - Queue that holds incoming messages for handling.
	 * @param c - Consumer representing callback for handling massages.
	 */
	public Transmitter(BlockingQueue<Envelope<Message>> q, BiConsumer<String, Message> c) {
		this.queue = q;
		this.consumer = c;
	}
	
	
	@Override
	public void run() {
		while (isActive) {
			Envelope<Message> env = null;
			
			try {
				synchronized (this.queue) { // sync for completing sending when transmitter is stopped
					env = this.queue.take(); // blocking call
					this.consumer.accept(env.address, env.payload);
				}
			} catch (InterruptedException e) {
				if (this.isActive) {
					System.out.println("transmitter thread unintentionally interrupted.");
				}
			}
		}
	}

	
	public void stopMe() {
		System.out.println("stopping transmitter.");
		this.isActive = false;
		
		synchronized (this.queue) { // allow for in-motion message to complete sending
			this.interrupt(); 
		}
	}
}
