package il.ac.technion.cs.sd.msg;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A ClientConnection manages receiving and sending custom messages, and allow user-defined handling of incoming messages on the client-side.
 * 
 * <p>
 * The client connection object takes care of reliability issues of low-level communication, and allows for simultaneous handling
 * of incoming and outgoing traffic. 
 * </p> 
 *
 * A typical ClientConnection creation and usage:<br><br>
 * 
 * <code> 
 * ClientConnection&lt;Message&gt; conn = new ClientConnection("client");
 * <br>conn.start(handle);
 * <br>...
 * <br>conn.kill();
 * </code><br><br>
 * 
 * <p>
 * where handle is a {@link Consumer}&lt;Message&gt;.<br>
 * i.e: handle = <code>(x) -> doSomething(x)</code>
 * </p>
 * <p>
 * e.g:<code> new ClientConnection("client", (x) -> doSomething(x)); </code>
 * </p>
 * 
 * @param <Message> User-defined type of message to be handled by this connection. Using application should send a prototype of all
 * messages it uses (either incoming or outgoing) and handle internally each possible sub-type of Message.
 */
public class ClientConnection<Message> {
	
	// INSTANCE VARIABLES
	private final Connection<Message> conn;
	private final String myServer;
	
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
		
		if (null == myAddress || "".equals(myAddress)) {
			throw new IllegalArgumentException("invalid client address - empty or null");
		}
		
		if (null == codec) {
			throw new IllegalArgumentException("codec cannot be null");
		}
		
		this.conn = new Connection<Message>(myAddress, codec);
		this.myServer = serverAddress;
	}

	
	/**
	 * Constructor. Creates a client connection, accepting and handling incoming messages as well as sending back outgoing replies, 
	 * using the default {@link Codec}. <br>
	 * <b>Notice:</b> created Connection is inactive until {@link #start} is invoked. 
	 * 
	 * @param myAddress - This client address.
	 * @param serverAddress - Address of the server via which this client communicates
	 */
	public ClientConnection(String serverAddress, String myAddress) {
		this(serverAddress, myAddress, new XStreamCodec<Envelope<Message>>());
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
	 * @param serverAddress - address of this ClientConnection's server
	 * @param connection - The Connection object to be used by this ServerConnection.
	 */
	public ClientConnection(String serverAddress, Connection<Message> connection) {
		if (null == connection) {
			throw new IllegalArgumentException("connection cannot be null");
		}
		
		if (null == serverAddress || "".equals(serverAddress)) {
			throw new IllegalArgumentException("invalid server address - empty or null");
		}
		
		this.myServer = serverAddress;
		this.conn = connection;
	}
	
	
	/**
	 * Starts this ClientConnection, enabling it to send and receive messages, handling each incoming message with 
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
	 * conn.start( (msg) -> handleMsg(msg) );<br>
	 * </code>
	 * <br>
	 * Where msg is the received message, of type Message used for this ClientConnection's initialization.
	 * </p>
	 *
	 * @param handler - A User (application) defined handler for all incoming messages, of type {@link Consumer}&lt;Message&gt;.<br>
	 * @see stop
	 */
	public void start(Consumer<Message> handler) {
		if (null == handler) {
			throw new IllegalArgumentException("handler cannot be null");
		}
		
		this.conn.start(env -> handler.accept(env.content)); // connection state validation is done inside this.conn
	}
	
	
	 /**
	 * Stop (Pause) this ClientConnection. 
	 * <p>When stopped, this connection does not receive, handle or send anything, but can be re-started
	 * using the {@link Start} method.<br>
	 * <br>
	 * If the Connection was already stopped upon invocation, this does nothing. 
	 * </p>
	 * 
	 * @see start
	 * @see kill
	 */
	public void stop() {
		this.conn.stop();
	}

	
	/**
	 * Send out a message. <b>Non-blocking</b> call.
	 * <p>
	 * The message is sent directly to this client's server address, supplied upon creation, adhering to the framework architecture.
	 * Upon arrival at the server-side, message will be handled, possibly triggering more communication with this or other clients.
	 * </p>
	 * 
	 * @param content - User-defined message object to be sent.
	 */
	public void send(Message content) {
		if ("".equals(content)) {
			throw new RuntimeException("client will not send empty messages");
		}
		
		conn.send(this.myServer, content); // contents and connection state validation is done inside this.conn
	}
	
	
	/**
	 * Terminate this connection. Stops all handling of incoming messages, receiving and sending messages,
	 * as well as killing its messenger.
	 */
	public void kill() {
		conn.kill(); // connection state validation is done inside this.conn
	}
	
	
	/**
	 * Get this ClientsConnection's server address.
	 * 
	 * @return this ClientConnection's server address.
	 */
	public String myServer() {
		return this.myServer;
	}
	
	
	/**
	 * Get this ClientConnection's address.
	 * 
	 * @return this ClientConnection's address.
	 */
	public String myAddress() {
		return this.conn.myAddress();
	}
	
}
