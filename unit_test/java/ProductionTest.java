import static org.junit.Assert.assertTrue;
import nez.NameSpace;
import nez.Grammar2;
import nez.expr.NezCombinator;

import org.junit.Test;


public class ProductionTest {

	@Test
	public void test() {
		NameSpace peg = NezCombinator.newGrammar();
		Grammar2 p = peg.newProduction("DIGIT");
		assertTrue(p.match("8"));
		assertTrue(!p.match("88"));
		assertTrue(!p.match("x"));
	}

}
