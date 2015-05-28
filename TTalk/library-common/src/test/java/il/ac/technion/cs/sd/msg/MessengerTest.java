package il.ac.technion.cs.sd.msg;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.junit.*;
import org.junit.rules.Timeout;

public class MessengerTest {
	
	// a helper method and list for creating messengers, so we can remember to kill them
	private final Collection<Messenger>	messengers			= new ArrayList<>();
	
	// all listened to incoming messages will be written here by default
	private final BlockingQueue<String>	incomingMessages	= new LinkedBlockingQueue<>();
	
	@Rule
	public Timeout						globaltime			= Timeout.seconds(5);
	
	private Messenger startAndAddToList() throws Exception {
		return startAndAddToList(messengers.size() + "", x -> incomingMessages.add(x));
	}
	
	private Messenger startAndAddToList(String address) throws Exception {
		return startAndAddToList(address, x -> incomingMessages.add(x));
	}
	
	private Messenger startAndAddToList(String address, Consumer<String> c) throws Exception {
		Messenger $ = new MessengerFactory().start(address, c);
		messengers.add($);
		return $;
	}
	
	@After
	public void teardown() throws Exception {
		// it is very important to kill all messengers,
		// to free their address and more importantly, their daemon threads
		for (Messenger m: messengers)
			try {
				m.kill();
			} catch (Exception e) {/* do nothing */}
	}
	
	@Test(expected = RuntimeException.class)
	public void shouldThrowExceptionOnNullConsumer() throws Exception {
		startAndAddToList("address", null);
	}
	
	@Test(expected = RuntimeException.class)
	public void shouldThrowAnExceptionOnNullAddress() throws Exception {
		startAndAddToList(null, x -> {});
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionOnNullRecepient() throws Exception {
		startAndAddToList().send(null, "data");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionOnNullPayload() throws Exception {
		startAndAddToList().send("him", null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowAnExceptionOnEmptyAddress() throws Exception {
		startAndAddToList("");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowAnExceptionOnWhitespaceAddress() throws Exception {
		startAndAddToList("    ");
	}
	
	@Test(expected = MessengerException.class)
	public void shouldThrowAnExceptionOnTwoIdentitcalNames() throws Exception {
		startAndAddToList("a");
		startAndAddToList("a");
	}
	
	@Test
	public void canCreateNewObjectWithSameAddressAfterKilled() throws Exception {
		startAndAddToList("a").kill();
		startAndAddToList("a");
	}
	
	@Test
	public void shouldReceiveASentMessage() throws Exception {
		Messenger m1 = startAndAddToList();
		Messenger m2 = startAndAddToList();
		m1.send(m2.getAddress(), "");
		assertEquals(incomingMessages.take(), "");
	}
	
	@Test(expected = MessengerException.class)
	public void cannotBeKilledTwice() throws Exception {
		Messenger $ = startAndAddToList();
		$.kill();
		$.kill();
	}
	
	@Test
	public void messagesAreLostAfterKilled() throws Exception {
		// empty messages cannot fail
		Messenger m1 = startAndAddToList();
		Messenger m2 = startAndAddToList();
		m1.send(m2.getAddress(), "");
		incomingMessages.clear();
		m2.kill();
		m2 = startAndAddToList();
		assertTrue(incomingMessages.isEmpty());
		m1.send(m2.getAddress(), "");
		assertEquals(incomingMessages.take(), "");
	}
	
	@Test(expected = MessengerException.class)
	public void cantSendSerializedData() throws Exception {
		Messenger m1 = startAndAddToList();
		Messenger m2 = startAndAddToList();
		List<Object> list = Arrays.asList("Hello!", "There!");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		new ObjectOutputStream(baos).writeObject(list);
		String wiseass = new String(baos.toByteArray());
		m1.send(m2.getAddress(), wiseass);
	}
	
	@Test
	public void messagesCanFailForNoReason() throws Exception {
		Messenger m1 = startAndAddToList();
		Messenger m2 = startAndAddToList();
		int num_tries = 300; // the odds of this test failing are lower than you winning the lottery
		for (int i = 0; i < num_tries; i++)
			m1.send(m2.getAddress(), "Message #" + i);
		assertTrue("better go buy that lottery ticket!", incomingMessages.size() < num_tries);
	}
	
	@Test
	public void emptyMessagesNeverFail() throws Exception {
		Messenger m1 = startAndAddToList();
		Messenger m2 = startAndAddToList();
		for (int i = 0; i < 100; i++) {
			m1.send(m2.getAddress(), "");
			incomingMessages.take();
		}
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void shouldBlockWhenNoMessage() throws Exception {
		Messenger $ = startAndAddToList();
		new Thread(() -> {
			$.waitForMessage();
			fail("Should have blocked");
		}).start();
		Thread.sleep(500);
	}
	
	@Test
	public void shouldReturnNullIfNoMessageIsReceived() throws Exception {
		Messenger $ = startAndAddToList();
		assertEquals($.getNextMessage(10), null);
	}
	
	@Test
	public void shouldGetNextMessageWhileConsuming() throws Exception {
		// the sends a message to the sleeper and waits for a reply waker then sleeps; the waker wakes the sleeper
		Messenger sleeper = new MessengerFactory().start("sleeper", (m, x) -> {
			try {
				m.send("miracle", "");
				m.getNextMessage(100);
				incomingMessages.add("Hi!");
			} catch (Exception e) {
				throw new AssertionError(e);
			}
		});
		Messenger waker = new MessengerFactory().start("miracle", (m, x) -> {
			try {
				m.send("sleeper", "");
			} catch (Exception e) {
				throw new AssertionError(e);
			}
		});
		waker.send(sleeper.getAddress(), "");
		assertEquals(incomingMessages.take(), "Hi!");
		sleeper.kill();
		waker.kill();
	}
}
