package nez.vm;

import nez.NezOption;
import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.expr.Pand;
import nez.lang.expr.Cany;
import nez.lang.expr.Xblock;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Xdefindent;
import nez.lang.expr.Xdef;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Tlink;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Tnew;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pnot;
import nez.lang.expr.Poption;
import nez.lang.expr.Pzero;
import nez.lang.expr.Pone;
import nez.lang.expr.Treplace;
import nez.lang.expr.Psequence;
import nez.lang.expr.Ttag;

public class PlainCompiler extends NezCompiler {

	protected final Instruction commonFailure = new IFail(null);

	public PlainCompiler(NezOption option) {
		super(option);
	}

	// encoding

	public Instruction encode(Expression e, Instruction next, Instruction failjump) {
		return e.encode(this, next, failjump);
	}

	public Instruction encodeCany(Cany p, Instruction next, Instruction failjump) {
		return new IAny(p, next);
	}

	public Instruction encodeCbyte(Cbyte p, Instruction next, Instruction failjump) {
		return new IByte(p, next);
	}

	public Instruction encodeCset(Cset p, Instruction next, Instruction failjump) {
		return new ISet(p, next);
	}

	@Override
	public Instruction encodeCmulti(Cmulti p, Instruction next, Instruction failjump) {
		return new IStr(p, next);
	}

	public Instruction encodePfail(Expression p) {
		return this.commonFailure;
	}

	public Instruction encodePoption(Poption p, Instruction next) {
		Instruction pop = new ISucc(p, next);
		return new IAlt(p, next, encode(p.get(0), pop, next));
	}

	public Instruction encodePzero(Pzero p, Instruction next) {
		// Expression skip = p.possibleInfiniteLoop ? new ISkip(p) : new
		// ISkip(p);
		Instruction skip = new ISkip(p);
		Instruction start = encode(p.get(0), skip, next/* FIXME */);
		skip.next = start;
		return new IAlt(p, next, start);
	}

	public Instruction encodePone(Pone p, Instruction next, Instruction failjump) {
		return encode(p.get(0), this.encodePzero(p, next), failjump);
	}

	public Instruction encodePand(Pand p, Instruction next, Instruction failjump) {
		Instruction inner = encode(p.get(0), new IBack(p, next), failjump);
		return new IPos(p, inner);
	}

	public Instruction encodePnot(Pnot p, Instruction next, Instruction failjump) {
		Instruction fail = new ISucc(p, new IFail(p));
		return new IAlt(p, next, encode(p.get(0), fail, failjump));
	}

	public Instruction encodePsequence(Psequence p, Instruction next, Instruction failjump) {
		// return encode(p.get(0), encode(p.get(1), next, failjump), failjump);
		Instruction nextStart = next;
		for (int i = p.size() - 1; i >= 0; i--) {
			Expression e = p.get(i);
			nextStart = encode(e, nextStart, failjump);
		}
		return nextStart;
	}

	public Instruction encodePchoice(Pchoice p, Instruction next, Instruction failjump) {
		Instruction nextChoice = encode(p.get(p.size() - 1), next, failjump);
		for (int i = p.size() - 2; i >= 0; i--) {
			Expression e = p.get(i);
			nextChoice = new IAlt(e, nextChoice, encode(e, new ISucc(e, next), nextChoice));
		}
		return nextChoice;
	}

	public Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump) {
		Production r = p.getProduction();
		return new ICall(r, next);
	}

	// AST Construction

	public Instruction encodeTlink(Tlink p, Instruction next, Instruction failjump) {
		if (this.option.enabledASTConstruction) {
			next = new ITPop(p, next);
			next = encode(p.get(0), next, failjump);
			return new ITPush(p, next);
		}
		return encode(p.get(0), next, failjump);
	}

	public Instruction encodeTnew(Tnew p, Instruction next) {
		if (this.option.enabledASTConstruction) {
			return p.leftFold ? new ITLeftFold(p, next) : new INew(p, next);
		}
		return next;
	}

	public Instruction encodeTcapture(Tcapture p, Instruction next) {
		if (this.option.enabledASTConstruction) {
			return new ICapture(p, next);
		}
		return next;
	}

	public Instruction encodeTtag(Ttag p, Instruction next) {
		if (this.option.enabledASTConstruction) {
			return new ITag(p, next);
		}
		return next;
	}

	public Instruction encodeTreplace(Treplace p, Instruction next) {
		if (this.option.enabledASTConstruction) {
			return new IReplace(p, next);
		}
		return next;
	}

	public Instruction encodeXblock(Xblock p, Instruction next, Instruction failjump) {
		next = new IEndSymbolScope(p, next);
		next = encode(p.get(0), next, failjump);
		return new IBeginSymbolScope(p, next);
	}

	public Instruction encodeXlocal(Xlocal p, Instruction next, Instruction failjump) {
		next = new IEndSymbolScope(p, next);
		next = encode(p.get(0), next, failjump);
		return new IBeginLocalScope(p, next);
	}

	public Instruction encodeXdef(Xdef p, Instruction next, Instruction failjump) {
		return new IPos(p, encode(p.get(0), new IDefSymbol(p, next), failjump));
	}

	public Instruction encodeXexists(Xexists p, Instruction next, Instruction failjump) {
		String symbol = p.getSymbol();
		if (symbol == null) {
			return new IExists(p, next);
		} else {
			return new IExistsSymbol(p, next);
		}
	}

	public Instruction encodeXmatch(Xmatch p, Instruction next, Instruction failjump) {
		return new IMatch(p, next);
	}

	public Instruction encodeXis(Xis p, Instruction next, Instruction failjump) {
		if (p.is) {
			return new IPos(p, encode(p.getSymbolExpression(), new IIsSymbol(p, next), failjump));
		} else {
			return new IPos(p, encode(p.getSymbolExpression(), new IIsaSymbol(p, next), failjump));
		}
	}

	public Instruction encodeXdefindent(Xdefindent p, Instruction next, Instruction failjump) {
		return new IDefIndent(p, next);
	}

	public Instruction encodeXindent(Xindent p, Instruction next, Instruction failjump) {
		return new IIsIndent(p, next);
	}

	@Override
	public Instruction encodeExtension(Expression p, Instruction next, Instruction failjump) {
		return next;
	}

}
