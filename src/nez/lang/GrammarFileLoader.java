package nez.lang;

import java.io.IOException;
import java.util.HashMap;

import nez.Grammar;
import nez.Parser;
import nez.Strategy;
import nez.Verbose;
import nez.ast.SourcePosition;
import nez.ast.Tree;
import nez.io.SourceContext;
import nez.lang.GrammarFileLoader.DefaultVisitor;
import nez.util.ConsoleUtils;
import nez.util.ExtensionLoader;
import nez.util.StringUtils;
import nez.util.VisitorMap;

public abstract class GrammarFileLoader extends VisitorMap<DefaultVisitor> {

	protected Grammar file;
	protected Strategy strategy;

	public GrammarFileLoader() {
	}

	public GrammarFileLoader(Class<?> baseClass) {
		init(baseClass, new DefaultVisitor());
	}

	public class DefaultVisitor {
		public void accept(Tree<?> node) {
		}

		public Expression toExpression(Tree<?> node) {
			return null;
		}

		public boolean parse(Tree<?> node) {
			return false;
		}
	}

	public Grammar newGrammar(String ns, String urn) {
		return new GrammarFile(ns, urn, null);
	}

	public final Grammar getGrammar() {
		return this.file;
	}

	public final GrammarFile getGrammarFile() {
		/* file is instantiated at newGrammar */
		if (this.file instanceof GrammarFile) {
			return (GrammarFile) this.file;
		}
		return null;
	}

	public final Strategy getStrategy() {
		return this.strategy;
	}

	public final void load(Grammar g, String urn, Strategy strategy) throws IOException {
		this.file = g;
		this.strategy = Strategy.nullCheck(strategy);

		SourceContext sc = SourceContext.newFileContext(urn);
		String desc = this.parseGrammarDescription(sc);
		if (desc != null) {
			g.setDesc(desc);
		}
		// while (sc.hasUnconsumed()) {
		Tree<?> node = getLoaderParser(null).parseCommonTree(sc);
		if (node == null) {
			ConsoleUtils.exit(1, sc.getSyntaxErrorMessage());
		}
		parse(node);
		// }
	}

	public void eval(Grammar g, String urn, int linenum, String text, Strategy strategy) {
		this.file = g;
		this.strategy = Strategy.nullCheck(strategy);

		SourceContext sc = SourceContext.newStringContext(urn, linenum, text);
		// while (sc.hasUnconsumed()) {
		Tree<?> node = getLoaderParser(null).parseCommonTree(sc);
		if (node == null) {
			Verbose.println(sc.getSyntaxErrorMessage());
		}
		parse(node);
		// }
	}

	public abstract Parser getLoaderParser(String start);

	public abstract void parse(Tree<?> node);

	public String parseGrammarDescription(SourceContext sc) {
		return null;
	}

	/* ------------------------------------------------------------------ */

	public final void reportError(SourcePosition s, String message) {
		if (this.strategy != null) {
			this.strategy.reportError(s, message);
		}
	}

	public final void reportWarning(SourcePosition s, String message) {
		if (this.strategy != null) {
			this.strategy.reportWarning(s, message);
		}
	}

	public final void reportNotice(SourcePosition s, String message) {
		if (this.strategy != null) {
			this.strategy.reportNotice(s, message);
		}
	}

	/* ------------------------------------------------------------------ */

	static HashMap<String, GrammarFileLoader> loaderMap = new HashMap<>();

	public static Grammar loadGrammar(String path, Strategy strategy) throws IOException {
		String ext = StringUtils.parseFileExtension(path);
		GrammarFileLoader fl = (GrammarFileLoader) ExtensionLoader.newInstance("nez.ext.G", ext);
		if (fl != null) {
			Grammar g = fl.newGrammar(ext, path);
			fl.load(g, path, strategy);
			return g;
		}
		return null;
	}

	public final static Grammar loadGrammarFile(String urn, Strategy strategy) throws IOException {
		return loadGrammar(urn, strategy);
	}

}
