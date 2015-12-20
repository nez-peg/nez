package nez.tool.peg;

import java.util.HashMap;

import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Nez;
import nez.lang.Production;
import nez.lang.expr.Cany;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Cset;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pand;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pempty;
import nez.lang.expr.Pfail;
import nez.lang.expr.Pnot;
import nez.lang.expr.Pone;
import nez.lang.expr.Poption;
import nez.lang.expr.Psequence;
import nez.lang.expr.Pzero;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tdetree;
import nez.lang.expr.Tlfold;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Xblock;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xif;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Xon;
import nez.lang.expr.Xsymbol;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
import nez.tool.parser.SourceGenerator;
import nez.util.ConsoleUtils;
import nez.util.FileBuilder;
import nez.util.StringUtils;

public abstract class GrammarTranslator extends Expression.Visitor implements SourceGenerator {
	protected Parser parser;
	protected ParserStrategy strategy;

	public GrammarTranslator() {
		this.file = null;
	}

	protected String path;
	protected FileBuilder file;

	@Override
	public final void init(Grammar g, Parser parser, String path) {
		this.strategy = parser.getParserStrategy();
		if (path == null) {
			this.file = new FileBuilder(null);
		} else {
			this.path = FileBuilder.extractFileName(path);
			String filename = FileBuilder.changeFileExtension(path, this.getFileExtension());
			this.file = new FileBuilder(filename);
			ConsoleUtils.println("generating " + filename + " ... ");
		}
	}

	protected abstract String getFileExtension();

	@Override
	public void generate() {
		generate(this.parser.getParserGrammar());
	}

	public void generate(Grammar g) {
		makeHeader(g);
		makeBody(g);
		makeFooter(g);
		file.writeNewLine();
		file.flush();
	}

	public void makeHeader(Grammar g) {
	}

	public void makeBody(Grammar g) {
		for (Production p : g) {
			visitProduction(g, p);
		}
	}

	public abstract void visitProduction(Grammar g, Production p);

	public void makeFooter(Grammar g) {
	}

	/* Name */

	HashMap<String, String> m = new HashMap<String, String>();

	protected String name(String s) {
		String name = m.get(s);
		if (name != null) {
			return name;
		}
		int loc = s.lastIndexOf(':');
		if (loc > 0) {
			name = s.substring(loc + 1).replace("!", "_").replace("-", "PEG");
		} else {
			name = s.replace("!", "_").replace("-", "PEG");
		}
		m.put(s, name);
		return name;
	}

	protected String name(Production p) {
		return name(p.getLocalName());
	}

	protected String unique(Expression e) {
		String key = e.toString() + " ";
		String unique = m.get(key);
		if (unique == null) {
			unique = "e" + m.size();
			m.put(key, unique);
		}
		return unique;
	}

	// Generator Macro

	protected String LineComment = "// ";
	protected String OpenClosure = "("; // C()
	protected String CloseClosure = ")"; // C()
	protected String ClosureDelim = ", "; // C()
	protected String BeginIndent = "{"; // Begin()
	protected String EndIndent = "}"; // End()

	@Deprecated
	protected GrammarTranslator inc() {
		file.incIndent();
		return this;
	}

	@Deprecated
	protected GrammarTranslator dec() {
		file.decIndent();
		return this;
	}

	public void pCommentLine(String line) {
		file.writeIndent(LineComment + line);
	}

	protected GrammarTranslator L(String line) {
		file.writeIndent(line);
		return this;
	}

	protected GrammarTranslator L() {
		file.writeIndent();
		return this;
	}

	protected GrammarTranslator W(String word) {
		file.write(word);
		return this;
	}

	protected GrammarTranslator Begin(String t) {
		W(t);
		file.incIndent();
		return this;
	}

	protected GrammarTranslator End(String t) {
		file.decIndent();
		if (t != null) {
			L(t);
		}
		return this;
	}

