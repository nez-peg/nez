import static org.junit.Assert.assertTrue;
import nez.lang.Grammar;
import nez.lang.NameSpace;
import nez.lang.NezCombinator;

import org.junit.Test;


public class ProductionTest {

	@Test
	public void test() {
		NameSpace ns = NezCombinator.newNameSpace();
		Grammar p = ns.newGrammar("DIGIT");
		assertTrue(p.match("8"));
		assertTrue(!p.match("88"));
		assertTrue(!p.match("x"));
	}

}
