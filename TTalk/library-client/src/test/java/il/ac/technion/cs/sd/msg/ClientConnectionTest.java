package il.ac.technion.cs.sd.msg;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;


public class ClientConnectionTest {

	private final String clientAddress = "clientAddress";
	private final String serverAddress = "serverAddress";

	private ClientConnection<String> cc;
	private Connection<String> conn;
	private Messenger m;
	Codec<Envelope<String>> codec;
	Consumer<String> consumer;
	List<String> receivedEnvelopes = new ArrayList<String>();

	private Consumer<String> defaultConsumer = (msg) -> receivedEnvelopes.add(msg);

	private void sendToConnection(Envelope<String> toSend)
			throws InterruptedException {
		consumer.accept(codec.encode(toSend));
		Thread.sleep(2L); // give connection threads chance to do their thing
	}

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		m = Mockito.mock(Messenger.class);
		codec = new XStreamCodec<Envelope<String>>();

		// Create a factory that extracts the consumer and passes the mock
		// messenger.
		MessengerFactory factory = Mockito.mock(MessengerFactory.class);
		Mockito.doAnswer(invocation -> {
			consumer = (Consumer<String>) invocation.getArguments()[1];
			return m;
		}).when(factory).start(Mockito.eq(clientAddress), Mockito.any());

		conn = new Connection<String>(clientAddress, new XStreamCodec<Envelope<String>>(), factory);
		cc = new ClientConnection<String>(serverAddress, conn);

		receivedEnvelopes.clear();
	}

	@After
	public void tearDown() throws Exception {
		if (conn.wasStarted()) {
			cc.kill();
		}
	}

	// ///////////////////////////////////////////////////////////
	// //////// LET THE TESTS BEGIN //////////////////////////////
	// ///////////////////////////////////////////////////////////

	@Test(expected = IllegalArgumentException.class)
	public void cantCreateConnectionWithNullServer() {
		new ClientConnection<>(null, "myself");
	}

	@Test(expected = IllegalArgumentException.class)
	public void cantCreateConnectionWithNullAddress() {
		new ClientConnection<>("server", (String)null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void cantCreateConnectionWithEmptyServerAddress() {
		new ClientConnection<>("", "myself");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void cantCreateConnectionWithEmptyAddress() {
		new ClientConnection<>("server", "");
	}

	@Test(expected = IllegalArgumentException.class)
	public void cantCreateConnectionWithNullCodec() {
		new ClientConnection<>("server", "client", null); // should throw exception
	}

	@Test(expected = RuntimeException.class)
	public void cantSendAfterKillingConnection() {
		cc.start(defaultConsumer);
		cc.kill();
		cc.send("should not be sent");
	}

	@Test(expected = RuntimeException.class)
	public void cantSendBeforeStartingConnection() {
		cc.send("should not be sent");
	}

	@Test(expected = RuntimeException.class)
	public void cantSendEmptyMessage() {
		cc.start(defaultConsumer);
		cc.send("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void cantSendNullMessage() {
		cc.start(defaultConsumer);
		cc.send(null);
	}

	@Test
	public void killingTwiceDoesNothing() {
		cc.start(defaultConsumer);
		cc.kill();
		cc.kill();
	}

	@Test(expected = IllegalArgumentException.class)
	public void cantStartWithNullConsumer() {
		cc.start(null);
	}

	@Test(expected = RuntimeException.class)
	public void cantRestartAfterKill() {
		cc.start(defaultConsumer);
		cc.kill();
		cc.start(defaultConsumer);
	}

	@Test
	public void restartChangesHandler() throws Exception {
		List<String> secondReceived = new ArrayList<String>();

		cc.start(defaultConsumer);
		sendToConnection(Envelope.wrap("someone", cc.myAddress(), "first"));
		cc.stop();

		cc.start((msg) -> secondReceived.add(msg));
		sendToConnection(Envelope.wrap("none", cc.myAddress(), "second"));

		assertFalse(receivedEnvelopes.contains("second"));
		assertTrue(secondReceived.contains("second"));
	}

	@Test
	public void repeatedStartDoesNothing() throws Exception {
		List<String> secondReceived = new ArrayList<String>();

		cc.start(defaultConsumer);
		cc.start((msg) -> secondReceived.add(msg));
		sendToConnection(Envelope.wrap("someone", cc.myAddress(), "first"));

		assertTrue(receivedEnvelopes.contains("first"));
		assertFalse(secondReceived.contains("first"));
	}

	@Test
	public void handlerIsExecuted() throws Exception {
		cc.start(defaultConsumer);
		sendToConnection(Envelope.wrap("none", cc.myAddress(), "something"));
		assertTrue(!receivedEnvelopes.isEmpty());
	}

}

