package il.ac.technion.cs.sd.msg;


/**
 * Wrapper object for messages being sent, received and handled by a {@link Connection} object.
 * 
 * <p>
 * An Envelope encapsulates a user-defined message object, attaching an address (of either source or destination),
 * to facilitate usage at server-side application.
 * </p>
 *
 * @param <Message> User-defined prototype of objects to be encapsulated by an Envelope/
 */
public class Envelope<Message> {

	// INSTANCE VARIABLES
	public final String address;
	public final Message content;
	
	/**
	 * Get an Envelope "wrapped" around a given message with an address.
	 * 
	 * @param addr Address to be attached to Envelope. Cannot be <b><code>null</code></b> or empty.
	 * @param content A non-null Message to be wrapped in the Envelope. Cannot be <b><code>null</code></b>.
	 * @return A new Envelope encapsulating the supplied message with the given address.
	 */
	public static <Message> Envelope<Message> wrap(String addr, Message content) {
		if (null == addr || null == content) {
			throw new IllegalArgumentException("address and payload can't be null");
		}
		
		if ("".equals(addr)) {
			throw new IllegalArgumentException("address can't be empty");
		}
		
		return new Envelope<Message>(addr, content);
	}
	
	/**
	 * C'tor for an Envelope. Intended for internal use only.<br><br> 
	 * You should probably use {@link wrap} instead.
	 */
	private Envelope(String addr, Message content) {
		this.address = addr;
		this.content = content;
	}
	
	/**
	 * Default C'tor for an Envelope. <b>This should NOT be used.</b> Use {@link wrap} instead.
	 */
	private Envelope() { // default constructor is blocked 
		this.address = null; 
		this.content = null;
	}
	
	
	@Override
	public String toString() {
		return "address = " + this.address + ", content = " + content.toString();
	}
}
