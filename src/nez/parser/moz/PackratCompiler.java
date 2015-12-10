package nez.parser.moz;

import nez.Verbose;
import nez.lang.Production;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Tlink;
import nez.parser.ParseFunc;
import nez.parser.ParserStrategy;

public class PackratCompiler extends OptimizedCompiler {

	public PackratCompiler(ParserStrategy option) {
		super(option);
	}

	@Override
	public final MozInst encodeNonTerminal(NonTerminal n, MozInst next, MozInst failjump) {
		Production p = n.getProduction();
		if (p == null) {
			Verbose.debug("[PANIC] unresolved: " + n.getLocalName() + " ***** ");
			return next;
		}
		ParseFunc f = this.getParseFunc(p);
		if (f.isInlined()) {
			this.optimizedInline(p);
			return encode(f.getExpression(), next, failjump);
		}
		if (f.getMemoPoint() != null) {
			if (!strategy.TreeConstruction || p.isNoNTreeConstruction()) {
				if (Verbose.PackratParsing) {
					Verbose.println("memoize: " + n.getLocalName() + " at " + this.getEncodingProduction().getLocalName());
				}
				return memoize(n, f, next);
			}
		}
		return new Moz.Call(f, n.getLocalName(), next);
	}

	private MozInst memoize(NonTerminal n, ParseFunc f, MozInst next) {
		MozInst inside = new Moz.Memo(n, f.getMemoPoint(), f.isStateful(), next);
		inside = new Moz.Call(f, n.getLocalName(), inside);
		inside = new Moz.Alt(n, new Moz.MemoFail(n, f.isStateful(), f.getMemoPoint()), inside);
		return new Moz.Lookup(n, f.getMemoPoint(), f.isStateful(), inside, next);
	}

	// private Instruction memoize2(NonTerminal n, ParseFunc f, Instruction
	// next) {
	// if (f.compiled_memo == null) {
	// f.compiled_memo = memoize(n, f, new Moz.Ret(n));
	// this.addCachedInstruction(f.compiled_memo);
	// }
	// return new Moz.Call(f, n.getLocalName(), f.compiled_memo, next);
	// }

	// AST Construction

	@Override
	public final MozInst encodeTlink(Tlink p, MozInst next, MozInst failjump) {
		if (strategy.TreeConstruction && p.get(0) instanceof NonTerminal) {
			NonTerminal n = (NonTerminal) p.get(0);
			ParseFunc f = this.getParseFunc(n.getProduction());
			if (f.getMemoPoint() != null) {
				if (Verbose.PackratParsing) {
					Verbose.println("memoize: @" + n.getLocalName() + " at " + this.getEncodingProduction().getLocalName());
				}
				return memoize(p, n, f, next);
			}
		}
		return super.encodeTlink(p, next, failjump);
	}

	private MozInst memoize(Tlink p, NonTerminal n, ParseFunc f, MozInst next) {
		MozInst inside = new Moz.TMemo(p, f.getMemoPoint(), f.isStateful(), next);
		inside = new Moz.Commit(p, inside);
		inside = super.encodeNonTerminal(n, inside, null);
		inside = new Moz.TStart(p, inside);
		inside = new Moz.Alt(p, new Moz.MemoFail(p, f.isStateful(), f.getMemoPoint()), inside);
		return new Moz.TLookup(p, f.getMemoPoint(), f.isStateful(), inside, next);
	}

	// private Instruction memoize2(Tlink p, NonTerminal n, ParseFunc f,
	// Instruction next) {
	// if (f.compiled_memoAST == null) {
	// f.compiled_memoAST = memoize(p, n, f, new Moz.Ret(p));
	// this.addCachedInstruction(f.compiled_memoAST);
	// }
	// return new Moz.Call(f, n.getLocalName(), f.compiled_memoAST, next);
	// }

}