	protected GrammarTranslator C(String name, Expression e) {
		int c = 0;
		W(name).W(OpenClosure);
		for (Expression sub : e) {
			if (c > 0) {
				W(ClosureDelim);
			}
			visitExpression(sub);
			c++;
		}
		W(CloseClosure);
		return this;
	}

	protected GrammarTranslator C(String name, String first, Expression e) {
		W(name);
		W(OpenClosure);
		W(first);
		W(ClosureDelim);
		for (Expression sub : e) {
			visitExpression(sub);
		}
		W(CloseClosure);
		return this;
	}

	protected GrammarTranslator C(String name) {
		W(name);
		W(OpenClosure);
		W(CloseClosure);
		return this;
	}

	protected GrammarTranslator C(String name, String arg) {
		if (arg.length() > 1 && arg.startsWith("\"") && arg.endsWith("\"")) {
		} else {
			arg = StringUtils.quoteString('"', arg, '"');
		}
		W(name);
		W(OpenClosure);
		W(arg);
		W(CloseClosure);
		return this;
	}

	protected GrammarTranslator C(String name, int arg) {
		W(name);
		W(OpenClosure);
		W(String.valueOf(arg));
		W(CloseClosure);
		return this;
	}

	protected GrammarTranslator C(String name, boolean[] arg) {
		int cnt = 0;
		W(name);
		W(OpenClosure);
		for (int c = 0; c < arg.length; c++) {
			if (arg[c]) {
				if (cnt > 0) {
					W(ClosureDelim);
				}
				W(String.valueOf(c));
				cnt++;
			}
		}
		W(CloseClosure);
		return this;
	}

	protected final void visitExpression(Expression e) {
		e.visit(this, null);
	}

	public abstract void visitNonTerminal(NonTerminal p);

	public abstract void visitEmpty(Expression p);

	public abstract void visitFail(Expression p);

	public abstract void visitAny(Nez.Any p);

	public abstract void visitByte(Nez.Byte p);

	public abstract void visitByteset(Nez.Byteset p);

	public abstract void visitString(Nez.String p);

	public abstract void visitOption(Nez.Option p);

	public abstract void visitZeroMore(Nez.ZeroMore p);

	public abstract void visitOneMore(Nez.OneMore p);

	public abstract void visitAnd(Nez.And p);

	public abstract void visitNot(Nez.Not p);

	public abstract void visitPair(Nez.Pair p);

	public abstract void visitChoice(Nez.Choice p);

	// AST Construction
	public abstract void visitLink(Nez.Link p);

	public abstract void visitPreNew(Nez.PreNew p);

	public abstract void visitLeftFold(Nez.LeftFold p);

	public abstract void visitNew(Nez.New p);

	public abstract void visitTag(Nez.Tag p);

	public abstract void visitReplace(Nez.Replace p);

	public abstract void visitDetree(Nez.Detree p);

	// Symbol Tables
	public abstract void visitXblock(Xblock p);

	public abstract void visitXlocal(Xlocal p);

	public abstract void visitXdef(Xsymbol p);

	public abstract void visitXexists(Xexists p);

	public abstract void visitXmatch(Xmatch p);

	public abstract void visitXis(Xis p);

	public abstract void visitXif(Xif p);

	public abstract void visitXon(Xon p);

	public abstract void visitXindent(Xindent p);

	public final Object visit(Expression e) {
		return e.visit(this, null);
	}

	public void visitUndefined(Expression e) {
		// TODO Auto-generated method stub

	}

	@Override
	public final Object visitAny(Nez.Any p, Object a) {
		this.visitAny(p);
		return null;
	}

	@Override
	public final Object visitByte(Nez.Byte p, Object a) {
		this.visitByte(p);
		return null;
	}

	@Override
	public final Object visitByteset(Nez.Byteset p, Object a) {
		this.visitByteset(p);
		return null;
	}

