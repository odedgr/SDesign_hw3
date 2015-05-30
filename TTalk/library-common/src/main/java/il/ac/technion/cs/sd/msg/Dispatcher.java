package il.ac.technion.cs.sd.msg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Dispatcher<Message> extends Thread {
	private final BlockingQueue<Envelope<Message>> queue;
	private Consumer<Envelope<Message>> consumer; // not final - is set upon each call to unpause()
	private boolean isAlive = false;
	private boolean isPaused = false;
	private boolean killed = false; // only set to 'true' after started and was killed
	private Object pauseFlag = new Object();
	
	// TODO update documentation
	/**
	 * Create a Dispatcher, for handling each message in the supplied queue, by order of insertion.
	 * 
	 * @param q - Queue that holds incoming messages for handling.
	 * @param c - Consumer representing callback for handling massages.
	 */
	public Dispatcher(BlockingQueue<Envelope<Message>> q) {
		if (null == q) {
			throw new IllegalArgumentException("queue cannot be null");
		}
		
		this.queue = q;
	}

	/**
	 * Create a Dispatcher, for handling each message by order of addition.
	 * 
	 * @param c - Consumer representing callback for handling massages.
	 */
	public Dispatcher() {
		this(new LinkedBlockingQueue<Envelope<Message>>());
	}
	
	// TODO document
	public synchronized void startMe() {
		if (null == this.consumer) {
			throw new RuntimeException("cannot start dispatcher before setting its handler");
		}
		
		this.isAlive = true;
		this.start();
	}
	
	@Override
	public void run() {
		while (isAlive) {
			
			try {
				if (this.isPaused) { // when pause() is called, dispatcher will wait until unpause() is called, notifying
					synchronized (this.pauseFlag) {
						this.pauseFlag.wait();
					}
				}
				
				synchronized (this.queue) { // sync for completing handling when dispatcher is stopped
					Envelope<Message> env = this.queue.poll(50L, TimeUnit.MILLISECONDS); // don't use take(), to allow for killing
					if (null != env) {
//						System.out.println("Handling " + env);
						this.consumer.accept(env);
					}
				}
			} catch (InterruptedException e) {
				if (this.isAlive) {
					System.out.println("dispatcher thread unintentionally interrupted.");
				}
			}
		}
	}

	/**
	 * Cleanly stops the dispatcher, allowing for a single Message to complete its handling.
	 */
	public void kill() {
		this.isAlive = false;
		
		synchronized (this.queue) { // allow for in-handling message to complete
			this.interrupt(); 
		}
		
		this.killed = true;
	}
	
	// TODO document
	public void pause() {
		if (!this.isAlive) {
			throw new RuntimeException("can only pause a started dispatcher");
		}
		
		this.isPaused = true; // don't care if already paused
	}
	
	// TODO document
	public void unpause() {
		if (!this.isAlive) { // catches cases where dispatcher was already stopped as well
			throw new RuntimeException("can only unpause a live (already started, not yet killed) dispatcher");
		}
		
		this.isPaused = false;
		
		synchronized (this.pauseFlag) {
			this.pauseFlag.notify();
		}
		
	}

	/**
	 * Add a message for the dispatcher to handle.
	 * 
	 * @param env - Envelope containing the message to handle.
	 * @throws InterruptedException
	 */
	public void enqueue(Envelope<Message> env) throws InterruptedException { // TODO consider returning boolean instead of throwing exception
		if (null == env) {
			throw new IllegalArgumentException("cannot add null envelope to dispatcher's queue");
		}
		
		if (this.killed) {
			throw new RuntimeException("cannot enqueue after dispatcher was killed");
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
	
	public boolean isActive() {
		return this.isAlive;
	}
	
	public void waitUntilEmpty() {
		while (!this.queue.isEmpty()) {
			try {
				Thread.sleep(5L);
			} catch (InterruptedException e) {
				System.out.println("interrupted while waiting for dispatcher queue to empty");
			}
		}
	}

	public void setHandler(Consumer<Envelope<Message>> handler) {
		if (this.killed) {
			throw new RuntimeException("cannot set a handler for a killed dispatcher");
		}
		
		if (this.isAlive && !this.isPaused) {
			throw new RuntimeException("cannot change handler 'on the fly'. must pause it first");
		}
		// TODO Auto-generated method stub
		
	}
	
}
