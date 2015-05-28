package il.ac.technion.cs.sd.msg;

import java.beans.XMLEncoder;
import java.beans.XMLDecoder;
import java.io.*;

public class XmlCodec<T> implements Codec<T> {

	@Override
	public String encode(T obj) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		XMLEncoder encoder = new XMLEncoder(bos);
		encoder.writeObject(obj);
		encoder.close();
		return bos.toString();
	}

	@Override
	public T decode(String str) {
		ByteArrayInputStream bis = new ByteArrayInputStream(str.getBytes());
		XMLDecoder decoder = new XMLDecoder(bis);
		T obj = (T) decoder.readObject();
		decoder.close();
		return obj;
	}
}
