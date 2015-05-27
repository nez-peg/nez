package nez.vm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import nez.lang.And;
import nez.lang.AnyChar;
import nez.lang.Block;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Capture;
import nez.lang.Choice;
import nez.lang.DefIndent;
import nez.lang.DefSymbol;
import nez.lang.ExistsSymbol;
import nez.lang.Expression;
import nez.lang.GrammarFactory;
import nez.lang.Grammar;
import nez.lang.IsIndent;
import nez.lang.IsSymbol;
import nez.lang.Link;
import nez.lang.LocalTable;
import nez.lang.GrammarReshaper;
import nez.lang.New;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.Option;
import nez.lang.Prediction;
import nez.lang.Production;
import nez.lang.Repetition;
import nez.lang.Repetition1;
import nez.lang.Replace;
import nez.lang.Sequence;
import nez.lang.Tagging;
import nez.main.Verbose;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UFlag;
import nez.util.UList;
import nez.util.UMap;

public class NezCompiler2 extends NezCompiler1 {
	
	HashMap<Integer, MemoPoint> memoMap;
	
	public NezCompiler2(int option) {
		super(option);
		if(this.enablePackratParsing()) {
			this.memoMap = new HashMap<Integer, MemoPoint>();
			this.visitedMap = new UMap<String>();
		}
	}

	protected Expression optimizeProduction(Production p) {
		return new GrammarOptimizer(this.option).optimize(p);
	}

	MemoPoint issueMemoPoint(String label, Expression e) {
		if(this.enablePackratParsing()) {
			Integer key = e.getId();
			assert(e.getId() != 0);
			MemoPoint m = this.memoMap.get(key);
			if(m == null) {
				m = new MemoPoint(this.memoMap.size(), label, e, this.isContextSensitive(e));
				this.visitedMap.clear();
				this.memoMap.put(key, m);
			}
			return m;
		}
		return null;
	}

	private UMap<String> visitedMap = null;

	private boolean isContextSensitive(Expression e) {
		if(e instanceof NonTerminal) {
			String un = ((NonTerminal) e).getUniqueName();
			if(visitedMap.get(un) == null) {
				visitedMap.put(un, un);
				return isContextSensitive(((NonTerminal) e).getProduction().getExpression());
			}
			return false;
		}
		for(int i = 0; i < e.size(); i++) {
			if(isContextSensitive(e.get(i))) {
				return true;
			}
		}
		return (e instanceof IsIndent || e instanceof IsSymbol);
	}
		
	public final int getMemoPointSize() {
		if(this.enablePackratParsing()) {
			return this.memoMap.size();
		}
		return 0;
	}
	
	public final UList<MemoPoint> getMemoPointList() {
		if(this.memoMap != null) {
			UList<MemoPoint> l = new UList<MemoPoint>(new MemoPoint[this.memoMap.size()]);
			for(Entry<Integer,MemoPoint> e : memoMap.entrySet()) {
				l.add(e.getValue());
			}
			return l;
		}
		return null;
	}
	
	public final Instruction encodeOption(Option p, Instruction next) {
		if(UFlag.is(option, Grammar.Specialization)) {
			Expression inner = p.get(0);
			if(inner instanceof ByteChar) {
				this.optimizedUnary(p);
				return new IOptionByteChar((ByteChar)inner, next);
			}
			if(inner instanceof ByteMap) {
				this.optimizedUnary(p);
				return new IOptionByteMap((ByteMap)inner, next);
			}
		}
		return super.encodeOption(p, next);
	}
	
	public final Instruction encodeRepetition(Repetition p, Instruction next) {
		if(UFlag.is(option, Grammar.Specialization)) {
			Expression inner = p.get(0);
			if(inner instanceof ByteChar) {
				this.optimizedUnary(p);
				return new IRepeatedByteMap((ByteChar)inner, next);
			}
			if(inner instanceof ByteMap) {
				this.optimizedUnary(p);
				return new IRepeatedByteMap((ByteMap)inner, next);
			}
		}
		return super.encodeRepetition(p, next);
	}


