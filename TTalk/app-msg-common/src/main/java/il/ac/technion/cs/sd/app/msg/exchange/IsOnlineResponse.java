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
	final public Optional<Boolean> response;
	
	/**
	 * Create a new IsOnlineResponse.
	 * @param who the name of the client that is asked if online.
	 * @param response the response to the request.
	 */
	public IsOnlineResponse(String who, Optional<Boolean> response) {
		this.who = who;
		this.response = response;
	}

	@Override
	public void accept(ExchangeVisitor v) {
		v.visit(this);
	}

}
