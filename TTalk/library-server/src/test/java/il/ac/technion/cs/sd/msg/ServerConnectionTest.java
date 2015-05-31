package il.ac.technion.cs.sd.msg;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class ServerConnectionTest {

	private Map<String, ServerConnection<?>> connections = new HashMap<String, ServerConnection<?>>();
	private BiConsumer<String, Object> shouldntHandleObject = (addr, msg) -> System.out.println("should not have been called");
	private Consumer<String> noConsumer = (x -> x = null) ;
	private final Collection<Messenger>	messengers = new ArrayList<>();
	
	// MESSENGER GENERATORS
	private Messenger startAndAddToList() throws Exception {
		return startAndAddToList(messengers.size() + "", x -> {
			/*System.out.println("[messenger consumer] got \"" + x + "\"")*/
			assertTrue(true);
		});
	}
	
	private Messenger startAndAddToList(String address) throws Exception {
		return startAndAddToList(address, noConsumer);
	}
	
	private Messenger startAndAddToList(String address, Consumer<String> c) throws Exception {
		Messenger $ = null;
		
		for(int i = 0; null == $ && i < 10; ++i) {
			try {
				$ = new MessengerFactory().start(address + "_m", c);
			} catch (MessengerException e) {
				address = address + i;
			}
		}
		
		messengers.add($);
		return $;
	}
	
	// CONNECTION GENERATORS
	private <T> ServerConnection<T> buildConnection(String address, Codec<Envelope<T>> codec) {
		ServerConnection<T> conn = null;
		
		for(int i = 0; null == conn && i < 10; ++i) {
			try {
				conn = new ServerConnection<>(address + i, codec);
			} catch (Exception e) {
				address = address + i;
			}
		}
		
		
		connections.put(address, conn);
		return conn;
	}
	
	private <T> ServerConnection<T> buildConnection(String address) {
		return buildConnection(address, new XStreamCodec<>());
	}
	
	private <T> ServerConnection<T> buildConnection(Codec<Envelope<T>> codec) {
		String num = getFreeNum();
		
		return buildConnection("server_" + num, codec);
	}
	
	private <T> ServerConnection<T> buildConnection() {
		String num = getFreeNum();
		
		return buildConnection("server_" + num);
	}

	private String getFreeNum() {
		String num = Integer.toString(this.connections.size());
		
		while (this.connections.containsKey("server_" + num)) {
			System.out.println("address \"" + num + "\" is taken...");
			num = Integer.toString(1 + Integer.parseInt(num));
		}
		return num;
	}
	
	private String getXmlEnvelope(String address, String content) {
		Envelope<String> dummyEnvelope = Envelope.wrap(address, content);
		return new XStreamCodec<>().encode(dummyEnvelope);
	}
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		for (ServerConnection<?> sc : this.connections.values()) {
			try {
				sc.kill();
			} catch (RuntimeException e) {
				// do nothing
			}
		}
		
		this.connections.clear();
		
		for (Messenger m: messengers) {
			try {
				m.kill();
			} catch (Exception e) {/* do nothing */}
			
		}
	}

	/////////////////////////////////////////////////////////////
	////////// LET THE TESTS BEGIN //////////////////////////////
	/////////////////////////////////////////////////////////////
	
	
	@Test (expected=IllegalArgumentException.class)
	public void cantCreateConnectionWithNullAddress() {
		new ServerConnection<>(null);
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
		ServerConnection<Object> sc = buildConnection();
		sc.start(shouldntHandleObject);
		sc.kill();
		sc.send("whoever", "should not be sent");
	}
	
	@Test (expected=RuntimeException.class)
	public void cantSendBeforeStartingConnection() {
		ServerConnection<String> sc = buildConnection();
		sc.send("whoever", "should not be sent");
	}
	
	@Test (expected=RuntimeException.class)
	public void cantSendEmptyMessage() {
		ServerConnection<Object> sc = buildConnection();
		sc.start(shouldntHandleObject);
		sc.send("whoever", "");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void cantSendNullMessage() {
		ServerConnection<Object> sc = buildConnection();
		sc.start(shouldntHandleObject);
		sc.send("whoever", null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void cantSendToNullAddress() {
		ServerConnection<Object> sc = buildConnection();
		sc.start(shouldntHandleObject);
		sc.send(null, "should not be sent");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void cantSendToEmptyAddress() {
		ServerConnection<Object> sc = buildConnection();
		sc.start(shouldntHandleObject);
		sc.send("", "should not be sent");
	}
	
	@Test
	public void killingTwiceDoesNothing() {
		ServerConnection<Object> sc = buildConnection();
		sc.start(shouldntHandleObject);
		sc.kill();
		sc.kill();
	}

	@Test (expected=IllegalArgumentException.class)
	public void cantStartWithNullConsumer() {
		ServerConnection<Object> sc = buildConnection();
		sc.start(null);		
	}
	
	@Test (expected=RuntimeException.class)
	public void cantRestartAfterKill() {
		ServerConnection<Object> sc = buildConnection();
		sc.start(shouldntHandleObject);
		sc.kill();
		sc.start(shouldntHandleObject);
	}
	
	@Test (timeout=10000L)
	public void restartChangesHandler() throws Exception { // TODO debug
		int iterations = 50;
		long sleeptime = 5L;
		
		List<String> first = new ArrayList<String>();
		List<String> second = new ArrayList<String>();
		
		Messenger m = startAndAddToList("m");
		String xml = getXmlEnvelope(m.getAddress(), "hi");
		ServerConnection<Object> sc = buildConnection();

		sc.start((addr, x) -> first.add((String) x));
		for (int i = 0; i < iterations; ++i) {
			Thread.sleep(sleeptime);
			m.send(sc.myAddress(), xml);
		}
		
		sc.stop();
		sc.start((addr, x) -> second.add((String) x));
		
		for (int i = 0; i < iterations; ++i) {
			Thread.sleep(sleeptime);
			m.send(sc.myAddress(), xml);
		}
		
		assertTrue(!first.isEmpty());
		assertTrue(!second.isEmpty());
	}
	
	@Test
	public void repeatedStartDoesNothing() throws Exception {
		int iterations = 20;
		long sleeptime = 5L;
		
		List<String> first = new ArrayList<String>();
		List<String> second = new ArrayList<String>();
		
		Messenger m = startAndAddToList();
		ServerConnection<String> sc = buildConnection();
		sc.start((addr, x) -> first.add(x));
		sc.start((addr, x) -> second.add(x));
		
		String xml = getXmlEnvelope(m.getAddress(), "hi");
		
		for (int i = 0; i < iterations; ++i) {
			Thread.sleep(sleeptime);
			m.send(sc.myAddress(), xml);
		}
		
		assertTrue(second.isEmpty());
	}
	
	@Test (timeout=10000L)
	public void handlerIsExecuted() throws Exception {
		int iterations = 100;
		Queue<String> list = new LinkedBlockingQueue<String>();
		Messenger m = startAndAddToList();
		String xml = getXmlEnvelope(m.getAddress(), "hi");
		ServerConnection<String> sc = buildConnection();
		
		sc.start((addr, x) -> list.add(x));
		for (int i = 0; i < iterations; ++i) { // send a lot to make sure something was received
			Thread.sleep(10L);
//			System.out.println("[tester] sending " + i);
			m.send(sc.myAddress(), xml);
		}
		
		assertTrue(!list.isEmpty());
		System.out.println("loss percentage: " + 100.0 * (1.0 - 1.0 * list.size() / iterations));
	}
	
}


	// TODO simple object is reconstructed after sending
	// TODO complex object is reconstructed after sending

