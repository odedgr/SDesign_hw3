package il.ac.technion.cs.sd.app.msg.exchange;

import java.util.List;

import il.ac.technion.cs.sd.app.msg.ExchangeVisitor;

public class ExchangeList implements Exchange {
	
	/**
	 * The list of exchanges passed on this exchange.
	 */
	public final List<Exchange> list;
	
	/**
	 * Create a new ExchangeList
	 * @param list The list of exchanges passed on this exchange.
	 */
	public ExchangeList(List<Exchange> list) {
		this.list = list;
	}

	@Override
	public void accept(ExchangeVisitor v) {
		v.visit(this);
	}

}
