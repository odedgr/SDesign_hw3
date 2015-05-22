package il.ac.technion.cs.sd.msg;

public class RunThisFirst {

	public static void main(String[] args) throws Exception {
		Messenger m = new MessengerFactory().start("a", System.out::println);
		System.out.println("Listening... press enter to exit");
		System.in.read();
		m.kill();
	}

}
