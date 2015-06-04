package il.ac.technion.cs.sd.msg;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MultiConnectionTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws InterruptedException {
		List<String> c1_in = new ArrayList<String>();
		List<String> c2_in = new ArrayList<String>();
		Connection<String> c1 = new Connection<String>("c1");
		Connection<String> c2 = new Connection<String>("c2");
		
		c1.start(x -> {
			System.out.println("c1 got message " + x.toString());
			c1_in.add(x.toString());
		});
		
		c2.start(x -> {
			System.out.println("c2 got message " + x.toString());
			c2_in.add(x.toString());
		});
		
		c1.send("c2", "hello");
		
		Thread.sleep(100L);
		
		c2.send("c1", "world");
		
		Thread.sleep(100L);
	}

}
