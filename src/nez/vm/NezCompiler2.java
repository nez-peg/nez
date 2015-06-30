package nez.vm;

import java.util.HashMap;

import nez.NezOption;
import nez.lang.AnyChar;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
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
import nez.util.UFlag;

public class NezCompiler2 extends NezCompiler1 {

	public NezCompiler2(NezOption option) {
		super(option);
	}

	protected Instruction encodeMemoizingProduction(CodePoint cp) {
		if(cp.memoPoint != null) {
			Production p = cp.production;
			boolean node = option.enabledASTConstruction ? !p.isNoNTreeConstruction() : false;
			boolean state = p.isContextual();
			Instruction next = new IMemoize(p, cp.memoPoint, node, state, new IRet(p));
			Instruction inside = new ICallPush(cp.production, next);
			return new ILookup(p, cp.memoPoint, node, state, inside, new IRet(p), new IMemoizeFail(p, state, cp.memoPoint));
		}
		return null;
	}

	public final Instruction encodeOption(Option p, Instruction next) {
		if(option.enabledLexicalOptimization) {
			Expression inner = GrammarOptimizer.resolveNonTerminal(p.get(0));
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
		if(option.enabledLexicalOptimization) {
			Expression inner = GrammarOptimizer.resolveNonTerminal(p.get(0));
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
		if(option.enabledLexicalOptimization) {
			Expression inner = GrammarOptimizer.resolveNonTerminal(p.get(0));
			if(inner instanceof ByteMap) {
				this.optimizedUnary(p);
				return new INotByteMap((ByteMap) inner, next);
			}
			if(inner instanceof ByteChar) {
				this.optimizedUnary(p);
				return new INotByteMap((ByteChar) inner, next);
			}
			if(inner instanceof AnyChar) {
				this.optimizedUnary(p);
				return new INotAnyChar(inner, ((AnyChar) inner).isBinary(), next);
			}
//			if(inner instanceof Sequence && ((Sequence) inner).isMultiChar()) {
//				this.optimizedUnary(p);
//				return new INotMultiChar((Sequence) inner, next);
//			}
		}
		return super.encodeNot(p, next, failjump);
	}

	public final Instruction encodeChoice(Choice p, Instruction next, Instruction failjump) {
		if(option.enabledPrediction && p.predictedCase != null) {
			return encodePredicatedChoice(p, next, failjump);
		}
		return this.encodeUnoptimizedChoice(p, next, failjump);
	}

	private final Instruction encodePredicatedChoice(Choice choice, Instruction next, Instruction failjump) {
		HashMap<Integer, Instruction> m = new HashMap<Integer, Instruction>();
		IPredictDispatch dispatch = new IPredictDispatch(choice, commonFailure);
		for(int ch = 0; ch < choice.predictedCase.length; ch++) {
			Expression predicted = choice.predictedCase[ch];
			if(predicted == null) {
				continue;
			}
			int id = predictId(choice.predictedCase, ch, predicted);
			Instruction inst = m.get(id);
			if(inst == null) {
				//System.out.println("creating '" + (char)ch + "'("+ch+"): " + e);
				if(predicted instanceof Choice) {
//					if(predicated == choice) {
					/* this is a rare case where the selected choice is the parent choice */
					/* this cause the repeated calls of the same matchers */
					inst = this.encodeUnoptimizedChoice(choice, next, failjump);
				}
				else {
					inst = encodeExpression(predicted, next, failjump);
				}
				m.put(id, inst);
			}
			dispatch.setJumpTable(ch, inst);
		}
		return dispatch;
	}

	private int predictId(Expression[] predictedCase, int max, Expression predicted) {
		if(predicted.isInterned()) {
			return predicted.getId();
		}
		for(int i = 0; i < max; i++) {
			if(predictedCase[i] != null && predicted.equalsExpression(predictedCase[i])) {
				return i;
			}
		}
		return max;
	}
	
	public final Instruction encodeUnoptimizedChoice(Choice p, Instruction next, Instruction failjump) {
		return super.encodeChoice(p, next, failjump);
	}

	public final Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump) {
		Production r = p.getProduction();
		CodePoint cp = this.getCodePoint(r);
		if(cp.inlining) {
			this.optimizedInline(r);
			return encodeExpression(cp.localExpression, next, failjump);
		}
		if(cp.memoPoint != null) {
			if(!option.enabledASTConstruction || r.isNoNTreeConstruction()) {
				return new IMemoCall(cp, next);
			}
		}
		return new ICallPush(r, next);
	}

	// AST Construction

	public final Instruction encodeLink(Link p, Instruction next, Instruction failjump) {
		if(!option.enabledASTConstruction && p.get(0) instanceof NonTerminal) {
			next = new ICommit(p, new ILink(p, next));
			next = encodeNonTerminal((NonTerminal) p.get(0), next, failjump);
			return new INodePush(p, next);
		}
		return super.encodeLink(p, next, failjump);
	}

}
