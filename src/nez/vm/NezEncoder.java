package nez.vm;

import java.util.HashMap;
import java.util.List;

import nez.Parser;
import nez.NezOption;
import nez.lang.Expression;
import nez.lang.GrammarOptimizer;
import nez.lang.Production;
import nez.lang.Typestate;
import nez.lang.expr.Uand;
import nez.lang.expr.Cany;
import nez.lang.expr.Xblock;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Xdefindent;
import nez.lang.expr.Xdef;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xif;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Tlink;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Tnew;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Unot;
import nez.lang.expr.Xon;
import nez.lang.expr.Uoption;
import nez.lang.expr.Uzero;
import nez.lang.expr.Uone;
import nez.lang.expr.Treplace;
import nez.lang.expr.Psequence;
import nez.lang.expr.Ttag;
import nez.main.Verbose;

public abstract class NezEncoder {
	protected NezOption option;

	public NezEncoder(NezOption option) {
		this.option = option;
	}

	public final NezOption getOption() {
		return this.option;
	}

	/* CodeMap */

	protected HashMap<String, ProductionCode> pcodeMap = null;

	protected ProductionCode newProductionCode(Production p, Expression localExpression) {
		return new ProductionCode(p, localExpression);
	}

	protected ProductionCode getCodePoint(Production p) {
		if (this.pcodeMap != null) {
			return pcodeMap.get(p.getUniqueName());
		}
		return null;
	}

	protected Expression optimizeLocalProduction(Production p) {
		return p.getExpression();
	}

	void count(Production p) {
		String uname = p.getUniqueName();
		ProductionCode c = this.pcodeMap.get(uname);
		if (c == null) {
			Expression deref = optimizeLocalProduction(p);
			// if (deref.isInterned()) {
			// String key = "#" + deref.getId();
			// c = this.pcodeMap.get(key);
			// if (c == null) {
			// c = newProductionCode(p, deref);
			// pcodeMap.put(key, c);
			// }
			// // else {
			// // Verbose.debug("alias " + uname + ", " +
			// // c.production.getUniqueName());
			// // }
			// pcodeMap.put(uname, c);
			// } else {
			c = newProductionCode(p, deref);
			pcodeMap.put(uname, c);
			// }
		}
		c.ref++;
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

	protected void initProductionCodeMap(Parser grammar, List<MemoPoint> memoPointList) {
		this.pcodeMap = new HashMap<String, ProductionCode>();
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
				ProductionCode cp = this.pcodeMap.get(p.getUniqueName());
				this.checkInlining(cp);
			}
		}
		if (memoPointList != null) {
			for (Production p : grammar.getProductionList()) {
				ProductionCode cp = this.pcodeMap.get(p.getUniqueName());
				this.checkMemoizing(cp, memoPointList);
			}
		}
	}

	protected void checkInlining(ProductionCode pcode) {
		if (pcode.ref == 1 || GrammarOptimizer.isSingleCharacter(pcode.localExpression)) {
			if (Verbose.PackratParsing) {
				Verbose.println("Inlining: " + pcode.getLocalName());
			}
			pcode.inlining = true;
		}
	}

	protected void checkMemoizing(ProductionCode pcode, List<MemoPoint> memoPointList) {
		if (pcode.inlining || pcode.memoPoint != null) {
			return;
		}
		Production p = pcode.production;
		if (pcode.ref > 1 && p.inferTypestate() != Typestate.OperationType) {
			int memoId = memoPointList.size();
			pcode.memoPoint = new MemoPoint(memoId, p.getLocalName(), pcode.localExpression, p.isContextual());
			memoPointList.add(pcode.memoPoint);
			if (Verbose.PackratParsing) {
				Verbose.println("MemoPoint: " + pcode.memoPoint + " ref=" + pcode.ref + " pure? " + p.isNoNTreeConstruction() + " rec? " + p.isRecursive());
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
