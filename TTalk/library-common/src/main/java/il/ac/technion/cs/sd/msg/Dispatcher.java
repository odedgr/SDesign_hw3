package il.ac.technion.cs.sd.msg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Handle messages in FIFO order, possibly from multiple producers at once, with a user-defined action for each message.
 * Messages can be added to the dispatcher via {@link #enqueue} by multiple threads, in a non-blocking fashion.
 * Actual handling will is performed by FIFO (serial), one message at a time.
 *
 * @param <Message> Type of "jobs" to be enqueued for handling.
 */
public class Dispatcher<Message> extends Thread {
	
	// INSTANCE VARIABLES
	private final BlockingQueue<Envelope<Message>> queue;
	private Consumer<Envelope<Message>> consumer; // not final - set using setHandler. must be set before start(), startMe()
	private Object pauseFlag = new Object(); // used as a monitor lock for synching
	
	// STATE FLAGS
	private boolean started = false; 	// set to 'true' upon calling startMe()
	private boolean isPaused = false;	// set to 'true' upon calling pause(), to 'false' upon calling unpause()
	private boolean killed = false; 	// only set to 'true' after started and was killed
	private boolean ignoreIncoming = false;
	
	
	/**
	 * Create a Dispatcher, for handling each message in the supplied queue, by order of insertion.
	 * 
	 * @param q - Queue that holds incoming messages for handling.
	 */
	public Dispatcher(BlockingQueue<Envelope<Message>> q) {
		if (null == q) {
			throw new IllegalArgumentException("queue cannot be null");
		}
		
		this.queue = q;
	}

	
	/**
	 * Create a Dispatcher, for handling each message by order of addition.
	 */
	public Dispatcher() {
		this(new LinkedBlockingQueue<Envelope<Message>>());
	}
	

	/**
	 * Starts this dispatcher operation. <br><br>
	 * <p>
	 * <b>Remarks:</b> 
	 * <br>(1) <i>DO NOT</i> call the {@link #start} method directly, but this one instead.
	 * <br>(2) before calling this method, a handler must be set for this dispatcher, via calling {@link #setHandler(Consumer)}
	 * <br>(3) Repeated calls to this method while dispatcher is active are ignored. Calling it after dispatcher has been killed would cause a
	 * {@link RuntimeException}.
	 * </p>
	 */
	public synchronized void startMe() {
		if (this.killed) {
			throw new RuntimeException("Nope. Dispatcher cannot be restarted after it was killed.");
		}
		
		if (null == this.consumer) {
			throw new RuntimeException("Cannot start dispatcher before setting its handler");
		}
		
		if (this.started) return; // ignore repetitive calls
		
		this.started = true;
		this.start();
	}
	
	
	@Override
	public void run() {
		if (!this.started) {
			throw new RuntimeException("started dispatcher thread wrong. did you call start() instead of startMe()?");
		}
		
		while (started) { // pausing is handled internally
			try {
				if (this.isPaused) { // when pause() is called, dispatcher will wait until unpause() is called, notifying
					synchronized (this.pauseFlag) {
						this.pauseFlag.wait();
					}
				}
				
				synchronized (this.queue) { // sync for completing handling when dispatcher is stopped
					tryToHandleFromQueue();
				}
			} catch (InterruptedException e) {
				if (this.started) { // interrupted NOT from kill()
					System.out.println("dispatcher thread unintentionally interrupted.");
				}
				else { // interrupted from a call to kill()
					this.killed = true;
					flushQueue(); // no need to explicitly block enqueuing, done within enqueue when killed
					this.ignoreIncoming = false; // set flag back, to invoke exceptions when trying to enqueue a killed dispatcher
				}
			}
		}
	}


	private void tryToHandleFromQueue() throws InterruptedException {
		Envelope<Message> env = this.queue.poll(50L, TimeUnit.MILLISECONDS); // don't use take(), to allow for killing
		if (null != env) {
			this.consumer.accept(env);
		}
	}

	/**
	 * Cleanly stops the dispatcher, allowing for a single Message to complete its handling.
	 */
	public void kill() {
		this.started = false;
		
		synchronized (this.queue) { // allow for in-handling message to complete
			this.interrupt(); 
		}
		
		this.ignoreIncoming = true;
	}
	
	
	/**
	 * Pause handling messages by this dispatcher. <br>
	 * <br>
	 * Messages are not lost, and any handling that has started will be complete.<br>
	 * Also, new messages <i>can</i> be enqueued while dispatcher is paused.
	 */
	public void pause() {
		if (!this.started) {
			throw new RuntimeException("can only pause a started dispatcher");
		}
		
		this.isPaused = true; // don't care if already paused
	}
	
	
	/**
	 * Resume handling messages by this dispatcher.
	 */
	public void unpause() {
		if (!this.started) { // catches cases where dispatcher was already stopped as well
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
		
		if (this.ignoreIncoming) {
			return;
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
	
	
	/**
	 * Check if this dispatcher is active (i.e: has been started). A dispatcher is considered active if it was started,
	 * and might, or might not be currently handling messages (e.g: paused or not).
	 * 
	 * @see isPaused
	 * 
	 * @return 'true' if the dispatcher is currently active, 'false' otherwise.
	 */
	public boolean isActive() {
		return this.started;
	}
	
	
	/**
	 * Check if this dispatcher is paused.
	 * 
	 * @return 'true' if the dispatcher is currently paused, 'false' otherwise.
	 */
	public boolean isPaused() {
		return this.isPaused;
	}
	

	/**
	 * Handle all remaining messages in queue upon killing the dispathcer.
	 */
	private void flushQueue() {
		if (!this.killed) {
			throw new RuntimeException("cannot flush queue before killing the dispatcher");
		}
		
		while (!this.queue.isEmpty()) {
			try {
				tryToHandleFromQueue();
			} catch (InterruptedException e) {
				System.out.println("interrupted while flushing queue. Stopped flushing.");
			}
		}
	}

	
	/**
	 * Set this dispatcher handler, to be used for handling each message enqueued for this dispatcher.
	 * 
	 * @param handler User-defined operation to be applied for each message this dispatcher gets.
	 */
	public void setHandler(Consumer<Envelope<Message>> handler) {
		if (this.killed) {
			throw new RuntimeException("cannot set a handler for a killed dispatcher");
		}
		
		if (this.started && !this.isPaused) {
			throw new RuntimeException("cannot change handler 'on the fly'. must pause it first");
		}
		
		if (null == handler) {
			throw new IllegalArgumentException("handler cannot be null");
		}
		
		this.consumer = handler;
	}
	
}
