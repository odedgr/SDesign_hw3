package il.ac.technion.cs.sd.app.msg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileDataSaverTest {
	
	FileDataSaver<String> fds_str;
	FileDataSaver<List<Integer>> fds_list;

	@Before
	public void setUp() throws Exception {
		fds_str = new FileDataSaver<String>("StringFileDataSaverTestFile");
		fds_list = new FileDataSaver<List<Integer>>("ListFileDataSaverTestFile");
	}

	@After
	public void tearDown() throws Exception {
		fds_str.clean();
		fds_list.clean();
	}

	@Test
	public void verifyEmptyAtFirst() {
		assertFalse(fds_str.load().isPresent());
	}
	
	@Test
	public void basicSaveLoad() {
		fds_str.save("Hello!");		
		assertTrue(fds_str.load().isPresent());
		assertEquals("Hello!", fds_str.load().get());
	}
	
	@Test
	public void saveAndReSave() {
		fds_str.save("Hello!");		
		assertTrue(fds_str.load().isPresent());
		assertEquals("Hello!", fds_str.load().get());
		fds_str.save("Hi");
		assertTrue(fds_str.load().isPresent());
		assertEquals("Hi", fds_str.load().get());
	}
	
	@Test
	public void saveAndCleanAndResave() {
		fds_str.save("Hello!");		
		fds_str.clean();
		fds_str.save("Bye");
		assertTrue(fds_str.load().isPresent());
		assertEquals("Bye", fds_str.load().get());
	}
	
	@Test
	public void testWithList() {
		assertFalse(fds_list.load().isPresent());
		assertFalse(fds_list.load().isPresent());
		ArrayList<Integer> arr = new ArrayList<Integer>();
		arr.add(1);
		arr.add(2);
		arr.add(3);
		fds_list.save(arr);		
		assertTrue(fds_list.load().isPresent());
		assertEquals(arr, fds_list.load().get());
		
		arr.add(200);
		fds_list.save(arr);		
		assertTrue(fds_list.load().isPresent());
		assertEquals(arr, fds_list.load().get());
		fds_list.clean();
		assertFalse(fds_list.load().isPresent());
	}
}
