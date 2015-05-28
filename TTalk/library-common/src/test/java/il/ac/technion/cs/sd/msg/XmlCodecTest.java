package il.ac.technion.cs.sd.msg;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;



public class XmlCodecTest {
	
//	@Test
//	public void testEncodeDecodeInt() {
//		Codec<Integer> codec = new XmlCodec<Integer>();
//		
//		Integer x = 7;
//		String xml = new XmlCodec<Integer>().encode(x);
//		Integer $ = codec.decode(xml);
//		
//		assertEquals(x, $);
//	}
	
	@Test
	public void testEncodeDecode() {
		AuxBase a = new AuxBase();
		a.s = "Ha";
		a.b = 4;
		a.c = 40.5123;
		a.list = Arrays.asList(1,2,3,4);
		
		Codec<AuxBase> codec = new XmlCodec<AuxBase>();
		String xml = new XmlCodec<AuxBase>().encode(a);
		AuxBase $ = codec.decode(xml);
		
		System.out.println(xml);
		
		assertEquals(a.s, $.s);
		assertEquals(a.b, $.b);
		assertEquals(a.c, $.c, 0.0001);
		assertEquals(a.list, $.list);
	}
	
//	@Test
//	public void testPolymorhpicType() {
//		A a = new B();
//		a.s = "Ha";
//		a.b = 4;
//		a.c = 40.5123;
//		a.list = Arrays.asList(1,2,3,4);
//		
//		String xml = new XmlCodec<A>().encode(a);
//		A $ = codec.decode(xml);
//		assertEquals(a.id(), $.id());
//		assertEquals("B", $.id());
//	}

}
