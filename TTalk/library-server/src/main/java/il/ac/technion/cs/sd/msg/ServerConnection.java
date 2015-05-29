package il.ac.technion.cs.sd.msg;

import java.util.function.BiConsumer;

public class ServerConnection<Message> extends Connection<Message> {

	// INSTANCE VARIABLES
	private final BiConsumer<String, Message> consumer;
	
	/**
	 * Constructor. Creates and starts a running connection, accepting and handling incoming messages as well as
	 * sending back outgoing replies, using a custom {@link Codec} to encode/decode messages into the set Message type of the connection.
	 * 
	 * @param address - This server's address.
	 * @param consumer - Application-defined function to handle incoming messages of type String. Will be applied for
	 * each incoming message (that is not an ACK). Different "types" of messages should be classified and handled
	 * accordingly on the application side.
	 * @param codec - Custom codec for encoding/decoding messages.
	 */
	public ServerConnection(String address, BiConsumer<String, Message> consumer, Codec<Envelope<Message>> codec) {
		super(address, codec);
		
		if (null == consumer) {
			throw new IllegalArgumentException("got null consumer");
		}
		
		this.consumer = consumer;
		
		start();
	}
	
	/**
	 * Constructor. Creates and starts a running server connection, accepting and handling incoming messages as well as
	 * sending back outgoing replies, using the default {@link Codec}.
	 * 
	 * @param address - This server's address.
	 * @param consumer - Application-defined function to handle incoming messages of type String. Will be applied for
	 * each incoming message (that is not an ACK). Different "types" of messages should be classified and handled
	 * accordingly on the application side.
	 */
	public ServerConnection(String address, BiConsumer<String, Message> consumer) {
		this(address, consumer, new XStreamCodec<Envelope<Message>>());
	}
	
	
	@Override
	protected void handleIncomingMessage(Envelope<Message> env) { 
		// dispatch handling to a separate thread, which might add an outgoing message later, using the send() method
		this.consumer.accept(env.address, env.payload);
	}
	
	/**
	 * Terminate this connection. Stops all handling of incoming messages, receiving and sending messages,
	 * as well as killing its messenger.
	 */
	public void kill() {
		super.kill();
	}
	
}
