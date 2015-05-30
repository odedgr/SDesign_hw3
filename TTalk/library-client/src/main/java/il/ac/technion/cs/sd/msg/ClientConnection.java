package il.ac.technion.cs.sd.msg;

import java.util.function.Consumer;

public class ClientConnection<Message> {
	
	// INSTANCE VARIABLES
	private final Connection<Message> conn;
	private final Consumer<Message> consumer;
	private final String myServer;
	
	
	/**
	 * Constructor. Creates a client connection, accepting and handling incoming messages as well as sending outgoing messages, 
	 * using a custom {@link Codec} to encode/decode messages into the set Message type of the connection.<br>
	 * <b>Notice:</b> created Connection is inactive until {@link #start} is invoked. 
	 * 
	 * @param myAddress - This client's address.
	 * @param consumer - Application-defined function to handle incoming messages (encoded as String). Will be applied for
	 * each incoming message (that is not an ACK). Different "types" of messages should be classified and handled
	 * accordingly on the application side.
	 * @param codec - Custom codec for encoding/decoding messages.
	 */
	public ClientConnection(String serverAddress, String myAddress, Consumer<Message> consumer, Codec<Envelope<Message>> codec) {
		if (null == serverAddress || "".equals(serverAddress)) {
			throw new IllegalArgumentException("invalid server address - empty or null");
		}
		
		if (null == consumer) {
			throw new IllegalArgumentException("got null consumer");
		}
		
		this.conn = new Connection<Message>(myAddress, codec, x -> handleIncomingMessage(x));
		this.myServer = serverAddress;
		this.consumer = consumer;
	}

	
	/**
	 * Constructor. Creates a client connection, accepting and handling incoming messages as well as sending back outgoing replies, 
	 * using the default {@link Codec}. <br>
	 * <b>Notice:</b> created Connection is inactive until {@link #start} is invoked. 
	 * 
	 * @param myAddress - This client address.
	 * @param consumer - Application-defined function to handle incoming messages (encoded as String). Will be applied for
	 * each incoming message (that is not an ACK). Different "types" of messages should be classified and handled
	 * accordingly on the application side.
	 */
	public ClientConnection(String serverAddress, String myAddress, Consumer<Message> consumer) {
		this(serverAddress, myAddress, consumer, new XStreamCodec<Envelope<Message>>());
	}
	
	 /**
	 * Starts this ClientConnection, enabling it to send and receive messages.
	 */
	public void start() {
		this.conn.start();
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
	 * Handle an incoming message, that is NOT an ACK (e.g: has actual contents).
	 * An ACK is implicitly sent back immediately to the sender of the message, and the message is handled using
	 * the consumer given to this ServerConnection upon initialization.
	 * 	
	 * @param env - Envelope containing the incoming message to be handled.
	 */
	private void handleIncomingMessage(Envelope<Message> env) { 
		// dispatch handling to a separate thread, which might add an outgoing message later, using the send() method
		this.consumer.accept(env.content);
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
