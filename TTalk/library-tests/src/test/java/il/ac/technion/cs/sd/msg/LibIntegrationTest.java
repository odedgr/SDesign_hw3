package il.ac.technion.cs.sd.msg;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import il.ac.technion.cs.sd.msg.ClientConnection;
import il.ac.technion.cs.sd.msg.ServerConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class LibIntegrationTest {
	
	private static final String serverAddress = "serv";
	private ServerConnection<String> server = new ServerConnection<String>(serverAddress);
	private BlockingQueue<Envelope<String>> serverMessages = new LinkedBlockingQueue<Envelope<String>>();  // Reuse envelope to store sender and message.
	
	private List<ClientConnection<String>> clients = new ArrayList<ClientConnection<String>>();
	private Map<String, BlockingQueue<String>> clientMessages = new HashMap<String, BlockingQueue<String>>();
	
	@Before
	public void setUp() throws Exception {
		server.start((from, msg) -> serverMessages.add(Envelope.wrap(from, "to", msg)));
	}
	
	private ClientConnection<String> buildClient(String name) {
		ClientConnection<String> client = new ClientConnection<String>(serverAddress, name);
		clients.add(client);
		clientMessages.put(name, new LinkedBlockingQueue<String>());
		
		client.start(msg -> clientMessages.get(name).add(msg));
		return client;
	}

	@After
	public void tearDown() throws Exception {
		server.kill();
		clients.stream().forEach(client->client.kill());
	}

	@Test
	public void ServerToClientReceivedInOrder() throws InterruptedException {
		ClientConnection<String> client1 = buildClient("client1");
		ClientConnection<String> client2 = buildClient("client2");
		server.send("client1", "one");
		server.send("client2", "two");
		server.send("client1", "three");
		
		assertEquals("two", clientMessages.get("client2").take());
		assertEquals("one", clientMessages.get("client1").take());
		assertEquals("three", clientMessages.get("client1").take());
	}
	
	@Test
	public void ClientToServerReceivedInOrder() throws InterruptedException {
		ClientConnection<String> client1 = buildClient("client1");
		ClientConnection<String> client2 = buildClient("client2");
		
		client1.send("one");
		client1.send("two");
		client2.send("three");
		client1.send("four");
		client2.send("five");
		
		{
			Envelope<String> env = serverMessages.take();
			assertEquals("one", env.content);
			assertEquals("client1", env.from);
		}
		
		{
			Envelope<String> env = serverMessages.take();
			assertEquals("two", env.content);
			assertEquals("client1", env.from);
		}
		{
			Envelope<String> env = serverMessages.take();
			assertEquals("three", env.content);
			assertEquals("client2", env.from);
		}
		{
			Envelope<String> env = serverMessages.take();
			assertEquals("four", env.content);
			assertEquals("client1", env.from);
		}
		{
			Envelope<String> env = serverMessages.take();
			assertEquals("five", env.content);
			assertEquals("client2", env.from);
		}
	}
	

}
