package il.ac.technion.cs.sd.msg;

import com.google.gson.Gson;

/**
 * A class for encoding/decoding a class to a Json string. This class also
 * encodes the type of the object passed, so it is safe to pass object that are
 * derived from the generic type T, and expect regular polymorphism behavior.
 * 
 * @param <T>
 */
public class JsonCodec<T> implements Codec<T> {
	
	static class ObjectWithType {
		public String object;
		public String type; 
	}
	
	static Gson gson = new Gson();

	@Override
	public String encode(T obj) {
		ObjectWithType owt = new ObjectWithType();
		owt.object = gson.toJson(obj);
		owt.type = obj.getClass().getName();
		return new Gson().toJson(owt);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T decode(String str) {
		ObjectWithType owt = gson.fromJson(str, ObjectWithType.class);
		try {
			Class<?> cls = Class.forName(owt.type);
			return (T) new Gson().fromJson(owt.object, cls);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

}
