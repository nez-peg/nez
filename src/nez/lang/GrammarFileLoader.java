package nez.lang;

import java.io.IOException;
import java.util.HashMap;

import nez.Grammar;
import nez.Parser;
import nez.Strategy;
import nez.ast.SourcePosition;
import nez.ast.Tree;
import nez.ast.TreeVisitor;
import nez.io.SourceContext;
import nez.main.Verbose;
import nez.peg.celery.Celery;
import nez.peg.dtd.DTDConverter;
import nez.util.ConsoleUtils;
import nez.util.ExtensionLoader;
import nez.util.StringUtils;

public abstract class GrammarFileLoader extends TreeVisitor {

	protected Grammar file;
	protected Strategy strategy;

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
		while (sc.hasUnconsumed()) {
			Tree<?> node = getLoaderParser().parseCommonTree(sc);
			if (node == null) {
				ConsoleUtils.exit(1, sc.getSyntaxErrorMessage());
			}
			parse(node);
		}
	}

	public void eval(Grammar g, String urn, int linenum, String text, Strategy strategy) {
		this.file = g;
		this.strategy = Strategy.nullCheck(strategy);

		SourceContext sc = SourceContext.newStringContext(urn, linenum, text);
		while (sc.hasUnconsumed()) {
			Tree<?> node = getLoaderParser().parseCommonTree(sc);
			if (node == null) {
				Verbose.println(sc.getSyntaxErrorMessage());
			}
			parse(node);
		}
	}

	public abstract Parser getLoaderParser();

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
		if (urn.endsWith(".dtd")) {
			return DTDConverter.loadGrammar(urn, strategy);
		}
		if (urn.endsWith(".celery")) {
			return Celery.loadGrammar(urn, strategy);
		}
		return loadGrammar(urn, strategy);
	}

}
