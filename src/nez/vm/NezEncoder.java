package nez.vm;

import java.util.HashMap;
import java.util.List;

import nez.NezOption;
import nez.Parser;
import nez.lang.Expression;
import nez.lang.GrammarOptimizer;
import nez.lang.Production;
import nez.lang.Typestate;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Cset;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Psequence;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Uand;
import nez.lang.expr.Unot;
import nez.lang.expr.Uone;
import nez.lang.expr.Uoption;
import nez.lang.expr.Uzero;
import nez.lang.expr.Xblock;
import nez.lang.expr.Xdef;
import nez.lang.expr.Xdefindent;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xif;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Xon;
import nez.main.Verbose;

public abstract class NezEncoder {
	protected NezOption option;

	public NezEncoder(NezOption option) {
		this.gg = null;
		this.option = option;
	}

	public final NezOption getOption() {
		return this.option;
	}

	/* CodeMap */
	private ParserGrammar gg = null;
	private HashMap<String, ParseFunc> funcMap = null;

	protected void setGenerativeGrammar(ParserGrammar gg) {
		this.gg = gg;
	}

	protected int getParseFuncSize() {
		if (gg != null) {
			return gg.size();
		}
		if (this.funcMap != null) {
			return funcMap.size();
		}
		return 0;
	}

	protected ParseFunc getParseFunc(Production p) {
		if (gg != null) {
			return gg.getParseFunc(p.getLocalName());
		}
		if (this.funcMap != null) {
			return funcMap.get(p.getUniqueName());
		}
		return null;
	}

	protected ParseFunc newParseFunc(Production p, Expression localExpression) {
		ParseFunc f = new ParseFunc(p.getLocalName(), p);
		f.setExpression(localExpression);
		return f;
	}

	void count(Production p) {
		String uname = p.getUniqueName();
		ParseFunc c = this.funcMap.get(uname);
		if (c == null) {
			Expression deref = p.getExpression();
			c = newParseFunc(p, deref);
			funcMap.put(uname, c);
		}
		c.refcount++;
	}

	void countNonTerminalReference(Expression e) {
		if (e instanceof NonTerminal) {
			Production p = ((NonTerminal) e).getProduction();
			count(p);
		}
		for (Expression sub : e) {
			countNonTerminalReference(sub);
		}
	}

	protected void initParseFuncMap(Parser grammar, List<MemoPoint> memoPointList) {
		this.funcMap = new HashMap<String, ParseFunc>();
		Production start = grammar.getStartProduction();
		count(start);
		countNonTerminalReference(start.getExpression());
		for (Production p : grammar.getProductionList()) {
			if (p != start) {
				this.countNonTerminalReference(p.getExpression());
			}
		}
		if (option.enabledInlining) {
			for (Production p : grammar.getProductionList()) {
				ParseFunc cp = this.funcMap.get(p.getUniqueName());
				this.checkInlining(cp);
			}
		}
		if (memoPointList != null) {
			for (Production p : grammar.getProductionList()) {
				ParseFunc cp = this.funcMap.get(p.getUniqueName());
				this.checkMemoizing(cp, memoPointList);
			}
		}
	}

	protected void checkInlining(ParseFunc pcode) {
		if (pcode.refcount == 1 || GrammarOptimizer.isSingleCharacter(pcode.e)) {
			if (Verbose.PackratParsing) {
				Verbose.println("Inlining: " + pcode.name);
			}
			pcode.inlining = true;
		}
	}

	protected void checkMemoizing(ParseFunc pcode, List<MemoPoint> memoPointList) {
		if (pcode.inlining || pcode.memoPoint != null) {
			return;
		}
		Production p = pcode.p;
		if (pcode.refcount > 1 && p.inferTypestate(null) != Typestate.OperationType) {
			int memoId = memoPointList.size();
			pcode.memoPoint = new MemoPoint(memoId, p.getLocalName(), pcode.e, p.isContextual());
			memoPointList.add(pcode.memoPoint);
			if (Verbose.PackratParsing) {
				Verbose.println("MemoPoint: " + pcode.memoPoint + " ref=" + pcode.refcount + " pure? " + p.isNoNTreeConstruction() + " rec? " + p.isRecursive());
			}
		}
	}

	// encoding

	// private Instruction failed = new IFail(null);

	public abstract Instruction encode(Expression e, Instruction next, Instruction failjump);

	public abstract Instruction encodePfail(Expression p);

	public abstract Instruction encodeCany(Cany p, Instruction next, Instruction failjump);

	public abstract Instruction encodeCbyte(Cbyte p, Instruction next, Instruction failjump);

	public abstract Instruction encodeCset(Cset p, Instruction next, Instruction failjump);

	public abstract Instruction encodeCmulti(Cmulti p, Instruction next, Instruction failjump);

	public abstract Instruction encodeUoption(Uoption p, Instruction next);

	public abstract Instruction encodeUzero(Uzero p, Instruction next);

	public abstract Instruction encodeUone(Uone p, Instruction next, Instruction failjump);

	public abstract Instruction encodeUand(Uand p, Instruction next, Instruction failjump);

	public abstract Instruction encodeUnot(Unot p, Instruction next, Instruction failjump);

	public abstract Instruction encodePsequence(Psequence p, Instruction next, Instruction failjump);

	public abstract Instruction encodePchoice(Pchoice p, Instruction next, Instruction failjump);

	public abstract Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump);

	// AST Construction
	public abstract Instruction encodeTlink(Tlink p, Instruction next, Instruction failjump);

	public abstract Instruction encodeTnew(Tnew p, Instruction next);

	public abstract Instruction encodeTcapture(Tcapture p, Instruction next);

	public abstract Instruction encodeTtag(Ttag p, Instruction next);

	public abstract Instruction encodeTreplace(Treplace p, Instruction next);

	// Symbol Tables
	public abstract Instruction encodeXblock(Xblock p, Instruction next, Instruction failjump);

	public abstract Instruction encodeXdef(Xdef p, Instruction next, Instruction failjump);

	public abstract Instruction encodeXmatch(Xmatch p, Instruction next, Instruction failjump);

	public abstract Instruction encodeXis(Xis p, Instruction next, Instruction failjump);

	public abstract Instruction encodeXdefindent(Xdefindent p, Instruction next, Instruction failjump);

	public abstract Instruction encodeXindent(Xindent p, Instruction next, Instruction failjump);

	public abstract Instruction encodeXexists(Xexists existsSymbol, Instruction next, Instruction failjump);

	public abstract Instruction encodeXlocal(Xlocal localTable, Instruction next, Instruction failjump);

	// Extension
	public abstract Instruction encodeExtension(Expression p, Instruction next, Instruction failjump);

	public Instruction encodeEmpty(Expression empty, Instruction next) {
		return next;
	}

	public Instruction encodeXon(Xon p, Instruction next, Instruction failjump) {
		return p.get(0).encode(this, next, failjump);
	}

	public Instruction encodeXif(Xif ifFlag, Instruction next, Instruction failjump) {
		return next;
	}

}
