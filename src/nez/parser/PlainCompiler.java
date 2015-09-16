package nez.parser;

import nez.Strategy;
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
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;

public class PlainCompiler extends NezCompiler {

	protected final Instruction commonFailure = new IFail(null);

	public PlainCompiler(Strategy option) {
		super(option);
	}

	// encoding

	@Override
	public Instruction encode(Expression e, Instruction next, Instruction failjump) {
		return e.encode(this, next, failjump);
	}

	@Override
	public Instruction encodeCany(Cany p, Instruction next, Instruction failjump) {
		return new IAny(p, next);
	}

	@Override
	public Instruction encodeCbyte(Cbyte p, Instruction next, Instruction failjump) {
		return new IByte(p, next);
	}

	@Override
	public Instruction encodeCset(Cset p, Instruction next, Instruction failjump) {
		return new ISet(p, next);
	}

	@Override
	public Instruction encodeCmulti(Cmulti p, Instruction next, Instruction failjump) {
		return new IStr(p, next);
	}

	@Override
	public Instruction encodePfail(Expression p) {
		return this.commonFailure;
	}

	@Override
	public Instruction encodePoption(Poption p, Instruction next) {
		Instruction pop = new ISucc(p, next);
		return new IAlt(p, next, encode(p.get(0), pop, next));
	}

	@Override
	public Instruction encodePzero(Pzero p, Instruction next) {
		// Expression skip = p.possibleInfiniteLoop ? new ISkip(p) : new
		// ISkip(p);
		Instruction skip = new ISkip(p);
		Instruction start = encode(p.get(0), skip, next/* FIXME */);
		skip.next = start;
		return new IAlt(p, next, start);
	}

	@Override
	public Instruction encodePone(Pone p, Instruction next, Instruction failjump) {
		return encode(p.get(0), this.encodePzero(p, next), failjump);
	}

	@Override
	public Instruction encodePand(Pand p, Instruction next, Instruction failjump) {
		Instruction inner = encode(p.get(0), new IBack(p, next), failjump);
		return new IPos(p, inner);
	}

	@Override
	public Instruction encodePnot(Pnot p, Instruction next, Instruction failjump) {
		Instruction fail = new ISucc(p, new IFail(p));
		return new IAlt(p, next, encode(p.get(0), fail, failjump));
	}

	@Override
	public Instruction encodePsequence(Psequence p, Instruction next, Instruction failjump) {
		// return encode(p.get(0), encode(p.get(1), next, failjump), failjump);
		Instruction nextStart = next;
		for (int i = p.size() - 1; i >= 0; i--) {
			Expression e = p.get(i);
			nextStart = encode(e, nextStart, failjump);
		}
		return nextStart;
	}

	@Override
	public Instruction encodePchoice(Pchoice p, Instruction next, Instruction failjump) {
		Instruction nextChoice = encode(p.get(p.size() - 1), next, failjump);
		for (int i = p.size() - 2; i >= 0; i--) {
			Expression e = p.get(i);
			nextChoice = new IAlt(e, nextChoice, encode(e, new ISucc(e, next), nextChoice));
		}
		return nextChoice;
	}

	@Override
	public Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump) {
		Production r = p.getProduction();
		return new ICall(r, next);
	}

	// AST Construction

	@Override
	public Instruction encodeTlink(Tlink p, Instruction next, Instruction failjump) {
		if (this.strategyASTConstruction) {
			next = new ITPop(p, next);
			next = encode(p.get(0), next, failjump);
			return new ITPush(p, next);
		}
		return encode(p.get(0), next, failjump);
	}

	@Override
	public Instruction encodeTnew(Tnew p, Instruction next) {
		if (this.strategyASTConstruction) {
			return new INew(p, next);
		}
		return next;
	}

	@Override
	public Instruction encodeTlfold(Tlfold p, Instruction next) {
		if (this.strategyASTConstruction) {
			return new ITLeftFold(p, next);
		}
		return next;
	}

	@Override
	public Instruction encodeTcapture(Tcapture p, Instruction next) {
		if (this.strategyASTConstruction) {
			return new ICapture(p, next);
		}
		return next;
	}

	@Override
	public Instruction encodeTtag(Ttag p, Instruction next) {
		if (this.strategyASTConstruction) {
			return new ITag(p, next);
		}
		return next;
	}

	@Override
	public Instruction encodeTreplace(Treplace p, Instruction next) {
		if (this.strategyASTConstruction) {
			return new IReplace(p, next);
		}
		return next;
	}

	@Override
	public Instruction encodeXblock(Xblock p, Instruction next, Instruction failjump) {
		next = new IEndSymbolScope(p, next);
		next = encode(p.get(0), next, failjump);
		return new IBeginSymbolScope(p, next);
	}

	@Override
	public Instruction encodeXlocal(Xlocal p, Instruction next, Instruction failjump) {
		next = new IEndSymbolScope(p, next);
		next = encode(p.get(0), next, failjump);
		return new IBeginLocalScope(p, next);
	}

	@Override
	public Instruction encodeXdef(Xdef p, Instruction next, Instruction failjump) {
		return new IPos(p, encode(p.get(0), new IDefSymbol(p, next), failjump));
	}

	@Override
	public Instruction encodeXexists(Xexists p, Instruction next, Instruction failjump) {
		String symbol = p.getSymbol();
		if (symbol == null) {
			return new IExists(p, next);
		} else {
			return new IExistsSymbol(p, next);
		}
	}

	@Override
	public Instruction encodeXmatch(Xmatch p, Instruction next, Instruction failjump) {
		return new IMatch(p, next);
	}

	@Override
	public Instruction encodeXis(Xis p, Instruction next, Instruction failjump) {
		if (p.is) {
			return new IPos(p, encode(p.getSymbolExpression(), new IIsSymbol(p, next), failjump));
		} else {
			return new IPos(p, encode(p.getSymbolExpression(), new IIsaSymbol(p, next), failjump));
		}
	}

	@Override
	public Instruction encodeXdefindent(Xdefindent p, Instruction next, Instruction failjump) {
		return new IDefIndent(p, next);
	}

	@Override
	public Instruction encodeXindent(Xindent p, Instruction next, Instruction failjump) {
		return new IIsIndent(p, next);
	}

	@Override
	public Instruction encodeExtension(Expression p, Instruction next, Instruction failjump) {
		return next;
	}

	@Override
	public Instruction encodeTdetree(Tdetree p, Instruction next, Instruction failjump) {
		return next;
	}

}
