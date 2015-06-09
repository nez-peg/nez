package nez.vm;

import java.util.HashMap;

import nez.lang.AnyChar;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Choice;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Link;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.Option;
import nez.lang.Production;
import nez.lang.Repetition;
import nez.lang.Sequence;
import nez.util.UFlag;

public class NezCompiler2 extends NezCompiler1 {

//	HashMap<Integer, MemoPoint> memoMap;

	public NezCompiler2(int option) {
		super(option);
	}

	protected Expression optimizeLocalProduction(Production p) {
		return new GrammarOptimizer(this.option).optimize(p);
	}

	protected Instruction encodeMemoizingProduction(CodePoint code) {
		if(UFlag.is(this.option, Grammar.PackratParsing)) {
			Production p = code.production;
			boolean state = p.isContextual();
			Instruction next = new IMemoize(p, code.memoPoint, !p.isNoNTreeConstruction(), state, new IMemoRet(p, null));
			Instruction inside = new ICallPush(code.production, next);
			return new ILookup(p, code.memoPoint, !p.isNoNTreeConstruction(), state, inside, next, new IMemoizeFail(p, state, code.memoPoint));
		}
		return null;
	}

	public final Instruction encodeOption(Option p, Instruction next) {
		if(UFlag.is(option, Grammar.Specialization)) {
			Expression inner = p.get(0);
			if(inner instanceof ByteChar) {
				this.optimizedUnary(p);
				return new IOptionByteChar((ByteChar) inner, next);
			}
			if(inner instanceof ByteMap) {
				this.optimizedUnary(p);
				return new IOptionByteMap((ByteMap) inner, next);
			}
		}
		return super.encodeOption(p, next);
	}

	public final Instruction encodeRepetition(Repetition p, Instruction next) {
		if(UFlag.is(option, Grammar.Specialization)) {
			Expression inner = p.get(0);
			if(inner instanceof ByteChar) {
				this.optimizedUnary(p);
				return new IRepeatedByteMap((ByteChar) inner, next);
			}
			if(inner instanceof ByteMap) {
				this.optimizedUnary(p);
				return new IRepeatedByteMap((ByteMap) inner, next);
			}
		}
		return super.encodeRepetition(p, next);
	}

	public final Instruction encodeNot(Not p, Instruction next, Instruction failjump) {
		if(UFlag.is(option, Grammar.Specialization)) {
			Expression inn = p.get(0);
			if(inn instanceof ByteMap) {
				this.optimizedUnary(p);
				return new INotByteMap((ByteMap) inn, next);
			}
			if(inn instanceof ByteChar) {
				this.optimizedUnary(p);
				return new INotByteMap((ByteChar) inn, next);
			}
			if(inn instanceof AnyChar) {
				this.optimizedUnary(p);
				return new INotAnyChar(inn, UFlag.is(this.option, Grammar.Binary), next);
			}
			if(inn instanceof Sequence && ((Sequence) inn).isMultiChar()) {
				this.optimizedUnary(p);
				return new INotMultiChar((Sequence) inn, next);
			}
		}
		return super.encodeNot(p, next, failjump);
	}

//	public final Instruction encodeSequence(Expression p, Instruction next, Instruction failjump) {
//		Expression pp = p.optimize(option);
//		if(pp != p) {
//			if(pp instanceof ByteMap) {
//				Verbose.noticeOptimize("ByteMap", p, pp);
//				return encodeByteMap((ByteMap)pp, next, failjump);
//			}
//		}
//		Instruction nextStart = next;
//		for(int i = p.size() -1; i >= 0; i--) {
//			Expression e = p.get(i);
//			nextStart = encodeExpression(e, nextStart, failjump);
//		}
////		if(pp != p) { 	// (!'ab' !'ac' .) => (!'a' .) / (!'ab' !'ac' .)
//		if(pp instanceof Choice && pp.get(0) instanceof ByteMap) {
//			Verbose.noticeOptimize("Prediction", pp);
//			ByteMap notMap = (ByteMap)pp.get(0);
//			IDfaDispatch match = new IDfaDispatch(p, next);
//			Instruction any = new IAnyChar(pp, next);
//			for(int ch = 0; ch < notMap.byteMap.length; ch++) {
//				if(notMap.byteMap[ch]) {
//					match.setJumpTable(ch, any);
//				}
//				else {
//					match.setJumpTable(ch, nextStart);
//				}
//			}
//			return match;
//		}
//		return nextStart;
//	}

