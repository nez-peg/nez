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

public class DeprecatedNezCompiler extends NezCompiler1 {
	
	HashMap<Integer, MemoPoint> memoMap;
	
	public DeprecatedNezCompiler(int option) {
		super(option);
		if(this.enablePackratParsing()) {
			this.memoMap = new HashMap<Integer, MemoPoint>();
			this.visitedMap = new UMap<String>();
		}
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
	

//	void test(Expression e, Instruction inst) {
//		boolean found = false;
//		for(int c = 0; c < 257; c++) {
//			if(e.acceptByte(c, this.option) != inst.isAcceptImpl(c)) {
//				found = true;
//				break;
//			}
//		}
//		if(found) {
//			Verbose.printSelfTesting("\nTesting prediction " +  e + " .. ");
//			for(int c = 0; c < 256; c++) {
//				short a = e.acceptByte(c, option);
//				short a2 = inst.isAcceptImpl(c);
//				if(a != a2) {
//					if(a == Prediction.Accept) {
//						Verbose.printSelfTestingIndent("[Failed] Accept " + StringUtils.formatChar(c) + ": " + a2);
//						break;
//					}
//					if(a == Prediction.Reject) {
//						Verbose.printSelfTestingIndent("[Failed] Reject " + StringUtils.formatChar(c) + ": " + a2);
//						break;
//					}
//					if(a == Prediction.Unconsumed) {
//						Verbose.printSelfTestingIndent("[Failed] Unconsumed " + StringUtils.formatChar(c) + ": " + a2);
//						break;
//					}
//				}
//			}
//			Verbose.printSelfTesting("\nPlease report the above to " + Verbose.BugsReport1);
//		}	
//	}
	
//	void verify(Instruction inst) {
//		if(inst != null) {
//			if(inst.id == -1) {
//				inst.id = this.codeList.size();
//				this.codeList.add(inst);
//				verify(inst.next);
//				if(inst.next != null && inst.id + 1 != inst.next.id) {
//					Instruction.labeling(inst.next);
//				}
//				verify(inst.branch());
//				if(inst instanceof IDfaDispatch) {
//					IDfaDispatch match = (IDfaDispatch)inst;
//					for(int ch = 0; ch < match.jumpTable.length; ch ++) {
//						verify(match.jumpTable[ch]);
//					}
//				}
//				//encode(inst.branch2());
//			}
//		}
//	}
	
//	public void dump(UList<Production> ruleList) {
//		for(Production r : ruleList) {
//			String uname = r.getUniqueName();
//			ConsoleUtils.println(uname + ":");
//			CodeBlock block = this.ruleMap.get(uname);
//			for(int i = block.start; i < block.end; i++) {
//				Instruction inst = codeList.ArrayValues[i];
//				if(inst.label) {
//					ConsoleUtils.println("" + inst.id + "*\t" + inst);
//				}
//				else {
//					ConsoleUtils.println("" + inst.id + "\t" + inst);
//				}
//				if(inst.next != null && inst.next.id != i+1) {
//					ConsoleUtils.println("\tjump " + Instruction.label(inst.next));
//				}
//			}
//		}
//	}
	
	
	public final Instruction encodeExpression(Expression e, Instruction next, Instruction failjump) {
		return e.encode(this, next, failjump);
	}
	
	public final Instruction encodeMatchAny(AnyChar p, Instruction next, Instruction failjump) {
		return new IAnyChar(p, next);
	}

	public final Instruction encodeByteChar(ByteChar p, Instruction next, Instruction failjump) {
		return new IByteChar(p, next);
	}

	public final Instruction encodeByteMap(ByteMap p, Instruction next, Instruction failjump) {
		return new IByteMap(p, next);
	}

	public Instruction encodeFail(Expression p) {
		return new IFail(p);
	}

//	public final boolean isDisjoint(boolean[] dfa, boolean[] dfa2) {
//		for(int i = 0; i < dfa.length; i++) {
//			if(dfa[i] && dfa2[i]) {
//				return false;
//			}
//		}
//		return true;
//	}
//
//	public final boolean[] and(boolean[] dfa, boolean[] dfa2) {
//		for(int i = 0; i < dfa.length; i++) {
//			if(dfa[i] && dfa2[i]) {
//				dfa[i] = true;
//			}
//			else {
//				dfa[i] = false;
//			}
//		}
//		return dfa;
//	}
//
//	
//	private boolean[] predictNext(Instruction next) {
//		boolean[] dfa = ByteMap.newMap(true);
//		for(int c = 0; c < dfa.length; c++) {
//			short r = next.isAcceptImpl(c);
//			if(r == Prediction.Reject) {
//				dfa[c] = false;
//			}
//		}
//		return dfa;
//	}
//
//	private boolean[] predictInner(Expression e, boolean[] dfa2) {
//		boolean[] dfa = dfa2.clone();
//		for(int c = 0; c < dfa.length; c++) {
//			short r = e.acceptByte(c, option);
//			if(r == Prediction.Accept) {
//				dfa[c] = true;
//			}
//			if(r == Prediction.Reject) {
//				dfa[c] = false;
//			}		
//		}
//		return dfa;
//	}
//
//	private boolean checkInstruction(Instruction next) {
//		return next instanceof IByteChar || next instanceof IByteMap;
//	}
//
//	private Instruction replaceConsumeInstruction(Instruction inst) {
//		if(inst instanceof IByteChar || inst instanceof IByteMap || inst instanceof IAnyChar) {
//			System.out.println("replaced: " + inst);
//			return new IConsume(inst.e, inst.next);
//		}
//		return inst;
//	}

	
	
	public final Instruction encodeOption(Option p, Instruction next) {
//		if(UFlag.is(option, Grammar.DFA) && checkInstruction(next)) {
//			boolean[] dfa = predictNext(next);
//			boolean[] optdfa = predictInner(p.get(0), dfa);
//			if(isDisjoint(dfa, optdfa)) {
//				Instruction opt = replaceConsumeInstruction(encodeExpression(p.get(0), next, failjump));
//				next = replaceConsumeInstruction(next);
//				IDfaDispatch match = new IDfaDispatch(p, commonFailure);
//				for(int ch = 0; ch < dfa.length; ch++) {
//					if(optdfa[ch]) {
//						dfa[ch] = true;
//						match.setJumpTable(ch, opt);
//						continue;
//					}
//					if(dfa[ch]) {
//						match.setJumpTable(ch, next);
//					}
//				}
////				if(!(next instanceof IByteChar)) {
////					System.out.println("DFA: " + p + " " + next.e + "  ## " + next.getClass());
////				}
//				return match;
//			}
//			System.out.println("NFA: " + p + " " + next.e);
//			System.out.println("NFA: " + StringUtils.stringfyCharClass(and(dfa, optdfa)));
//		}
		if(UFlag.is(option, Grammar.Specialization)) {
			Expression inner = p.get(0).optimize(option);
			if(inner instanceof ByteChar) {
				Verbose.noticeOptimize("Specialization", p, inner);
				return new IOptionByteChar((ByteChar)inner, next);
			}
			if(inner instanceof ByteMap) {
				Verbose.noticeOptimize("Specialization", p, inner);
				return new IOptionByteMap((ByteMap)inner, next);
			}
		}
		if(UFlag.is(option, Grammar.DFA)) {
			Verbose.printNFA(p + " " + next.e);
		}
		Instruction pop = new IFailPop(p, next);
		return new IFailPush(p, next, encodeExpression(p.get(0), pop, next));
	}
	
	public final Instruction encodeRepetition(Repetition p, Instruction next) {
//		if(UFlag.is(option, Grammar.DFA) && !p.possibleInfiniteLoop && checkInstruction(next)) {
//			boolean[] dfa = predictNext(next);
//			boolean[] optdfa = predictInner(p.get(0), dfa);
//			if(isDisjoint(dfa, optdfa)) {
//				IDfaDispatch match = new IDfaDispatch(p, commonFailure);
//				Instruction opt = replaceConsumeInstruction(encodeExpression(p.get(0), match, failjump));
//				next = replaceConsumeInstruction(next);
//				System.out.println("DFA: " + p + " " + next.e);
//				System.out.println("  1 " + p + " " + StringUtils.stringfyCharClass(optdfa));
//				System.out.println("  2 " + next.e + " " + StringUtils.stringfyCharClass(dfa));
//				for(int ch = 0; ch < dfa.length; ch++) {
//					if(optdfa[ch]) {
//						match.setJumpTable(ch, opt);
//						continue;
//					}
//					if(dfa[ch]) {
//						match.setJumpTable(ch, next);
//					}
//				}
//				return match;
//			}
//		}
		if(UFlag.is(option, Grammar.Specialization)) {
			Expression inner = p.get(0).optimize(option);
			if(inner instanceof ByteChar) {
				Verbose.noticeOptimize("Specialization", p, inner);
				return new IRepeatedByteMap((ByteChar)inner, next);
			}
			if(inner instanceof ByteMap) {
				Verbose.noticeOptimize("Specialization", p, inner);
				return new IRepeatedByteMap((ByteMap)inner, next);
			}
		}
		if(UFlag.is(option, Grammar.DFA)) {
			Verbose.printNFA(p + " " + next.e);
		}
		IFailSkip skip = p.possibleInfiniteLoop ? new IFailCheckSkip(p) : new IFailCheckSkip(p);
		Instruction start = encodeExpression(p.get(0), skip, next);
		skip.next = start;
		return new IFailPush(p, next, start);
	}

	public final Instruction encodeRepetition1(Repetition1 p, Instruction next, Instruction failjump) {
		return encodeExpression(p.get(0), this.encodeRepetition(p, next), failjump);
	}

	public final Instruction encodeAnd(And p, Instruction next, Instruction failjump) {
		Instruction inner = encodeExpression(p.get(0), new IPosBack(p, next), failjump);
		return new IPosPush(p, inner);
	}

	public final Instruction encodeNot(Not p, Instruction next, Instruction failjump) {
		if(UFlag.is(option, Grammar.Specialization)) {
			Expression inn = p.get(0).optimize(option);
			if(inn instanceof ByteMap) {
				Verbose.noticeOptimize("Specilization", p);
				return new INotByteMap((ByteMap)inn, next);
			}
			if(inn instanceof ByteChar) {
				Verbose.noticeOptimize("Specilization", p);
				return new INotByteMap((ByteChar)inn, next);
			}
			if(inn instanceof AnyChar) {
				Verbose.noticeOptimize("Specilization", p);
				return new INotAnyChar(inn, UFlag.is(this.option, Grammar.Binary), next);
			}
			if(inn instanceof Sequence && ((Sequence)inn).isMultiChar()) {
				Verbose.noticeOptimize("Specilization", p);
				return new INotMultiChar((Sequence)inn, next);
			}
		}
		Instruction fail = new IFailPop(p, new IFail(p));
		return new INotFailPush(p, next, encodeExpression(p.get(0), fail, failjump));
	}

	public final Instruction encodeSequence(Expression p, Instruction next, Instruction failjump) {
		Expression pp = p.optimize(option);
		if(pp != p) {
			if(pp instanceof ByteMap) {
				Verbose.noticeOptimize("ByteMap", p, pp);
				return encodeByteMap((ByteMap)pp, next, failjump);
			}
		}
		Instruction nextStart = next;
		for(int i = p.size() -1; i >= 0; i--) {
			Expression e = p.get(i);
			nextStart = encodeExpression(e, nextStart, failjump);
		}
//		if(pp != p) { 	// (!'ab' !'ac' .) => (!'a' .) / (!'ab' !'ac' .)
		if(pp instanceof Choice && pp.get(0) instanceof ByteMap) {
			Verbose.noticeOptimize("Prediction", pp);
			ByteMap notMap = (ByteMap)pp.get(0);
			IDfaDispatch match = new IDfaDispatch(p, next);
			Instruction any = new IAnyChar(pp, next);
			for(int ch = 0; ch < notMap.byteMap.length; ch++) {
				if(notMap.byteMap[ch]) {
					match.setJumpTable(ch, any);
				}
				else {
					match.setJumpTable(ch, nextStart);
				}
			}
			return match;
		}
		return nextStart;
	}

	public final Instruction encodeChoice(Choice p, Instruction next, Instruction failjump) {
		Expression pp = p.optimize(option);
		if(pp instanceof ByteMap) {
			Verbose.noticeOptimize("ByteMap", p, pp);
			return encodeByteMap((ByteMap)pp, next, failjump);
		}
		if(UFlag.is(option, Grammar.Prediction) && p.predictedCase != null) {
			return encodePredicatedChoice(p, next, failjump);
		}
		return this.encodeUnoptimizedChoice(p, next, failjump);
	}
	
	private final Instruction encodeBacktrack(Expression e, int ch, Instruction next) {
		//System.out.println("backtrack("+ch+"): " + e);
		return new IBacktrack(e, ch, next);
	}

	private final Instruction encodePredicatedChoice(Choice choice, Instruction next, Instruction failjump) {
		HashMap<Integer, Instruction> m = new HashMap<Integer, Instruction>();
		IDfaDispatch dispatch = new IDfaDispatch(choice, null);
		for(int ch = 0; ch < choice.predictedCase.length; ch++) {
			Expression predicated = choice.predictedCase[ch];
			Instruction inst = (predicated == null) ? commonFailure : m.get(predicated.getId());
			if(inst == null) {
				//System.out.println("creating '" + (char)ch + "'("+ch+"): " + e);
				if(predicated instanceof Choice) {
//					if(predicated == choice) {
						/* this is a rare case where the selected choice is the parent choice */
						/* this cause the repeated calls of the same matchers */
						inst = encodeBacktrack(choice, ch, this.encodeUnoptimizedChoice(choice, next, failjump));
						continue;
//					}
				}
				Expression factored = CharacterFactoring.s.tryFactoringCharacter(predicated);
				if(factored != null) {
					//System.out.println("factored("+ch+"): " + factored);
					inst = encodeExpression(factored, next, failjump);
				}
				else {
					inst = encodeBacktrack(predicated, ch, encodeExpression(predicated, next, failjump));
				}
				m.put(predicated.getId(), inst);
			}
			dispatch.setJumpTable(ch, inst);
		}
		return dispatch;
	}
	

	public final Instruction encodeUnoptimizedChoice(Choice p, Instruction next, Instruction failjump) {
		//System.out.println("@@@@@" + p);
		Instruction nextChoice = encodeExpression(p.get(p.size()-1), next, failjump);
		for(int i = p.size() -2; i >= 0; i--) {
			Expression e = p.get(i);
			nextChoice = new IFailPush(e, nextChoice, encodeExpression(e, new IFailPop(e, next), failjump));
		}
		return nextChoice;
	}

//	public final Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump) {
//		Production r = p.getProduction();
//		Expression pp = p.optimize(option);
//		if(pp instanceof ByteChar || pp instanceof ByteMap || pp instanceof AnyChar) {
//			Verbose.noticeOptimize("Inlining", p, pp);
//			return encodeExpression(pp, next, failjump);
//		}
//		if(r.isInline() && UFlag.is(option, Grammar.Inlining)) {
//			Verbose.noticeOptimize("Inlining", p, r.getExpression());
//			return encodeExpression(r.getExpression(), next, failjump);
//		}
//		if(this.enablePackratParsing()) {
//			if(!this.enableASTConstruction() || r.isPurePEG()) {
//				Expression ref = GrammarFactory.resolveNonTerminal(r.getExpression());
//				MemoPoint m = this.issueMemoPoint(r.getUniqueName(), ref);
//				if(m != null) {
//					if(UFlag.is(option, Grammar.Tracing)) {
//						IMonitoredSwitch monitor = new IMonitoredSwitch(p, new ICallPush(p.getProduction(), next));
//						Instruction inside = new ICallPush(r, newMemoize(p, monitor, m, next));
//						monitor.setActivatedNext(newLookup(p, monitor, m, inside, next, newMemoizeFail(p, monitor, m)));
//						return monitor;
//					}
//					Instruction inside = new ICallPush(r, newMemoize(p, IMonitoredSwitch.dummyMonitor, m, next));
//					return newLookup(p, IMonitoredSwitch.dummyMonitor, m, inside, next, newMemoizeFail(p, IMonitoredSwitch.dummyMonitor, m));
//				}
//			}
//		}	
//		return new ICallPush(r, next);
//	}
	
//	private Instruction newLookup(Expression e, IMonitoredSwitch monitor, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
//		if(m.contextSensitive) {
//			return new IStateLookup(e, monitor, m, next, skip, failjump);
//		}
//		return new ILookup(e, monitor, m, next, skip, failjump);
//	}
//
//	private Instruction newMemoize(Expression e, IMonitoredSwitch monitor, MemoPoint m, Instruction next) {
//		if(m.contextSensitive) {
//			return new IStateMemoize(e, monitor, m, next);
//		}
//		return new IMemoize(e, monitor, m, next);
//	}
//
//	private Instruction newMemoizeFail(Expression e, IMonitoredSwitch monitor, MemoPoint m) {
//		if(m.contextSensitive) {
//			return new IStateMemoizeFail(e, monitor, m);
//		}
//		return new IMemoizeFail(e, monitor, m);
//	}

	
	// AST Construction
	
	public final Instruction encodeLink(Link p, Instruction next, Instruction failjump) {
		if(this.enableASTConstruction()) {
//			if(this.enablePackratParsing()) {
//				Expression inner = GrammarFactory.resolveNonTerminal(p.get(0));
//				MemoPoint m = this.issueMemoPoint(p.toString(), inner);
//				if(m != null) {
//					if(UFlag.is(option, Grammar.Tracing)) {
//						IMonitoredSwitch monitor = new IMonitoredSwitch(p, encodeExpression(p.get(0), next, failjump));
//						Instruction inside = encodeExpression(p.get(0), newMemoizeNode(p, monitor, m, next), failjump);
//						monitor.setActivatedNext(newLookupNode(p, monitor, m, inside, next, new IMemoizeFail(p, monitor, m)));
//						return monitor;
//					}
//					Instruction inside = encodeExpression(p.get(0), newMemoizeNode(p, IMonitoredSwitch.dummyMonitor, m, next), failjump);
//					return newLookupNode(p, IMonitoredSwitch.dummyMonitor, m, inside, next, new IMemoizeFail(p, IMonitoredSwitch.dummyMonitor, m));
//				}
//			}
			return new INodePush(p, encodeExpression(p.get(0), new INodeStore(p, next), failjump));
		}
		return encodeExpression(p.get(0), next, failjump);
	}

//	private Instruction newLookupNode(Link e, IMonitoredSwitch monitor, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
//		if(m.contextSensitive) {
//			return new IStateLookupNode(e, monitor, m, next, skip, failjump);
//		}
//		return new ILookupNode(e, monitor, m, next, skip, failjump);
//	}
//
//	private Instruction newMemoizeNode(Link e, IMonitoredSwitch monitor, MemoPoint m, Instruction next) {
//		if(m.contextSensitive) {
//			return new IStateMemoizeNode(e, monitor, m, next);
//		}
//		return new IMemoizeNode(e, monitor, m, next);
//	}

	
	
}
