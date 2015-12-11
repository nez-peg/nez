package nez.parser;

import java.util.HashMap;

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
import nez.lang.expr.Xdefindent;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xif;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Xon;
import nez.lang.expr.Xsymbol;
import nez.parser.moz.MozInst;
import nez.util.Verbose;

public abstract class AbstractGenerator {
	protected ParserStrategy strategy;

	public AbstractGenerator(ParserStrategy strategy) {
		this.gg = null;
		this.setStrategy(strategy);
	}

	protected void setStrategy(ParserStrategy strategy) {
		this.strategy = strategy;
	}

	public final ParserStrategy getStrategy() {
		return this.strategy;
	}

	/* CodeMap */
	private ParserGrammar gg = null;

	protected void setGenerativeGrammar(ParserGrammar gg) {
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

	public abstract MozInst encode(Expression e, MozInst next, MozInst failjump);

	public abstract MozInst encodePfail(Expression p);

	public abstract MozInst encodeCany(Cany p, MozInst next, MozInst failjump);

	public abstract MozInst encodeCbyte(Cbyte p, MozInst next, MozInst failjump);

	public abstract MozInst encodeCset(Cset p, MozInst next, MozInst failjump);

	public abstract MozInst encodeCmulti(Cmulti p, MozInst next, MozInst failjump);

	public abstract MozInst encodePoption(Poption p, MozInst next);

	public abstract MozInst encodePzero(Pzero p, MozInst next);

	public abstract MozInst encodePone(Pone p, MozInst next, MozInst failjump);

	public abstract MozInst encodePand(Pand p, MozInst next, MozInst failjump);

	public abstract MozInst encodePnot(Pnot p, MozInst next, MozInst failjump);

	public abstract MozInst encodePsequence(Psequence p, MozInst next, MozInst failjump);

	public abstract MozInst encodePchoice(Pchoice p, MozInst next, MozInst failjump);

	public abstract MozInst encodeNonTerminal(NonTerminal p, MozInst next, MozInst failjump);

	// AST Construction
	public abstract MozInst encodeTnew(Tnew p, MozInst next);

	public abstract MozInst encodeTlfold(Tlfold p, MozInst next);

	public abstract MozInst encodeTlink(Tlink p, MozInst next, MozInst failjump);

	public abstract MozInst encodeTcapture(Tcapture p, MozInst next);

	public abstract MozInst encodeTtag(Ttag p, MozInst next);

	public abstract MozInst encodeTreplace(Treplace p, MozInst next);

	public abstract MozInst encodeTdetree(Tdetree p, MozInst next, MozInst failjump);

	// Symbol Tables
	public abstract MozInst encodeXblock(Xblock p, MozInst next, MozInst failjump);

	public abstract MozInst encodeXsymbol(Xsymbol p, MozInst next, MozInst failjump);

	public abstract MozInst encodeXmatch(Xmatch p, MozInst next, MozInst failjump);

	public abstract MozInst encodeXis(Xis p, MozInst next, MozInst failjump);

	public abstract MozInst encodeXdefindent(Xdefindent p, MozInst next, MozInst failjump);

	public abstract MozInst encodeXindent(Xindent p, MozInst next, MozInst failjump);

	public abstract MozInst encodeXexists(Xexists existsSymbol, MozInst next, MozInst failjump);

	public abstract MozInst encodeXlocal(Xlocal localTable, MozInst next, MozInst failjump);

	// Extension
	public abstract MozInst encodeExtension(Expression p, MozInst next, MozInst failjump);

	public MozInst encodePempty(Expression empty, MozInst next) {
		return next;
	}

	public MozInst encodeXon(Xon p, MozInst next, MozInst failjump) {
		return p.get(0).encode(this, next, failjump);
	}

	public MozInst encodeXif(Xif ifFlag, MozInst next, MozInst failjump) {
		return next;
	}

}
