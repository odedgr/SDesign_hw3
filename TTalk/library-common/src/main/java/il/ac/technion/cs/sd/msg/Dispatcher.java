package il.ac.technion.cs.sd.msg;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

/**
 * Handle messages in FIFO order, possibly from multiple producers at once, with a user-defined action for each message.
 * Messages can be added to the dispatcher via {@link #enqueue} by multiple threads, in a non-blocking fashion.
 * Actual handling will is performed by FIFO (serial), one message at a time.
 *
 * @param <T> Type of "jobs" to be enqueued for handling.
 */
public class Dispatcher<T> {
	
	// Send stop signal to the queue by adding an empty element to the queue.
	// Empty elements are not allowed otherwise.
	private final Optional<T> STOP_DISPATCHER_ELEMENT = Optional.empty();
	
	// INSTANCE VARIABLES
	private final BlockingQueue<Optional<T>> queue;
	private final Consumer<T> handler;
	private Thread thread;  // The actual asyncronous thread that dispatches.

	private Semaphore stoppingDone;
	
	/**
	 * Create a Dispatcher, for handling each message by order of addition.
	 * @param handler the function to handle queue elements.
	 */
	public Dispatcher(Consumer<T> handler) {
		if (handler == null) {
			throw new IllegalArgumentException();
		}
		this.handler = handler;
		this.queue = new LinkedBlockingQueue<Optional<T>>();
		stoppingDone = new Semaphore(0);
	}
	
	public void start() {
		if (thread != null) {
			throw new UnsupportedOperationException("Dispatcher has already started.");
		}
		thread = new Thread(() -> {
			while (true) {
				try {
					Optional<T> element = queue.take();
					if (element.equals(STOP_DISPATCHER_ELEMENT)) {
						break;
					}
					handler.accept(element.get());
				} catch (InterruptedException e) {
					// Should not be interrupted.
					throw new RuntimeException(e);
				}
			}
			stoppingDone.release();
		});
		thread.start();
	}
	
	/**
	 * Cleanly stops the dispatcher, allowing for a single Message to complete its handling.
	 */
	public void stop() {
		if (thread == null) {
			// The dispatcher has already been stopped.
			return;
		}
		// Signal that the dispatcher should be stopped by sending a notification as a queue element. 
		queue.add(STOP_DISPATCHER_ELEMENT);
		try {
			// Wait until dispatcher thread stops.
			stoppingDone.acquire();
			thread = null;
			queue.clear();
		} catch (InterruptedException e) {
			// Should not be interrupted...
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Add a message for the dispatcher to handle.
	 * 
	 * @param element - Envelope containing the message to handle.
	 * @throws InterruptedException
	 */
	public void enqueue(T element) {
		if (null == element) {
			throw new IllegalArgumentException("cannot add null elements to dispatcher's queue");
		}
		if (null == thread) {
			throw new RuntimeException("cannot enqueue - dispatcher is stopped.");
		}
		this.queue.add(Optional.of(element));
	}
}
