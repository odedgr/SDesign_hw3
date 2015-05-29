package il.ac.technion.cs.sd.msg;

import java.security.InvalidParameterException;

public class Envelope<Message> {

	public final String address;
	public final Message payload;
	
	public static <Message> Envelope<Message> wrap(String addr, Message payload) {
		if (null == addr || null == payload) {
			throw new InvalidParameterException("address and payload can't be null");
		}
		
		if ("".equals(addr)) {
			throw new InvalidParameterException("address can't be empty");
		}
		
		return new Envelope<Message>(addr, payload);
	}
	
	private Envelope(String addr, Message payload) {
		this.address = addr;
		this.payload = payload;
	}
	
	public boolean isEmptyMessage() {
		return "".equals(this.payload);
	}
	
	@Override
	public String toString() {
		
		return "address = " + this.address + ", payload = " + payload.toString();
	}
}
