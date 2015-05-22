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

}
