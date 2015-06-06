package il.ac.technion.cs.sd.app.msg;

import static org.junit.Assert.*;
import il.ac.technion.cs.sd.app.msg.exchange.ConnectRequest;
import il.ac.technion.cs.sd.app.msg.exchange.DisconnectRequest;
import il.ac.technion.cs.sd.app.msg.exchange.Exchange;
import il.ac.technion.cs.sd.app.msg.exchange.ExchangeList;
import il.ac.technion.cs.sd.app.msg.exchange.FriendRequest;
import il.ac.technion.cs.sd.app.msg.exchange.FriendResponse;
import il.ac.technion.cs.sd.app.msg.exchange.IsOnlineRequest;
import il.ac.technion.cs.sd.app.msg.exchange.IsOnlineResponse;
import il.ac.technion.cs.sd.app.msg.exchange.SendInstantMessageRequest;
import il.ac.technion.cs.sd.msg.ClientConnection;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

 public class ClientMsgApplicationTest {
	
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
	
	private static final String clientAddress = "ClientAddress";
	
	ClientMsgApplication client;
	
	private BlockingQueue<InstantMessage> messages = new LinkedBlockingQueue<InstantMessage>();
	private BlockingQueue<FriendshipReply> replies = new LinkedBlockingQueue<FriendshipReply>();
	
	
	Consumer<Exchange> clientConsumer;
	ClientConnection<Exchange> connection;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		connection = Mockito.mock(ClientConnection.class);
		client = ClientMsgApplication.createWithMockConnection(clientAddress, connection);
	}

	@After
	public void tearDown() throws Exception {
		client.stop();
	}
	
	@SuppressWarnings("unchecked")
	private void loginClient(Consumer<InstantMessage> messageConsumer,
			Function<String, Boolean> friendshipRequestHandler,
			BiConsumer<String, Boolean> friendshipReplyConsumer) {
		
		// Get the client's consumer sent to connection's start method.
		Mockito.doAnswer(invocation -> {
			clientConsumer = (Consumer<Exchange>) invocation.getArguments()[0];
			return null;
		}).when(connection).start(Mockito.any());
		
		Mockito.doAnswer(invocation -> {
			clientConsumer.accept(new ExchangeList(Collections.emptyList()));
			return null;
		}).when(connection).send(new ConnectRequest());
		client.login(messageConsumer, friendshipRequestHandler, friendshipReplyConsumer);
		
		Mockito.verify(connection).start(Mockito.any());
		Mockito.verify(connection).send(new ConnectRequest());
	}
	
	private void sendToClient(Exchange exchange) {
		clientConsumer.accept(exchange);
	}
	
	@Test
	public void connectRequestSent() throws InterruptedException {
		loginClient(im -> {}, s -> true, (x, y) -> {});
		client.logout();
		Mockito.verify(connection).send(new DisconnectRequest());
		Mockito.verify(connection).stop();
	}
	
	@Test
	public void severalConnectAndDisconnects() throws InterruptedException {
		loginClient(im -> {}, s -> true, (x, y) -> {});

		client.logout();
		Mockito.verify(connection, Mockito.atLeastOnce()).send(new DisconnectRequest());
		Mockito.verify(connection, Mockito.atLeastOnce()).stop();
		
		client.login(im -> {}, s -> true, (x, y) -> {});
		Mockito.verify(connection, Mockito.atLeast(2)).start(Mockito.any());
		Mockito.verify(connection, Mockito.atLeast(2)).send(new ConnectRequest());

		client.logout();
		Mockito.verify(connection, Mockito.atLeastOnce()).send(new DisconnectRequest());
		Mockito.verify(connection, Mockito.atLeastOnce()).stop();
	}

	@Test
	public void instantMessageReceived() throws InterruptedException {
		loginClient(im->messages.add(im) , s->true, (x,y)->{});
		InstantMessage msg1 = new InstantMessage("a", "b", "c");
		InstantMessage msg2 = new InstantMessage("d", "e", "f");
		
		sendToClient(new SendInstantMessageRequest(msg1));
		sendToClient(new SendInstantMessageRequest(msg2));
		
		assertEquals(msg1, messages.take());
		assertEquals(msg2, messages.take());
	}
	
	@Test
	public void friendReplyReceived() throws InterruptedException {
		loginClient(im -> {}, s -> true, (x, y) -> replies.add(new FriendshipReply(x, y)));
		
		sendToClient(new FriendResponse(new FriendInvitation(clientAddress, "friendlie"), Optional.of(true)));
		sendToClient(new FriendResponse(new FriendInvitation(clientAddress, "enemie"), Optional.of(false)));
		
		assertEquals(new FriendshipReply("friendlie", true), replies.take());
		assertEquals(new FriendshipReply("enemie", false), replies.take());
	}
	
	@Test
	public void answeringFriendRequests() {
		loginClient(im -> {}, s -> s.startsWith("A"), (x, y) -> {});

		FriendInvitation invite1 = new FriendInvitation("Moshe", clientAddress);
		FriendInvitation invite2 = new FriendInvitation("Aaron", clientAddress);

		sendToClient(new FriendRequest(invite1));
		Mockito.verify(connection).send(new FriendResponse(invite1, Optional.of(false)));
		sendToClient(new FriendRequest(invite2));
		Mockito.verify(connection).send(new FriendResponse(invite2, Optional.of(true)));
	}
	
	@Test
	public void isOnlineReturned() throws InterruptedException {
		loginClient(im -> {}, s -> true, (x, y) -> {});
		
		// When a request is sent via connection, send a response back to the client.
		Mockito.doAnswer(invocation -> { 
			sendToClient(new IsOnlineResponse("Someone", Optional.of(true)));
			return null;
		}).when(connection).send(new IsOnlineRequest("Someone"));
		
		
		assertEquals(Optional.of(true), client.isOnline("Someone"));
		// Verify that a request was sent.
		Mockito.verify(connection).send(new IsOnlineRequest("Someone"));
	}
}
