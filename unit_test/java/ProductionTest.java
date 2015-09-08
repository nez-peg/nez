import static org.junit.Assert.assertTrue;
import nez.Parser;
import nez.lang.GrammarFile;
import nez.lang.NezGrammar;

import org.junit.Test;


public class ProductionTest {

	@Test
	public void test() {
		GrammarFile ns = NezGrammar.newGrammarFile();
		Parser p = ns.newGrammar("DIGIT");
		assertTrue(p.match("8"));
		assertTrue(!p.match("88"));
		assertTrue(!p.match("x"));
	}

}
