package nez.junks;

import java.io.IOException;
import java.util.HashMap;

import nez.ast.Source;
import nez.ast.SourceLocation;
import nez.ast.Tree;
import nez.ast.TreeVisitorMap;
import nez.io.CommonSource;
import nez.junks.GrammarFileLoader.DefaultVisitor;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.schema.Type;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
import nez.util.ConsoleUtils;
import nez.util.ExtensionLoader;
import nez.util.FileBuilder;

public abstract class GrammarFileLoader extends TreeVisitorMap<DefaultVisitor> {

	protected Grammar file;
	protected ParserStrategy strategy;

	public GrammarFileLoader() {
	}

	protected class DefaultVisitor {
		public void accept(Tree<?> node) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted in GrammarFileLoader #" + node));
		}

		public Expression toExpression(Tree<?> node) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted in GrammarFileLoader #" + node));
			return null;
		}

		public Expression toExpression(Tree<?> node, Expression expr) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted in GrammarFileLoader #" + node));
			return null;
		}

		public Expression toExpression(Tree<?> node, Expression expr1, Expression expr2) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted in GrammarFileLoader #" + node));
			return null;
		}

		public boolean parse(Tree<?> node) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted in GrammarFileLoader #" + node));
			return false;
		}

		public Type toSchema(Tree<?> node) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted in GrammarFileLoader #" + node));
			return null;
		}
	}

	public Grammar newGrammar(String ns, String urn) {
		Grammar g = new Grammar(ns, null);
		g.setURN(urn);
		return g;
	}

	public final Grammar getGrammar() {
		return this.file;
	}

	// public final GrammarFile getGrammarFile() {
	// /* file is instantiated at newGrammar */
	// if (this.file instanceof GrammarFile) {
	// return (GrammarFile) this.file;
	// }
	// return null;
	// }

	public final ParserStrategy getStrategy() {
		return this.strategy;
	}

	public final void load(Grammar g, String urn, ParserStrategy strategy) throws IOException {
		this.file = g;
		this.strategy = ParserStrategy.nullCheck(strategy);

		Source sc = CommonSource.newFileSource(urn);
		String desc = this.parseGrammarDescription(sc);
		if (desc != null) {
			g.setDesc(desc);
		}
		Parser p = getLoaderParser(null);
		Tree<?> node = p.parse(sc);
		p.ensureNoErrors();
		parse(node);
	}

	public void eval(Grammar g, String urn, int linenum, String text, ParserStrategy strategy) {
		this.file = g;
		this.strategy = ParserStrategy.nullCheck(strategy);

		Source sc = CommonSource.newStringSource(urn, linenum, text);
		Parser p = getLoaderParser(null);
		Tree<?> node = p.parse(sc);
		if (node == null) {
			p.showErrors();
		}
		parse(node);
	}

	public abstract Parser getLoaderParser(String start);

	public abstract void parse(Tree<?> node);

	public String parseGrammarDescription(Source sc) {
		return null;
	}

	/* ------------------------------------------------------------------ */

	public final void reportError(SourceLocation s, String message) {
		if (this.strategy != null) {
			this.strategy.reportError(s, message);
		}
	}

	public final void reportWarning(SourceLocation s, String message) {
		if (this.strategy != null) {
			this.strategy.reportWarning(s, message);
		}
	}

	public final void reportNotice(SourceLocation s, String message) {
		if (this.strategy != null) {
			this.strategy.reportNotice(s, message);
		}
	}

	/* ------------------------------------------------------------------ */

	static HashMap<String, GrammarFileLoader> loaderMap = new HashMap<>();

	public static Grammar loadGrammar(String path, ParserStrategy strategy) throws IOException {
		String ext = FileBuilder.extractFileExtension(path);
		GrammarFileLoader fl = (GrammarFileLoader) ExtensionLoader.newInstance("nez.ext.G", ext);
		if (fl != null) {
			Grammar g = fl.newGrammar(ext, path);
			fl.load(g, path, strategy);
			return g;
		}
		return null;
	}

	public final static Grammar loadGrammarFile(String urn, ParserStrategy strategy) throws IOException {
		return loadGrammar(urn, strategy);
	}

}
