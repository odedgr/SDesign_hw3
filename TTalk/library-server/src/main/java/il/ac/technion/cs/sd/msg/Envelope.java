package il.ac.technion.cs.sd.msg;

import java.security.InvalidParameterException;

public class Envelope {

	public final String address;
	public final String payload;
	
	public static Envelope wrap(String addr, String payload) {
		if (null == addr || null == payload) {
			throw new InvalidParameterException("address and payload can't be null");
		}
		
		if ("".equals(addr)) {
			throw new InvalidParameterException("address can't be empty");
		}
		
		return new Envelope(new String(addr), new String(payload)); // defensive copying
	}
	
	private Envelope(String addr, String payload) {
		this.address = addr;
		this.payload = payload;
	}
	
	public boolean isAck() {
		return "".equals(this.payload);
	}
}
