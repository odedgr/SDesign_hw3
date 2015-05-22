package il.ac.technion.cs.sd.app.msg.exchange;

import il.ac.technion.cs.sd.app.msg.ExchangeVisitor;

/**
 * A request from the client to connect to the server.
 */
public class ConnectRequest implements Exchange {

	@Override
	public void accept(ExchangeVisitor v) {
		v.visit(this);
	}
}
