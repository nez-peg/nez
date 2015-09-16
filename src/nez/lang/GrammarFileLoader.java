package nez.lang;

import java.io.IOException;
import java.util.HashMap;

import nez.Grammar;
import nez.Strategy;
import nez.Parser;
import nez.ast.AbstractTree;
import nez.ast.AbstractTreeVisitor;
import nez.ast.Reporter;
import nez.ast.SourcePosition;
import nez.io.SourceContext;
import nez.main.Verbose;
import nez.peg.celery.Celery;
import nez.peg.dtd.DTDConverter;
import nez.util.ConsoleUtils;
import nez.util.ExtensionLoader;
import nez.util.StringUtils;

public abstract class GrammarFileLoader extends AbstractTreeVisitor {

	protected Grammar file;
	protected Strategy option;
	protected Reporter repo;

	public Grammar newGrammar(String ns, String urn) {
		return new GrammarFile(ns, urn, null);
	}

	public final Grammar getGrammar() {
		return this.file;
	}

	public final GrammarFile getGrammarFile() {
		/* file is instantiated at newGrammar */
		return (GrammarFile) this.file;
	}

	public final Strategy getGrammarOption() {
		return this.option;
	}

	public final void load(Grammar g, String urn, Strategy option, Reporter repo) throws IOException {
		this.file = g;
		this.option = Strategy.nullObject(option);
		this.repo = repo;

		SourceContext sc = SourceContext.newFileContext(urn);
		while (sc.hasUnconsumed()) {
			AbstractTree<?> node = getLoaderGrammar().parseCommonTree(sc);
			if (node == null) {
				ConsoleUtils.exit(1, sc.getSyntaxErrorMessage());
			}
			parse(node);
		}
	}

	public void eval(Grammar g, String urn, int linenum, String text, Strategy option, Reporter repo) {
		this.file = g;
		this.option = Strategy.nullObject(option);
		this.repo = repo;

		SourceContext sc = SourceContext.newStringSourceContext(urn, linenum, text);
		while (sc.hasUnconsumed()) {
			AbstractTree<?> node = getLoaderGrammar().parseCommonTree(sc);
			if (node == null) {
				Verbose.println(sc.getSyntaxErrorMessage());
			}
			parse(node);
		}
	}

	// @Deprecated
	// public final void load(String urn) throws IOException {
	// SourceContext sc = SourceContext.newFileContext(urn);
	// while (sc.hasUnconsumed()) {
	// AbstractTree<?> node = getLoaderGrammar().parseCommonTree(sc);
	// if (node == null) {
	// ConsoleUtils.exit(1, sc.getSyntaxErrorMessage());
	// }
	// parse(node);
	// }
	// }

	public abstract Parser getLoaderGrammar();

	public abstract void parse(AbstractTree<?> node);

	/* ------------------------------------------------------------------ */

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

	public static Grammar loadGrammar(String path, Strategy option, Reporter repo) throws IOException {
		String ext = StringUtils.parseFileExtension(path);
		GrammarFileLoader fl = (GrammarFileLoader) ExtensionLoader.newInstance("nez.ext.G", ext);
		if (fl != null) {
			Grammar g = fl.newGrammar(ext, path);
			fl.load(g, path, option, repo);
			return g;
		}
		return null;
	}

	public final static Grammar loadGrammarFile(String urn, Strategy option) throws IOException {
		if (urn.endsWith(".dtd")) {
			return DTDConverter.loadGrammar(urn, option);
		}
		if (urn.endsWith(".celery")) {
			return Celery.loadGrammar(urn, option);
		}
		return loadGrammar(urn, option, null);
	}

}
