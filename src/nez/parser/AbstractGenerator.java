package nez.parser;

import java.util.HashMap;

import nez.NezOption;
import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Cset;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pand;
import nez.lang.expr.Pchoice;
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

public abstract class AbstractGenerator {
	protected NezOption option;

	public AbstractGenerator(NezOption option) {
		this.gg = null;
		this.option = option;
	}

	public final NezOption getOption() {
		return this.option;
	}

	/* CodeMap */
	private GenerativeGrammar gg = null;

	protected void setGenerativeGrammar(GenerativeGrammar gg) {
		this.gg = gg;
	}

	private HashMap<String, ParseFunc> funcMap = null;

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
			ParseFunc f = gg.getParseFunc(p.getLocalName());
			if (f == null) {
				f = gg.getParseFunc(p.getUniqueName());
			}
			if (f == null) {
				Verbose.debug("unfound parsefunc: " + p.getLocalName() + " " + p.getUniqueName());
			}
			return f;
		}
		if (this.funcMap != null) {
			return funcMap.get(p.getUniqueName());
		}
		return null;
	}

	// encoding

	public abstract Instruction encode(Expression e, Instruction next, Instruction failjump);

	public abstract Instruction encodePfail(Expression p);

	public abstract Instruction encodeCany(Cany p, Instruction next, Instruction failjump);

	public abstract Instruction encodeCbyte(Cbyte p, Instruction next, Instruction failjump);

	public abstract Instruction encodeCset(Cset p, Instruction next, Instruction failjump);

	public abstract Instruction encodeCmulti(Cmulti p, Instruction next, Instruction failjump);

	public abstract Instruction encodePoption(Poption p, Instruction next);

	public abstract Instruction encodePzero(Pzero p, Instruction next);

	public abstract Instruction encodePone(Pone p, Instruction next, Instruction failjump);

	public abstract Instruction encodePand(Pand p, Instruction next, Instruction failjump);

	public abstract Instruction encodePnot(Pnot p, Instruction next, Instruction failjump);

	public abstract Instruction encodePsequence(Psequence p, Instruction next, Instruction failjump);

	public abstract Instruction encodePchoice(Pchoice p, Instruction next, Instruction failjump);

	public abstract Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump);

	// AST Construction
	public abstract Instruction encodeTnew(Tnew p, Instruction next);

	public abstract Instruction encodeTlfold(Tlfold p, Instruction next);

	public abstract Instruction encodeTlink(Tlink p, Instruction next, Instruction failjump);

	public abstract Instruction encodeTcapture(Tcapture p, Instruction next);

	public abstract Instruction encodeTtag(Ttag p, Instruction next);

	public abstract Instruction encodeTreplace(Treplace p, Instruction next);

	public abstract Instruction encodeTdetree(Tdetree p, Instruction next, Instruction failjump);

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

	public Instruction encodeTempty(Expression empty, Instruction next) {
		return next;
	}

	public Instruction encodeXon(Xon p, Instruction next, Instruction failjump) {
		return p.get(0).encode(this, next, failjump);
	}

	public Instruction encodeXif(Xif ifFlag, Instruction next, Instruction failjump) {
		return next;
	}

}
