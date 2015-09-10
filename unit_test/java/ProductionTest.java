import static org.junit.Assert.assertTrue;
import nez.Parser;
import nez.lang.GrammarFile;
import nez.lang.NezGrammar2;

import org.junit.Test;


public class ProductionTest {

	@Test
	public void test() {
		GrammarFile ns = NezGrammar2.newGrammarFile();
		Parser p = ns.newGrammar("DIGIT");
		assertTrue(p.match("8"));
		assertTrue(!p.match("88"));
		assertTrue(!p.match("x"));
	}

}
