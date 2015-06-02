package il.ac.technion.cs.sd.msg;

import static org.junit.Assert.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ConnectionUnitTest {
	
	private final String clientAddress = "clientAddress";
	
	private BlockingQueue<Envelope<String>> receivedEnvelopes = new LinkedBlockingQueue<>();
	
	Connection<String> connection;
	
	Messenger messenger;
	Consumer<String> consumer;
	Codec<Envelope<String>> codec;
	
	private void sendToConnection(Envelope<String> toSend) {
		consumer.accept(codec.encode(toSend));
	}
	
	private void simluateAckToConnection() throws InterruptedException {
		Thread.sleep(connection.getAckTimeout() / 2);
		consumer.accept(""); // simulate receiving ACK
	}

	@Before
	public void setUp() throws Exception {
		messenger = Mockito.mock(Messenger.class);
		codec = new XStreamCodec<Envelope<String>>();
		
		// Create a factory that extracts the consumer and passes the mock messenger.
		MessengerFactory factory = Mockito.mock(MessengerFactory.class);
		Mockito.doAnswer(invocation -> {
			consumer = (Consumer<String>) invocation.getArguments()[1];
			return messenger;
		}).when(factory).start(Mockito.eq(clientAddress), Mockito.any());
		
		connection = new Connection<String>(clientAddress, new XStreamCodec<Envelope<String>>(), factory);
		connection.start(env->receivedEnvelopes.add(env));
	}

	@After
	public void tearDown() throws Exception {
		connection.stop();
	}

	@Test
	public void testStart() {
		connection.start(env -> {});
	}

	@Test
	public void testStop() throws MessengerException {
		connection.stop();
		Mockito.verify(messenger).kill();
	}
	
	@Test
	public void testKill() throws MessengerException {
		connection.kill();
		Mockito.verify(messenger).kill();
	}

	@Test
	public void testMyAddress() {
		assertEquals(clientAddress, connection.myAddress());
	}

	@Test
	public void testIsAlive() {
		assertTrue(connection.isAlive());
		connection.stop();
		assertFalse(connection.isAlive());
		connection.start(e -> {});
		assertTrue(connection.isAlive());
		connection.kill();
		assertFalse(connection.isAlive());
	}
	
	@Test
	public void testSend() throws MessengerException, InterruptedException {
		connection.send("aFriend", "Yoyoyoyoyo");
		// It may take time until the message is actually invoked.
		Thread.sleep(10);
		Mockito.verify(messenger).send("aFriend", codec.encode(Envelope.<String>wrap("aFriend", "Yoyoyoyoyo")));
	}
	
	@Test(timeout=1000)
	public void verifyAllMessagesAreSentBeforeConnectionStops() throws MessengerException, InterruptedException {
		connection.send("aFriend", "Yoyoyoyoyo");
		simluateAckToConnection();
		connection.stop();
		Mockito.verify(messenger).send("aFriend", codec.encode(Envelope.<String>wrap("aFriend", "Yoyoyoyoyo")));
	}
	
	@Test (timeout=1000)
	public void verifyAllMessagesAreSentBeforeConnectionKilled() throws MessengerException, InterruptedException {
		connection.send("aFriend", "Yoyoyoyoyo");
		simluateAckToConnection();
		connection.kill();
		Mockito.verify(messenger).send("aFriend", codec.encode(Envelope.<String>wrap("aFriend", "Yoyoyoyoyo")));
	}

	@Test
	public void testReceive() throws MessengerException, InterruptedException {
		String message = "Howdy 1!";
		Envelope<String> env = Envelope.<String>wrap("addr", message);
		sendToConnection(env);
		
		Thread.sleep(50L); // give chance to handle incoming message
		
		// Verify this string was accepted by the connection
		Envelope<String> received = receivedEnvelopes.take(); 
				assertTrue(receivedEnvelopes.isEmpty());
		assertEquals(env.address, received.address);
		assertEquals(env.content, received.content);
	}
	
	@Test (timeout=1000)
	public void verifyAllMessagesAreReceivedBeforeConnectionStops() throws MessengerException, InterruptedException {
		String message = "Howdy 2!";
		Envelope<String> env = Envelope.<String>wrap("addr", message);
		sendToConnection(env);
		connection.stop();
		
		Thread.sleep(50L); // give chance to handle incoming message
		
		// Verify this string was accepted by the connection
		Envelope<String> received = receivedEnvelopes.take(); 
				assertTrue(receivedEnvelopes.isEmpty());
		assertEquals(env.address, received.address);
		assertEquals(env.content, received.content);
	}
}