	public final Instruction encodeChoice(Choice p, Instruction next, Instruction failjump) {
		if(UFlag.is(option, Grammar.Prediction) && p.predictedCase != null) {
			return encodePredicatedChoice(p, next, failjump);
		}
		return this.encodeUnoptimizedChoice(p, next, failjump);
	}

	private final Instruction encodeBacktrack(Expression e, int ch, Instruction next) {
		//System.out.println("backtrack("+ch+"): " + e);
		//return new IBacktrack(e, ch, next);
		return next;
	}

	private final Instruction encodePredicatedChoice(Choice choice, Instruction next, Instruction failjump) {
		HashMap<Integer, Instruction> m = new HashMap<Integer, Instruction>();
		IDfaDispatch dispatch = new IDfaDispatch(choice, commonFailure);
		for(int ch = 0; ch < choice.predictedCase.length; ch++) {
			Expression predicated = choice.predictedCase[ch];
			if(predicated == null) {
				continue;
			}
			Instruction inst = m.get(predicated.getId());
			if(inst == null) {
				//System.out.println("creating '" + (char)ch + "'("+ch+"): " + e);
				if(predicated instanceof Choice) {
//					if(predicated == choice) {
					/* this is a rare case where the selected choice is the parent choice */
					/* this cause the repeated calls of the same matchers */
					inst = encodeBacktrack(choice, ch, this.encodeUnoptimizedChoice(choice, next, failjump));
					m.put(predicated.getId(), inst);
					dispatch.setJumpTable(ch, inst);
					continue;
//					}
				}
//				Expression factored = CharacterFactoring.s.tryFactoringCharacter(predicated);
//				if(factored != null) {
//					//System.out.println("factored("+ch+"): " + factored);
//					inst = encodeExpression(factored, next, failjump);
//				}
//				else {
				inst = encodeBacktrack(predicated, ch, encodeExpression(predicated, next, failjump));
//				}
				m.put(predicated.getId(), inst);
			}
			dispatch.setJumpTable(ch, inst);
		}
		return dispatch;
	}

	public final Instruction encodeUnoptimizedChoice(Choice p, Instruction next, Instruction failjump) {
		return super.encodeChoice(p, next, failjump);
	}

	public final Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump) {
		Production r = p.getProduction();
		CodePoint code = this.codeMap.get(r.getUniqueName());
		if(code.inlining) {
			this.optimizedInline(r);
			return encodeExpression(code.localExpression, next, failjump);
		}
		if(this.enablePackratParsing() && code.memoPoint != null) {
			if(!this.enableASTConstruction() || r.isNoNTreeConstruction()) {
				return new IMemoCall(code, next);
			}
		}
		return new ICallPush(r, next);
	}

	// AST Construction

	public final Instruction encodeLink(Link p, Instruction next, Instruction failjump) {
		if(this.enableASTConstruction()) {
			next = new ICommit(p, new ILink(p, next));
			if(this.enablePackratParsing() && p.get(0) instanceof NonTerminal) {
				next = encodeLinkedNonterminal((NonTerminal) p.get(0), next, failjump);
			}
			else {
				next = encodeExpression(p.get(0), next, failjump);
			}
			return new INodePush(p, next);
		}
		return encodeExpression(p.get(0), next, failjump);
	}

	private Instruction encodeLinkedNonterminal(NonTerminal p, Instruction next, Instruction failjump) {
		Production r = p.getProduction();
		CodePoint code = this.codeMap.get(r.getUniqueName());
		if(code.inlining) {
			this.optimizedInline(r);
			return encodeExpression(code.localExpression, next, failjump);
		}
		if(this.enablePackratParsing() && code.memoPoint != null) {
			return new IMemoCall(code, next);
		}
		return new ICallPush(r, next);
	}

}
