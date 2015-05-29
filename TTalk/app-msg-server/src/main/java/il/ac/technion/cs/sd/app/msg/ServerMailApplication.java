package il.ac.technion.cs.sd.app.msg;

import java.util.List;
import java.util.Optional;

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


/**
 * The server side of the TMail application. <br>
 * This class is mainly used in our tests to start, stop, and clean the server
 */
public class ServerMailApplication {
	
	private ServerConnection<Exchange> connection;
	final private String address;
	
	private ServerData data = new ServerData();
	private DataSaver<ServerData> dataSaver;
	
    // TODO: add createWithMockConnection factory method. 
	
	/**
     * Starts a new mail server. Servers with the same name retain all their information until
     * {@link ServerMailApplication#clean()} is called.
     *
     * @param name The name of the server by which it is known.
     */
	public ServerMailApplication(String name) {
		this.address = name;
		this.data = new ServerData();
		this.dataSaver = new FileDataSaver<ServerData>("app-msg-data-" + address);
		this.connection = new ServerConnection<Exchange>(address, (sender, message) -> message.accept(new Visitor(sender)));
	}
	
	/**
	 * @return the server's address; this address will be used by clients connecting to the server
	 */
	public String getAddress() {
		return address;
	}
	
	/**
	 * Starts the server; any previously sent mails, data and indices are loaded.
	 * This should be a <b>non-blocking</b> call.
	 */
	public void start() {
		connection.start();
		loadData();
	}
	
	/**
	 * Stops the server. A stopped server can't accept messages, but doesn't delete any data (messages that weren't received).
	 */
	public void stop() {
		connection.stop();
		connection.kill();
		saveData();
	}
	
	/**
	 * Deletes <b>all</b> previously saved data. This method will be used between tests to assure that each test will
	 * run on a new, clean server. you may assume the server is stopped before this method is called.
	 */
	public void clean() {
		data = new ServerData();
		dataSaver.clean();
	}
	
	/**
	 * Store all of this server's current data in a file. 
	 */
	private void saveData() {
		dataSaver.save(data);
	}
	
	/**
	 * Load a previously stored data to the server. Returns empty serverData if there is no previously saved data.
	 */
	private void loadData() {
		Optional<ServerData> loaded_data = dataSaver.load();
		if (loaded_data.isPresent()) {
			data = loaded_data.get();
		} else {
			data = new ServerData();
		}
	}
	
	/**
	 * Send an exchange to a client if he is online.
	 * If not, adds the exchange to the client pending messages queue.
	 * @param exchange the request/response to send.
	 */
	private void sendIfOnline(String client, Exchange exchange) {
		if (data.isConnected(client)) {
			connection.send(client, exchange);
		} else {
			data.addPendingClientMessage(client, exchange);
		}
	}
	
	private class Visitor implements ExchangeVisitor {
		
		String client;
		
		Visitor(String requestingClient) {
			this.client = requestingClient;
		}

		@Override
		public void visit(ConnectRequest request) {
			data.connect(client);
			List<Exchange> pendingMessages = data.getAndClearPendingClientMessages(client);
			if (!pendingMessages.isEmpty()) {
				connection.send(client, new ExchangeList(pendingMessages));
			}
		}

		@Override
		public void visit(DisconnectRequest request) {
			data.disconnect(client);
		}

		@Override
		public void visit(SendInstantMessageRequest request) {
			// TODO: remove before submission?
			if (request.message.from != client) {
				throw new UnsupportedOperationException("A client attempts to send a message by a different name.");
			}
			
			sendIfOnline(request.message.to, request);
		}

		@Override
		public void visit(FriendRequest request) {
			// TODO: remove before submission?
			if (!request.invitation.from.equals(client)) {
				throw new UnsupportedOperationException("A client attempts to send a friend request by a different name.");
			}
			sendIfOnline(request.invitation.to, request);
		}

		@Override
		public void visit(FriendResponse response) {
			// TODO: remove before submission?
			if (response.invitation.to != client) {
				throw new UnsupportedOperationException("A client attempts to answer a friend request by a different name.");
			}
			
			if (response.isAccepted) {
				data.addFriendship(response.invitation.from, response.invitation.to);
			}
			
			sendIfOnline(response.invitation.from, response);
		}

		@Override
		public void visit(IsOnlineRequest request) {
			// If users are not friend, return an empty response.
			Optional<Boolean> response = Optional.empty();
			if (data.areFriends(client, request.who)) {
				response = Optional.of(data.isConnected(request.who));
			}
			sendIfOnline(client, new IsOnlineResponse(request.who, response));
		}

		@Override
		public void visit(IsOnlineResponse response) {
			throw new UnsupportedOperationException("The server should not get IsOnlineResponse.");
		}

		@Override
		public void visit(ExchangeList exchangeList) {
			throw new UnsupportedOperationException("The server should not get ExchangeList.");
		}
	}
}
