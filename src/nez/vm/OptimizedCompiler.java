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
import nez.lang.Not;
import nez.lang.Option;
import nez.lang.Repetition;
import nez.main.Verbose;

public class OptimizedCompiler extends PlainCompiler {

	public OptimizedCompiler(NezOption option) {
		super(option);
	}

	public final Instruction encodeOption(Option p, Instruction next) {
		if(option.enabledLexicalOptimization) {
			Expression inner = GrammarOptimizer.resolveNonTerminal(p.get(0));
			if(inner instanceof ByteChar) {
				this.optimizedUnary(p);
				return new IOByte((ByteChar) inner, next);
			}
			if(inner instanceof ByteMap) {
				this.optimizedUnary(p);
				return new IOSet((ByteMap) inner, next);
			}
			if(inner instanceof CharMultiByte) {
				this.optimizedUnary(p);
				return new IOStr((CharMultiByte)inner, next);
			}
		}
		return super.encodeOption(p, next);
	}

	public final Instruction encodeRepetition(Repetition p, Instruction next) {
		if(option.enabledLexicalOptimization) {
			Expression inner = GrammarOptimizer.resolveNonTerminal(p.get(0));
			if(inner instanceof ByteChar) {
				this.optimizedUnary(p);
				return new IRByte((ByteChar) inner, next);
			}
			if(inner instanceof ByteMap) {
				this.optimizedUnary(p);
				return new IRSet((ByteMap) inner, next);
			}
			if(inner instanceof CharMultiByte) {
				this.optimizedUnary(p);
				return new IRStr((CharMultiByte)inner, next);
			}
		}
		return super.encodeRepetition(p, next);
	}

	public final Instruction encodeNot(Not p, Instruction next, Instruction failjump) {
		if(option.enabledLexicalOptimization) {
			Expression inner = GrammarOptimizer.resolveNonTerminal(p.get(0));
			if(inner instanceof ByteMap) {
				this.optimizedUnary(p);
				return new INSet((ByteMap) inner, next);
			}
			if(inner instanceof ByteChar) {
				this.optimizedUnary(p);
				return new INByte((ByteChar) inner, next);
			}
			if(inner instanceof AnyChar) {
				this.optimizedUnary(p);
				return new INAny(inner, ((AnyChar) inner).isBinary(), next);
			}
			if(inner instanceof CharMultiByte) {
				this.optimizedUnary(p);
				return new INStr((CharMultiByte)inner, next);
			}
		}
		return super.encodeNot(p, next, failjump);
	}

	public final Instruction encodeChoice(Choice p, Instruction next, Instruction failjump) {
		if(option.enabledPrediction && p.predictedCase != null) {
			return encodePredicatedChoice(p, next, failjump);
		}
		return super.encodeChoice(p, next, failjump);
	}

	private final Instruction encodePredicatedChoice(Choice choice, Instruction next, Instruction failjump) {
		HashMap<Integer, Instruction> m = new HashMap<Integer, Instruction>();
		IFirst dispatch = new IFirst(choice, commonFailure);
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
					inst = encode(predicted, next, failjump);
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

}
