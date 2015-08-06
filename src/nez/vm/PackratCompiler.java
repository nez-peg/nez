package nez.vm;

import java.util.HashMap;

import nez.NezOption;
import nez.lang.AnyChar;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.CharMultiByte;
import nez.lang.Choice;
import nez.lang.Expression;
import nez.lang.GrammarOptimizer;
import nez.lang.Link;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.Option;
import nez.lang.Production;
import nez.lang.Repetition;
import nez.lang.Sequence;
import nez.main.Verbose;
import nez.util.UFlag;

public class PackratCompiler extends OptimizedCompiler {

	public PackratCompiler(NezOption option) {
		super(option);
	}

	protected Instruction encodeMemoizingProduction(CodePoint cp) {
//		if(cp.memoPoint != null) {
//			Production p = cp.production;
//			//boolean node = option.enabledASTConstruction ? !p.isNoNTreeConstruction() : false;
//			boolean state = p.isContextual();
//			Instruction next = new IMemo(p, cp.memoPoint, state, new IRet(p));
//			Instruction inside = new ICall(cp.production, next);
//			inside = new IAlt(p, new IMemoFail(p, state, cp.memoPoint), inside);
//			return new ILookup(p, cp.memoPoint, state, inside, new IRet(p));
//		}
		return null;
	}

	public final Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump) {
		Production r = p.getProduction();
		CodePoint cp = this.getCodePoint(r);
		if(cp.inlining) {
			this.optimizedInline(r);
			return encode(cp.localExpression, next, failjump);
		}
		if(cp.memoPoint != null) {
			if(!option.enabledASTConstruction || r.isNoNTreeConstruction()) {
				if(Verbose.PackratParsing) {
					Verbose.debug("memoize: " + p.getLocalName());
				}
				Instruction inside = new IMemo(p, cp.memoPoint, cp.state, next);
				inside = new ICall(cp.production, inside);
				inside = new IAlt(p, new IMemoFail(p, cp.state, cp.memoPoint), inside);
				return new ILookup(p, cp.memoPoint, cp.state, inside, next);
			}
		}
		return new ICall(r, next);
	}

	// AST Construction

	public final Instruction encodeLink(Link p, Instruction next, Instruction failjump) {
		if(option.enabledASTConstruction && p.get(0) instanceof NonTerminal) {
			NonTerminal n = (NonTerminal) p.get(0);
			CodePoint cp = this.getCodePoint(n.getProduction());
			if(cp.memoPoint != null) {
				Instruction inside = new ITMemo(p, cp.memoPoint, cp.state, next);
				inside = new ICommit(p, inside);
				inside = super.encodeNonTerminal(n, inside, failjump);
				inside = new ITStart(p, inside);
				inside = new IAlt(p, new IMemoFail(p, cp.state, cp.memoPoint), inside);
				return new ITLookup(p, cp.memoPoint, cp.state, inside, next);
			}
		}
		return super.encodeLink(p, next, failjump);
	}

}
