package il.ac.technion.cs.sd.msg;

import java.util.function.BiConsumer;

public class ServerConnection<Message> {

	// INSTANCE VARIABLES
	private final Connection<Message> conn;
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
		if (null == consumer) {
			throw new IllegalArgumentException("got null consumer");
		}
		
		this.conn = new Connection<Message>(address, codec, x->handleIncomingMessage(x));
		this.consumer = consumer;
	}

	
	// TODO document
	public void start() {
		this.conn.start();
	}
	
	
	// TODO document
	public void stop() {
		this.conn.stop();
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
	
	/**
	 * Handle an incoming message, that is NOT an ACK (e.g: has actual contents).
	 * An ACK is implicitly sent back immediately to the sender of the message, and the message is handled using
	 * the consumer given to this ServerConnection upon initialization.
	 * 	
	 * @param env - Envelope containing the incoming message to be handled.
	 */
	private void handleIncomingMessage(Envelope<Message> env) { 
		// dispatch handling to a separate thread, which might add an outgoing message later, using the send() method
		this.consumer.accept(env.address, env.payload);
	}
	
	public void send(String to, Message payload) {
		conn.send(to, payload);
	}
		
	/**
	 * Terminate this connection. Stops all handling of incoming messages, receiving and sending messages,
	 * as well as killing its messenger.
	 */
	public void kill() {
		conn.kill();
	}
	
}
