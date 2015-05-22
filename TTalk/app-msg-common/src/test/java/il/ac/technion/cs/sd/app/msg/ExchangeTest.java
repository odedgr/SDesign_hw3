package il.ac.technion.cs.sd.app.msg;

import java.util.Optional;

import il.ac.technion.cs.sd.app.msg.exchange.ConnectRequest;
import il.ac.technion.cs.sd.app.msg.exchange.DisconnectRequest;
import il.ac.technion.cs.sd.app.msg.exchange.Exchange;
import il.ac.technion.cs.sd.app.msg.exchange.FriendRequest;
import il.ac.technion.cs.sd.app.msg.exchange.FriendResponse;
import il.ac.technion.cs.sd.app.msg.exchange.IsOnlineRequest;
import il.ac.technion.cs.sd.app.msg.exchange.IsOnlineResponse;
import il.ac.technion.cs.sd.app.msg.exchange.SendInstantMessageRequest;

import org.junit.Test;
import org.mockito.Mockito;

public class ExchangeTest {

	@Test
	public void testConnectRequest() {
		Exchange request = new ConnectRequest();
		ExchangeVisitor visitor = Mockito.mock(ExchangeVisitor.class);
		request.accept(visitor);
		Mockito.verify(visitor).visit((ConnectRequest)request);
	}
	
	@Test
	public void testDisconnectRequest() {
		Exchange request = new DisconnectRequest();
		ExchangeVisitor visitor = Mockito.mock(ExchangeVisitor.class);
		request.accept(visitor);
		Mockito.verify(visitor).visit((DisconnectRequest)request);
	}
	
	@Test
	public void testSendInstantMessageRequest() {
		Exchange request = new SendInstantMessageRequest(new InstantMessage("me", "you", "whazzup?"));
		ExchangeVisitor visitor = Mockito.mock(ExchangeVisitor.class);
		request.accept(visitor);
		Mockito.verify(visitor).visit((SendInstantMessageRequest)request);
	}
	
	@Test
	public void testFriendRequest() {
		Exchange request = new FriendRequest(new FriendInvitation("me", "you"));
		ExchangeVisitor visitor = Mockito.mock(ExchangeVisitor.class);
		request.accept(visitor);
		Mockito.verify(visitor).visit((FriendRequest)request);
	}
	
	@Test
	public void testFriendResponse() {
		Exchange response = new FriendResponse(new FriendInvitation("me", "you"), true);
		ExchangeVisitor visitor = Mockito.mock(ExchangeVisitor.class);
		response.accept(visitor);
		Mockito.verify(visitor).visit((FriendResponse)response);
	}
	
	@Test
	public void testIsOnlineRequest() {
		Exchange request = new IsOnlineRequest("you");
		ExchangeVisitor visitor = Mockito.mock(ExchangeVisitor.class);
		request.accept(visitor);
		Mockito.verify(visitor).visit((IsOnlineRequest)request);
	}
	
	@Test
	public void testIsOnlineResponse() {
		Exchange response = new IsOnlineResponse("you", Optional.of(true));
		ExchangeVisitor visitor = Mockito.mock(ExchangeVisitor.class);
		response.accept(visitor);
		Mockito.verify(visitor).visit((IsOnlineResponse)response);
	}
}
