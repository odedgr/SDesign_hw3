package il.ac.technion.cs.sd.app.msg.exchange;

import java.util.Optional;

import il.ac.technion.cs.sd.app.msg.ExchangeVisitor;

/**
 * Response for IsOnline request.
 */
public class IsOnlineResponse implements Exchange {
	
	/**
	 * The name of the client that is asked if online.
	 */
	final public String who;
	/**
	 * The response whether the user is online (or empty if the requesting user is not a friend). 
	 */
	final public Optional<Boolean> answer;
	
	/**
	 * Create a new IsOnlineResponse.
	 * @param who the name of the client that is asked if online.
	 * @param answer the answer to the request.
	 */
	public IsOnlineResponse(String who, Optional<Boolean> answer) {
		this.who = who;
		this.answer = answer;
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
		IsOnlineResponse other = (IsOnlineResponse)obj;
		
		if (who == null) {
			if (other.who != null)
				return false;
		} else if (!who.equals(other.who))
			return false;
		
		if (answer == null) {
			if (other.answer != null)
				return false;
		} else if (!answer.equals(other.answer))
			return false;
		
		return true;
	}
}
