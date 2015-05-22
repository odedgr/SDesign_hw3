package il.ac.technion.cs.sd.app.msg;

import il.ac.technion.cs.sd.app.msg.exchange.ConnectRequest;
import il.ac.technion.cs.sd.app.msg.exchange.DisconnectRequest;
import il.ac.technion.cs.sd.app.msg.exchange.FriendRequest;
import il.ac.technion.cs.sd.app.msg.exchange.FriendResponse;
import il.ac.technion.cs.sd.app.msg.exchange.IsOnlineRequest;
import il.ac.technion.cs.sd.app.msg.exchange.IsOnlineResponse;
import il.ac.technion.cs.sd.app.msg.exchange.SendInstantMessageRequest;

/**
 * A visitor for the exchange class. Each class that uses the Exchange class
 * (client and server application classes) will implement this visitor in order
 * to handle different types requests and responses.
 */
public interface ExchangeVisitor {
	/**
	 * Accept and handle ConnectRequest.
	 * @param request the ConnectRequest to handle.
	 */
	void visit(ConnectRequest request);
	
	/**
	 * Accept and handle DisconnectRequest.
	 * @param request the DisconnectRequest to handle.
	 */
	void visit(DisconnectRequest request);
	
	/**
	 * Accept and handle SendMessageRequest.
	 * @param request the SendMessageRequest to handle.
	 */
	void visit(SendInstantMessageRequest request);

	/**
	 * Accept and handle FriendRequest.
	 * @param request the FriendRequest to handle.
	 */
	void visit(FriendRequest request);
	
	/**
	 * Accept and handle FriendResponse.
	 * @param response the FriendResponse to handle.
	 */
	void visit(FriendResponse response);

	/**
	 * Accept and Handle IsOnlineRequest.
	 * @param request the IsOnlineRequest to handle.
	 */
	void visit(IsOnlineRequest request);

	/**
	 * Accept and Handle IsOnlineRequest.
	 * @param response the IsOnlineResponse to handle.
	 */
	void visit(IsOnlineResponse response);
}
