package il.ac.technion.cs.sd.app.msg.exchange;

import il.ac.technion.cs.sd.app.msg.ExchangeVisitor;
import il.ac.technion.cs.sd.app.msg.FriendInvitation;

/**
 * A request for sending a friend invitation from one client to another.
 */
public class FriendRequest implements Exchange {
	
	/**
	 * The friend invitation attached to this request.
	 */
	final public FriendInvitation invitation;
	
	/**
	 * Create a new friend request.
	 * @param invitation the friend invitation to attach to this request.
	 */
	public FriendRequest(FriendInvitation invitation) {
		this.invitation = invitation;
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
		FriendRequest other = (FriendRequest)obj;
		if (invitation == null) {
			if (other.invitation != null)
				return false;
		} else if (!invitation.equals(other.invitation))
			return false;
		return true;
	}
}
