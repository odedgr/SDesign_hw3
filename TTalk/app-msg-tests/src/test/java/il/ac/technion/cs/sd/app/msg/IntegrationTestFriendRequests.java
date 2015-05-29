package il.ac.technion.cs.sd.app.msg;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IntegrationTestFriendRequests {

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
	public void simpleFriendRequest() throws Exception {
		// Kippy accepts everyone.
		ClientMsgApplication client1 = buildClient("Kippy", (s) -> true);
		// Moishe declines everyone.
		ClientMsgApplication client2 = buildClient("Moishe", (s) -> false);
		
		client1.requestFriendship("Moishe");
		assertEquals(new FriendshipReply("Moishe", false), replies.get("Kippy").take());
		
		client2.requestFriendship("Kippy");
		assertEquals(new FriendshipReply("Kippy", true), replies.get("Moishe").take());
	}
	
	@Test
	public void nameBasedFriendReply() throws Exception {
		// Mad Max only accepts happy people.
		ClientMsgApplication client1 = buildClient("MadMax", (s) -> s.startsWith("Happy"));
		
		ClientMsgApplication client2 = buildClient("HappyHarry", (s) -> false);
		ClientMsgApplication client3 = buildClient("HappyBarry", (s) -> false);
		ClientMsgApplication client4 = buildClient("SadLarry", (s) -> false);
		
		client2.requestFriendship("MadMax");
		client3.requestFriendship("MadMax");
		client4.requestFriendship("MadMax");
		
		assertEquals(new FriendshipReply("HappyHarry", true), replies.get("MadMax").take());
		assertEquals(new FriendshipReply("HappyBarry", true), replies.get("MadMax").take());
		assertEquals(new FriendshipReply("SadLarry", false), replies.get("MadMax").take());
	}

}
