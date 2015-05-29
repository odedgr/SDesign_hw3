package il.ac.technion.cs.sd.app.msg;

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

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The client side of the TMail application. Allows sending and getting messages to and from other clients using a server. <br>
 * You should implement all the methods in this class
 */
public class ClientMsgApplication {
	
	final String username;
	
	ClientConnection<Exchange> connection;
	
	BlockingQueue<Optional<Boolean>> isOnlineResponseQueue;
	
	Consumer<InstantMessage> messageConsumer;
	Function<String, Boolean> friendshipRequestHandler;
	BiConsumer<String, Boolean> friendshipReplyConsumer;
	
	/**
	 * Creates a new application, tied to a single user
	 * 
	 * @param serverAddress The address of the server to connect to for sending and receiving messages
	 * @param username The username that will be sending and accepting the messages using this object
	 */
	public ClientMsgApplication(String serverAddress, String username) {
		if (serverAddress == null || serverAddress.isEmpty() || username == null || username.isEmpty()) {
			throw new IllegalArgumentException();
		}
		
		this.username = username;
		this.connection = new ClientConnection<Exchange>(serverAddress, username, message -> message.accept(new Visitor()));
	}
	
	/**
	 * Creates a client mail application that uses a given mock connection.
	 * Used for testing purposes.
	 * @param username The username that will be sending and accepting the messages using this object.
	 * @param connection The mock connection to use.
	 * @return
	 */
	static ClientMsgApplication createWithMockConnection(String username, ClientConnection<Exchange> connection) {
		if (username == null || username.isEmpty() || connection == null) {
			throw new IllegalArgumentException();
		}
		return new ClientMsgApplication(username, connection);
	}
	
	/**
	 * Creates a new client application that uses given connection.
	 * @param username
	 * @param connection
	 */
	private ClientMsgApplication(String username, ClientConnection<Exchange> connection) {
		if (username == null || username.isEmpty() || connection == null) {
			throw new IllegalArgumentException();
		}
		this.username = username;
		this.connection = connection;
	}
	
	/**
	 * Logs the client to the server. Any incoming messages from the server will be routed to the provided consumer. If
	 * the client missed any messages while he was offline, all of these will be first routed to client in the order
	 * that they were sent
	 * 
	 * @param messageConsumer The consumer to handle all incoming messages
	 * @param friendshipRequestHandler The callback to handle all incoming friend requests. It accepts the user requesting
	 *        the friendship as input and outputs the reply.
	 * @param friendshipReplyConsumer The consumer to handle all friend requests replies (replies to outgoing
	 *        friends requests). The consumer accepts the user requested and his reply.	
	 */
	public void login(Consumer<InstantMessage> messageConsumer,
			Function<String, Boolean> friendshipRequestHandler,
			BiConsumer<String, Boolean> friendshipReplyConsumer) {
		
		connection.start();
		
		connection.send(new ConnectRequest());
		this.messageConsumer = messageConsumer;
		this.friendshipRequestHandler = friendshipRequestHandler;
		this.friendshipReplyConsumer = friendshipReplyConsumer;
	}
	
	/**
	 * Logs the client out, cleaning any resources the client may be using. A logged out client cannot accept any
	 * messages. A client can login (using {@link ClientMsgApplication#login(Consumer, Function, BiConsumer)} after logging out.
	 */
	public void logout() {
		connection.send(new DisconnectRequest());
		connection.stop();
	}
	
	/**
	 * Sends a message to another user
	 * 
	 * @param target The recipient of the message
	 * @param what The message to send
	 */
	public void sendMessage(String target, String what) {
		connection.send(new SendInstantMessageRequest(new InstantMessage(username, target, what)));
	}
	
	/**
	 * Requests the friendship of another user. Friends can see each other online using
	 * {@link ClientMsgApplication#isOnline(String)}. Friend requests are handled similarly to messages. An incoming
	 * friend request is consumed by the friendRequestsConsumer. An incoming friend request <i>reply</i> is consumed by
	 * the friendRequestRepliesConsumer.
	 * 
	 * @param who The recipient of the friend request.
	 */
	public void requestFriendship(String who) {
		connection.send(new FriendRequest(new FriendInvitation(username, who)));
	}
	
	/**
	 * Checks if another user is online; the client can only ask if friends are online
	 * 
	 * @param who The person to check if he is online
	 * @return A wrapped <code>true</code> if the user is a friend and is offline; a wrapped <code>false</code> if the
	 *         user is a friend and is offline; an empty {@link Optional} if the user isn't a friend of the client
	 */
	public Optional<Boolean> isOnline(String who) {
		connection.send(new IsOnlineRequest(who));
		try {
			// Wait for a response which would arrive asynchronously.
			return isOnlineResponseQueue.take();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
    /**
     * A stopped client does not use any system resources (e.g., messengers).
     * This is mainly used to clean resource use in test cleanup code.
     * You can assume that a stopped client won't be restarted using {@link ClientMsgApplication#login(Consumer, Function, BiConsumer)}
     */
    public void stop() {
    	connection.kill();
    }
    
    class Visitor implements ExchangeVisitor {

		@Override
		public void visit(ConnectRequest request) {
			throw new UnsupportedOperationException("The client should not get ConnectRequest.");
		}

		@Override
		public void visit(DisconnectRequest request) {
			throw new UnsupportedOperationException("The client should not get DisconnectRequest.");
			
		}

		@Override
		public void visit(SendInstantMessageRequest request) {
			throw new UnsupportedOperationException("The client should not get SendInstantMessageRequest.");
		}

		@Override
		public void visit(FriendRequest request) {
			boolean answer = friendshipRequestHandler.apply(request.invitation.from);
			connection.send(new FriendResponse(request.invitation, answer));
		}

		@Override
		public void visit(FriendResponse response) {
			friendshipReplyConsumer.accept(response.invitation.to, response.isAccepted);
		}

		@Override
		public void visit(IsOnlineRequest request) {
			throw new UnsupportedOperationException("The client should not get IsOnlineRequest.");
		}

		@Override
		public void visit(IsOnlineResponse response) {
			try {
				// Answer and notify isOnline method to return an answer.
				isOnlineResponseQueue.put(response.answer);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void visit(ExchangeList exchangeList) {
			// Go over all messages in the list and visit them.
			for (Exchange exchange : exchangeList.list) {
				exchange.accept(this);
			}
		}
    	
    }
}
