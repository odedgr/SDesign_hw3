package il.ac.technion.cs.sd.msg;

import static org.junit.Assert.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DispatcherTest {
	
	private Dispatcher<String> dispatcher;
	private BlockingQueue<String> handledStrings = new LinkedBlockingQueue<String>();
	

	@Before
	public void setUp() throws Exception {
		dispatcher = new Dispatcher<String>(s -> handledStrings.add(s));
		dispatcher.start();
	}

	@After
	public void tearDown() throws Exception {
		dispatcher.stop();
	}

	@Test
	public void OneMessageArrives() throws InterruptedException {
		dispatcher.enqueue("One");
		assertEquals("One", handledStrings.take());
	}
	
	@Test
	public void MultipleMessagesArrive() throws InterruptedException {
		dispatcher.enqueue("One");
		dispatcher.enqueue("Two");
		dispatcher.enqueue("Three");
		assertEquals("One", handledStrings.take());
		assertEquals("Two", handledStrings.take());
		assertEquals("Three", handledStrings.take());
	}

	@Test
	public void testQueued() throws InterruptedException {
		// Replace dispatcher with a new dispatcher with inherent delay.
		dispatcher.stop();
		dispatcher = new Dispatcher<String>(s -> {
			try {
				Thread.sleep(100);
				handledStrings.add(s);
			} catch (Exception e) {}
		});
		dispatcher.start();
		
		dispatcher.enqueue("second");  // Because this dispatcher is delayed, 'first' should overtake 'second'.
		handledStrings.add("first");
		
		dispatcher.enqueue("third");
		dispatcher.stop();  // stop method should not return until queue is empty, i.e. third is sent.
		handledStrings.add("fourth"); // ...so fourth would not overcome third.
		
		assertEquals("first", handledStrings.take());
		assertEquals("second", handledStrings.take());
		assertEquals("third", handledStrings.take());
		assertEquals("fourth", handledStrings.take());
	}

	@Test(expected=IllegalArgumentException.class)
	public void checkExceptionWhenArgumentIsNull() {
		new Dispatcher<Integer>(null);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void cannotEnququqNullElement() throws InterruptedException {
		dispatcher.enqueue(null);
	}
	
	@Test (expected = RuntimeException.class)
	public void cannotEnqueueWhenDispatcherIsStopped() throws InterruptedException {
		dispatcher.stop();
		dispatcher.enqueue("Karamba!");
	}
}
