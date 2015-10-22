package nez.parser;

import nez.Strategy;
import nez.lang.Production;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Tlink;
import nez.main.Verbose;

public class PackratCompiler extends OptimizedCompiler {

	public PackratCompiler(Strategy option) {
		super(option);
	}

	@Override
	public final Instruction encodeNonTerminal(NonTerminal n, Instruction next, Instruction failjump) {
		Production p = n.getProduction();
		if (p == null) {
			Verbose.debug("[PANIC] unresolved: " + n.getLocalName() + " ***** ");
			return next;
		}
		ParseFunc f = this.getParseFunc(p);
		if (f.inlining) {
			this.optimizedInline(p);
			return encode(f.getExpression(), next, failjump);
		}
		if (f.memoPoint != null) {
			if (!enabledASTConstruction || p.isNoNTreeConstruction()) {
				if (Verbose.PackratParsing) {
					Verbose.println("memoize: " + n.getLocalName() + " at " + this.getEncodingProduction().getLocalName());
				}
				return memoize(n, f, next);
			}
		}

		// add for left recursion supporter
		if (strategy.isEnabled("SLR", Strategy.SLR)) {
			ILRGrow grow = new ILRGrow(f, n.getLocalName(), next);
			return new ILRCall(f, n.getLocalName(), grow);
		}

		return new ICall(f, n.getLocalName(), next);
	}

	private Instruction memoize(NonTerminal n, ParseFunc f, Instruction next) {
		Instruction inside = new IMemo(n, f.memoPoint, f.state, next);
		inside = new ICall(f, n.getLocalName(), inside);
		inside = new IAlt(n, new IMemoFail(n, f.state, f.memoPoint), inside);
		return new ILookup(n, f.memoPoint, f.state, inside, next);
	}

	private Instruction memoize2(NonTerminal n, ParseFunc f, Instruction next) {
		if (f.compiled_memo == null) {
			f.compiled_memo = memoize(n, f, new IRet(n));
			this.addCachedInstruction(f.compiled_memo);
		}
		return new ICall(f, n.getLocalName(), f.compiled_memo, next);
	}

	// AST Construction

	@Override
	public final Instruction encodeTlink(Tlink p, Instruction next, Instruction failjump) {
		if (enabledASTConstruction && p.get(0) instanceof NonTerminal) {
			NonTerminal n = (NonTerminal) p.get(0);
			ParseFunc f = this.getParseFunc(n.getProduction());
			if (f.memoPoint != null) {
				if (Verbose.PackratParsing) {
					Verbose.println("memoize: @" + n.getLocalName() + " at " + this.getEncodingProduction().getLocalName());
				}
				return memoize(p, n, f, next);
			}
		}
		return super.encodeTlink(p, next, failjump);
	}

	private Instruction memoize(Tlink p, NonTerminal n, ParseFunc f, Instruction next) {
		Instruction inside = new ITMemo(p, f.memoPoint, f.state, next);
		inside = new ICommit(p, inside);
		inside = super.encodeNonTerminal(n, inside, null);
		inside = new ITStart(p, inside);
		inside = new IAlt(p, new IMemoFail(p, f.state, f.memoPoint), inside);
		return new ITLookup(p, f.memoPoint, f.state, inside, next);
	}

	private Instruction memoize2(Tlink p, NonTerminal n, ParseFunc f, Instruction next) {
		if (f.compiled_memoAST == null) {
			f.compiled_memoAST = memoize(p, n, f, new IRet(p));
			this.addCachedInstruction(f.compiled_memoAST);
		}
		return new ICall(f, n.getLocalName(), f.compiled_memoAST, next);
	}

}
