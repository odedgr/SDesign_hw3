package il.ac.technion.cs.sd.msg;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConnectionTest {

	private Codec<Envelope<String>> defaultCodec = new XStreamCodec<>();
	static final String DUMMY_CONN_ADDRESS = "dummy10";
	
	private List<Connection<?>> connections = new ArrayList<Connection<?>>();
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		for (Connection<?> c : connections) {
			if (c.isAlive()) {
				c.kill();
			}
		}
		connections.clear();
	}

	public <T> Connection<T> buildConnection(String address, Consumer<Envelope<T>> consumer) {
		Connection<T> conn = new Connection<T>(DUMMY_CONN_ADDRESS, new XStreamCodec<>(), consumer);
		connections.add(conn);
		conn.start();
		return conn;
	}
	
	public <T> Connection<T> buildConnection(String address) {
		return buildConnection(address, x -> stubHandle((Envelope<T>)x));
	}
	
	public void stubHandle(Envelope<? extends Object> env) {
		
	}
	
	// can't create connection with null address
	@Test (expected=IllegalArgumentException.class)
	public void cantCreateConnectionWithNullAddress() {
		new Connection<String>(null, defaultCodec, x -> stubHandle(x));
 	}
	
	// can't create connection with empty address
	@Test (expected=IllegalArgumentException.class)
	public void cantCreateConnectionWithEmptyAddress() {
		new Connection<String>("", defaultCodec, x -> stubHandle(x));
 	}
	
	// can't create connection with null codec
	@Test (expected=IllegalArgumentException.class)
	public void cantCreateConnectionWithNullCodec() {
		new Connection<String>("", defaultCodec, x -> stubHandle(x));
 	}
	
	// can't send after killing connection
	@Test (expected=RuntimeException.class)
	public void cantSendAfterKillingConnection() throws InterruptedException {
		Connection<String> conn = buildConnection(DUMMY_CONN_ADDRESS);
		Thread.sleep(10L);
		conn.kill();
		conn.send("dummy2", "never sent");
 	}
	
	// can't send before staring connection
	@Test (expected=RuntimeException.class)
	public void cantSendBeforeStartingConnection() throws InterruptedException {
		Connection<String> conn = new Connection<String>(DUMMY_CONN_ADDRESS, new XStreamCodec<>(), x -> stubHandle(x)); 
		Thread.sleep(10L);
		conn.send("dummy2", "never sent");
 	}
	
	// re-send until receiving an ACK
	@Test
	public void resendMessageUntilAckIsReceived() throws MessengerException, InterruptedException {
		BlockingQueue<String> messengerReceivedMessages = new LinkedBlockingQueue<String>();
		Connection<String> conn = buildConnection(DUMMY_CONN_ADDRESS, x -> {
				System.out.println("conn got message");
			});
		
		final Messenger m = new MessengerFactory().start("m", x -> {
//				System.out.println("m got message");	
				messengerReceivedMessages.offer(x);
			});
		
		conn.send("m", "hello");
		Thread.sleep(conn.getAckTimeout() * 3); // plenty of time to send more than a single message
		
		m.send(DUMMY_CONN_ADDRESS, ""); // send ACK to stop re-sending
		Thread.sleep(10L);
		int amountSentFromConn = messengerReceivedMessages.size(); 
		assertTrue("Connection should have re-sent at least once", amountSentFromConn > 1);
		
		Thread.sleep(conn.getAckTimeout() * 2); // give time to re-send more (shouldn't happen)
		assertEquals("should have received no more messages after the ACK", amountSentFromConn, messengerReceivedMessages.size());
		
		m.kill();
 	}
	
	// TODO identify a received ACK
	// TODO ACK is sent after a non-empty message is received
	// TODO ACK is NOT sent in response to receiving an ACK
	// TODO simple object is reconstructed after sending
	// TODO complex object is reconstructed after sending
	// TODO P2MP connection - messages are sent to correct addresses
	// TODO pausing & resuming & starting & killing
	
}
