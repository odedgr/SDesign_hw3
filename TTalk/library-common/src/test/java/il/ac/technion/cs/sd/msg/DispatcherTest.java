package il.ac.technion.cs.sd.msg;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class DispatcherTest {

	private static final Envelope<Object> dummyEnvelope = Envelope.wrap("dummy envelope", new Object());
	List<Dispatcher<Object>> dispatchers = new ArrayList<Dispatcher<Object>>();
	Dispatcher<Object> d;
	
	@SuppressWarnings("unchecked")
	private <T> Dispatcher<T> buildDispatcher(Consumer<Envelope<T>> c) {
		Dispatcher<T> disp = new Dispatcher<T>(c);
		dispatchers.add((Dispatcher<Object>) disp);
		return disp;
	}
	
	private void stubHandler(Envelope<Object> x) {
		System.out.println("stub-handled object envelope " + x.toString());
	}
	
	public void blockHandler(Envelope<Object> env, long sleepTime) {
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			System.out.println("block handler interrupted");
		}
	}
	
	@Before
	public void setUp() throws Exception {
		d = new Dispatcher<Object>(x -> stubHandler(x));
		d.start();
	}

	@After
	public void tearDown() throws Exception {
		if (d.isActive()) {
			d.kill();
		}
		
		for (Dispatcher<Object> d : dispatchers) {
			if (d.isActive()) {
				d.kill();
			}
		}
	}

	@Test (expected = IllegalArgumentException.class)
	public void cantCreateDispatcherWithNullConsumer() {
		new Dispatcher<>(null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void cantEnqueueNullEnvelope() throws InterruptedException {
		d.enqueue(null);
	}

	@Test (expected = RuntimeException.class)
	public void cantEnqueueAfterDispatcherStopped() throws InterruptedException {
		Dispatcher<Object> disp = buildDispatcher(x -> stubHandler(x));
		disp.kill();
		disp.enqueue(dummyEnvelope);
	}

	@Test
	public void allUnhandledJobsAreReturned() throws InterruptedException {
		Dispatcher<Object> disp = buildDispatcher(x -> stubHandler(x));
		
		List<Integer> jobs = Arrays.asList(1, 2, 3, 4);
		List<Envelope<Object>> jobsInEnvelopes = new ArrayList<>();
		
		for (Integer i : jobs) {
			Envelope<Object> env = Envelope.wrap(i.toString(), i);
			jobsInEnvelopes.add(env);
			disp.enqueue(env);
		}
		
		assertEquals(jobsInEnvelopes, disp.getUnhandled());
	}
	
	@Test
	public void jobsAreHandledByOrderOfInsertion() throws InterruptedException {
		
		List<Integer> jobs = Arrays.asList(1, 2, 3, 4);
		List<Envelope<Object>> jobsInEnvelopes = new ArrayList<>();
		List<Integer> handledJobs = new ArrayList<Integer>();
		
		Dispatcher<Object> disp = buildDispatcher(x -> handledJobs.add((Integer)x.content));
		disp.start();

		for (Integer i : jobs) {
			Envelope<Object> env = Envelope.wrap(i.toString(), i);
			jobsInEnvelopes.add(env);
			disp.enqueue(env);
		}
		
		disp.waitUntilEmpty();
		
		assertEquals(jobs, handledJobs);
	}
	
	@Test
	public void onlyUnhandledJobsReturned() throws InterruptedException {
		
		final long BLOCK_TIME = 10L;
		
		List<Integer> jobs = Arrays.asList(1, 2, 3, 4);
		List<Envelope<Object>> jobsInEnvelopes = new ArrayList<>();
		List<Integer> unhandledJobs = new ArrayList<Integer>(jobs);
		
		Dispatcher<Object> disp = buildDispatcher(x -> { unhandledJobs.remove(x.content); blockHandler(x, BLOCK_TIME); });
		disp.start();

		for (Integer i : jobs) {
			Envelope<Object> env = Envelope.wrap(i.toString(), i);
			jobsInEnvelopes.add(env);
			disp.enqueue(env);
		}
		
		Thread.sleep((long)(BLOCK_TIME * 1.2)); // allow for a single job to be handled
		
		assertEquals(unhandledJobs, disp.getUnhandled().stream().map(x -> x.content).collect(Collectors.toList()));
	}
	

	static class Producer extends Thread {
		private int id;
		private Dispatcher<Object> disp;
		
		public Producer(int id, Dispatcher<Object> disp) {
			this.id = id;
			this.disp = disp;
		}
		
		@Override
		public void run() {
			for (int j = 0; j < 10; ++j) {
				try {
					this.disp.enqueue(Envelope.wrap(Integer.toString(id), j));
					if (0 == System.nanoTime() % 2) {
						Random rand = new Random();
						int randNum = rand.nextInt(5000);
						Thread.sleep(randNum / 1000, randNum % 1000);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}			
		}
		
	}
	
	@Test
	public void jobHandledInOrderMultithreadedOffline() throws InterruptedException {
		List<Integer> handledJobs = new ArrayList<Integer>();
		Dispatcher<Object> disp = buildDispatcher(x -> handledJobs.add((Integer) x.content));
		List<Thread> producers = new ArrayList<Thread>();

		for (int i = 0; i < 5; ++i) {
			producers.add(new Producer(i, disp));
		}
		
		for (Thread p : producers) { p.start(); }
		for (Thread p : producers) { p.join(); }
		
		disp.start();
		List<Integer> dispInitJobs = disp.getUnhandled()
				.stream()
				.map(x -> (Integer)x.content)
				.collect(Collectors.toList());
		
		disp.waitUntilEmpty();
		
		assertEquals(dispInitJobs, handledJobs);
	}
	
	
	// TODO pausing & resuming & starting & killing
	
}
