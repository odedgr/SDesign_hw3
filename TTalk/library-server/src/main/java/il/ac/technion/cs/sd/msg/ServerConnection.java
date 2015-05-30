package il.ac.technion.cs.sd.msg;

import java.util.function.BiConsumer;

/**
 * A ServerConnection manages receiving and sending custom messages, and allow user-defined handling of incoming messages on the server-side.
 * 
 * <p>
 * The server connection object takes care of reliability issues of low-level communication, and allows for simultaneous handling
 * of incoming and outgoing traffic. 
 * </p> 
 *
 * A typical ServerConnection creation and usage:<br><br>
 * 
 * <code> 
 * ServerConnection&lt;Message&gt; conn = new ServerConnection("server", handle);
 * <br>conn.start();
 * <br>...
 * <br>conn.kill();
 * </code><br><br>
 * 
 * <p>
 * where handle is a {@link BiConsumer}&lt;String, Message&gt;.<br>
 * i.e: handle = <code>(x, y) -> doSomething(x, y)</code>
 * </p>
 * <p>
 * e.g:<code> new ServerConnection("server", (x,y) -> doSomething(x,y)); </code>
 * </p>
 * 
 * @param <Message> User-defined type of message to be handled by this connection. Using application should send a prototype of all
 * messages it uses (either incoming or outgoing) and handle internally each possible sub-type of Message.
 */
public class ServerConnection<Message> {

	// INSTANCE VARIABLES
	private final Connection<Message> conn;
	private final BiConsumer<String, Message> consumer;
	
	
	/**
	 * Constructor. Creates a connection for accepting and handling incoming messages as well as sending back outgoing replies, 
	 * using a custom {@link Codec} to encode/decode messages into the set Message type of the connection.<br>
	 * <b>Notice:</b> created Connection is inactive until {@link #start} is invoked. 
	 * 
	 * @param address - This server's address.
	 * @param consumer - Application-defined function to handle incoming messages (encoded as String). Will be applied for
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

	
	/**
	 * Constructor. Creates a server connection, accepting and handling incoming messages as well as sending back outgoing replies, 
	 * using the default {@link Codec}. <br>
	 * <b>Notice:</b> created Connection is inactive until {@link #start} is invoked. 
	 * 
	 * @param address - This server's address.
	 * @param consumer - Application-defined function to handle incoming messages (encoded as String). Will be applied for
	 * each incoming message (that is not an ACK). Different "types" of messages should be classified and handled
	 * accordingly on the application side.
	 */
	public ServerConnection(String address, BiConsumer<String, Message> consumer) {
		this(address, consumer, new XStreamCodec<Envelope<Message>>());
	}

	
	/**
	 * Starts this ServerConnection, enabling it to send and receive messages.
	 */
	public void start() {
		this.conn.start();
	}
	
	
	/**
	 * Stop (Pause) this ServerConnection. 
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
		this.consumer.accept(env.address, env.content);
	}
	
	
	/**
	 * Send out a message to a specific (client's) address. <b>Non-blocking</b> call.
	 * 
	 * @param to - Address to which message will be sent.
	 * @param content - User-defined message object to be sent.
	 */
	public void send(String to, Message content) {
		conn.send(to, content);
	}
		
	
	/**
	 * Terminate this connection. Stops all handling of incoming messages, receiving and sending messages,
	 * as well as killing its messenger.
	 */
	public void kill() {
		conn.kill();
	}
	
}
