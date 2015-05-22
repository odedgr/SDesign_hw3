package il.ac.technion.cs.sd.msg;

import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

public class Dispatcher extends Thread {
	private final BlockingQueue<Envelope> queue;
	private final Consumer<String> consumer;
	private boolean isActive = true;
	
	public Dispatcher(BlockingQueue<Envelope> q, Consumer<String> c) {
		this.queue = q;
		this.consumer = c;
	}
	
	@Override
	public void run() {

		while (isActive && !this.queue.isEmpty()) {
			
			try {
				String msg = this.queue.take().payload;
				this.consumer.accept(msg);
			} catch (InterruptedException e) {
				System.out.println("dispatcher thread interrupted while trying to take message from queue. ignoring it.");
			}
		}

	}
	
	public void stopMe() {
		this.isActive = false;
	}

}
