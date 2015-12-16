package nez.main.parser;

import java.util.HashMap;

import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Production;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
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
import nez.parser.ParserGrammar;
import nez.parser.ParserStrategy;
import nez.util.ConsoleUtils;
import nez.util.FileBuilder;
import nez.util.StringUtils;

public abstract class SourceGenerator extends Expression.Visitor {
	protected Parser parser;
	protected ParserStrategy strategy;

	public SourceGenerator() {
		this.file = null;
	}

	protected String path;
	protected FileBuilder file;

	public final void init(Grammar g, Parser parser, String path) {
		this.strategy = parser.getParserStrategy();
		if (path == null) {
			this.file = new FileBuilder(null);
		} else {
			this.path = FileBuilder.extractFileName(path);
			this.file = new FileBuilder(FileBuilder.changeFileExtension(path, this.getFileExtension()));
			ConsoleUtils.println("generating " + path + " ... ");
		}
	}

	protected abstract String getFileExtension();

	public void generate(ParserGrammar g) {
		makeHeader(g);
		makeBody(g);
		makeFooter(g);
		file.writeNewLine();
		file.flush();
	}

	public void makeHeader(ParserGrammar g) {
	}

	public void makeBody(ParserGrammar g) {
		for (Production p : g) {
			visitProduction(g, p);
		}
	}

	public abstract void visitProduction(Grammar g, Production p);

	public void makeFooter(ParserGrammar g) {
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
	protected SourceGenerator inc() {
		file.incIndent();
		return this;
	}

	@Deprecated
	protected SourceGenerator dec() {
		file.decIndent();
		return this;
	}

	public void pCommentLine(String line) {
		file.writeIndent(LineComment + line);
	}

	protected SourceGenerator L(String line) {
		file.writeIndent(line);
		return this;
	}

	protected SourceGenerator L() {
		file.writeIndent();
		return this;
	}

	protected SourceGenerator W(String word) {
		file.write(word);
		return this;
	}

	protected SourceGenerator Begin(String t) {
		W(t);
		file.incIndent();
		return this;
	}

	protected SourceGenerator End(String t) {
		file.decIndent();
		if (t != null) {
			L(t);
		}
		return this;
	}

	protected SourceGenerator C(String name, Expression e) {
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

	protected SourceGenerator C(String name, String first, Expression e) {
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

	protected SourceGenerator C(String name) {
		W(name);
		W(OpenClosure);
		W(CloseClosure);
		return this;
	}

	protected SourceGenerator C(String name, String arg) {
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

	protected SourceGenerator C(String name, int arg) {
		W(name);
		W(OpenClosure);
		W(String.valueOf(arg));
		W(CloseClosure);
		return this;
	}

	protected SourceGenerator C(String name, boolean[] arg) {
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

	public abstract void visitPempty(Expression p);

	public abstract void visitPfail(Expression p);

	public abstract void visitCany(Cany p);

	public abstract void visitCbyte(Cbyte p);

	public abstract void visitCset(Cset p);

	public abstract void visitCmulti(Cmulti p);

	public abstract void visitPoption(Poption p);

	public abstract void visitPzero(Pzero p);

	public abstract void visitPone(Pone p);

	public abstract void visitPand(Pand p);

	public abstract void visitPnot(Pnot p);

	public abstract void visitPsequence(Psequence p);

	public abstract void visitPchoice(Pchoice p);

	// AST Construction
	public abstract void visitTlink(Tlink p);

	public abstract void visitTnew(Tnew p);

	public abstract void visitTlfold(Tlfold p);

	public abstract void visitTcapture(Tcapture p);

	public abstract void visitTtag(Ttag p);

	public abstract void visitTreplace(Treplace p);

	public abstract void visitTdetree(Tdetree p);

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
	public final Object visitCany(Cany p, Object a) {
		this.visitCany(p);
		return null;
	}

	@Override
	public final Object visitCbyte(Cbyte p, Object a) {
		this.visitCbyte(p);
		return null;
	}

	@Override
	public final Object visitCset(Cset p, Object a) {
		this.visitCset(p);
		return null;
	}

	@Override
	public final Object visitCmulti(Cmulti p, Object a) {
		this.visitCmulti(p);
		return null;
	}

	@Override
	public final Object visitPfail(Pfail p, Object a) {
		this.visitPfail(p);
		return null;
	}

	@Override
	public final Object visitPoption(Poption p, Object next) {
		this.visitPoption(p);
		return null;
	}

	@Override
	public final Object visitPzero(Pzero p, Object next) {
		this.visitPzero(p);
		return null;
	}

	@Override
	public final Object visitPone(Pone p, Object a) {
		this.visitPone(p);
		return null;
	}

	@Override
	public final Object visitPand(Pand p, Object a) {
		this.visitPand(p);
		return null;
	}

	@Override
	public final Object visitPnot(Pnot p, Object a) {
		this.visitPnot(p);
		return null;
	}

	@Override
	public final Object visitPsequence(Psequence p, Object a) {
		this.visitPsequence(p);
		return null;
	}

	@Override
	public final Object visitPchoice(Pchoice p, Object a) {
		this.visitPchoice(p);
		return null;
	}

	@Override
	public final Object visitNonTerminal(NonTerminal p, Object a) {
		this.visitNonTerminal(p);
		return null;
	}

	// AST Construction

	@Override
	public final Object visitTdetree(Tdetree p, Object a) {
		if (strategy.TreeConstruction) {
			this.visitTdetree(p);
		} else {
			this.visitExpression(p.get(0));
		}
		return null;
	}

	@Override
	public final Object visitTlink(Tlink p, Object a) {
		if (strategy.TreeConstruction) {
			this.visitTlink(p);
		} else {
			this.visitExpression(p.get(0));
		}
		return null;
	}

	@Override
	public final Object visitTnew(Tnew p, Object next) {
		if (strategy.TreeConstruction) {
			this.visitTnew(p);
		}
		return null;
	}

	@Override
	public final Object visitTlfold(Tlfold p, Object next) {
		if (strategy.TreeConstruction) {
			this.visitTlfold(p);
		}
		return null;
	}

	@Override
	public final Object visitTcapture(Tcapture p, Object next) {
		if (strategy.TreeConstruction) {
			this.visitTcapture(p);
		}
		return null;
	}

	@Override
	public final Object visitTtag(Ttag p, Object next) {
		if (strategy.TreeConstruction) {
			this.visitTtag(p);
		}
		return null;
	}

	@Override
	public final Object visitTreplace(Treplace p, Object next) {
		if (strategy.TreeConstruction) {
			this.visitTreplace(p);
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
	public final Object visitPempty(Pempty p, Object a) {
		this.visitPempty(p);
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
