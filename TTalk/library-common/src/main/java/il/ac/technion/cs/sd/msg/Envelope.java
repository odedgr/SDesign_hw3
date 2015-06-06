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
	public final String from;
	public final String to;
	public final Message content;
	
	/**
	 * Get an Envelope "wrapped" around a given message with an address.
	 * 
	 * @param from - Address of message source to be attached to Envelope. Cannot be <b><code>null</code></b> or empty.
	 * @param to - Address of message destination to be attached to Envelope. Cannot be <b><code>null</code></b> or empty.
	 * @param content - A non-null Message to be wrapped in the Envelope. Cannot be <b><code>null</code></b>.
	 * @return A new Envelope encapsulating the supplied message with the given address.
	 */
	public static <Message> Envelope<Message> wrap(String from, String to, Message content) {
		if (null == from || null == to || null == content) {
			throw new IllegalArgumentException("address and payload can't be null");
		}
		
		if ("".equals(from) || "".equals(to)) {
			throw new IllegalArgumentException("address can't be empty");
		}
		
		return new Envelope<Message>(from, to, content);
	}
	
	/**
	 * C'tor for an Envelope. Intended for internal use only.<br><br> 
	 * You should probably use {@link wrap} instead.
	 */
	private Envelope(String from, String to, Message content) {
		this.from = from;
		this.to = to;
		this.content = content;
		
	}
	
	/**
	 * Default C'tor for an Envelope. <b>This should NOT be used.</b> Use {@link wrap} instead.
	 */
	private Envelope() { // default constructor is blocked 
		this.from = "nothing";
		this.to = "none";
		this.content = null;
	}
	
	
	@Override
	public String toString() {
		return "from = " + this.from + ", to = " + this.to + ", content = " + content.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("unchecked")
		Envelope<Message> other = (Envelope<Message>) obj;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;

		if (to == null) {
			if (other.to != null)
				return false;
		} else if (!to.equals(other.to))
			return false;

		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;

		return true;
	}
}
