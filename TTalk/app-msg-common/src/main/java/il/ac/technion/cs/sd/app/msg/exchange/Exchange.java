package il.ac.technion.cs.sd.app.msg.exchange;

import il.ac.technion.cs.sd.app.msg.ExchangeVisitor;

/**
 * An exchange between the application server and client. 
 */
public interface Exchange {
	void accept(ExchangeVisitor v);
}
