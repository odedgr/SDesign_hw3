package il.ac.technion.cs.sd.msg;

import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

public class Dispatcher<Message> extends Thread {
	private final BlockingQueue<Envelope<Message>> queue;
	private final Consumer<Envelope<Message>> consumer;
	private boolean isActive = true;
	
	/**
	 * Create a Dispatcher, for handling each message in the queue, by order of insertion.
	 * 
	 * @param q - Queue that holds incoming messages for handling.
	 * @param c - Consumer representing callback for handling massages.
	 */
	public Dispatcher(BlockingQueue<Envelope<Message>> q, Consumer<Envelope<Message>> c) {
		this.queue = q;
		this.consumer = c;
	}
	
	@Override
	public void run() {
		while (isActive) {
			Envelope<Message> env = null;
			
			try {
				synchronized (this.queue) { // sync for completing handling when dispatcher is stopped
					env = this.queue.take();
					this.consumer.accept(env);
				}
			} catch (InterruptedException e) {
				if (this.isActive) {
					System.out.println("dispatcher thread unintentionally interrupted.");
				}
			}
		}
	}

	/**
	 * Cleanly stops the dispatcher, allowing for a single Message to complete its handling.
	 */
	public void stopMe() {
		System.out.println("stopping dispatcher.");
		this.isActive = false;
		
		synchronized (this.queue) { // allow for in-handling message to complete
			this.interrupt(); 
		}
	}
}
