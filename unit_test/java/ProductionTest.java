import static org.junit.Assert.assertTrue;
import nez.lang.Grammar;
import nez.lang.GrammarFile;
import nez.lang.NezGrammar;

import org.junit.Test;


public class ProductionTest {

	@Test
	public void test() {
		GrammarFile ns = NezGrammar.newGrammarFile();
		Grammar p = ns.newGrammar("DIGIT");
		assertTrue(p.match("8"));
		assertTrue(!p.match("88"));
		assertTrue(!p.match("x"));
	}

}
