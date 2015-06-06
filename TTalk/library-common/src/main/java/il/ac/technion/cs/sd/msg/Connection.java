package il.ac.technion.cs.sd.msg;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A Connection manages receiving and sending custom messages, and allow user-defined handling of incoming messages.
 * 
 * <p>
 * A connection object takes care of reliability issues of low-level communication, and allows for simultaneous handling
 * of incoming and outgoing traffic. 
 * </p> 
 *
 * A typical Connection creation and usage:<br><br>
 * 
 * <code> 
 * Connection&lt;Message&gt; conn = new Connection("myAddress");
 * <br>conn.start(x -> handleMessage_1(x));
 * <br>...
 * <br>conn.stop()
 * <br>...
 * <br>conn.start(x -> handleMessage_2(x));
 * <br>...
 * <br>conn.kill();
 * </code><br><br>
 * 
 * where handleMessage() has a signature of:<br>
 * <code>
 * void handleMessage({@link Envelope}&lt;Message&gt; m)
 * </code><br><br>
 * 
 * @param <Message> User-defined type of message to be handled by this connection. Using application should send a prototype of all
 * messages it uses (either incoming or outgoing) and handle internally each possible sub-type of Message.
 */
public class Connection<Message> {
	
	public static final long ACK_TIMEOUT_IN_MILLISECONDS = 50L; // time to wait for an ACK before re-sending

	// CONSTANTS
	private static final String ACK = "";
	
	// INSTANCE VARIABLES
	private Dispatcher<Envelope<Message>> receiver; // thread taking each incoming message from queue and dispatching a handler
	private Dispatcher<Envelope<Message>> sender; // thread in charge of sending outgoing messages from queue
	private Messenger messenger = null;
	
	private final Codec<Envelope<Message>> codec;
	private final MessengerFactory messengerFactory;
	private final String myAddress;
	
	private Semaphore ackNotifier;
	
	/**
	 * Constructor. Creates a connection for accepting and handling incoming messages as well as sending back outgoing replies, 
	 * using a custom {@link Codec} to encode/decode messages into the set Message type of the connection, and a custom MessengerFactory.<br>
	 * <b>Notice:</b> created Connection is inactive until {@link #start(Consumer)} is invoked. 
	 * 
	 * @param myAddress - This connection's address.
	 * @param codec - Custom {@link Codec} for encoding/decoding messages.
	 * @param messengerFactory - {@link MessengerFactory}, used for creating {@link Messenger messengers} to handle low-level communication.
	 */
	public Connection(String myAddress, Codec<Envelope<Message>> codec, MessengerFactory messengerFactory) {
		if (null == myAddress || "".equals(myAddress)) {
			throw new IllegalArgumentException("invalid server address - empty or null");
		}
		
		if (null == codec) {
			throw new IllegalArgumentException("got null codec");
		}

		this.messengerFactory = messengerFactory;
		this.myAddress = myAddress;
		this.codec = codec;
	}
	
	
	/**
	 * Constructor. Creates a connection for accepting and handling incoming messages as well as sending back outgoing replies, 
	 * using a custom {@link Codec} to encode/decode messages into the set Message type of the connection, and the default MessengerFactory.<br>
	 * <b>Notice:</b> created Connection is inactive until {@link #start(Consumer)} is invoked. 
	 * 
	 * @param myAddress - This connection's address.
	 * @param codec - Custom {@link Codec} for encoding/decoding messages.
	 */
	public Connection(String myAddress, Codec<Envelope<Message>> codec) {
		this(myAddress, codec, new MessengerFactory());
	}
	
	
	/**
	 * Constructor. Creates a connection for accepting and handling incoming messages as well as sending back outgoing replies, 
	 * using a default {@link Codec} to encode/decode messages into the set Message type of the connection, and the default MessengerFactory.<br>
	 * <b>Notice:</b> created Connection is inactive until {@link #start(Consumer)} is invoked. 
	 * 
	 * @param myAddress - This connection's address.
	 */
	public Connection(String myAddress) {
		this(myAddress, new XStreamCodec<>(), new MessengerFactory());
	}
	
