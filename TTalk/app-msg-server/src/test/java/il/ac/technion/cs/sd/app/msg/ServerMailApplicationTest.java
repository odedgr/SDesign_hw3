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
import il.ac.technion.cs.sd.msg.ServerConnection;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ServerMailApplicationTest {
	
private static final String serverAddress = "ServerAddress";
	
	ServerMailApplication server;
	
	private BlockingQueue<InstantMessage> messages = new LinkedBlockingQueue<InstantMessage>();
	
	BiConsumer<String, Exchange> serverConsumer;
	ServerConnection<Exchange> connection;

	@Before
	public void setUp() throws Exception {
		connection = Mockito.mock(ServerConnection.class);
		server = ServerMailApplication.createWithMockConnection(serverAddress, connection);
		
		// Get the client's consumer sent to connection's start method.
		Mockito.doAnswer(invocation -> {
			serverConsumer = (BiConsumer<String, Exchange>) invocation.getArguments()[0];
			return null;
		}).when(connection).start(Mockito.any());
		
		server.start();
		Mockito.verify(connection).start(Mockito.any());
	}

	@After
	public void tearDown() throws Exception {
		server.stop();
		server.clean();
		Mockito.verify(connection).kill();
	}
	
	private void sendToServer(String sender, Exchange exchange) {
		serverConsumer.accept(sender, exchange);
	}

	@Test
	public void testGetAddress() {
		assertEquals(serverAddress, server.getAddress());
	}
	
	@Test
	public void imRequestPassedToOnlineTarget() {
		sendToServer("aTarget", new ConnectRequest());
		
		Exchange request = new SendInstantMessageRequest(new InstantMessage("aClient", "aTarget", "Howdy!"));
		sendToServer("aClient", request);
		Mockito.verify(connection).send("aTarget", request);
	}
	
	@Test
	public void imRequestListPassedToTargetWhenConnecting() throws InterruptedException {
		Exchange request1 = new SendInstantMessageRequest(new InstantMessage("aClient", "aTarget", "Howdy!"));
		Exchange request2 = new SendInstantMessageRequest(new InstantMessage("aClient", "aTarget", "Ho!"));
		sendToServer("aClient", request1);
		sendToServer("aClient", request2);
		Mockito.verify(connection, Mockito.never()).send(Mockito.anyString(), Mockito.any());
		sendToServer("aTarget", new ConnectRequest());
		Thread.sleep(100);
		Mockito.verify(connection).send("aTarget", new ExchangeList(Arrays.asList(request1, request2)));
	}
	
	@Test
	public void friendRequestPassed() {
		sendToServer("aClient", new ConnectRequest());
		sendToServer("aFriend", new ConnectRequest());
		Exchange request = new FriendRequest(new FriendInvitation("aClient", "aFriend"));
		sendToServer("aClient", request);
		Mockito.verify(connection).send("aFriend", request);
	}
	
	@Test
	public void friendResponsePassed() {
		sendToServer("aClient", new ConnectRequest());
		sendToServer("aFriend", new ConnectRequest());
		Exchange response = new FriendResponse(new FriendInvitation("aClient", "aFriend"), true);
		sendToServer("aFriend", response);
		Mockito.verify(connection).send("aClient", response);
	}
	
	@Test
	public void emptyIsOnlineResponseToNotFriends() {
		sendToServer("aClient", new ConnectRequest());
		sendToServer("aClient", new IsOnlineRequest("anotherClient"));
		Mockito.verify(connection).send("aClient", new IsOnlineResponse("anotherClient", Optional.empty()));
	}
	
	@Test
	public void IsOnlineResponseWhenFriends() {
		// Connect both clients.
		sendToServer("aClient", new ConnectRequest());
		sendToServer("aFriend", new ConnectRequest());
		// Make clients friends.
		Exchange response = new FriendResponse(new FriendInvitation("aClient", "aFriend"), true);
		sendToServer("aFriend", response);
		Mockito.verify(connection).send("aClient", response);
		
		// Send isOnlineRequests
		sendToServer("aClient", new IsOnlineRequest("aFriend"));
		Mockito.verify(connection).send("aClient", new IsOnlineResponse("aFriend", Optional.of(true)));
		
		sendToServer("aFriend", new DisconnectRequest());
		sendToServer("aClient", new IsOnlineRequest("aFriend"));
		Mockito.verify(connection).send("aClient", new IsOnlineResponse("aFriend", Optional.of(false)));
	}
	
	@Test
	public void testClientQueueSavedAfterStopAndStart() throws Exception {
		Exchange request1 = new SendInstantMessageRequest(new InstantMessage("aClient", "aTarget", "Howdy!"));
		Exchange request2 = new SendInstantMessageRequest(new InstantMessage("aClient", "aTarget", "Ho!"));
		sendToServer("aClient", request1);
		sendToServer("aClient", request2);
		Mockito.verify(connection, Mockito.never()).send(Mockito.anyString(), Mockito.any());
		
		server.stop();
		setUp();
		
		
		sendToServer("aTarget", new ConnectRequest());
		Thread.sleep(100);
		Mockito.verify(connection).send("aTarget", new ExchangeList(Arrays.asList(request1, request2)));
	}

	@Test
	public void testNoMessagesAfterClean() throws Exception {
		Exchange request1 = new SendInstantMessageRequest(new InstantMessage("aClient", "aTarget", "Howdy!"));
		Exchange request2 = new SendInstantMessageRequest(new InstantMessage("aClient", "aTarget", "Ho!"));
		sendToServer("aClient", request1);
		sendToServer("aClient", request2);
		Mockito.verify(connection, Mockito.never()).send(Mockito.anyString(), Mockito.any());
		
		server.stop();
		server.clean();
		setUp();
		
		sendToServer("aTarget", new ConnectRequest());
		Thread.sleep(100);
		Mockito.verify(connection, Mockito.never()).send(Mockito.anyString(), Mockito.any());
	}

}
