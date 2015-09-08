package nez.vm;

import java.util.HashMap;

import nez.NezOption;
import nez.lang.Expression;
import nez.lang.GrammarOptimizer;
import nez.lang.Production;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.Choice;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Unot;
import nez.lang.expr.Uoption;
import nez.lang.expr.Uzero;
import nez.lang.expr.Sequence;
import nez.main.Verbose;

public class OptimizedCompiler extends PlainCompiler {

	public OptimizedCompiler(NezOption option) {
		super(option);
	}

	protected void optimizedUnary(Expression p) {
		Verbose.noticeOptimize("specialization", p);
	}

	protected void optimizedInline(Production p) {
		Verbose.noticeOptimize("inlining", p.getExpression());
	}

	public final Expression getInnerExpression(Expression p) {
		Expression inner = GrammarOptimizer.resolveNonTerminal(p.get(0));
		if (option.enabledStringOptimization && inner instanceof Sequence) {
			inner = ((Sequence) inner).toMultiCharSequence();
			// System.out.println("Stringfy:" + inner);
		}
		return inner;
	}

	@Override
	public final Instruction encodeUoption(Uoption p, Instruction next) {
		if (option.enabledLexicalOptimization) {
			Expression inner = getInnerExpression(p);
			if (inner instanceof Cbyte) {
				this.optimizedUnary(p);
				return new IOByte((Cbyte) inner, next);
			}
			if (inner instanceof Cset) {
				this.optimizedUnary(p);
				return new IOSet((Cset) inner, next);
			}
			if (inner instanceof Cmulti) {
				this.optimizedUnary(p);
				return new IOStr((Cmulti) inner, next);
			}
		}
		return super.encodeUoption(p, next);
	}

	@Override
	public final Instruction encodeUzero(Uzero p, Instruction next) {
		if (option.enabledLexicalOptimization) {
			Expression inner = getInnerExpression(p);
			if (inner instanceof Cbyte) {
				this.optimizedUnary(p);
				return new IRByte((Cbyte) inner, next);
			}
			if (inner instanceof Cset) {
				this.optimizedUnary(p);
				return new IRSet((Cset) inner, next);
			}
			if (inner instanceof Cmulti) {
				this.optimizedUnary(p);
				return new IRStr((Cmulti) inner, next);
			}
		}
		return super.encodeUzero(p, next);
	}

	@Override
	public final Instruction encodeUnot(Unot p, Instruction next, Instruction failjump) {
		if (option.enabledLexicalOptimization) {
			Expression inner = getInnerExpression(p);
			if (inner instanceof Cset) {
				this.optimizedUnary(p);
				return new INSet((Cset) inner, next);
			}
			if (inner instanceof Cbyte) {
				this.optimizedUnary(p);
				return new INByte((Cbyte) inner, next);
			}
			if (inner instanceof Cany) {
				this.optimizedUnary(p);
				return new INAny(inner, ((Cany) inner).isBinary(), next);
			}
			if (inner instanceof Cmulti) {
				this.optimizedUnary(p);
				return new INStr((Cmulti) inner, next);
			}
		}
		return super.encodeUnot(p, next, failjump);
	}

	@Override
	public final Instruction encodeChoice(Choice p, Instruction next, Instruction failjump) {
		if (option.enabledPrediction && p.predictedCase != null) {
			return encodePredicatedChoice(p, next, failjump);
		}
		return super.encodeChoice(p, next, failjump);
	}

	private final Instruction encodePredicatedChoice(Choice choice, Instruction next, Instruction failjump) {
		HashMap<Integer, Instruction> m = new HashMap<Integer, Instruction>();
		IFirst dispatch = new IFirst(choice, commonFailure);
		for (int ch = 0; ch < choice.predictedCase.length; ch++) {
			Expression predicted = choice.predictedCase[ch];
			if (predicted == null) {
				continue;
			}
			int id = predictId(choice.predictedCase, ch, predicted);
			Instruction inst = m.get(id);
			if (inst == null) {
				// System.out.println("creating '" + (char)ch + "'("+ch+"): " +
				// e);
				if (predicted instanceof Choice) {
					// if(predicated == choice) {
					/*
					 * this is a rare case where the selected choice is the
					 * parent choice
					 */
					/* this cause the repeated calls of the same matchers */
					inst = this.encodeUnoptimizedChoice(choice, next, failjump);
				} else {
					inst = encode(predicted, next, failjump);
				}
				m.put(id, inst);
			}
			dispatch.setJumpTable(ch, inst);
		}
		return dispatch;
	}

	private int predictId(Expression[] predictedCase, int max, Expression predicted) {
		// if (predicted.isInterned()) {
		// return predicted.getId();
		// }
		for (int i = 0; i < max; i++) {
			if (predictedCase[i] != null && predicted.equalsExpression(predictedCase[i])) {
				return i;
			}
		}
		return max;
	}

	public final Instruction encodeUnoptimizedChoice(Choice p, Instruction next, Instruction failjump) {
		return super.encodeChoice(p, next, failjump);
	}

}
