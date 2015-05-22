package il.ac.technion.cs.sd.app.msg.exchange;

import il.ac.technion.cs.sd.app.msg.ExchangeVisitor;

public class IsOnlineRequest implements Exchange {
	
	/**
	 * The name of the client to ask about.
	 */
	final public String who;
	
	/**
	 * Create a new IsOnlineRequest.
	 * @param who the client that is being asked if online.
	 */
	public IsOnlineRequest(String who) {
		this.who = who;
	}

	@Override
	public void accept(ExchangeVisitor v) {
		v.visit(this);

	}

}
