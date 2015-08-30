package nez.lang;

import java.io.IOException;

import nez.SourceContext;
import nez.ast.AbstractTree;
import nez.ast.AbstractTreeVisitor;
import nez.ast.CommonTree;
import nez.ast.SourcePosition;
import nez.main.Verbose;
import nez.util.ConsoleUtils;

public abstract class GrammarLoader extends AbstractTreeVisitor {
	private GrammarFile file;
	
	public GrammarLoader(GrammarFile file) {
		this.file = file;
	}
	public final GrammarFile getGrammarFile() {
		return this.file;
	}
	
	public abstract Grammar getGrammar();
	
	public void eval(String urn, int linenum, String text) {
		SourceContext sc = SourceContext.newStringSourceContext(urn, linenum, text);
		while(sc.hasUnconsumed()) {
			AbstractTree<?> node = getGrammar().parseCommonTree(sc);
			if(node == null) {
				ConsoleUtils.println(sc.getSyntaxErrorMessage());
			}
			parse(node);
		}
	}
	
	public final void load(String urn) throws IOException {
		SourceContext sc = SourceContext.newFileContext(urn);
		while(sc.hasUnconsumed()) {
			AbstractTree<?> node = getGrammar().parseCommonTree(sc);
			if(node == null) {
				ConsoleUtils.exit(1, sc.getSyntaxErrorMessage());
			}
			parse(node);
		}
		file.verify();
	}

	public abstract void parse(AbstractTree<?> node);

	public final void reportError(SourcePosition s, String message) {
		this.file.reportError(s, message);
	}

	public final void reportWarning(SourcePosition s, String message) {
		this.file.reportWarning(s, message);
	}

	public final void reportNotice(SourcePosition s, String message) {
		this.file.reportNotice(s, message);
	}
}
