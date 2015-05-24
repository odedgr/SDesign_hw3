package il.ac.technion.cs.sd.app.msg;

import static org.junit.Assert.assertEquals;

import java.util.Optional;
import java.util.concurrent.*;

import org.junit.*;

public class TMsgTest {
	private ServerMailApplication			server				= new ServerMailApplication("Server");
	// all listened to incoming messages will be written here
	// a blocking queue is used to overcome threading issues
	private BlockingQueue<Boolean>			friendshipReplies	= new LinkedBlockingQueue<>();
	private BlockingQueue<InstantMessage>	messages			= new LinkedBlockingQueue<>();
	
	private ClientMsgApplication buildClient(String login) {
		return new ClientMsgApplication(server.getAddress(), login);
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
	public void basicTest() throws Exception {
		ClientMsgApplication gal = buildClient("Gal");
		gal.login(x -> {}, x -> true, (x, y) -> friendshipReplies.add(y));
		assertEquals(Optional.empty(), gal.isOnline("Itay")); // Itay isn't a friend
		gal.sendMessage("Itay", "Hi");
		ClientMsgApplication itay = buildClient("Itay");
		itay.login(x -> messages.add(x), x -> true, (x, y) -> {});
		assertEquals(messages.take(), new InstantMessage("Gal", "Itay", "Hi")); // Itay received the message as soon as he logged in
		gal.requestFriendship("Itay");
		assertEquals(true, friendshipReplies.take()); // itay auto replies yes
		assertEquals(Optional.of(true), gal.isOnline("Itay")); // itay is a friend and is online
		itay.logout();
		assertEquals(Optional.of(false), gal.isOnline("Itay")); // itay is a friend and is offline
		gal.logout();
	}
}
