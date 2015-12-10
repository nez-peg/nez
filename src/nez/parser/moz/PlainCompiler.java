package nez.parser.moz;

import nez.ParserStrategy;
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
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Xsymbol;
import nez.parser.ParseFunc;

public class PlainCompiler extends NezCompiler {

	protected final MozInst commonFailure = new Moz.Fail(null);

	public PlainCompiler(ParserStrategy option) {
		super(option);
	}

	// encoding

	@Override
	public MozInst encode(Expression e, MozInst next, MozInst failjump) {
		return e.encode(this, next, failjump);
	}

	@Override
	public MozInst encodeCany(Cany p, MozInst next, MozInst failjump) {
		return new Moz.Any(p, next);
	}

	@Override
	public MozInst encodeCbyte(Cbyte p, MozInst next, MozInst failjump) {
		return new Moz.Byte(p, next);
	}

	@Override
	public MozInst encodeCset(Cset p, MozInst next, MozInst failjump) {
		return new Moz.Set(p, next);
	}

	@Override
	public MozInst encodeCmulti(Cmulti p, MozInst next, MozInst failjump) {
		return new Moz.Str(p, next);
	}

	@Override
	public MozInst encodePfail(Expression p) {
		return this.commonFailure;
	}

	@Override
	public MozInst encodePoption(Poption p, MozInst next) {
		MozInst pop = new Moz.Succ(p, next);
		return new Moz.Alt(p, next, encode(p.get(0), pop, next));
	}

	@Override
	public MozInst encodePzero(Pzero p, MozInst next) {
		// Expression skip = p.possibleInfiniteLoop ? new Moz.Skip(p) : new
		// ISkip(p);
		MozInst skip = new Moz.Skip(p);
		MozInst start = encode(p.get(0), skip, next/* FIXME */);
		skip.next = start;
		return new Moz.Alt(p, next, start);
	}

	@Override
	public MozInst encodePone(Pone p, MozInst next, MozInst failjump) {
		return encode(p.get(0), this.encodePzero(p, next), failjump);
	}

	@Override
	public MozInst encodePand(Pand p, MozInst next, MozInst failjump) {
		MozInst inner = encode(p.get(0), new Moz.Back(p, next), failjump);
		return new Moz.Pos(p, inner);
	}

	@Override
	public MozInst encodePnot(Pnot p, MozInst next, MozInst failjump) {
		MozInst fail = new Moz.Succ(p, new Moz.Fail(p));
		return new Moz.Alt(p, next, encode(p.get(0), fail, failjump));
	}

	@Override
	public MozInst encodePsequence(Psequence p, MozInst next, MozInst failjump) {
		// return encode(p.get(0), encode(p.get(1), next, failjump), failjump);
		MozInst nextStart = next;
		for (int i = p.size() - 1; i >= 0; i--) {
			Expression e = p.get(i);
			nextStart = encode(e, nextStart, failjump);
		}
		return nextStart;
	}

	@Override
	public MozInst encodePchoice(Pchoice p, MozInst next, MozInst failjump) {
		MozInst nextChoice = encode(p.get(p.size() - 1), next, failjump);
		for (int i = p.size() - 2; i >= 0; i--) {
			Expression e = p.get(i);
			nextChoice = new Moz.Alt(e, nextChoice, encode(e, new Moz.Succ(e, next), nextChoice));
		}
		return nextChoice;
	}

	@Override
	public MozInst encodeNonTerminal(NonTerminal n, MozInst next, MozInst failjump) {
		Production p = n.getProduction();
		ParseFunc f = this.getParseFunc(p);
		return new Moz.Call(f, p.getLocalName(), next);
	}

	// AST Construction

	@Override
	public MozInst encodeTlink(Tlink p, MozInst next, MozInst failjump) {
		if (this.enabledASTConstruction) {
			next = new Moz.TPop(p, next);
			next = encode(p.get(0), next, failjump);
			return new Moz.TPush(p, next);
		}
		return encode(p.get(0), next, failjump);
	}

	@Override
	public MozInst encodeTnew(Tnew p, MozInst next) {
		if (this.enabledASTConstruction) {
			return new Moz.New(p, next);
		}
		return next;
	}

	@Override
	public MozInst encodeTlfold(Tlfold p, MozInst next) {
		if (this.enabledASTConstruction) {
			return new Moz.TLeftFold(p, next);
		}
		return next;
	}

	@Override
	public MozInst encodeTcapture(Tcapture p, MozInst next) {
		if (this.enabledASTConstruction) {
			return new Moz.Capture(p, next);
		}
		return next;
	}

	@Override
	public MozInst encodeTtag(Ttag p, MozInst next) {
		if (this.enabledASTConstruction) {
			return new Moz.Tag(p, next);
		}
		return next;
	}

	@Override
	public MozInst encodeTreplace(Treplace p, MozInst next) {
		if (this.enabledASTConstruction) {
			return new Moz.Replace(p, next);
		}
		return next;
	}

	@Override
	public MozInst encodeXblock(Xblock p, MozInst next, MozInst failjump) {
		next = new Moz.EndSymbolScope(p, next);
		next = encode(p.get(0), next, failjump);
		return new Moz.BeginSymbolScope(p, next);
	}

	@Override
	public MozInst encodeXlocal(Xlocal p, MozInst next, MozInst failjump) {
		next = new Moz.EndSymbolScope(p, next);
		next = encode(p.get(0), next, failjump);
		return new Moz.BeginLocalScope(p, next);
	}

	@Override
	public MozInst encodeXsymbol(Xsymbol p, MozInst next, MozInst failjump) {
		return new Moz.Pos(p, encode(p.get(0), new Moz.DefSymbol(p, next), failjump));
	}

	@Override
	public MozInst encodeXexists(Xexists p, MozInst next, MozInst failjump) {
		String symbol = p.getSymbol();
		if (symbol == null) {
			return new Moz.Exists(p, next);
		} else {
			return new Moz.ExistsSymbol(p, next);
		}
	}

	@Override
	public MozInst encodeXmatch(Xmatch p, MozInst next, MozInst failjump) {
		return new Moz.Match(p, next);
	}

	@Override
	public MozInst encodeXis(Xis p, MozInst next, MozInst failjump) {
		if (p.is) {
			return new Moz.Pos(p, encode(p.get(0), new Moz.IsSymbol(p, next), failjump));
		} else {
			return new Moz.Pos(p, encode(p.get(0), new Moz.IsaSymbol(p, next), failjump));
		}
	}

	@Override
	public MozInst encodeXdefindent(Xdefindent p, MozInst next, MozInst failjump) {
		return next;
	}

	@Override
	public MozInst encodeXindent(Xindent p, MozInst next, MozInst failjump) {
		return next;
	}

	@Override
	public MozInst encodeExtension(Expression p, MozInst next, MozInst failjump) {
		return next;
	}

	@Override
	public MozInst encodeTdetree(Tdetree p, MozInst next, MozInst failjump) {
		return next;
	}

}
