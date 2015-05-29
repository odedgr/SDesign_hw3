package il.ac.technion.cs.sd.msg;

import java.util.function.Consumer;

public class ClientConnection<Message> extends Connection<Message> {
	
	// INSTANCE VARIABLES
	private final Consumer<Message> consumer;
	private final String myServer;
	
	/**
	 * Constructor. Creates and starts a running connection, accepting and handling incoming messages as well as
	 * sending back outgoing replies, using a custom {@link Codec} to encode/decode messages into the set Message type of the connection.
	 * 
	 * @param myAddress - This client's address.
	 * @param consumer - Application-defined function to handle incoming messages of type String. Will be applied for
	 * each incoming message (that is not an ACK). Different "types" of messages should be classified and handled
	 * accordingly on the application side.
	 * @param codec - Custom codec for encoding/decoding messages.
	 */
	public ClientConnection(String serverAddress, String myAddress, Consumer<Message> consumer, Codec<Envelope<Message>> codec) {
		super(myAddress, codec);

		if (null == serverAddress || "".equals(serverAddress)) {
			throw new IllegalArgumentException("invalid server address - empty or null");
		}
		
		if (null == consumer) {
			throw new IllegalArgumentException("got null consumer");
		}
		
		this.myServer = serverAddress;
		this.consumer = consumer;
		
		start();
	}

	/**
	 * Constructor. Creates and starts a running connection, accepting and handling incoming messages as well as
	 * sending back outgoing replies, using the default {@link Codec}.
	 * 
	 * @param myAddress - This client address.
	 * @param consumer - Application-defined function to handle incoming messages of type String. Will be applied for
	 * each incoming message (that is not an ACK). Different "types" of messages should be classified and handled
	 * accordingly on the application side.
	 */
	public ClientConnection(String serverAddress, String myAddress, Consumer<Message> consumer) {
		// TODO update documentation
		this(serverAddress, myAddress, consumer, new XStreamCodec<Envelope<Message>>());
	}
	
	@Override 
	protected void handleIncomingMessage(Envelope<Message> env) { 
		// dispatch handling to a separate thread, which might add an outgoing message later, using the send() method
		this.consumer.accept(env.payload);
	}
	
	public void send(Message payload) {
		super.send(this.myServer, payload);
	}
	
	
	/**
	 * Terminate this connection. Stops all handling of incoming messages, receiving and sending messages,
	 * as well as killing its messenger.
	 */
	public void kill() {
		super.kill();
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
