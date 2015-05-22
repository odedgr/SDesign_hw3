package il.ac.technion.cs.sd.msg;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class JsonCodecTest {
	
	static class A {
		String s;
		int b;
		double c;
		
		List<Integer> list;
		
		String id() {
			return "A";
		}
	}
	static class B extends A {
		@Override
		String id() {
			return "B";
		}
	}
	
	@Test
	public void testEncodeDecode() {
		A a = new A();
		a.s = "Ha";
		a.b = 4;
		a.c = 40.5123;
		a.list = Arrays.asList(1,2,3,4);
		
		String json = new JsonCodec<A>().encode(a);
		A $ = codec.decode(json);
		
		assertEquals(a.s, $.s);
		assertEquals(a.b, $.b);
		assertEquals(a.c, $.c, 0.0001);
		assertEquals(a.list, $.list);
	}
	
	 JsonCodec<A> codec = new JsonCodec<A>();
	
	@Test
	public void testPolymorhpicType() {
		A a = new B();
		a.s = "Ha";
		a.b = 4;
		a.c = 40.5123;
		a.list = Arrays.asList(1,2,3,4);
		
		String json = new JsonCodec<A>().encode(a);
		A $ = codec.decode(json);
		assertEquals(a.id(), $.id());
		assertEquals("B", $.id());
	}

}