	@Override
	public final Object visitString(Nez.String p, Object a) {
		this.visitString(p);
		return null;
	}

	@Override
	public final Object visitFail(Nez.Fail p, Object a) {
		this.visitFail(p);
		return null;
	}

	@Override
	public final Object visitOption(Nez.Option p, Object next) {
		this.visitOption(p);
		return null;
	}

	@Override
	public final Object visitZeroMore(Nez.ZeroMore p, Object next) {
		this.visitZeroMore(p);
		return null;
	}

	@Override
	public final Object visitOneMore(Nez.OneMore p, Object a) {
		this.visitOneMore(p);
		return null;
	}

	@Override
	public final Object visitAnd(Nez.And p, Object a) {
		this.visitAnd(p);
		return null;
	}

	@Override
	public final Object visitNot(Nez.Not p, Object a) {
		this.visitNot(p);
		return null;
	}

	@Override
	public final Object visitPair(Nez.Pair p, Object a) {
		this.visitPair(p);
		return null;
	}

	@Override
	public final Object visitChoice(Nez.Choice p, Object a) {
		this.visitChoice(p);
		return null;
	}

	@Override
	public final Object visitNonTerminal(NonTerminal p, Object a) {
		this.visitNonTerminal(p);
		return null;
	}

	// AST Construction

	@Override
	public final Object visitDetree(Nez.Detree p, Object a) {
		if (strategy.TreeConstruction) {
			this.visitDetree(p);
		} else {
			this.visitExpression(p.get(0));
		}
		return null;
	}

	@Override
	public final Object visitLink(Nez.Link p, Object a) {
		if (strategy.TreeConstruction) {
			this.visitLink(p);
		} else {
			this.visitExpression(p.get(0));
		}
		return null;
	}

	@Override
	public final Object visitPreNew(Nez.PreNew p, Object next) {
		if (strategy.TreeConstruction) {
			this.visitPreNew(p);
		}
		return null;
	}

	@Override
	public final Object visitLeftFold(Nez.LeftFold p, Object next) {
		if (strategy.TreeConstruction) {
			this.visitLeftFold(p);
		}
		return null;
	}

	@Override
	public final Object visitNew(Nez.New p, Object next) {
		if (strategy.TreeConstruction) {
			this.visitNew(p);
		}
		return null;
	}

	@Override
	public final Object visitTag(Nez.Tag p, Object next) {
		if (strategy.TreeConstruction) {
			this.visitTag(p);
		}
		return null;
	}

	@Override
	public final Object visitReplace(Nez.Replace p, Object next) {
		if (strategy.TreeConstruction) {
			this.visitReplace(p);
		}
		return null;
	}

	@Override
	public final Object visitXblock(Xblock p, Object a) {
		this.visitXblock(p);
		return null;
	}

	@Override
	public final Object visitXlocal(Xlocal p, Object a) {
		this.visitXlocal(p);
		return null;
	}

	@Override
	public final Object visitXdef(Xsymbol p, Object a) {
		this.visitXdef(p);
		return null;
	}

	@Override
	public final Object visitXexists(Xexists p, Object a) {
		this.visitXexists(p);
		return null;
	}

	@Override
	public final Object visitXmatch(Xmatch p, Object a) {
		this.visitXmatch(p);
		return null;
	}

	@Override
	public final Object visitXis(Xis p, Object a) {
		this.visitXis(p);
		return null;
	}

	@Override
	public final Object visitXindent(Xindent p, Object a) {
		this.visitXindent(p);
		return null;
	}

	@Override
	public final Object visitEmpty(Nez.Empty p, Object a) {
		this.visitEmpty(p);
		return null;
	}

	@Override
	public final Object visitXon(Xon p, Object a) {
		this.visitXon(p);
		return null;
	}

	@Override
	public final Object visitXif(Xif p, Object a) {
		this.visitXif(p);
		return null;
	}

}