	/**
	 * Add an outgoing message (either with or without content) to the outgoing message queue. 
	 * For convenience, you might consider using the {@link #sendAck(String to) sendAck} instead. 
	 * 
	 * @param to - Address of destination to whom the message will be sent.
	 * @param message - Contents of message to be sent..
	 * @see {@link #sendAck(String to)}
	 */
	protected void send(String to, Message message) {
		if (null == to || "".equals(to)) {
			throw new IllegalArgumentException("recepient address cannot be null nor enpty");
		}
		
		if (null == message) {
			throw new IllegalArgumentException("contents to send cannot be null");
		}
		
		if (messenger == null) {
			throw new RuntimeException("cannot send when connection is inactive");
		}
		
		if (ACK.equals(message)) { // send an ACK - no need to wait for incoming ACK in return
			throw new UnsupportedOperationException("don't use send() to send empty messages.");
		}
		safeSend(Envelope.wrap(myAddress, to, message));
		
		
//		this.sender.enqueue(Envelope.wrap(myAddress, to, message));
	}
	
	
	/**
	 * Do actual sending, with validation of arrival at the receiver side, re-sending periodically, until an ACK is received.
	 * <br><br>
	 * This is a <b>blocking</b> call.
	 * 
	 * @param env - Envelope to be sent.
	 */
	private void safeSend(Envelope<Message> env) {
		this.ackNotifier = new Semaphore(0);
		while (true) {
			System.out.println("Trying to send: " + env);
			try {
				this.messenger.send(env.to, this.codec.encode(env));
				if (this.ackNotifier.tryAcquire(ACK_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS)) {
					System.out.println("succeeded!");
					break;
				} else {
					System.out.println("timeout.");
				}
			} catch (InterruptedException | MessengerException e) {
				throw new RuntimeException(e);
			}
		}
	}

	
	/**
	 * Sends an ACK (empty string) to a given address, guaranteed to be received by the recipient.
	 * <br><br>
	 * This is a <b>non-blocking</b> call.
	 * 
	 * @param to - Address of the ACK receiver.
	 */
	private void sendAck(String to) {
		try {
			this.messenger.send(to, ACK);
		} catch (MessengerException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Receive a raw incoming message, and put it in a FIFO queue for appropriate handling. If incoming message is
	 * an ACK, than it is handled immediately, and not pushed into the incoming message queue for further handling. 
	 *  
	 * @param inMsg - Raw incoming message, as received from messenger.
	 */
	private void receiveIncomingMessage(String inMsg) {
		if (null == inMsg) {
			throw new RuntimeException("Received a null incoming message");
		}

		if (inMsg.equals(ACK)) {
			ackNotifier.release();
			return;
		}
		Envelope<Message> env = codec.decode(inMsg);
		// Send an ack to the sender.
		sendAck(env.from);
		this.receiver.enqueue(env);
	}
	
	/**
	 * Starts this Connection, enabling it to send and receive messages.
	 * 
	 * <p>
	 * <b>Notice:</b><br>
	 * Calling this method while Connection is already started (e.g: without calling {@link stop}) will be ignored
	 * and the message handler will <b>not</b> be changed.
	 * </p>
	 * 
	 * <p>
	 * <b>Example use: </b><br>
	 * <code>
	 * conn.start(x -> handleIt(x))<br>
	 * </code>
	 * <br>
	 * Where handleIt has a signature of: <br>
	 * <br>
	 * <code>void handleIt(Message m)</code><br>
	 * <br>
	 * And Message is the prototype supplied upon Connection instantiation.
	 * </p>
	 * 
	 * @param handler - User-defined consumer to handle incoming messages.
	 */
	public void start(Consumer<Envelope<Message>> handler) {
		if (messenger != null) { // already started - ignoring call
			return;
		}

		// Start sender and receiver.
		sender = new Dispatcher<Envelope<Message>>(x -> safeSend(x));
		sender.start();
		
		receiver = new Dispatcher<Envelope<Message>>(x -> handler.accept(x));
		receiver.start();
		
		try {
			messenger = messengerFactory.start(myAddress, x -> receiveIncomingMessage(x));
		} catch (MessengerException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * Stop (Pause) this Connection. <br>
	 * When stopped, this connection does not receive, handle or send anything, but can be re-started
	 * using the {@link Start} method.<br>
	 * <br>
	 * If the Connection was already stopped upon invocation, this does nothing.
	 */
	synchronized public void stop() {
		if (messenger == null) {
			// Already stopped; Do nothing.
			return;
		}
		
		sender.stop();
		receiver.stop();
		
		try {
			this.messenger.kill();
		} catch (MessengerException e) {
			throw new RuntimeException(e);
		} finally {
			this.messenger = null;
		}
	}
	
	/**
	 * Get this Connection's address.
	 * 
	 * @return this Connection's own address.
	 */
	public String myAddress() {
		return this.myAddress;
	}
}


