package il.ac.technion.cs.sd.msg;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


public class Connection<Message> {

	
	// CONSTANTS
	private static final String ACK = "";
	private static final long ACK_TIMEOUT_IN_MILLISECONDS = 50L;
	
	// INSTANCE VARIABLES
	private final Dispatcher<Message> receiver; // thread taking each incoming message from queue and dispatching a handler
	private final Dispatcher<Message> sender; // thread in charge of sending outgoing messages from queue
	private Messenger messenger; // can't be final, to support stop and restart
	private final Codec<Envelope<Message>> codec;
	private final MessengerFactory factory;
	
	private final String myAddress;
	private boolean gotAck = false;
	private final Object ackNotifier = new Object();
	
	/* Connection state */
	private boolean isActive = false; // set to 'true' upon each call to start(), 'false' upon stop() or kill()
	private boolean started = false; // set to 'true' upon first call to start()
	private boolean killed = false; // set to 'true' upon call to kill()
	
	private final ExecutorService executor; // thread pool, for message handlers using supplied consumer
	
	
	// TODO document
	public Connection(String myAddress, Codec<Envelope<Message>> codec, Consumer<Envelope<Message>> handler, MessengerFactory factory) {
		if (null == myAddress || "".equals(myAddress)) {
			throw new IllegalArgumentException("invalid server address - empty or null");
		}
		
		if (null == codec) {
			throw new IllegalArgumentException("got null codec");
		}
		
		this.factory   = (null != factory) ? factory : new MessengerFactory();
		this.myAddress = myAddress;
		this.executor  = Executors.newCachedThreadPool(); // TODO maybe get rid of this
		this.receiver  = new Dispatcher<Message>(x -> { sendAck(x.address); handler.accept(x);; } );
		this.sender    = new Dispatcher<Message>(x -> safeSend(x));
		this.codec     = codec;
	}
	
	
	// TODO document
	public Connection(String myAddress, Codec<Envelope<Message>> codec, Consumer<Envelope<Message>> handler) {
		this(myAddress, codec, handler, null);
	}

	
	// TODO document
	private Messenger startMessenger(String myAddress) {
		try {
			return this.factory.start(myAddress, x -> receiveIncomingMessage(x));
		} catch (MessengerException e) {
			System.out.println(e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
	
	// TODO document
		private void killMessenger() { // TODO document
			if (null != this.messenger) {
				try {
					this.messenger.kill();
				} catch (MessengerException e) {
					System.out.println("MessengerException when trying to kill messenger in Connection");
					throw new RuntimeException(e);
				}
				this.messenger = null;
			}
		}
	
	/**
	 * Add an outgoing message to a client (either with or without content) to the outgoing message queue. 
	 * For convenience, you might consider using the {@link #sendAck(String to) sendAck} instead. 
	 * 
	 * @param to - Address of client to whom the message will be sent.
	 * @param payload - Contents of message to send to the client.
	 * @see {@link #sendAck(String to)}
	 */
	protected void send(String to, Message payload) {
		if (null == to || "".equals(to)) {
			throw new IllegalArgumentException("recepient address was null or empty");
		}
		
		if (null == payload) {
			throw new IllegalArgumentException("payload to send was null");
		}
		
		if (!this.isActive) {
			throw new RuntimeException("cannot send when connection is inactive");
		}
		
		if (ACK.equals(payload)) { // send an ACK - no need to wait for incoming ACK in return
			sendAck(to);
			return;
		}
		
		// by here we know we are trying to send a message with contents - need to make sure it was received
		try {
			this.sender.enqueue(Envelope.wrap(to, payload));
		} catch (InterruptedException e) {
			System.out.println("interrupted while trying to enqueue envelope in sender");
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Do actual sending, with validation of arrival at the receiver side, re-sending periodically, until an ACK is received.
	 * 
	 * @param env Envelope to be sent.
	 */
	protected void safeSend(Envelope<Message> env) {
		this.gotAck = false;
		
		synchronized (this.ackNotifier) {
			do {
				try {
//					System.out.println("sending...");
					this.messenger.send(env.address, this.codec.encode(env));
					this.ackNotifier.wait(ACK_TIMEOUT_IN_MILLISECONDS);
				} catch (MessengerException e) {
					throw new RuntimeException(e);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} while (!this.gotAck);
		}
	}
	
	/**
	 * Sends an ACK (empty string) to a given address, guaranteed to be received by the recipient.
	 * 
	 * @param to - Address of the ACK receiver.
	 */
	public void sendAck(String to) {
		try {
			System.out.println("seding ACK");
			this.messenger.send(to, ACK);
		} catch (MessengerException e) {
			System.out.println("DEBUG - ServerConnection.sendAck() - MessengerException when tried to send ACK to " + to);
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * Handles a received ACK. Used to notify the transmitter thread that an awaited ACK has been received and it
	 * can move on to the next outgoing message.
	 */
	protected void receivedAck() {
		synchronized (ackNotifier) {
			this.gotAck = true;
			ackNotifier.notify();
		}
	}
	
	
	/**
	 * Receive a raw incoming message, and put it in a FIFO queue for appropriate handling. If incoming message is
	 * an ACK, than it is handled immediately, and not pushed into the incoming message queue. 
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
			return;
		}

		// add incoming message to queue for handling
		try {
			this.receiver.enqueue(codec.decode(inMsg));
		} catch (InterruptedException e) {
			System.out.println("interrupted while trying to put an incoming message in the incoming queue. ignoring it.");
		}
	}
	
	
	/**
	 * Get a COPY of all incoming Envelopes that were not yet handled (e.g: the incoming message queue).<br>
	 * <b>Can only be called when connection is inactive (e.g: after calling {@link #kill})</b>
	 * 
	 * @return
	 */
	public Collection<Envelope<Message>> getUnhandeled() {
		if (this.isActive) {
			throw new RuntimeException("can only be called when connection is inactive.");
		}
		
		return this.receiver.getUnhandled();
	}
	
	
	/**
	 * Get all the outgoing Envelopes that were not yet sent out.<br>
	 * <b>Can only be called when connection is inactive (e.g: after calling {@link #kill})</b>
	 * 
	 * @return
	 */
	public Collection<Envelope<Message>> getUnsent() {
		if (this.isActive) {
			throw new RuntimeException("can only be called when connection is inactive.");
		}
		
		return this.sender.getUnhandled();
	}
	
	
	/**
	 * Starts this Connection, enabling it to send and receive messages.
	 */
	synchronized public void start() {
		if (this.isActive) { // already started - ignoring call
			return;
		}
		
		if (this.killed) {
			throw new RuntimeException("can only start a connection that was has never started before, or was stopped, but not if it was killed");
		}
		
		if (!this.started) {
			this.receiver.start(); // start to take incoming messages from queue and handle them
			this.sender.start();   // start to take outgoing messages from queue and send them one-by-one
		}
		
		this.messenger = startMessenger(myAddress);
		this.started = true;
		this.isActive = true;
		this.receiver.unpause();
		this.sender.unpause();
	}
	
	
	// TODO document
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
	 */
	synchronized public void kill() {
		this.receiver.kill();
		this.sender.kill();
		this.isActive = false;
		this.killed  = true;
		this.executor.shutdown();
		
		killMessenger();
	}
	
	
	/**
	 * Get this Connection's address.
	 * 
	 * @return this Connection's own address.
	 */
	public String myAddress() {
		return this.myAddress;
	}
	
	
	// TODO document
	public long getAckTimeout() {
		return ACK_TIMEOUT_IN_MILLISECONDS;
	}


	// TODO document
	public boolean isAlive() {
		return this.isActive;
	}
	
}


