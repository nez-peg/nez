package nez.peg.celery;

import java.io.IOException;

import nez.NezException;
import nez.NezOption;
import nez.Parser;
import nez.SourceContext;
import nez.ast.CommonTree;
import nez.lang.GrammarFile;
import nez.util.ConsoleUtils;

public class Celery {
	static GrammarFile celeryGrammar = null;

	public final static GrammarFile loadGrammar(String filePath, NezOption option) throws IOException {
		option.setOption("notice", false);
		if (celeryGrammar == null) {
			try {
				celeryGrammar = GrammarFile.loadGrammarFile("celery.nez", NezOption.newDefaultOption());
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
		JSONCeleryConverter converter = new JSONCeleryConverter(!option.disabledNezExtension);
		converter.setRootClassName(filePath);
		GrammarFile gfile = GrammarFile.newGrammarFile(filePath, option);
		converter.convert(node, gfile);
		gfile.verify();
		return gfile;
	}
}
