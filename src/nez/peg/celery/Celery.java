package nez.peg.celery;

import java.io.IOException;

import nez.Grammar;
import nez.NezException;
import nez.Parser;
import nez.Strategy;
import nez.ast.CommonTree;
import nez.io.SourceContext;
import nez.lang.GrammarFile;
import nez.lang.GrammarFileLoader;
import nez.util.ConsoleUtils;

public class Celery {
	static Grammar celeryGrammar = null;

	public final static GrammarFile loadGrammar(String filePath, Strategy option) throws IOException {
		option.setEnabled("Wnotice", false);
		if (celeryGrammar == null) {
			try {
				celeryGrammar = GrammarFileLoader.loadGrammar("celery.nez", null);
			} catch (IOException e) {
				ConsoleUtils.exit(1, "can't load celery.nez");
			}
		}
		Parser p = celeryGrammar.newParser("File");
		SourceContext celeryFile = SourceContext.newFileContext(filePath);
		CommonTree node = p.parseCommonTree(celeryFile);
		if (node == null) {
			throw new NezException(celeryFile.getSyntaxErrorMessage());
		}
		if (celeryFile.hasUnconsumed()) {
			throw new NezException(celeryFile.getUnconsumedMessage());
		}
		JSONCeleryConverter converter = new JSONCeleryConverter(!option.isEnabled("peg", Strategy.PEG));
		converter.setRootClassName(filePath);
		GrammarFile gfile = GrammarFile.newGrammarFile(filePath, option);
		converter.convert(node, gfile);
		return gfile;
	}
}
