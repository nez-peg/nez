import static org.junit.Assert.assertTrue;
import nez.Grammar;
import nez.Parser;
import nez.lang.NezGrammar1;

import org.junit.Test;

public class ProductionTest {

	@Test
	public void test() {
		Grammar g = new NezGrammar1();
		Parser p = g.newParser("DIGIT");
		assertTrue(p.match("8"));
		assertTrue(!p.match("88"));
		assertTrue(!p.match("x"));
	}

}
