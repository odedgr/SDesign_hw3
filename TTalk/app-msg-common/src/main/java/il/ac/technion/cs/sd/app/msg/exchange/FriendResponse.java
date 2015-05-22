package il.ac.technion.cs.sd.app.msg.exchange;

import il.ac.technion.cs.sd.app.msg.ExchangeVisitor;
import il.ac.technion.cs.sd.app.msg.FriendInvitation;

/**
 * A response to a friend request.
 */
public class FriendResponse implements Exchange {
	
	/**
	 * The friend invitation that has been responded in this 
	 */
	final public FriendInvitation invitation;
	/**
	 * The response (if there is any).
	 */
	final public boolean isAccepted;

	/**
	 * Create a new FriendResponse.
	 * @param invitation the invitation that is responded.
	 * @param isAccepted the response to the invitation.
	 */
	public FriendResponse(FriendInvitation invitation, boolean isAccepted) {
		this.invitation = invitation;
		this.isAccepted = isAccepted;
	}

	@Override
	public void accept(ExchangeVisitor v) {
		v.visit(this);

	}

}
