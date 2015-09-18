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
			Verbose.debug("unref: " + n.getLocalName());
		}
		ParseFunc pcode = this.getParseFunc(p);
		if (pcode.inlining) {
			this.optimizedInline(p);
			return encode(pcode.e, next, failjump);
		}
		if (pcode.memoPoint != null) {
			if (!enabledASTConstruction || p.isNoNTreeConstruction()) {
				if (Verbose.PackratParsing) {
					Verbose.println("memoize: " + n.getLocalName() + " at " + this.getEncodingProduction().getLocalName());
				}
				Instruction inside = new IMemo(n, pcode.memoPoint, pcode.state, next);
				inside = new ICall(p, inside);
				inside = new IAlt(n, new IMemoFail(n, pcode.state, pcode.memoPoint), inside);
				return new ILookup(n, pcode.memoPoint, pcode.state, inside, next);
			}
		}
		return new ICall(p, next);
	}

	// AST Construction

	@Override
	public final Instruction encodeTlink(Tlink p, Instruction next, Instruction failjump) {
		if (enabledASTConstruction && p.get(0) instanceof NonTerminal) {
			NonTerminal n = (NonTerminal) p.get(0);
			ParseFunc pcode = this.getParseFunc(n.getProduction());
			if (pcode.memoPoint != null) {
				if (Verbose.PackratParsing) {
					Verbose.println("memoize: @" + n.getLocalName() + " at " + this.getEncodingProduction().getLocalName());
				}
				Instruction inside = new ITMemo(p, pcode.memoPoint, pcode.state, next);
				inside = new ICommit(p, inside);
				inside = super.encodeNonTerminal(n, inside, failjump);
				inside = new ITStart(p, inside);
				inside = new IAlt(p, new IMemoFail(p, pcode.state, pcode.memoPoint), inside);
				return new ITLookup(p, pcode.memoPoint, pcode.state, inside, next);
			}
		}
		return super.encodeTlink(p, next, failjump);
	}

}
