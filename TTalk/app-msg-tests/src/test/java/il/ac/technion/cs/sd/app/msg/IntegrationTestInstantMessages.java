package il.ac.technion.cs.sd.app.msg;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class IntegrationTestInstantMessages {
	private ServerMailApplication			server				= new ServerMailApplication("Server");
	
	private Map<String, BlockingQueue<InstantMessage>> messages = new HashMap<String, BlockingQueue<InstantMessage>>();
	
	private ClientMsgApplication buildClient(String login) {
		ClientMsgApplication $ = new ClientMsgApplication(server.getAddress(), login);
		messages.put(login, new LinkedBlockingQueue<InstantMessage>());
		
		$.login(x -> messages.get(login).add(x), s -> false, (x, y) -> {});
		
		return $;
		
	}
	
	@Before
	public void setp() {
		server.start(); // non-blocking
	}
	
	@After
	public void teardown() {
		server.stop();
		server.clean();
	}
	
	@Test
	public void testSimpleIM() throws Exception {
		
		ClientMsgApplication client1 = buildClient("Moshe");
		ClientMsgApplication client2 = buildClient("Haim");
		
		client1.sendMessage("Haim", "Hi!");
		assertEquals(new InstantMessage("Moshe", "Haim", "Hi!"), messages.get("Haim").take());
		
		client1.stop();
		client2.stop();
	}

	@Test
	public void messagesToSelf() throws Exception {
		
		ClientMsgApplication client1 = buildClient("Moshe");
		
		client1.sendMessage("Moshe", "Yoohoo");
		client1.sendMessage("Moshe", "Yahoo");
		client1.sendMessage("Moshe", "Yeehoo");
		assertEquals(new InstantMessage("Moshe", "Moshe", "Yoohoo"), messages.get("Moshe").take());
		assertEquals(new InstantMessage("Moshe", "Moshe", "Yahoo"), messages.get("Moshe").take());
		assertEquals(new InstantMessage("Moshe", "Moshe", "Yeehoo"), messages.get("Moshe").take());
		
		client1.stop();
	}
	
	@Test
	public void testMessageToRightClient() throws Exception {
		
		ClientMsgApplication client1 = buildClient("Moshe");
		ClientMsgApplication client2 = buildClient("Haim");
		ClientMsgApplication client3 = buildClient("Noa");
		
		client1.sendMessage("Haim", "Hi!");
		client1.sendMessage("Noa", "Wazzup?");
		
		assertEquals(new InstantMessage("Moshe", "Haim", "Hi!"), messages.get("Haim").take());
		assertEquals(new InstantMessage("Moshe", "Noa", "Wazzup?"), messages.get("Noa").take());
		
		client1.stop();
		client2.stop();
		client3.stop();
	}
	
	@Test
	public void MessagesArriveInOrder() throws Exception {
		
		ClientMsgApplication client1 = buildClient("Moshe");
		ClientMsgApplication client2 = buildClient("Haim");
	
		client1.sendMessage("Haim", "Hi!");
		client1.sendMessage("Haim", "Nice weather, isn't it?");
		
		assertEquals(new InstantMessage("Moshe", "Haim", "Hi!"), messages.get("Haim").take());
		assertEquals(new InstantMessage("Moshe", "Haim", "Nice weather, isn't it?"), messages.get("Haim").take());
		
		client1.stop();
		client2.stop();
	}
	
	@Test
	public void MutualMessagesArrive() throws Exception {
		
		ClientMsgApplication client1 = buildClient("Moshe");
		ClientMsgApplication client2 = buildClient("Haim");
	
		client1.sendMessage("Haim", "Hi!");
		client2.sendMessage("Moshe", "Go away.");
		
		assertEquals(new InstantMessage("Moshe", "Haim", "Hi!"), messages.get("Haim").take());
		assertEquals(new InstantMessage("Haim", "Moshe", "Go away."),  messages.get("Moshe").take());
		
		client1.stop();
		client2.stop();
	}
}
