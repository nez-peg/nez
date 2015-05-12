import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;

import nez.NameSpace;

import org.junit.Test;


public class ExampleTest {

	@Test
	public void test() throws IOException, URISyntaxException {
		NameSpace g = NameSpace.newNameSpace("");
		assertTrue(g != null);
//		List<String> contents = Files.readAllLines(Paths.get(this.getClass().getResource("sample.txt").toURI()));
//		assertEquals(1, contents.size());
	}

}
