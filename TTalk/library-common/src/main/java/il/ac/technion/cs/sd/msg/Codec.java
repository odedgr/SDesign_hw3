package il.ac.technion.cs.sd.msg;

/**
 * A class for encoding/decoding an object into a string.
 * @param <T> The object type to encode/decode.
 */
public interface Codec<T> {
	/**
	 * Encodes an object into a Json string.
	 * @param obj the object to encode.
	 */
	public String encode(T obj);
	
	/**
	 * Decode a Json string into a concrete Java object.
	 * @param str the string to decode.
	 */
	public T decode(String str);
}
