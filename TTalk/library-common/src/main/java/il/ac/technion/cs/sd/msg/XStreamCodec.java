package il.ac.technion.cs.sd.msg;

import com.thoughtworks.xstream.XStream;

/**
 * XML Codec, using the XStream implementation, for encoding / decoding messages. 
 *
 * @param <T> Type of objects to be encoded / decoded.
 */
public class XStreamCodec<T> implements Codec<T> {

	private XStream xstream;
	
	/**
	 * Default constructor.
	 */
	public XStreamCodec() {
		this.xstream = new XStream();
	}
	
	/**
	 * Get the encoded T object as an encoded XML in a String object.
	 */
	@Override
	public String encode(T obj) {
		return this.xstream.toXML(obj);
	}

	/**
	 * Reconstruct an object of type T from an encoded XML in String form.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T decode(String xml) {
		return (T) this.xstream.fromXML(xml);
	}

}
