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
	

	/**
	 * Constructor. Creates a server connection for accepting and handling incoming messages as well as sending back outgoing replies, 
	 * using a custom {@link Codec} to encode/decode messages into the set Message type of the connection.<br>
	 * <b>Notice:</b> created Connection is inactive until {@link #start} is invoked. 
	 * 
	 * @param address - This server's address.
	 * @param codec - Custom codec for encoding/decoding messages.
	 */
	public ServerConnection(String address, Codec<Envelope<Message>> codec) {
		if (null == address || "".equals(address)) {
			throw new IllegalArgumentException("address cannot be null or empty");
		}
		
		if (null == codec) {
			throw new IllegalArgumentException("codec cannot be null");
		}
		
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
	 * Constructor. Creates a server connection, accepting and handling incoming messages as well as sending back outgoing replies, 
	 * using a custom Connection<Message> object. <br> The state of the new ServerConnection will match that of the given Connection.
	 * E.g: will need to call {@link #start} with a supplied {@link BiConsumer} if Connection.start(...) wasn't previously called.
	 * 
	 * <p>
	 * <code>
	 * Connection&lt;String&gt; conn = new Connection<String>(serverAddress); <br>
	 * ServerConnection&lt;String&gt; sc = new ServerConnection<String>(conn);<br>
	 * sc.start((addr, msg) -> System.out.println("message " + msg.toString());
	 * </code>
	 * </p>
	 * 
	 * @param connection - The Connection object to be used by this ServerConnection.
	 */
	public ServerConnection(Connection<Message> connection) {
		if (null == connection) {
			throw new IllegalArgumentException("connection cannot be null");
		}
		
		this.conn = connection;
	}
	
	
	/**
	 * Starts this ServerConnection, enabling it to send and receive messages, handling each incoming message with 
	 * the supplied User-defined handler. <br>
	 * 
	 * <p>
	 * <b>Notice:</b><br>
	 * <br>
	 * Calling this method when connection is already active will be ignored, and handler will <b>NOT</b> be changed.
	 * <br>
	 * </p>
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
	 * @param handler - A User (application) defined handler for all incoming messages, of type {@link BiConsumer}&lt;String, Message&gt;.<br>
	 * @see stop
	 */
	public void start(BiConsumer<String, Message> handler) {
		if (null == handler) {
			throw new IllegalArgumentException("handler cannot be null");
		}
		
		this.conn.start(env -> handler.accept(env.address, env.content));
	}
	
	
	/**
	 * Stop (Pause) this ServerConnection. 
	 * <p>When stopped, this connection does not receive, handle or send anything, but can be re-started
	 * using the {@link Start} method.<br>
	 * <br>
	 * If the Connection was already stopped upon invocation, this does nothing. </p>
	 * 
	 * @see start
	 * @see kill
	 */
	public void stop() {
		this.conn.stop();
	}

	
	/**
	 * Send out a message to a specific (client's) address. This is a <b>non-blocking</b> call.
	 * 
	 * @param to - Address to which message will be sent.
	 * @param content - User-defined message object to be sent.
	 */
	public void send(String to, Message content) {
		if ("".equals(content)) {
			throw new RuntimeException("server will not send empty messages");
		}
		
		conn.send(to, content); // contents and connection state validation is done inside this.conn
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
	public void kill() {
		conn.kill(); // connection state validation is done inside this.conn
	}
	
	
	/**
	 * Get this ServerConnection's address.
	 * 
	 * @return this ServerConnection's address.
	 */
	public String myAddress() {
		return this.conn.myAddress();
	}
	
}
