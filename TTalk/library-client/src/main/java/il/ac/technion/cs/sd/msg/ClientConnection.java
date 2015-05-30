package il.ac.technion.cs.sd.msg;

import java.util.function.Consumer;

public class ClientConnection<Message> {
	
	// INSTANCE VARIABLES
	private final Connection<Message> conn;
	private final String myServer;
	
//	TODO update documentation
	/**
	 * Constructor. Creates a client connection, accepting and handling incoming messages as well as sending outgoing messages, 
	 * using a custom {@link Codec} to encode/decode messages into the set Message type of the connection.<br>
	 * <b>Notice:</b> created Connection is inactive until {@link #start} is invoked. 
	 * 
	 * @param serverAddress
	 * @param myAddress - This client's address.
	 * @param codec - Custom codec for encoding/decoding messages.
	 */
	public ClientConnection(String serverAddress, String myAddress, Codec<Envelope<Message>> codec) {
		if (null == serverAddress || "".equals(serverAddress)) {
			throw new IllegalArgumentException("invalid server address - empty or null");
		}
		
		this.conn = new Connection<Message>(myAddress, codec);
		this.myServer = serverAddress;
	}

	
	// TODO update documentation
	/**
	 * Constructor. Creates a client connection, accepting and handling incoming messages as well as sending back outgoing replies, 
	 * using the default {@link Codec}. <br>
	 * <b>Notice:</b> created Connection is inactive until {@link #start} is invoked. 
	 * 
	 * @param myAddress - This client address.
	 * @param serverAddress
	 */
	public ClientConnection(String serverAddress, String myAddress) {
		this(serverAddress, myAddress, new XStreamCodec<Envelope<Message>>());
	}
	
	
	// TODO update documentation
	/**
	 * Starts this ClientConnection, enabling it to send and receive messages.
	 */
	public void start(Consumer<Message> handler) {
		if (null == handler) {
			throw new IllegalArgumentException("handler cannot be null");
		}
		
		this.conn.start(env -> handler.accept(env.content));
	}
	
	
	 /**
	 * Stop (Pause) this ClientConnection. 
	 * <p>When stopped, this connection does not receive, handle or send anything, but can be re-started
	 * using the {@link Start} method.
	 * <br>If the Connection was already stopped upon invocation, this does nothing. </p>
	 */
	public void stop() {
		this.conn.stop();
	}

	
	/**
	 * Send out a message. <b>Non-blocking</b> call.
	 *  <p>
	 * The message is sent directly to this client's server address, supplied upon creation, adhering to the framework architecture.
	 * Upon arrival at the server-side, message will be handled, possibly triggering more communication with this or other clients.
	 * </p>
	 * 
	 * @param content - User-defined message object to be sent.
	 */
	public void send(Message content) {
		conn.send(this.myServer, content);
	}
	
	
	/**
	 * Terminate this connection. Stops all handling of incoming messages, receiving and sending messages,
	 * as well as killing its messenger.
	 */
	public void kill() {
		conn.kill();
	}
	
	
	/**
	 * Get this ClientsConnection's server address.
	 * 
	 * @return this ClientConnection's server address.
	 */
	public String myServer() {
		return this.myServer;
	}
	
}
