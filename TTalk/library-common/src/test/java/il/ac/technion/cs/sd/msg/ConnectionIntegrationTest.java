package il.ac.technion.cs.sd.msg;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConnectionIntegrationTest {
	
	Set<Connection<String>> connections = new HashSet<Connection<String>>();
	Map<String, BlockingQueue<Envelope<String>>> received = new HashMap<String, BlockingQueue<Envelope<String>>>();
	
	private Connection<String> buildConnection(String address) {
		Connection<String> $ = new Connection<String>(address);
		connections.add($);
		received.put(address, new LinkedBlockingQueue<Envelope<String>>());
		
		$.start(env -> received.get(address).add(env));
		
		return $;
	}
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		for (Connection<String> c : connections) {
			c.stop();
		}
	}
	
	@Test
	public void testOneMessage() throws InterruptedException {
		Connection<String> c1 = buildConnection("c1");
		Connection<String> c2 = buildConnection("c2");
		
		c1.send("c2", "hello");
		assertEquals(Envelope.<String>wrap("c1", "c2", "hello"), received.get("c2").take());
	}

	@Test
	public void testMultipleMessages() throws InterruptedException {
		Connection<String> c1 = buildConnection("c1");
		Connection<String> c2 = buildConnection("c2");
		
		c1.send("c2", "hello");
		c1.send("c2", "how");
		c1.send("c2", "are");
		c1.send("c2", "you");
		c1.send("c2", "today");
		assertEquals(Envelope.<String>wrap("c1", "c2", "hello"), received.get("c2").take());
		assertEquals(Envelope.<String>wrap("c1", "c2", "how"), received.get("c2").take());
		assertEquals(Envelope.<String>wrap("c1", "c2", "are"), received.get("c2").take());
		assertEquals(Envelope.<String>wrap("c1", "c2", "you"), received.get("c2").take());
		assertEquals(Envelope.<String>wrap("c1", "c2", "today"), received.get("c2").take());
	}
	
	@Test
	public void testMutualMessages() throws InterruptedException {
		Connection<String> c1 = buildConnection("c1");
		Connection<String> c2 = buildConnection("c2");
		
		c1.send("c2", "hello");
		c2.send("c1", "hi there");
		
		assertEquals(Envelope.<String>wrap("c1", "c2", "hello"), received.get("c2").take());
		assertEquals(Envelope.<String>wrap("c2", "c1", "hi there"), received.get("c1").take());
	}
	
	@Test
	public void testMultipleMutualMessages() throws InterruptedException {
		Connection<String> c1 = buildConnection("c1");
		Connection<String> c2 = buildConnection("c2");
		
		c1.send("c2", "hello");
		c2.send("c1", "hi there");
		c1.send("c2", "what's up");
		c1.send("c2", "what's with you today?");
		c2.send("c1", "i'm fine");
		c2.send("c1", "thank you!");
		
		assertEquals(Envelope.<String>wrap("c1", "c2", "hello"), received.get("c2").take());
		assertEquals(Envelope.<String>wrap("c1", "c2", "what's up"), received.get("c2").take());
		assertEquals(Envelope.<String>wrap("c1", "c2", "what's with you today?"), received.get("c2").take());
		
		assertEquals(Envelope.<String>wrap("c2", "c1", "hi there"), received.get("c1").take());
		assertEquals(Envelope.<String>wrap("c2", "c1", "i'm fine"), received.get("c1").take());
		assertEquals(Envelope.<String>wrap("c2", "c1", "thank you!"), received.get("c1").take());
	}
	
	@Test
	public void testSelfMessage() throws InterruptedException {
		Connection<String> c = buildConnection("c");
		c.send("c", "hello myself!");
		c.send("c", "aren't you handsome!");
		c.send("c", "no, you are more handsome!");
		c.send("c", "oh, stop it you");
		c.send("c", "no, you stop it!");
		
		assertEquals(Envelope.<String>wrap("c", "c", "hello myself!"), received.get("c").take());
		assertEquals(Envelope.<String>wrap("c", "c", "aren't you handsome!"), received.get("c").take());
		assertEquals(Envelope.<String>wrap("c", "c", "no, you are more handsome!"), received.get("c").take());
		assertEquals(Envelope.<String>wrap("c", "c", "oh, stop it you"), received.get("c").take());
		assertEquals(Envelope.<String>wrap("c", "c", "no, you stop it!"), received.get("c").take());
	}
	
	@Test
	public void stressTest() throws InterruptedException {
		Connection<String> c1 = buildConnection("c1");
		Connection<String> c2 = buildConnection("c2");
		
		for (int i=0; i < 300; i++) {
			c1.send("c2", ""+i);
		}
		
		for (int i=0; i < 300; i++) {
			assertEquals(Envelope.<String>wrap("c1", "c2", ""+i), received.get("c2").take());
		}
	}
}
