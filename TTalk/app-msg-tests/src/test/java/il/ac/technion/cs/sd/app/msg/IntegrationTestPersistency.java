package il.ac.technion.cs.sd.app.msg;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IntegrationTestPersistency {
	
	static class FriendshipReply {
		public final String name;
		public final boolean reply;
		
		FriendshipReply(String name, boolean reply) {
			this.name = name;
			this.reply = reply;
		}
		
		@Override
		public boolean equals(Object obj) {
			FriendshipReply fr = (FriendshipReply)obj;
			return fr.name.equals(this.name) && fr.reply == this.reply; 
		}
	}

	private ServerMailApplication server = new ServerMailApplication("Server");
	
	private Map<String, BlockingQueue<InstantMessage>> messages = new HashMap<String, BlockingQueue<InstantMessage>>();
	private Map<String, BlockingQueue<FriendshipReply>> replies = new HashMap<String, BlockingQueue<FriendshipReply>>();
	
	private ClientMsgApplication buildClient(String login) {
		ClientMsgApplication $ = new ClientMsgApplication(server.getAddress(), login);
		messages.put(login, new LinkedBlockingQueue<InstantMessage>());
		replies.put(login, new LinkedBlockingQueue<FriendshipReply>());
		
		return $;
		
	}
	
	private void loginClient(ClientMsgApplication client, String name) {
		client.login(x -> messages.get(name).add(x), s -> true,
				(x, y) -> replies.get(name).add(new FriendshipReply(x, y)));
	}
	
	private void restartServer() throws InterruptedException {
		server.stop();
		server = new ServerMailApplication("Server");
		server.start();
	}
	
	private void cleanServer() {
		server.stop();
		server.clean();
		server.start();
	}
	
	@Before
	public void setp() {
		server.clean();
		server.start(); // non-blocking
	}
	
	@After
	public void teardown() {
		server.stop();
		server.clean();
	}

	@Test
	public void messageArriveAfterLogin() throws InterruptedException {
		ClientMsgApplication client1 = buildClient("Alice");
		ClientMsgApplication client2 = buildClient("Bob");
		
		loginClient(client1, "Alice");
		client1.sendMessage("Bob", "Hi!");
		client1.sendMessage("Bob", "How are you?");
		restartServer();
		// No messages arrived to bob before he logged in.
		assertTrue(messages.get("Bob").isEmpty());
		
		restartServer();
		loginClient(client2, "Bob");
		// Now all bob's messages arrive.
		assertEquals(new InstantMessage("Alice", "Bob", "Hi!"), messages.get("Bob").take());
		assertEquals(new InstantMessage("Alice", "Bob", "How are you?"), messages.get("Bob").take());
		
		client1.stop();
		client2.stop();
	}
	
	@Test
	public void friendRequestsArriveAfterLogin() throws InterruptedException {
		ClientMsgApplication client1 = buildClient("Alice");
		ClientMsgApplication client2 = buildClient("Bob");
		
		loginClient(client1, "Alice");
		client1.requestFriendship("Bob");
		restartServer();
		
		// They are not friends yet, so isOnline returns an empty optional.
		loginClient(client1, "Alice");
		assertFalse(client1.isOnline("Bob").isPresent());
		restartServer();
		
		loginClient(client1, "Alice");
		loginClient(client2, "Bob");
		// Now when bob logged in, then the friend request arrived and processed,
		// so they are friends and both online, so isOnline returns true. 
		assertTrue(client1.isOnline("Bob").isPresent());
		assertTrue(client1.isOnline("Bob").get());
		
		client1.stop();
		client2.stop();
	}
	
	@Test
	public void friendResponseArriveAfterLogin() throws InterruptedException {
		ClientMsgApplication client1 = buildClient("Alice");
		ClientMsgApplication client2 = buildClient("Bob");
		
		restartServer();
		
		// Alice sends a friend request and logs out before bob logged in.
		loginClient(client1, "Alice");
		client1.requestFriendship("Bob");
		// Alice logs in and out several times, no response from bob.
		assertTrue(replies.get("Alice").isEmpty());
		client1.logout();
		assertTrue(replies.get("Alice").isEmpty());
		loginClient(client1, "Alice");
		assertTrue(replies.get("Alice").isEmpty());
		client1.logout();
		assertTrue(replies.get("Alice").isEmpty());
		
		restartServer();
		
		loginClient(client2, "Bob");
		// Still no response, because alice did not log in to get the reply (which waits at the server at this point).
		assertTrue(replies.get("Alice").isEmpty());
		restartServer();
		
		loginClient(client1, "Alice");
		restartServer();
		assertEquals(new FriendshipReply("Bob", true), replies.get("Alice").take());
		
		restartServer();
		
		client1.stop();		
		client2.stop();
	}
	
	@Test
	public void pendingMessagesCleared() throws InterruptedException {
		ClientMsgApplication client1 = buildClient("Alice");
		ClientMsgApplication client2 = buildClient("Bob");
		loginClient(client1, "Alice");
		client1.sendMessage("Bob", "Hi!");
		client1.sendMessage("Bob", "How are you?");
		// No messages arrived to bob before he logged in.
		assertTrue(messages.get("Bob").isEmpty());
		
		cleanServer();

		loginClient(client2, "Bob");
		// Still no messages for bob since data was cleared.
		assertTrue(messages.get("Bob").isEmpty());
		client1.stop();
		client2.stop();
	}
	
	@Test
	public void friendshipDataCleared() {
		ClientMsgApplication client1 = buildClient("Alice");
		ClientMsgApplication client2 = buildClient("Bob");
		
		loginClient(client1, "Alice");
		loginClient(client2, "Bob");
		client1.requestFriendship("Bob");
		
		assertTrue(client1.isOnline("Bob").get());
		
		cleanServer();
		loginClient(client1, "Alice");
		loginClient(client2, "Bob");
		
		assertFalse(client1.isOnline("Bob").isPresent());
		
		client1.stop();		
		client2.stop();
	}
}