	public final Instruction encodeNot(Not p, Instruction next, Instruction failjump) {
		if(UFlag.is(option, Grammar.Specialization)) {
			Expression inn = p.get(0);
			if(inn instanceof ByteMap) {
				this.optimizedUnary(p);
				return new INotByteMap((ByteMap)inn, next);
			}
			if(inn instanceof ByteChar) {
				this.optimizedUnary(p);
				return new INotByteMap((ByteChar)inn, next);
			}
			if(inn instanceof AnyChar) {
				this.optimizedUnary(p);
				return new INotAnyChar(inn, UFlag.is(this.option, Grammar.Binary), next);
			}
			if(inn instanceof Sequence && ((Sequence)inn).isMultiChar()) {
				this.optimizedUnary(p);
				return new INotMultiChar((Sequence)inn, next);
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
		ProductionCode code = this.codeMap.get(r.getUniqueName());
		if(code.inlining) {
			this.optimizedInline(r);
			return encodeExpression(code.localExpression, next, failjump);
		}
		if(this.enablePackratParsing()) {
			if(!this.enableASTConstruction() || r.isPurePEG()) {
				Expression ref = GrammarFactory.resolveNonTerminal(r.getExpression());
				MemoPoint m = this.issueMemoPoint(r.getUniqueName(), ref);
				if(m != null) {
					if(UFlag.is(option, Grammar.Tracing)) {
						IMonitoredSwitch monitor = new IMonitoredSwitch(p, new ICallPush(p.getProduction(), next));
						Instruction inside = new ICallPush(r, newMemoize(p, monitor, m, next));
						monitor.setActivatedNext(newLookup(p, monitor, m, inside, next, newMemoizeFail(p, monitor, m)));
						return monitor;
					}
					Instruction inside = new ICallPush(r, newMemoize(p, IMonitoredSwitch.dummyMonitor, m, next));
					return newLookup(p, IMonitoredSwitch.dummyMonitor, m, inside, next, newMemoizeFail(p, IMonitoredSwitch.dummyMonitor, m));
				}
			}
		}	
		return new ICallPush(r, next);
	}
	
	private Instruction newLookup(Expression e, IMonitoredSwitch monitor, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		if(m.contextSensitive) {
			return new IStateLookup(e, monitor, m, next, skip, failjump);
		}
		return new ILookup(e, monitor, m, next, skip, failjump);
	}

	private Instruction newMemoize(Expression e, IMonitoredSwitch monitor, MemoPoint m, Instruction next) {
		if(m.contextSensitive) {
			return new IStateMemoize(e, monitor, m, next);
		}
		return new IMemoize(e, monitor, m, next);
	}

	private Instruction newMemoizeFail(Expression e, IMonitoredSwitch monitor, MemoPoint m) {
		if(m.contextSensitive) {
			return new IStateMemoizeFail(e, monitor, m);
		}
		return new IMemoizeFail(e, monitor, m);
	}

	
	// AST Construction
	
	public final Instruction encodeLink(Link p, Instruction next, Instruction failjump) {
		if(this.enableASTConstruction()) {
			if(this.enablePackratParsing()) {
				Expression inner = GrammarFactory.resolveNonTerminal(p.get(0));
				MemoPoint m = this.issueMemoPoint(p.toString(), inner);
				if(m != null) {
					if(UFlag.is(option, Grammar.Tracing)) {
						IMonitoredSwitch monitor = new IMonitoredSwitch(p, encodeExpression(p.get(0), next, failjump));
						Instruction inside = encodeExpression(p.get(0), newMemoizeNode(p, monitor, m, next), failjump);
						monitor.setActivatedNext(newLookupNode(p, monitor, m, inside, next, new IMemoizeFail(p, monitor, m)));
						return monitor;
					}
					Instruction inside = encodeExpression(p.get(0), newMemoizeNode(p, IMonitoredSwitch.dummyMonitor, m, next), failjump);
					return newLookupNode(p, IMonitoredSwitch.dummyMonitor, m, inside, next, new IMemoizeFail(p, IMonitoredSwitch.dummyMonitor, m));
				}
			}
			return new INodePush(p, encodeExpression(p.get(0), new INodeStore(p, next), failjump));
		}
		return encodeExpression(p.get(0), next, failjump);
	}

	private Instruction newLookupNode(Link e, IMonitoredSwitch monitor, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		if(m.contextSensitive) {
			return new IStateLookupNode(e, monitor, m, next, skip, failjump);
		}
		return new ILookupNode(e, monitor, m, next, skip, failjump);
	}

	private Instruction newMemoizeNode(Link e, IMonitoredSwitch monitor, MemoPoint m, Instruction next) {
		if(m.contextSensitive) {
			return new IStateMemoizeNode(e, monitor, m, next);
		}
		return new IMemoizeNode(e, monitor, m, next);
	}

}
