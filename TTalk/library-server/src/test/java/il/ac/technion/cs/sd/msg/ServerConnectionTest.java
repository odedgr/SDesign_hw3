package il.ac.technion.cs.sd.msg;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;


public class ServerConnectionTest {

	private final String serverAddress = "serverAddress";
	
	private ServerConnection<String> sc;
	private Connection<String> conn;
	private Messenger m;
	Codec<Envelope<String>> codec;
	Consumer<String> consumer;
	Map<String, String> receivedEnvelopes = new HashMap<String, String>();

	private BiConsumer<String, String> defaultBiConsumer = (addr, msg) -> receivedEnvelopes.put(addr, msg);
//	private BiConsumer<String, String> printerBiConsumer = (addr, msg) -> System.out.println("Connection got \"" + msg + "\" from " + addr);
	
	private void sendToConnection(Envelope<String> toSend) throws InterruptedException {
		consumer.accept(codec.encode(toSend));
		Thread.sleep(2L); // give connection threads chance to do their thing
	}
	
	
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		m = Mockito.mock(Messenger.class);
		codec = new XStreamCodec<Envelope<String>>();
		
		// Create a factory that extracts the consumer and passes the mock messenger.
		MessengerFactory factory = Mockito.mock(MessengerFactory.class);
		Mockito.doAnswer(invocation -> {
			consumer = (Consumer<String>) invocation.getArguments()[1];
			return m;
		}).when(factory).start(Mockito.eq(serverAddress), Mockito.any());
		
		conn = new Connection<String>(serverAddress, new XStreamCodec<Envelope<String>>(), factory);
		sc = new ServerConnection<String>(conn);
		
		receivedEnvelopes.clear();
	}

	@After
	public void tearDown() throws Exception {
		if (conn.wasStarted()) {
			sc.kill();
		}
	}

	/////////////////////////////////////////////////////////////
	////////// LET THE TESTS BEGIN //////////////////////////////
	/////////////////////////////////////////////////////////////
	
	
	@Test (expected=IllegalArgumentException.class)
	public void cantCreateConnectionWithNullAddress() {
		new ServerConnection<>((String) null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void cantCreateConnectionWithNullConnection() {
		new ServerConnection<>((Connection<?>) null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void cantCreateConnectionWithEmptyAddress() {
		new ServerConnection<>("");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void cantCreateConnectionWithNullCodec() {
		new ServerConnection<>("sc", null); // should throw exception
	}
	
	@Test (expected=RuntimeException.class)
	public void cantSendAfterKillingConnection() {
		sc.start(defaultBiConsumer);
		sc.kill();
		sc.send("whoever", "should not be sent");
	}
	
	@Test (expected=RuntimeException.class)
	public void cantSendBeforeStartingConnection() {
		sc.send("whoever", "should not be sent");
	}
	
	@Test (expected=RuntimeException.class)
	public void cantSendEmptyMessage() {
		sc.start(defaultBiConsumer);
		sc.send("whoever", "");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void cantSendNullMessage() {
		sc.start(defaultBiConsumer);
		sc.send("whoever", null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void cantSendToNullAddress() {
		sc.start(defaultBiConsumer);
		sc.send(null, "should not be sent");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void cantSendToEmptyAddress() {
		sc.start(defaultBiConsumer);
		sc.send("", "should not be sent");
	}
	
	@Test
	public void killingTwiceDoesNothing() {
		sc.start(defaultBiConsumer);
		sc.kill();
		sc.kill();
	}

	@Test (expected=IllegalArgumentException.class)
	public void cantStartWithNullConsumer() {
		sc.start(null);		
	}
	
	@Test (expected=RuntimeException.class)
	public void cantRestartAfterKill() {
		sc.start(defaultBiConsumer);
		sc.kill();
		sc.start(defaultBiConsumer);
	}
	
	@Test
	public void restartChangesHandler() throws Exception { 
		Map<String, String> secondReceived = new HashMap<String, String>();
		
		sc.start(defaultBiConsumer);
		sendToConnection(Envelope.wrap("first", "bla bla"));
		sc.stop();
		
		sc.start((addr, msg) -> secondReceived.put(addr, msg));
		sendToConnection(Envelope.wrap("second", "bla bla"));
		
		assertFalse(receivedEnvelopes.keySet().contains("second"));
		assertTrue(secondReceived.keySet().contains("second"));
	}
	
	@Test
	public void repeatedStartDoesNothing() throws Exception {
		Map<String, String> secondReceived = new HashMap<String, String>();
		
		sc.start(defaultBiConsumer);
		sc.start((addr, msg) -> secondReceived.put(addr, msg));
		sendToConnection(Envelope.wrap("first", "bla bla"));
		
		assertTrue(receivedEnvelopes.keySet().contains("first"));
		assertFalse(secondReceived.keySet().contains("first"));
	}
	
	@Test
	public void handlerIsExecuted() throws Exception {
		sc.start(defaultBiConsumer);
		sendToConnection(Envelope.wrap("none", "something"));
		assertTrue(!receivedEnvelopes.isEmpty());
	}
	
}


	// TODO simple object is reconstructed after sending
	// TODO complex object is reconstructed after sending

