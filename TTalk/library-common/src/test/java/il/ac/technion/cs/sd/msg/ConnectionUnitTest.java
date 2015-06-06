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
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		// Create a messenger that sometimes returns an ack.
		messenger = Mockito.mock(Messenger.class);
		Mockito.doAnswer(invocation -> {
			if (((String)invocation.getArguments()[1]).equals("")) {
				// Never return acks to acks.
				return null;
			}
			
			if (Math.random() < 0.3) {
				Thread.sleep(Connection.ACK_TIMEOUT_IN_MILLISECONDS / 2);
				consumer.accept("");
			}
			return null;
		}).when(messenger).send(Mockito.anyString(), Mockito.any());
		
		codec = new XStreamCodec<Envelope<String>>();
		
		// Create a factory that extracts the consumer and passes the mock messenger.
		MessengerFactory factory = Mockito.mock(MessengerFactory.class);
		Mockito.doAnswer(invocation -> {
			consumer = (Consumer<String>) invocation.getArguments()[1];
			return messenger;
		}).when(factory).start(Mockito.eq(clientAddress), Mockito.any());
		
		connection = new Connection<String>(clientAddress, codec, factory);
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
	public void testMyAddress() {
		assertEquals(clientAddress, connection.myAddress());
	}

	@Test
	public void testSend() throws MessengerException, InterruptedException {
		connection.send("aFriend", "Yoyoyoyoyo");
		// It may take time until the message is actually invoked.
		Thread.sleep(10);
		Mockito.verify(messenger, Mockito.atLeastOnce()).send("aFriend", codec.encode(Envelope.<String>wrap(connection.myAddress(), "aFriend", "Yoyoyoyoyo")));
	}
	
	@Test(timeout=1000)
	public void verifyAllMessagesAreSentBeforeConnectionStops() throws MessengerException, InterruptedException {
		connection.send("aFriend", "Yoyoyoyoyo");
		connection.stop();
		Mockito.verify(messenger, Mockito.atLeastOnce()).send("aFriend", codec.encode(Envelope.<String>wrap(connection.myAddress(), "aFriend", "Yoyoyoyoyo")));
	}

	@Test
	public void testReceive() throws MessengerException, InterruptedException {
		String message = "Howdy 1!";
		Envelope<String> env = Envelope.<String>wrap(connection.myAddress(), "addr", message);
		sendToConnection(env);
		
		Thread.sleep(50L); // give chance to handle incoming message
		
		// Verify this string was accepted by the connection
		Envelope<String> received = receivedEnvelopes.take(); 
				assertTrue(receivedEnvelopes.isEmpty());
		assertEquals(env.from, received.from);
		assertEquals(env.content, received.content);
	}
	
	@Test (timeout=1000)
	public void verifyAllMessagesAreReceivedBeforeConnectionStops() throws MessengerException, InterruptedException {
		String message = "Howdy 2!";
		Envelope<String> env = Envelope.<String>wrap(connection.myAddress(), "addr", message);
		sendToConnection(env);
		connection.stop();
		
		Thread.sleep(50L); // give chance to handle incoming message
		
		// Verify this string was accepted by the connection
		Envelope<String> received = receivedEnvelopes.take(); 
				assertTrue(receivedEnvelopes.isEmpty());
		assertEquals(env.from, received.from);
		assertEquals(env.content, received.content);
	}
}
