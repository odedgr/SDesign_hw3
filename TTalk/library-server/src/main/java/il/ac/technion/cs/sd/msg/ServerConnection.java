package il.ac.technion.cs.sd.msg;

import java.security.InvalidParameterException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import com.google.gson.Gson;

public class ServerConnection {

	// CONSTANTS
	private static final String ACK = "";
	
	// INSTANCE VARIABLES
	private final BlockingQueue<Envelope> incomingMessages = new LinkedBlockingQueue<>();
	private final BlockingQueue<Envelope> outgoingMessages = new LinkedBlockingQueue<>();
	private final String address;
	private final Messenger messenger;
	private final Consumer<String> consumer;
	private final ExecutorService executor; // thread pool, for message handlers using supplied consumer
	private final Thread receiver; // thread taking each incoming message from queue and dispatching a handler
	private final Thread sender; // thread in charge of sending outgoing messages from queue
	private final Object ackNotifier = new Object();
	private final Gson gson = new Gson();
	
	
	/**
	 * Constructor. Creates and starts a running connection, accepting and handling incoming messages as well as
	 * sending back outgoing replies.
	 * 
	 * @param address - This server's address.
	 * @param consumer - Application-defined function to handle incoming messages of type String. Will be applied for
	 * each incoming message (that is not an ACK). Different "types" of messages should be classified and handled
	 * accordingly on the application side.
	 */
	public ServerConnection(String address, Consumer<String> consumer) {
		if (null == address || "".equals(address)) {
			throw new InvalidParameterException("invalid server address - empty or null");
		}
		
		if (null == consumer) {
			throw new InvalidParameterException("got null consumer");
		}
		
		this.address = address;
		this.consumer = consumer;
		
		try {
			messenger = new MessengerFactory().start(address, x -> receiveIncomingMessage(x));
		} catch (MessengerException e) {
			System.out.println(e.getMessage());
			throw new RuntimeException(e);
		}
		
		this.executor = Executors.newCachedThreadPool();
		this.receiver = new Dispatcher(this.incomingMessages, x -> handleIncomingMessage(x));
		this.sender   = new Transmitter(this.outgoingMessages, (addr, msg) -> safeSend(addr, msg));
		
		start();
	}
	
	/**
	 * Do actual sending, with validation of arrival at the receiver side, re-sending periodically, until an ACK is received.
	 * 
	 * @param to
	 * @param payload
	 * @return
	 */
	private void safeSend(String to, String payload) {
		// TODO Auto-generated method stub
	}

	/**
	 * Handle an incoming message, that is NOT an ACK (e.g: has actual contents).
	 * An ACK is sent back immediately to the sender of the message, and the message is handled using
	 * the consumer sent to this ServerConnection upon initialization.
	 * 	
	 * @param inMsg
	 */
	private void handleIncomingMessage(String inMsg) { // TODO convert inMsg to Envelope using Gson ?
		// add incoming message to queue for handling and send ACK to sender
		String sender = getSenderOfMessage(inMsg);
		String message = getMessageContent(inMsg);
		sendAck(sender);
		
		// dispatch handling to a separate thread, which might add an outgoing message later, using the send() method
		executor.execute(() -> this.consumer.accept(message));
	}
	
	private String getMessageContent(String inMsg) {
		// TODO convert to envelope using Gson, and extract payload
		// TODO implement
		return "";
	}

	private String getSenderOfMessage(String inMsg) {
		// TODO convert to envelope using Gson, and extract sender
		// TODO implement
		return "";
	}

	/**
	 * Handles a received ACK. Used to notify the transmitter thread that an awaited ACK has been received and it
	 * can move on to the next outgoing message.
	 */
	private void receivedAck() {
		this.ackNotifier.notify();
	}

	/**
	 * Receive a raw incoming message, and put it in a FIFO queue for appropriate handling. If incoming message is
	 * an ACK, than it is handled immediately, and not pushed into the incoming message queue. 
	 *  
	 * @param inMsg - Raw incoming message, as received from messenger.
	 */
	private void receiveIncomingMessage(String inMsg) {
		if (null == inMsg) {
			throw new RuntimeException("Received a null incoming message");
		}

		if ("".equals(inMsg)) {
			receivedAck();
			return;
		}
		
		// TODO "convert" inMsg to Envelope using Gson ?
		try {
			String sender = getSenderOfMessage(inMsg);
			String payload = getMessageContent(inMsg);
			
			this.incomingMessages.put(Envelope.wrap(sender, payload));
		} catch (InterruptedException e) {
			System.out.println("interrupted while trying to put an incoming message in the incoming queue. ignoring it.");
		}
	}

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
	 * Starts this ServerConnection, enabling it to send and receive messages.
	 */
	private void start() {
		this.receiver.start(); // start to take incoming messages from queue and handle them
		this.sender.start();   // start to take outgoing messages from queue and send them one-by-one
	}
	
	/**
	 * Terminate this connection. Stops all handling of incoming messages, receiving and sending messages,
	 * as well as killing its messenger.
	 */
	public void kill() {
		// TODO maybe need to first clear out incoming and outgoing queues? or maybe app should take care of that
		((Dispatcher)this.receiver).stopMe();
		this.executor.shutdown(); // TODO consider using shutdownNow() or awaitTermination() instead
		((Transmitter)this.sender).stopMe();
		
		
		try {
			this.messenger.kill();
		} catch (MessengerException e) {
			System.out.println("MessengerException when trying to kill it from ServerConnection.kill()");
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get this ServerConnection's address.
	 * 
	 * @return this ServerConnection's address.
	 */
	public String address() {
		return this.address;
	}
	
	/**
	 * Add an outgoing message to a client (either with or without content) to the outgoing message queue. 
	 * For convenience, you might consider using the {@link #sendAck(String to) sendAck} instead. 
	 * 
	 * @param to - Address of client to whom the message will be sent.
	 * @param payload - Contents of message to send to the client.
	 * @see {@link #sendAck(String to)}
	 */
	public void send(String to, String payload) {
		if (null == to || "".equals(to)) {
			throw new InvalidParameterException("recepient address was null or empty");
		}
		
		if (null == payload) {
			throw new InvalidParameterException("payload to send was null");
		}
		
		if ("".equals(payload)) { // send an ACK - no need to wait for incoming ACK in return
			sendAck(to);
			return;
		}
		
		// by here we know we are trying to send a message with contents - need to make sure it was received
		outgoingMessages.add(Envelope.wrap(to, payload));
	}
}
