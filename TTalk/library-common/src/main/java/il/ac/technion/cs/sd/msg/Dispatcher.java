package il.ac.technion.cs.sd.msg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class Dispatcher<Message> extends Thread {
	private final BlockingQueue<Envelope<Message>> queue;
	private final Consumer<Envelope<Message>> consumer;
	private boolean isActive = true;
	
	/**
	 * Create a Dispatcher, for handling each message in the supplied queue, by order of insertion.
	 * 
	 * @param q - Queue that holds incoming messages for handling.
	 * @param c - Consumer representing callback for handling massages.
	 */
	public Dispatcher(BlockingQueue<Envelope<Message>> q, Consumer<Envelope<Message>> c) {
		this.queue = q;
		this.consumer = c;
	}

	/**
	 * Create a Dispatcher, for handling each message by order of addition.
	 * 
	 * @param c - Consumer representing callback for handling massages.
	 */
	public Dispatcher(Consumer<Envelope<Message>> c) {
		this(new LinkedBlockingQueue<Envelope<Message>>(), c);
	}
	
	
	@Override
	public void run() {
		while (isActive) {
			try {
				synchronized (this.queue) { // sync for completing handling when dispatcher is stopped
					Envelope<Message> env = this.queue.take();
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

	/**
	 * Add a message for the dispatcher to handle.
	 * 
	 * @param env - Envelope containing the message to handle.
	 * @throws InterruptedException
	 */
	public void enqueue(Envelope<Message> env) throws InterruptedException {
		if (null == env) {
			throw new IllegalArgumentException("cannot add null envelope to dispatcher's queue");
		}

		this.queue.put(env);
	}
	
	/**
	 * Get a collection of all the envelopes that were added to dispatcher, but have yet to be handled by it.<br><br>
	 * 
	 * <b>Notice:</b> If called while dispatcher is active, returned collection only represents state at time of calling,
	 * and will NOT change upon enqueuing of handling messages later on.
	 * 
	 * @return A Collection of all messages enqueued to this dispatcher, but have yet to be handled.
	 */
	public Collection<Envelope<Message>> getUnhandled() {
		return new ArrayList<Envelope<Message>>(this.queue); // defensive copying
	}
}
