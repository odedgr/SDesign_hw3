package il.ac.technion.cs.sd.app.msg.exchange;

import il.ac.technion.cs.sd.app.msg.ExchangeVisitor;
import il.ac.technion.cs.sd.app.msg.InstantMessage;

/**
 * A request to send an instant message from one client to another.
 */
public class SendInstantMessageRequest implements Exchange {
	
	public final InstantMessage message;
	
	public SendInstantMessageRequest(InstantMessage message) {
		this.message = message;
	}

	@Override
	public void accept(ExchangeVisitor v) {
		v.visit(this);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SendInstantMessageRequest other = (SendInstantMessageRequest)obj;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		
		return true;
	}

}
