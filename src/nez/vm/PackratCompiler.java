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

public class PackratCompiler extends PlainCompiler {

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
		return this.encodeUnoptimizedChoice(p, next, failjump);
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
