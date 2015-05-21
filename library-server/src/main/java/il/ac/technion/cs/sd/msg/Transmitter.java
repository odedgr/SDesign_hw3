package il.ac.technion.cs.sd.msg;

import java.util.concurrent.BlockingQueue;
import java.util.function.BiConsumer;

public class Transmitter extends Thread {
	private final BlockingQueue<Envelope> queue;
	private final BiConsumer<String, String> consumer; // (String so, String payload)
	private boolean isActive = true;
	
	public Transmitter(BlockingQueue<Envelope> q, BiConsumer<String, String> c) {
		this.queue = q;
		this.consumer = c;
	}
	
	
	@Override
	public void run() {

while (isActive && !this.queue.isEmpty()) {
			
			try {
				Envelope env = this.queue.take(); 
				String destination = env.address;
				String msg = env.payload;
				this.consumer.accept(destination, msg);
			} catch (InterruptedException e) {
				System.out.println("transmitter thread interrupted while trying to take message from queue. ignoring it.");
			}
		}
		
	}

	
	public void stopMe() {
		this.isActive = false;
	}
}
