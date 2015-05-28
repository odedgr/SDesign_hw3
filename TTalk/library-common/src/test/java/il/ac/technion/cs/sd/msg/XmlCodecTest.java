package il.ac.technion.cs.sd.msg;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;



public class XmlCodecTest {
	
	@Test
	public void testEncodeDecodeInt() {
		Codec<Integer> codec = new XStreamCodec<Integer>();
		
		Integer x = 7;
		String xml = codec.encode(x);
		Integer $ = codec.decode(xml);
		
		assertEquals(x, $);
	}
	
	@Test
	public void testEncodeDecode() {
		AuxBase a = new AuxBase();
		a.s = "Ha";
		a.b = 4;
		a.c = 40.5123;
		a.list = Arrays.asList(1,2,3,4);
		
		Codec<AuxBase> codec = new XStreamCodec<AuxBase>();
		String xml = codec.encode(a);
		AuxBase $ = codec.decode(xml);
		
		System.out.println(xml);
		
		assertEquals(a.s, $.s);
		assertEquals(a.b, $.b);
		assertEquals(a.c, $.c, 0.0001);
		assertEquals(a.list, $.list);
	}
	
	@Test
	public void testPolymorhpicType() {
		AuxBase a = new AuxDerived();
		a.s = "Ha";
		a.b = 4;
		a.c = 40.5123;
		a.list = Arrays.asList(1,2,3,4);
		
		Codec<AuxBase> codec = new XStreamCodec<AuxBase>();
		
		String xml = codec.encode(a);
		AuxBase $ = codec.decode(xml);
		assertEquals(a.id(), $.id());
		assertEquals("DERIVED", $.id());
	}

}
