package nez.parser.moz;

import java.util.HashMap;

import nez.Verbose;
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
import nez.parser.ParserStrategy;

public class OptimizedCompiler extends PlainCompiler {

	public OptimizedCompiler(ParserStrategy option) {
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
		if (strategy.Ostring && inner instanceof Psequence) {
			inner = ((Psequence) inner).toMultiCharSequence();
			// System.out.println("Stringfy:" + inner);
		}
		return inner;
	}

	@Override
	public final MozInst encodePoption(Poption p, MozInst next) {
		if (strategy.Olex) {
			Expression inner = getInnerExpression(p);
			if (inner instanceof Cbyte) {
				this.optimizedUnary(p);
				return new Moz.OByte((Cbyte) inner, next);
			}
			if (inner instanceof Cset) {
				this.optimizedUnary(p);
				return new Moz.OSet((Cset) inner, next);
			}
			if (inner instanceof Cmulti) {
				this.optimizedUnary(p);
				return new Moz.OStr((Cmulti) inner, next);
			}
		}
		return super.encodePoption(p, next);
	}

	@Override
	public final MozInst encodePzero(Pzero p, MozInst next) {
		if (strategy.Olex) {
			Expression inner = getInnerExpression(p);
			if (inner instanceof Cbyte) {
				this.optimizedUnary(p);
				return new Moz.RByte((Cbyte) inner, next);
			}
			if (inner instanceof Cset) {
				this.optimizedUnary(p);
				return new Moz.RSet((Cset) inner, next);
			}
			if (inner instanceof Cmulti) {
				this.optimizedUnary(p);
				return new Moz.RStr((Cmulti) inner, next);
			}
		}
		return super.encodePzero(p, next);
	}

	@Override
	public final MozInst encodePnot(Pnot p, MozInst next, MozInst failjump) {
		if (strategy.Olex) {
			Expression inner = getInnerExpression(p);
			if (inner instanceof Cset) {
				this.optimizedUnary(p);
				return new Moz.NSet((Cset) inner, next);
			}
			if (inner instanceof Cbyte) {
				this.optimizedUnary(p);
				return new Moz.NByte((Cbyte) inner, next);
			}
			if (inner instanceof Cany) {
				this.optimizedUnary(p);
				return new Moz.NAny(inner, ((Cany) inner).isBinary(), next);
			}
			if (inner instanceof Cmulti) {
				this.optimizedUnary(p);
				return new Moz.NStr((Cmulti) inner, next);
			}
		}
		return super.encodePnot(p, next, failjump);
	}

	@Override
	public final MozInst encodePchoice(Pchoice p, MozInst next, MozInst failjump) {
		if (/* strategy.isEnabled("Ofirst", Strategy.Ofirst) && */p.predictedCase != null) {
			if (p.isTrieTree && strategy.Odfa) {
				return encodeDFirstChoice(p, next, failjump);
			}
			return encodeFirstChoice(p, next, failjump);
		}
		return super.encodePchoice(p, next, failjump);
	}

	private final MozInst encodeFirstChoice(Pchoice choice, MozInst next, MozInst failjump) {
		MozInst[] compiled = new MozInst[choice.firstInners.length];
		// Verbose.debug("TrieTree: " + choice.isTrieTree + " " + choice);
		Moz.First dispatch = new Moz.First(choice, commonFailure);
		for (int ch = 0; ch < choice.predictedCase.length; ch++) {
			Expression predicted = choice.predictedCase[ch];
			if (predicted == null) {
				continue;
			}
			int index = findIndex(choice, predicted);
			MozInst inst = compiled[index];
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

	private final MozInst encodeDFirstChoice(Pchoice choice, MozInst next, MozInst failjump) {
		MozInst[] compiled = new MozInst[choice.firstInners.length];
		Moz.DFirst dispatch = new Moz.DFirst(choice, commonFailure);
		for (int ch = 0; ch < choice.predictedCase.length; ch++) {
			Expression predicted = choice.predictedCase[ch];
			if (predicted == null) {
				continue;
			}
			int index = findIndex(choice, predicted);
			MozInst inst = compiled[index];
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

	private final MozInst encodePredicatedChoice0(Pchoice choice, MozInst next, MozInst failjump) {
		HashMap<Integer, MozInst> m = new HashMap<Integer, MozInst>();
		Moz.First dispatch = new Moz.First(choice, commonFailure);
		for (int ch = 0; ch < choice.predictedCase.length; ch++) {
			Expression predicted = choice.predictedCase[ch];
			if (predicted == null) {
				continue;
			}
			int id = predictId(choice.predictedCase, ch, predicted);
			MozInst inst = m.get(id);
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

	public final MozInst encodeUnoptimizedChoice(Pchoice p, MozInst next, MozInst failjump) {
		return super.encodePchoice(p, next, failjump);
	}

}
