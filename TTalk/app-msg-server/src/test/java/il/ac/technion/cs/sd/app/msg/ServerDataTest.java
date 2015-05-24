package il.ac.technion.cs.sd.app.msg;

import static org.junit.Assert.*;

import java.util.Arrays;

import il.ac.technion.cs.sd.app.msg.exchange.ConnectRequest;
import il.ac.technion.cs.sd.app.msg.exchange.Exchange;
import il.ac.technion.cs.sd.app.msg.exchange.FriendRequest;

import org.junit.Test;

// TODO split the test to functions.

public class ServerDataTest {
	
	private ServerData data = new ServerData();

	@Test
	public void testConnectDisconnect() {
		assertFalse(data.isConnected("Haim"));
		assertFalse(data.isConnected("Moshe"));
		
		data.connect("Haim");
		assertTrue(data.isConnected("Haim"));
		assertFalse(data.isConnected("Moshe"));
		
		data.connect("Moshe");
		assertTrue(data.isConnected("Haim"));
		assertTrue(data.isConnected("Moshe"));
		
		data.disconnect("Haim");
		assertFalse(data.isConnected("Haim"));
		assertTrue(data.isConnected("Moshe"));
		
		data.disconnect("Moshe");
		assertFalse(data.isConnected("Haim"));
		assertFalse(data.isConnected("Moshe"));
	}

	@Test
	public void testFriendship() {
		assertFalse(data.areFriends("a", "b"));
		assertFalse(data.areFriends("b", "a"));
		
		data.addFriendship("a", "b");
		assertTrue(data.areFriends("a", "b"));
		assertTrue(data.areFriends("b", "a"));
		
		assertFalse(data.areFriends("a", "c"));
		assertFalse(data.areFriends("c", "a"));
		
		data.addFriendship("c", "a");
		assertTrue(data.areFriends("a", "c"));
		assertTrue(data.areFriends("c", "a"));
		assertFalse(data.areFriends("b", "c"));
		assertFalse(data.areFriends("c", "b"));
	}

	@Test
	public void testPendingClientMessage() {
		Exchange msg1 = new ConnectRequest();
		Exchange msg2 = new FriendRequest(new FriendInvitation("Danny", "Yossi"));
		
		assertTrue(data.getAndClearPendingClientMessages("Danny").isEmpty());
		
		data.addPendingClientMessage("Danny", msg1);
		assertTrue(data.getAndClearPendingClientMessages("Yossi").isEmpty());
		assertEquals(data.getAndClearPendingClientMessages("Danny"), Arrays.asList(msg1));
		assertTrue(data.getAndClearPendingClientMessages("Danny").isEmpty());
		
		data.addPendingClientMessage("Danny", msg1);
		data.addPendingClientMessage("Danny", msg2);
		assertTrue(data.getAndClearPendingClientMessages("Yossi").isEmpty());
		assertEquals(data.getAndClearPendingClientMessages("Danny"), Arrays.asList(msg1, msg2));
		
		assertTrue(data.getAndClearPendingClientMessages("Danny").isEmpty());
	}
}
