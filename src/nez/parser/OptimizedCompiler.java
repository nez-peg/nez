package nez.parser;

import java.util.HashMap;

import nez.Strategy;
import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Cset;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pnot;
import nez.lang.expr.Poption;
import nez.lang.expr.Psequence;
import nez.lang.expr.Pzero;
import nez.main.Verbose;

public class OptimizedCompiler extends PlainCompiler {

	public OptimizedCompiler(Strategy option) {
		super(option);
	}

	protected void optimizedUnary(Expression p) {
		Verbose.noticeOptimize("specialization", p);
	}

	protected void optimizedInline(Production p) {
		Verbose.noticeOptimize("inlining", p.getExpression());
	}

	public final Expression getInnerExpression(Expression p) {
		Expression inner = ExpressionCommons.resolveNonTerminal(p.get(0));
		if (strategy.isEnabled("Ostr", Strategy.Ostr) && inner instanceof Psequence) {
			inner = ((Psequence) inner).toMultiCharSequence();
			// System.out.println("Stringfy:" + inner);
		}
		return inner;
	}

	@Override
	public final Instruction encodePoption(Poption p, Instruction next) {
		if (strategy.isEnabled("Olex", Strategy.Olex)) {
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
		return super.encodePoption(p, next);
	}

	@Override
	public final Instruction encodePzero(Pzero p, Instruction next) {
		if (strategy.isEnabled("Olex", Strategy.Olex)) {
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
		return super.encodePzero(p, next);
	}

	@Override
	public final Instruction encodePnot(Pnot p, Instruction next, Instruction failjump) {
		if (strategy.isEnabled("Olex", Strategy.Olex)) {
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
		return super.encodePnot(p, next, failjump);
	}

	@Override
	public final Instruction encodePchoice(Pchoice p, Instruction next, Instruction failjump) {
		if (/* strategy.isEnabled("Ofirst", Strategy.Ofirst) && */p.predictedCase != null) {
			if (p.isTrieTree) {
				return encodeDFirstChoice(p, next, failjump);
			}
			return encodeFirstChoice(p, next, failjump);
		}
		return super.encodePchoice(p, next, failjump);
	}

	private final Instruction encodeFirstChoice(Pchoice choice, Instruction next, Instruction failjump) {
		Instruction[] compiled = new Instruction[choice.firstInners.length];
		// Verbose.debug("TrieTree: " + choice.isTrieTree + " " + choice);
		IFirst dispatch = new IFirst(choice, commonFailure);
		for (int ch = 0; ch < choice.predictedCase.length; ch++) {
			Expression predicted = choice.predictedCase[ch];
			if (predicted == null) {
				continue;
			}
			int index = findIndex(choice, predicted);
			Instruction inst = compiled[index];
			if (inst == null) {
				// System.out.println("creating '" + (char)ch + "'("+ch+"): " +
				// e);
				if (predicted instanceof Pchoice) {
					assert (((Pchoice) predicted).predictedCase == null);
					inst = this.encodeUnoptimizedChoice(choice, next, failjump);
				} else {
					inst = encode(predicted, next, failjump);
				}
				compiled[index] = inst;
			}
			dispatch.setJumpTable(ch, inst);
		}
		return dispatch;
	}

	private final Instruction encodeDFirstChoice(Pchoice choice, Instruction next, Instruction failjump) {
		Instruction[] compiled = new Instruction[choice.firstInners.length];
		IDFirst dispatch = new IDFirst(choice, commonFailure);
		for (int ch = 0; ch < choice.predictedCase.length; ch++) {
			Expression predicted = choice.predictedCase[ch];
			if (predicted == null) {
				continue;
			}
			int index = findIndex(choice, predicted);
			Instruction inst = compiled[index];
			if (inst == null) {
				Expression next2 = predicted.getNext();
				if (next2 != null) {
					inst = encode(next2, next, failjump);
				} else {
					inst = next;
				}
				compiled[index] = inst;
			}
			dispatch.setJumpTable(ch, inst);
		}
		return dispatch;
	}

	private int findIndex(Pchoice choice, Expression e) {
		for (int i = 0; i < choice.firstInners.length; i++) {
			if (choice.firstInners[i] == e) {
				return i;
			}
		}
		return -1;
	}

	private final Instruction encodePredicatedChoice0(Pchoice choice, Instruction next, Instruction failjump) {
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
				if (predicted instanceof Pchoice) {
					assert (((Pchoice) predicted).predictedCase == null);
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

	public final Instruction encodeUnoptimizedChoice(Pchoice p, Instruction next, Instruction failjump) {
		return super.encodePchoice(p, next, failjump);
	}

}
