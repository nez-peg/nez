package nez.lang;

import java.io.IOException;
import java.util.HashMap;

import nez.ast.Source;
import nez.ast.SourcePosition;
import nez.ast.Tree;
import nez.io.SourceStream;
import nez.lang.GrammarFileLoader.DefaultVisitor;
import nez.lang.schema.Type;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
import nez.util.ConsoleUtils;
import nez.util.ExtensionLoader;
import nez.util.StringUtils;
import nez.util.VisitorMap;

public abstract class GrammarFileLoader extends VisitorMap<DefaultVisitor> {

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

	public final ParserStrategy getStrategy() {
		return this.strategy;
	}

	public final void load(Grammar g, String urn, ParserStrategy strategy) throws IOException {
		this.file = g;
		this.strategy = ParserStrategy.nullCheck(strategy);

		SourceStream sc = SourceStream.newFileContext(urn);
		String desc = this.parseGrammarDescription(sc);
		if (desc != null) {
			g.setDesc(desc);
		}
		Parser p = getLoaderParser(null);
		Tree<?> node = p.parseCommonTree(sc);
		p.ensureNoErrors();
		parse(node);
	}

	public void eval(Grammar g, String urn, int linenum, String text, ParserStrategy strategy) {
		this.file = g;
		this.strategy = ParserStrategy.nullCheck(strategy);

		SourceStream sc = SourceStream.newStringContext(urn, linenum, text);
		Parser p = getLoaderParser(null);
		Tree<?> node = p.parseCommonTree(sc);
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

	public static Grammar loadGrammar(String path, ParserStrategy strategy) throws IOException {
		String ext = StringUtils.parseFileExtension(path);
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
