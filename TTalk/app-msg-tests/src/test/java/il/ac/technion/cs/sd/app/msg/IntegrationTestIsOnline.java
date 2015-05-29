package il.ac.technion.cs.sd.app.msg;

import static org.junit.Assert.*;
import il.ac.technion.cs.sd.app.msg.IntegrationTestFriendRequests.FriendshipReply;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IntegrationTestIsOnline {
	
	private ServerMailApplication server = new ServerMailApplication("Server");
	private Map<String, BlockingQueue<FriendshipReply>> replies = new HashMap<String, BlockingQueue<FriendshipReply>>();
	
	private ClientMsgApplication buildClient(String login, Function<String, Boolean> friendRequestHandler) {
		ClientMsgApplication $ = new ClientMsgApplication(server.getAddress(), login);
		replies.put(login, new LinkedBlockingQueue<FriendshipReply>());
		
		$.login(x -> {}, friendRequestHandler, (x, y) -> replies.get(login).add(new FriendshipReply(x, y)));
		
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
	public void basicOnline() {
		ClientMsgApplication client1 = buildClient("Dudu", s -> true);
		ClientMsgApplication client2 = buildClient("Gulu", s -> true);
		
		client1.requestFriendship("Gulu");
		
		assertTrue(client1.isOnline("Gulu").get());		
		assertTrue(client2.isOnline("Dudu").get());
		
		client1.stop();
		client2.stop();
	}
	
	@Test
	public void basicNotOnline() {
		ClientMsgApplication client1 = buildClient("Dudu", s -> true);
		ClientMsgApplication client2 = buildClient("Gulu", s -> true);
		client1.requestFriendship("Gulu");
		
		client2.logout();
		
		assertFalse(client1.isOnline("Gulu").get());
		
		client1.stop();
	}
	
	@Test
	public void emptyResponseWhenNotFriends() {
		ClientMsgApplication client1 = buildClient("Dudu", s -> true);
		ClientMsgApplication client2 = buildClient("Gulu", s -> true);
		
		assertFalse(client1.isOnline("Gulu").isPresent());
		
		client1.stop();
		client2.stop();
	}
	
	@Test
	public void emptyResponseWhenFriendRequestDeclined() {
		ClientMsgApplication client1 = buildClient("Dudu", s -> true);
		ClientMsgApplication client2 = buildClient("Gulu", s -> false);
		
		client1.requestFriendship("Gulu");
		assertFalse(client1.isOnline("Gulu").isPresent());
		
		client1.requestFriendship("Dudu");
		assertTrue(client1.isOnline("Gulu").get());
		
		client1.stop();
		client2.stop();
	}
	
	@Test
	public void logoutAndThenLogin() {
		ClientMsgApplication client1 = buildClient("Dudu", s -> true);
		ClientMsgApplication client2 = buildClient("Gulu", s -> true);
		client1.requestFriendship("Gulu");
		
		assertTrue(client1.isOnline("Gulu").get());
		client2.logout();
		assertFalse(client1.isOnline("Gulu").get());
		client2.login(x -> {}, s -> true, (x,y) -> {});
		assertTrue(client1.isOnline("Gulu").get());
		
		client1.stop();
		client2.stop();
	}

}
