package il.ac.technion.cs.sd.app.msg;


/**
 * A class that represents a friendship between two people;
 * Practically, an unordered pair of strings.
 */
public class Friendship {
	final private String friend1;
	final private String friend2;

	/**
	 * Create a new friendship object. The order of the parameters is not important.
	 * @param friend1 a name of one friend in this friendship.
	 * @param friend2 a name of another friend in this friendship.
	 */
	Friendship(String friend1, String friend2) {
		if (friend1 == null || friend2 == null) {
			throw new IllegalArgumentException();
		}
		if (friend1.compareTo(friend2) < 0) {			
			this.friend1 = friend1;
			this.friend2 = friend2;
		} else {
			this.friend1 = friend2;
			this.friend2 = friend1;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Friendship other = (Friendship) obj;
		if (friend1 == null) {
			if (other.friend1 != null)
				return false;
		} else if (!friend1.equals(other.friend1))
			return false;
		if (friend2 == null) {
			if (other.friend2 != null)
				return false;
		} else if (!friend2.equals(other.friend2))
			return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((friend1 == null) ? 0 : friend1.hashCode());
		result = prime * result + ((friend2 == null) ? 0 : friend2.hashCode());
		return result;
	}
}
