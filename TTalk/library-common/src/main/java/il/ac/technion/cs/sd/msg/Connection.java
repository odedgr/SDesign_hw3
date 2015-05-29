package il.ac.technion.cs.sd.msg;


import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public abstract class Connection<Message> {
	
	// CONSTANTS
	private static final String ACK = "";
	private static final long ACK_TIMEOUT_IN_MILLISECONDS = 50L;
	
	// INSTANCE VARIABLES
	private final Dispatcher<Message> receiver; // thread taking each incoming message from queue and dispatching a handler
	private final Dispatcher<Message> sender; // thread in charge of sending outgoing messages from queue
	private final Messenger messenger;
	private final Codec<Envelope<Message>> codec;
	
	private final String myAddress;
	private boolean gotAck = false;
	private boolean isActive = false;
	private final Object ackNotifier = new Object();
	
	private final ExecutorService executor; // thread pool, for message handlers using supplied consumer
	
	public Connection(String myAddress, Codec<Envelope<Message>> codec) {
		if (null == myAddress || "".equals(myAddress)) {
			throw new InvalidParameterException("invalid server address - empty or null");
		}
		
		if (null == codec) {
			throw new InvalidParameterException("got null codec");
		}
		
		try {
			messenger = new MessengerFactory().start(myAddress, x -> receiveIncomingMessage(x));
		} catch (MessengerException e) {
			System.out.println(e.getMessage());
			throw new RuntimeException(e);
		}
		
		this.myAddress = myAddress;
		this.executor  = Executors.newCachedThreadPool(); // TODO maybe get rid of this
		this.receiver  = new Dispatcher<Message>(x -> { sendAck(x.address); handleIncomingMessage(x); } );
		this.sender    = new Dispatcher<Message>(x -> safeSend(x));
		this.codec     = codec;
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
			throw new InvalidParameterException("recepient address was null or empty");
		}
		
		if (null == payload) {
			throw new InvalidParameterException("payload to send was null");
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
	 * Handle an incoming message, that is NOT an ACK (e.g: has actual contents).
	 * An ACK is implicitly sent back immediately to the sender of the message, and the message is handled using
	 * the consumer given to this ServerConnection upon initialization.
	 * 	
	 * @param env - Envelope containing the incoming message to be handled.
	 */
	protected abstract void handleIncomingMessage(Envelope<Message> env); // TODO update documentation
	
	/**
	 * Sends an ACK (empty string) to a given address, guaranteed to be received by the recipient.
	 * 
	 * @param to - Address of the ACK receiver.
	 */
	public void sendAck(String to) {
		try {
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
	 * Starts this ServerConnection, enabling it to send and receive messages.
	 */
	protected void start() {
		this.receiver.start(); // start to take incoming messages from queue and handle them
		this.sender.start();   // start to take outgoing messages from queue and send them one-by-one
		this.isActive = true;
	}
	
	/**
	 * Terminate this connection. Stops all handling of incoming messages, receiving and sending messages,
	 * as well as killing its messenger.
	 */
	public void kill() {
		this.receiver.kill();
		this.sender.kill();
		this.isActive = false;
		this.executor.shutdown();
		
		try {
			this.messenger.kill();
		} catch (MessengerException e) {
			System.out.println("MessengerException when trying to kill it from ServerConnection.kill()");
			throw new RuntimeException(e);
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
	
	public long getAckTimeout() {
		return ACK_TIMEOUT_IN_MILLISECONDS;
	}
	
}


