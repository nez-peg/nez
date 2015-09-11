package nez.lang;

import java.io.IOException;
import java.util.HashMap;

import nez.NezOption;
import nez.Parser;
import nez.SourceContext;
import nez.ast.AbstractTree;
import nez.ast.AbstractTreeVisitor;
import nez.ast.Reporter;
import nez.ast.SourcePosition;
import nez.util.ConsoleUtils;

public abstract class GrammarFileLoader extends AbstractTreeVisitor {

	private GrammarFile file; /* loaded */
	private Reporter repo;

	public GrammarFileLoader(GrammarFile file, Reporter repo) {
		this.file = file;
		this.repo = repo;
	}

	public final GrammarFile getGrammarFile() {
		return this.file;
	}

	public final NezOption getGrammarOption() {
		return this.file.getOption();
	}

	public abstract Parser getStartGrammar();

	public void eval(String urn, int linenum, String text) {
		SourceContext sc = SourceContext.newStringSourceContext(urn, linenum, text);
		while (sc.hasUnconsumed()) {
			AbstractTree<?> node = getStartGrammar().parseCommonTree(sc);
			if (node == null) {
				ConsoleUtils.println(sc.getSyntaxErrorMessage());
			}
			parse(node);
		}
	}

	public final void load(String urn) throws IOException {
		SourceContext sc = SourceContext.newFileContext(urn);
		while (sc.hasUnconsumed()) {
			AbstractTree<?> node = getStartGrammar().parseCommonTree(sc);
			if (node == null) {
				ConsoleUtils.exit(1, sc.getSyntaxErrorMessage());
			}
			parse(node);
		}
		file.verify();
	}

	public abstract void parse(AbstractTree<?> node);

	public final void reportError(SourcePosition s, String message) {
		if (this.repo != null) {
			this.repo.reportError(s, message);
		}
	}

	public final void reportWarning(SourcePosition s, String message) {
		if (this.repo != null) {
			this.repo.reportWarning(s, message);
		}
	}

	public final void reportNotice(SourcePosition s, String message) {
		if (this.repo != null) {
			this.repo.reportNotice(s, message);
		}
	}

	/* ------------------------------------------------------------------ */

	static HashMap<String, GrammarFileLoader> loaderMap = new HashMap<>();

}
