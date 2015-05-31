package il.ac.technion.cs.sd.app.msg;

import static org.junit.Assert.*;
import il.ac.technion.cs.sd.app.msg.exchange.ConnectRequest;
import il.ac.technion.cs.sd.app.msg.exchange.Exchange;
import il.ac.technion.cs.sd.app.msg.exchange.FriendRequest;
import il.ac.technion.cs.sd.app.msg.exchange.FriendResponse;
import il.ac.technion.cs.sd.app.msg.exchange.IsOnlineRequest;
import il.ac.technion.cs.sd.app.msg.exchange.IsOnlineResponse;
import il.ac.technion.cs.sd.app.msg.exchange.SendInstantMessageRequest;
import il.ac.technion.cs.sd.msg.ClientConnection;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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
	
	private static final String serverAddress = "ServerAddress";
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
	
	private void loginClient(Consumer<InstantMessage> messageConsumer,
			Function<String, Boolean> friendshipRequestHandler,
			BiConsumer<String, Boolean> friendshipReplyConsumer) {
		client.login(messageConsumer, friendshipRequestHandler, friendshipReplyConsumer);
		ArgumentCaptor<Consumer> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
		Mockito.verify(connection).start(consumerCaptor.capture());
		clientConsumer = consumerCaptor.getValue();
		Mockito.verify(connection).send(new ConnectRequest());
	}
	
	private void sendToClient(Exchange exchange) {
		clientConsumer.accept(exchange);
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
		
		sendToClient(new FriendResponse(new FriendInvitation(clientAddress, "friendlie"), true));
		sendToClient(new FriendResponse(new FriendInvitation(clientAddress, "enemie"), false));
		
		assertEquals(new FriendshipReply("friendlie", true), replies.take());
		assertEquals(new FriendshipReply("enemie", false), replies.take());
	}
	
	@Test
	public void answeringFriendRequests() {
		loginClient(im -> {}, s -> s.startsWith("A"), (x, y) -> {});

		FriendInvitation invite1 = new FriendInvitation("Moshe", clientAddress);
		FriendInvitation invite2 = new FriendInvitation("Aaron", clientAddress);

		sendToClient(new FriendRequest(invite1));
		Mockito.verify(connection).send(new FriendResponse(invite1, false));

		sendToClient(new FriendRequest(invite2));
		Mockito.verify(connection).send(new FriendResponse(invite2, true));
	}
	
	@Test
	public void isOnlineReturned() throws InterruptedException {
		loginClient(im -> {}, s -> true, (x, y) -> {});

		Thread t = new Thread(() -> {
			assertEquals(Optional.of(true), client.isOnline("Someone"));
		});
		t.start();
		Thread.sleep(100);
		
		// Verify that a request was sent.
		Mockito.verify(connection).send(new IsOnlineRequest("Someone"));
		// Send response to client.
		sendToClient(new IsOnlineResponse("Someone", Optional.of(false)));
	}
}
