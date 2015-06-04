package il.ac.technion.cs.sd.msg;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
	
	// CONSTANTS
	private static final String ACK = "";
	private static final long ACK_TIMEOUT_IN_MILLISECONDS = 50L; // time to wait for an ACK before re-sending
	
	// INSTANCE VARIABLES
	private final Dispatcher<Message> receiver; // thread taking each incoming message from queue and dispatching a handler
	private final Dispatcher<Message> sender; // thread in charge of sending outgoing messages from queue
	private Messenger messenger; // can't be final, to support stop and restart
	private final Codec<Envelope<Message>> codec;
	private final MessengerFactory factory; // used for messenger creation upon every call to start(...)
	
	private final String myAddress;
	private boolean gotAck = false;
	private final Object ackNotifier = new Object();
	private Object messengerLock = new Object();
	
	/* Connection state */
	private boolean isActive = false; // set to 'true' upon each call to startMe(), 'false' upon stop() or kill()
	private boolean started = false; // set to 'true' upon first call to startMe()
	private boolean killed = false; // set to 'true' upon call to kill()
	
	private final ExecutorService executor; // thread pool, for message handlers using supplied consumer
	
	/**
	 * Constructor. Creates a connection for accepting and handling incoming messages as well as sending back outgoing replies, 
	 * using a custom {@link Codec} to encode/decode messages into the set Message type of the connection, and a custom MessengerFactory.<br>
	 * <b>Notice:</b> created Connection is inactive until {@link #start(Consumer)} is invoked. 
	 * 
	 * @param myAddress - This connection's address.
	 * @param codec - Custom {@link Codec} for encoding/decoding messages.
	 * @param factory - {@link MessengerFactory}, used for creating {@link Messenger messengers} to handle low-level communication.
	 */
	public Connection(String myAddress, Codec<Envelope<Message>> codec, MessengerFactory factory) {
		if (null == myAddress || "".equals(myAddress)) {
			throw new IllegalArgumentException("invalid server address - empty or null");
		}
		
		if (null == codec) {
			throw new IllegalArgumentException("got null codec");
		}
		
		this.factory   = (null != factory) ? factory : new MessengerFactory();
		this.myAddress = myAddress;
		this.executor  = Executors.newCachedThreadPool(); // TODO maybe get rid of this
		this.receiver  = new Dispatcher<Message>();
		this.sender    = new Dispatcher<Message>();
		this.codec     = codec;
		
		this.receiver.setName("Dispatcher-Receiver");
		this.sender.setName("Dispatcher-Sender");
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
		this(myAddress, codec, null);
	}
	
	
	/**
	 * Constructor. Creates a connection for accepting and handling incoming messages as well as sending back outgoing replies, 
	 * using a default {@link Codec} to encode/decode messages into the set Message type of the connection, and the default MessengerFactory.<br>
	 * <b>Notice:</b> created Connection is inactive until {@link #start(Consumer)} is invoked. 
	 * 
	 * @param myAddress - This connection's address.
	 */
	public Connection(String myAddress) {
		this(myAddress, new XStreamCodec<>(), null);
	}
	
	
	/**
	 * Create and start a new Messenger to be used by this Connection, with a given address.
	 * <p> 
	 * <b>Notice:</b>
	 * <br>(1) If a Messenger was already active, this does nothing. 
	 * <br>(2) If messenger creation failed, connection's state would be like after killing its Messenger.
	 * <br> --> consecutive calls are safe.
	 * </p>
	 * 
	 * @param myAddress - Address to be bound to (used by) the newly created messenger.
	 */
	private void startMessenger(String myAddress) { // TODO handle failure in calling method ?
		if (null != this.messenger) return; // messenger already running for this connection
		
		Messenger newMessenger = null;
		try {
			newMessenger = this.factory.start(myAddress, x -> receiveIncomingMessage(x));
		} catch (MessengerException e) {
			System.out.println(e.getMessage());
			throw new RuntimeException(e);
		} finally {
			this.messenger = newMessenger; // might be null if messenger creation failed
		}
	}
	
	
	/**
	 * Kill this connection's Messenger. If no Messenger was active - this does nothing.
	 * <br><br>
	 * consecutive calls are safe.
	 */
	private void killMessenger() {
		if (null == this.messenger) return; // connection doesn't have an active messenger
		
		synchronized (this.messengerLock) {
			try {
				this.messenger.kill();
			} catch (MessengerException e) {
				System.out.println("MessengerException when trying to kill messenger in Connection");
				throw new RuntimeException(e);
			} finally {
				this.messenger = null;
			}
		}
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
		
		if (!this.isActive || this.killed) {
			throw new RuntimeException("cannot send when connection is inactive");
		}
		
		if (ACK.equals(message)) { // send an ACK - no need to wait for incoming ACK in return
			System.out.println("don't use send() to send ACK or empty messages. use sendAck instead");
			return;
		}
		
		// by here we know we are trying to send a message with contents - need to make sure it was received
		try {
//			System.out.println("enqueueing to sender: " + Envelope.wrap(to, message).toString());
			System.out.println(myAddress + " sending " + to + " message: \"" + message + "\"");
			this.sender.enqueue(Envelope.wrap(myAddress, to, message));
		} catch (InterruptedException e) {
			System.out.println("interrupted while trying to enqueue envelope in sender");
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Do actual sending, with validation of arrival at the receiver side, re-sending periodically, until an ACK is received.
	 * <br><br>
	 * This is a <b>blocking</b> call.
	 * 
	 * @param env - Envelope to be sent.
	 */
	private void safeSend(Envelope<Message> env) {
		this.gotAck = false;
//		System.out.println("safe sending " + env.toString());
		synchronized (this.ackNotifier) {
			do {
				try {
					synchronized (this.messengerLock) { // protect in case messenger was killed by calling stop()
						if (null != this.messenger) {
							this.messenger.send(env.to, this.codec.encode(env));
						}
					}
					this.ackNotifier.wait(ACK_TIMEOUT_IN_MILLISECONDS);
				} catch (MessengerException e) {
					throw new RuntimeException(e);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} while (!this.gotAck && !this.killed);
		}
	}
	
	
	/**
	 * Sends an ACK (empty string) to a given address, guaranteed to be received by the recipient.
	 * <br><br>
	 * This is a <b>non-blocking</b> call.
	 * 
	 * @param to - Address of the ACK receiver.
	 */
	public void sendAck(String to) {
		try {
			synchronized (this.messengerLock ) {
				if (null != this.messenger) {
					System.out.println(myAddress + " sending ACK to " + to);
					
					this.messenger.send(to, ACK);
				}
			}
		} catch (MessengerException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * Handles a received ACK. Used to notify the sender dispatcher that an awaited ACK has been received and it
	 * can move on to the next outgoing message.
	 */
	protected void receivedAck() {
		
		System.out.println(myAddress + " got an ACK");
		
		synchronized (ackNotifier) {
			this.gotAck = true;
			ackNotifier.notify();
		}
	}
	
	
	/**
	 * Receive a raw incoming message, and put it in a FIFO queue for appropriate handling. If incoming message is
	 * an ACK, than it is handled immediately, and not pushed into the incoming message queue for further handling. 
	 *  
	 * @param inMsg - Raw incoming message, as received from messenger.
	 */
	private void receiveIncomingMessage(String inMsg) {
		// TODO add mechanism to drop duplicate messages (in case received, sent ACK, but before other side got the ACK - 
		// it resent the same message. use time-stamp of first sent trial of each message as sequence number.
		
		if (null == inMsg) {
			throw new RuntimeException("Received a null incoming message");
		}

		if (inMsg.equals(ACK)) {
			receivedAck();
			return; // no further handling
		}

		// add incoming message to queue for handling
		try {
//			System.out.println("enqueueing to receiver: " + inMsg);
			this.receiver.enqueue(codec.decode(inMsg));
		} catch (InterruptedException e) {
			System.out.println("interrupted while trying to put an incoming message in the incoming queue. ignoring it.");
		}
	}
	
	
	/**
	 * Get a COPY of all incoming Envelopes that were not yet handled (e.g: the incoming message queue).<br>
	 * <br>
	 * <b>Can only be called when connection is inactive (e.g: after calling {@link #kill() or {@link #stop}})</b>
	 * 
	 * @return a Collection of all incoming messages that have not yet been handled.
	 */
	public Collection<Envelope<Message>> getUnhandled() {
		if (this.isActive) {
			throw new RuntimeException("can only be called when connection is inactive.");
		}
		
		return this.receiver.getUnhandled();
	}
	
	
	/**
	 * Get all the outgoing Envelopes that were not yet sent out.
	 * <br><br>
	 * <b>Can only be called when connection is inactive (e.g: after calling {@link #kill} or {@link #stop})</b>
	 * 
	 * @return a Collection of all outgoing messages that have not yet been sent.
	 */
	public Collection<Envelope<Message>> getUnsent() {
		if (this.isActive) {
			throw new RuntimeException("can only be called when connection is inactive.");
		}
		
		return this.sender.getUnhandled();
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
	synchronized public void start(Consumer<Envelope<Message>> handler) {
		if (this.isActive) { // already started - ignoring call
			return;
		}
		
		if (this.killed) {
			throw new RuntimeException("connection cannot be re-started after it was killed");
		}
		
		if (null == handler) {
			throw new IllegalArgumentException("handler cannot be null");
		}
		
		this.receiver.setHandler(x -> { sendAck(x.from); handler.accept(x); } ); // set upon each start, unlike sender
		
		if (!this.started) { // this block is only executed once
			this.receiver.startMe(); // start to take incoming messages from queue and handle them
			this.sender.setHandler(x -> safeSend(x)); // only set once, upon initial start
			this.sender.startMe();   // start to take outgoing messages from queue and send them one-by-one
			this.started = true;
		}
		
		startMessenger(myAddress); // will throw RuntimeException if a Messenger with same address is already active
		this.isActive = true;
		this.receiver.unpause();
		this.sender.unpause();
	}
	
	
	/**
	 * Stop (Pause) this Connection. <br>
	 * When stopped, this connection does not receive, handle or send anything, but can be re-started
	 * using the {@link Start} method.<br>
	 * <br>
	 * If the Connection was already stopped upon invocation, this does nothing.
	 */
	synchronized public void stop() {
		if (!this.isActive) { // already stopped - ignoring call
			return;
		}
		
		this.receiver.pause();
		this.sender.pause();
		killMessenger();
		this.isActive = false;
	}
	
	
	/**
	 * Terminate this connection. Stops all handling of incoming messages, receiving and sending messages,
	 * as well as killing its messenger.
	 * 
	 * <p>
	 * <b>Notice:</b><br>
	 * (1) A killed connection <b>cannot</b> be restarted, a different one has to be created instead. <br>
	 * (2) Any un-handled messages are retrievable via {@link #getUnhandled()} and {@link #getUnsent()} <br>
	 * (3) Calling this on an already killed connection is ignored. <br>  
	 * </p>
	 * 
	 * @see stop
	 */
	synchronized public void kill() {
		if (this.killed) {
			return; // ignore repeated calls
		}
		
		if (!this.started) {
			throw new RuntimeException("cannot kill the connection before starting it...");
		}
		
		this.receiver.kill();
		this.sender.kill();
		this.isActive = false;
		this.killed  = true;
		this.executor.shutdown();
		
		killMessenger(); // has to be the last call, because dispatechers depend on it
	}
	
	
	/**
	 * Get this Connection's address.
	 * 
	 * @return this Connection's own address.
	 */
	public String myAddress() {
		return this.myAddress;
	}
	
	
	/**
	 * Get the amount of time (in milliseconds) this connection waits to receive an ACK, before trying to re-send a message.
	 *  
	 * @return timeout in milliseconds for an ACK before trying to re-send a message.
	 */
	public long getAckTimeout() {
		return ACK_TIMEOUT_IN_MILLISECONDS;
	}


	/**
	 * Check if this Connection is active. i.e receiving and handling requests as well as sending out messages.
	 * <br><br>
	 * This will return 'false' if any of the following apply:
	 * <br>(1) The connection has not been started yet.
	 * <br>(2) The connection has been killed
	 * <br>(3) The connection has been started, but stopped. 
	 * 
	 * @return 'true' when this connection is receiving, handling and sending messages, 'false' otherwise.
	 */
	public boolean isAlive() {
		return this.isActive;
	}
	
	
	/**
	 * Check if this Connection has been killed.
	 * 
	 * @return 'true' if {@link #kill()} was invoked, 'false' otherwise.
	 */
	public boolean wasKilled() {
		return this.killed;
	}
	
	
	/**
	 * Check if this Connection has been started.
	 * 
	 * @return 'true' if {@link #kill()} was invoked, 'false' otherwise.
	 */
	public boolean wasStarted() {
		return this.started;
	}
	
}


