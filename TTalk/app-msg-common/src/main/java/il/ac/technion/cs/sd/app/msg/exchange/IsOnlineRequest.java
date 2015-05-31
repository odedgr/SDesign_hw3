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
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IsOnlineRequest other = (IsOnlineRequest)obj;
		
		if (who == null) {
			if (other.who != null)
				return false;
		} else if (!who.equals(other.who))
			return false;
		
		return true;
	}

}
