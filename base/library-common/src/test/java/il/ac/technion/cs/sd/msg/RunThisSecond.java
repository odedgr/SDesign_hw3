package il.ac.technion.cs.sd.msg;

import java.util.Date;

public class RunThisSecond {
	public static void main(String[] args) throws Exception {
		Messenger m = new MessengerFactory().start("b", x -> {});
		m.send("a", ("The time is now: " + new Date()));
		m.kill();
	}

}