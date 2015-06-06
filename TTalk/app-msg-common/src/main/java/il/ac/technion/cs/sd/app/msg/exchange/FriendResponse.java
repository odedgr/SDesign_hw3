package il.ac.technion.cs.sd.app.msg.exchange;

import java.util.Optional;

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
	final public Optional<Boolean> isAccepted;

	/**
	 * Create a new FriendResponse.
	 * @param invitation the invitation that is responded.
	 * @param isAccepted the response to the invitation.
	 */
	public FriendResponse(FriendInvitation invitation, Optional<Boolean> isAccepted) {
		this.invitation = invitation;
		this.isAccepted = isAccepted;
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
		FriendResponse other = (FriendResponse)obj;
		if (invitation == null) {
			if (other.invitation != null)
				return false;
		} else if (!invitation.equals(other.invitation))
			return false;
		if (isAccepted == null) {
			if (other.isAccepted != null)
				return false;
		} else if (!isAccepted.equals(other.isAccepted))
			return false;
		
		return true;
	}
}
