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
 * ServerConnection&lt;Message&gt; conn = new ServerConnection("server");
 * <br>conn.start(handle);
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
	private BiConsumer<String, Message> consumer;
	private boolean isActive = false;
	private boolean killed = false;;
	
	/**
	 * Constructor. Creates a server connection for accepting and handling incoming messages as well as sending back outgoing replies, 
	 * using a custom {@link Codec} to encode/decode messages into the set Message type of the connection.<br>
	 * <b>Notice:</b> created Connection is inactive until {@link #start} is invoked. 
	 * 
	 * @param address - This server's address.
	 * @param codec - Custom codec for encoding/decoding messages.
	 */
	public ServerConnection(String address, Codec<Envelope<Message>> codec) {
		this.conn = new Connection<Message>(address, codec);
	}

	/**
	 * Constructor. Creates a server connection, accepting and handling incoming messages as well as sending back outgoing replies, 
	 * using the default {@link Codec}. <br>
	 * <b>Notice:</b> created Connection is inactive until {@link #start} is invoked. 
	 * 
	 * @param address - This server's address.
	 */
	public ServerConnection(String address) {
		this(address, new XStreamCodec<Envelope<Message>>());
	}

	/**
	 * Starts this ServerConnection, enabling it to send and receive messages, handling each incoming message with 
	 * the supplied User-defined handler. <br>
	 * 
	 * <p>
	 * <b>Example usage:</b><br>
	 * <br>
	 * <code>
	 * conn.start( (addr, msg) -> handleMsgFrom(msg, addr) );<br>
	 * </code>
	 * <br>
	 * Where addr is always the source address, and msg is the received message, of type Message used for this ServerConnection's initialization.
	 * </p>
	 *
	 *<p>
	 * <b>Notice:</b><br>
	 * <br>
	 * Calling this method when connection is already active will be ignored, and handler will <b>NOT</b> be changed.
	 * <br>
	 * </p>
	 * 
	 * @param handler - A User (application) defined handler for all incoming messages, of type {@link BiConsumer}&lt;String, Message&gt;.<br>
	 * @see stop
	 */
	synchronized public void start(BiConsumer<String, Message> handler) {
		if (this.isActive) return; // ignore when already active
		
		if (this.killed ) {
			throw new IllegalArgumentException("connection cannot be re-started after it was killed");
		}
		
		if (null == handler) {
			throw new IllegalArgumentException("handler cannot be null");
		}
		
		this.consumer = handler;
		this.conn.start(x -> handleIncomingMessage(x));
		this.isActive  = true;
	}
	
	
	/**
	 * Stop (Pause) this ServerConnection. 
	 * <p>When stopped, this connection does not receive, handle or send anything, but can be re-started
	 * using the {@link Start} method.
	 * <br>If the Connection was already stopped upon invocation, this does nothing. </p>
	 * @see start
	 * @see kill
	 */
	synchronized public void stop() {
		this.conn.stop();
		this.isActive = false;
	}

	
	/**
	 * Handle an incoming message, that is NOT an ACK (e.g: has actual contents).
	 * An ACK is implicitly sent back immediately to the sender of the message, and the message is handled using
	 * the consumer given to this ServerConnection upon initialization.
	 * 	
	 * @param env - Envelope containing the incoming message to be handled.
	 */
	private void handleIncomingMessage(Envelope<Message> env) {
		if (null == this.consumer) {
			throw new RuntimeException("WTF?! somehow started running and tried to handle before setting the consumer");
		}
		
		// dispatch handling to a separate thread, which might add an outgoing message later, using the send() method
		this.consumer.accept(env.address, env.content);
	}
	
	
	/**
	 * Send out a message to a specific (client's) address. This is a <b>non-blocking</b> call.
	 * 
	 * @param to - Address to which message will be sent.
	 * @param content - User-defined message object to be sent.
	 */
	public void send(String to, Message content) {
		if (this.killed) {
			throw new RuntimeException("cannot send a message from a killed ServerConnection");
		}
		
		if (!this.isActive) {
			throw new RuntimeException("cannot send a message while ServerConnection is not active. Was it started? stoppped?");
		}
		
		conn.send(to, content);
	}
		
	
	/**
	 * Terminate this connection. Stops all handling of incoming messages, receiving and sending messages,
	 * as well as killing its messenger.
	 * 
	 * <p>
	 * <b>Notice:</b><br>
	 * Repeated calls are ignored.
	 * </p>
	 */
	synchronized public void kill() {
		if (this.killed) {
			System.out.println("Tried to kill an already dead server connection... ignored");
			return; // ignore trying to kill the connection more than once.
		}
		
		conn.kill();
		this.isActive = false;
		this.killed = true;
	}
	
}
