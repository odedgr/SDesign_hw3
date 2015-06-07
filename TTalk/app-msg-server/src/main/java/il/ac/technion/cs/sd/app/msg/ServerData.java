package il.ac.technion.cs.sd.app.msg;

import il.ac.technion.cs.sd.app.msg.exchange.Exchange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class which encapsulates all server related data: online users, friendships and user pending messages.  
 */
public class ServerData {
	private Set<String> online = Collections.synchronizedSet(new HashSet<String>());
	private Set<Friendship> friendships = Collections.synchronizedSet(new HashSet<Friendship>());
	
	private Map<String, List<Exchange>> userQueues = new ConcurrentHashMap<String, List<Exchange>>();
	
	/**
	 * Set a client as connected to the server.
	 * @param clientName the name of the client to connect.
	 */
	public void connect(String clientName) {
		online.add(clientName);
	}
	
	/**
	 * Set a client as disconnected from the server.
	 * @param clientName the name of the client to disconnect.
	 */
	public void disconnect(String clientName) {
		online.remove(clientName);
	}
	
	/**
	 * Check if a client is connected.
	 * @param clientName the name of the client to check.
	 * @return a boolean value indicating if the user is online.
	 */
	public boolean isConnected(String clientName) {
		return online.contains(clientName);
	}
	
	/**
	 * Marks two clients as friends.
	 * @param friend1 the name of one client to mark as friend.
	 * @param friend2 the name of another client to mark as friend.
	 */
	public void addFriendship(String friend1, String friend2) {
		friendships.add(new Friendship(friend1, friend2));
	}
	
	/**
	 * Returns whether two clients are friends.
	 * @param friend1 the name of one client to check.
	 * @param friend2 the name of another client to check.
	 * @return a boolean indicating whether the two clients are friends.
	 */
	public boolean areFriends(String friend1, String friend2) {
		return friendships.contains(new Friendship(friend1, friend2));
	}
	
	/**
	 * Add a message to the queue of messages waiting for the client.
	 * @param client the client to add the message to its queue.
	 * @param message the message to add to the queue.
	 */
	public void addPendingClientMessage(String client, Exchange message) {
		if (!userQueues.containsKey(client)) {
			userQueues.put(client, Collections.synchronizedList(new ArrayList<Exchange>()));
		}
		userQueues.get(client).add(message);
	}
	
	/**
	 * Get the queue of the messages waiting for the client.
	 * Also clears the queue.
	 * @param client the name of the client to get and clear its queue.
	 * @return the queue of all the client's pending messages.
	 */
	public List<Exchange> getAndClearPendingClientMessages(String client) {
		if (!userQueues.containsKey(client)) {
			return Collections.emptyList();
		}
		List<Exchange> $ = userQueues.get(client);
		userQueues.remove(client);
		return $;
	}

	/**
	 * Clear all online users (before closing the server).
	 */
	public void disconnectAll() {
		online.clear();
	}
}
