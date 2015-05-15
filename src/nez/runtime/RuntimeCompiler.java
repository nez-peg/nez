package nez.runtime;

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
import nez.lang.Expression;
import nez.lang.Factory;
import nez.lang.Grammar;
import nez.lang.IsIndent;
import nez.lang.IsSymbol;
import nez.lang.Link;
import nez.lang.Manipulator;
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

public class RuntimeCompiler {
	final int option;
	
	class CodeBlock {
		Instruction head;
		int start;
		int end;
	}

	public UList<Instruction> codeList;
	UMap<CodeBlock> ruleMap;
	HashMap<Integer, MemoPoint> memoMap;
	
	public RuntimeCompiler(int option) {
		this.option = option;
		this.codeList = new UList<Instruction>(new Instruction[64]);
		this.ruleMap = new UMap<CodeBlock>();
		if(this.enablePackratParsing()) {
			this.memoMap = new HashMap<Integer, MemoPoint>();
			this.visitedMap = new UMap<String>();
		}
	}
	
	protected final boolean enablePackratParsing() {
		return UFlag.is(this.option, Grammar.PackratParsing);
	}

	protected final boolean enableASTConstruction() {
		return UFlag.is(this.option, Grammar.ASTConstruction);
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
	
	public final int getInstructionSize() {
		return this.codeList.size();
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
	
	public final Instruction encode(UList<Production> ruleList) {
		long t = System.nanoTime();
		for(Production r : ruleList) {
			String uname = r.getUniqueName();
			if(Verbose.Debug) {
				Verbose.debug("compiling .. " + r);
			}
			Expression e = r.getExpression();
			if(UFlag.is(option, Grammar.Inlining)  && this.ruleMap.size() > 0 && r.isInline() ) {
				//System.out.println("skip .. " + r.getLocalName() + "=" + e);
				continue;
			}
			if(!UFlag.is(option, Grammar.ASTConstruction)) {
				e = e.reshape(Manipulator.RemoveAST);
			}
			CodeBlock block = new CodeBlock();
			block.head = encodeExpression(e, new IRet(r));
			block.start = codeList.size();
			this.ruleMap.put(uname, block);
			verify(block.head);
			block.end = codeList.size();
		}
		for(Instruction inst : codeList) {
			if(inst instanceof ICallPush) {
				CodeBlock deref = this.ruleMap.get(((ICallPush) inst).rule.getUniqueName());
				((ICallPush) inst).setResolvedJump(deref.head);
			}
		}
		long t2 = System.nanoTime();
		//Verbose.printElapsedTime("CompilingTime", t, t2);
		return this.codeList.ArrayValues[0];
	}

	void test(Expression e, Instruction inst) {
		boolean found = false;
		for(int c = 0; c < 257; c++) {
			if(e.acceptByte(c, this.option) != inst.isAcceptImpl(c)) {
				found = true;
				break;
			}
		}
		if(found) {
			Verbose.printSelfTesting("\nTesting prediction " +  e + " .. ");
			for(int c = 0; c < 256; c++) {
				short a = e.acceptByte(c, option);
				short a2 = inst.isAcceptImpl(c);
				if(a != a2) {
					if(a == Prediction.Accept) {
						Verbose.printSelfTestingIndent("[Failed] Accept " + StringUtils.formatChar(c) + ": " + a2);
						break;
					}
					if(a == Prediction.Reject) {
						Verbose.printSelfTestingIndent("[Failed] Reject " + StringUtils.formatChar(c) + ": " + a2);
						break;
					}
					if(a == Prediction.Unconsumed) {
						Verbose.printSelfTestingIndent("[Failed] Unconsumed " + StringUtils.formatChar(c) + ": " + a2);
						break;
					}
				}
			}
			Verbose.printSelfTesting("\nPlease report the above to " + Verbose.BugsReport1);
		}	
	}
	
	void verify(Instruction inst) {
		if(inst != null) {
			if(inst.id == -1) {
				inst.id = this.codeList.size();
				this.codeList.add(inst);
				verify(inst.next);
				if(inst.next != null && inst.id + 1 != inst.next.id) {
					Instruction.labeling(inst.next);
				}
				verify(inst.branch());
				if(inst instanceof IDfaDispatch) {
					IDfaDispatch match = (IDfaDispatch)inst;
					for(int ch = 0; ch < match.jumpTable.length; ch ++) {
						verify(match.jumpTable[ch]);
					}
				}
				//encode(inst.branch2());
			}
		}
	}
	
	public void dump(UList<Production> ruleList) {
		for(Production r : ruleList) {
			String uname = r.getUniqueName();
			ConsoleUtils.println(uname + ":");
			CodeBlock block = this.ruleMap.get(uname);
			for(int i = block.start; i < block.end; i++) {
				Instruction inst = codeList.ArrayValues[i];
				if(inst.label) {
					ConsoleUtils.println("" + inst.id + "*\t" + inst);
				}
				else {
					ConsoleUtils.println("" + inst.id + "\t" + inst);
				}
				if(inst.next != null && inst.next.id != i+1) {
					ConsoleUtils.println("\tjump " + Instruction.label(inst.next));
				}
			}
		}
	}
	
	// encoding

	private Instruction failed = new IFail(null);
	
	public final Instruction encodeExpression(Expression e, Instruction next) {
		return e.encode(this, next);
	}
	
	public final Instruction encodeMatchAny(AnyChar p, Instruction next) {
		return new IAnyChar(p, next);
	}

	public final Instruction encodeByteChar(ByteChar p, Instruction next) {
		return new IByteChar(p, next);
	}

	public final Instruction encodeByteMap(ByteMap p, Instruction next) {
		return new IByteMap(p, next);
	}

	public Instruction encodeFail(Expression p) {
		return new IFail(p);
	}

	public final boolean isDisjoint(boolean[] dfa, boolean[] dfa2) {
		for(int i = 0; i < dfa.length; i++) {
			if(dfa[i] && dfa2[i]) {
				return false;
			}
		}
		return true;
	}

	public final boolean[] and(boolean[] dfa, boolean[] dfa2) {
		for(int i = 0; i < dfa.length; i++) {
			if(dfa[i] && dfa2[i]) {
				dfa[i] = true;
			}
			else {
				dfa[i] = false;
			}
		}
		return dfa;
	}

	
	private boolean[] predictNext(Instruction next) {
		boolean[] dfa = ByteMap.newMap(true);
		for(int c = 0; c < dfa.length; c++) {
			short r = next.isAcceptImpl(c);
			if(r == Prediction.Reject) {
				dfa[c] = false;
			}
		}
		return dfa;
	}

	private boolean[] predictInner(Expression e, boolean[] dfa2) {
		boolean[] dfa = dfa2.clone();
		for(int c = 0; c < dfa.length; c++) {
			short r = e.acceptByte(c, option);
			if(r == Prediction.Accept) {
				dfa[c] = true;
			}
			if(r == Prediction.Reject) {
				dfa[c] = false;
			}		
		}
		return dfa;
	}

	private boolean checkInstruction(Instruction next) {
		return next instanceof IByteChar || next instanceof IByteMap;
	}

	private Instruction replaceConsumeInstruction(Instruction inst) {
		if(inst instanceof IByteChar || inst instanceof IByteMap || inst instanceof IAnyChar) {
			System.out.println("replaced: " + inst);
			return new IConsume(inst.e, inst.next);
		}
		return inst;
	}

	
	
	public final Instruction encodeOption(Option p, Instruction next) {
		if(UFlag.is(option, Grammar.DFA) && checkInstruction(next)) {
			boolean[] dfa = predictNext(next);
			boolean[] optdfa = predictInner(p.get(0), dfa);
			if(isDisjoint(dfa, optdfa)) {
				Instruction opt = replaceConsumeInstruction(encodeExpression(p.get(0), next));
				next = replaceConsumeInstruction(next);
				IDfaDispatch match = new IDfaDispatch(p, failed);
				for(int ch = 0; ch < dfa.length; ch++) {
					if(optdfa[ch]) {
						dfa[ch] = true;
						match.setJumpTable(ch, opt);
						continue;
					}
					if(dfa[ch]) {
						match.setJumpTable(ch, next);
					}
				}
//				if(!(next instanceof IByteChar)) {
//					System.out.println("DFA: " + p + " " + next.e + "  ## " + next.getClass());
//				}
				return match;
			}
//			System.out.println("NFA: " + p + " " + next.e);
//			System.out.println("NFA: " + StringUtils.stringfyCharClass(and(dfa, optdfa)));
		}
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
		return new IFailPush(p, next, encodeExpression(p.get(0), pop));
	}
	
	public final Instruction encodeRepetition(Repetition p, Instruction next) {
		if(UFlag.is(option, Grammar.DFA) && !p.possibleInfiniteLoop && checkInstruction(next)) {
			boolean[] dfa = predictNext(next);
			boolean[] optdfa = predictInner(p.get(0), dfa);
			if(isDisjoint(dfa, optdfa)) {
				IDfaDispatch match = new IDfaDispatch(p, failed);
				Instruction opt = replaceConsumeInstruction(encodeExpression(p.get(0), match));
				next = replaceConsumeInstruction(next);
				System.out.println("DFA: " + p + " " + next.e);
				System.out.println("  1 " + p + " " + StringUtils.stringfyCharClass(optdfa));
				System.out.println("  2 " + next.e + " " + StringUtils.stringfyCharClass(dfa));
				for(int ch = 0; ch < dfa.length; ch++) {
					if(optdfa[ch]) {
						match.setJumpTable(ch, opt);
						continue;
					}
					if(dfa[ch]) {
						match.setJumpTable(ch, next);
					}
				}
				return match;
			}
		}
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
		Instruction start = encodeExpression(p.get(0), skip);
		skip.next = start;
		return new IFailPush(p, next, start);
	}

	public final Instruction encodeRepetition1(Repetition1 p, Instruction next) {
		return encodeExpression(p.get(0), this.encodeRepetition(p, next));
	}

	public final Instruction encodeAnd(And p, Instruction next) {
		Instruction inner = encodeExpression(p.get(0), new IPosBack(p, next));
		return new IPosPush(p, inner);
	}

	public final Instruction encodeNot(Not p, Instruction next) {
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
		return new INotFailPush(p, next, encodeExpression(p.get(0), fail));
	}

	public final Instruction encodeSequence(Expression p, Instruction next) {
		Expression pp = p.optimize(option);
		if(pp != p) {
			if(pp instanceof ByteMap) {
				Verbose.noticeOptimize("ByteMap", p, pp);
				return encodeByteMap((ByteMap)pp, next);
			}
		}
		Instruction nextStart = next;
		for(int i = p.size() -1; i >= 0; i--) {
			Expression e = p.get(i);
			nextStart = encodeExpression(e, nextStart);
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

	public final Instruction encodeChoice(Choice p, Instruction next) {
		Expression pp = p.optimize(option);
		if(pp instanceof ByteMap) {
			Verbose.noticeOptimize("ByteMap", p, pp);
			return encodeByteMap((ByteMap)pp, next);
		}
		if(UFlag.is(option, Grammar.DFA)) {
			if(pp instanceof Choice) {
				p = (Choice)pp;
			}
			return encodeDfaChoice(p, next);
		}
		if(p.matchCase != null) {
			return encodePrefetchChoice(p, next);
		}
		return this.encodeUnoptimizedChoice(p, next);
	}
	
	private void predictAll(Expression e, boolean[] dfa, Instruction next, short[] nextAcceptance) {
		for(int c = 0; c < dfa.length; c++) {
			short r = e.acceptByte(c, option);
			if(r == Prediction.Reject) {
				dfa[c] = false;
				continue;
			}
			if(r == Prediction.Unconsumed) {
				if(nextAcceptance[c] == -1) {
					nextAcceptance[c] = next.isAcceptImpl(c);
				}
				if(nextAcceptance[c] == Prediction.Reject) {
					dfa[c] = false;
					continue;
				}
			}
		}
	}
	
	private Instruction encodeDfaChoice(Choice p, Instruction next) {
		boolean[][] dfas = new boolean[p.size()][];
		short[] nextAcceptCache = new short[257];
		Arrays.fill(nextAcceptCache, (short)-1);
		for(int i = 0; i < p.size(); i++) {
			dfas[i] = ByteMap.newMap(true);
			predictAll(p.get(i), dfas[i], next, nextAcceptCache);
		}
		//System.out.println("start .. " + p);
		HashMap<Integer, Instruction> m = new HashMap<Integer, Instruction>();
		IDfaDispatch dispatch = new IDfaDispatch(p, null);
		for(int c = 0; c < 257; c++) {
			Expression merged = null;
			for(int i = 0; i < p.size(); i++) {
				if(dfas[i][c]) {
					merged = mergeChoice(merged, p.get(i).optimize(option));
				}
			}
			if(merged == null) {
				dispatch.setJumpTable(c, failed);
				continue;
			}
			Integer key = merged.getId();
			Instruction inst = m.get(key);
			if(inst == null) {
//				if(p.matchCase != null && p.matchCase[c] != merged) {
//					System.out.println("# " + p);
//					System.out.println("NEW " + StringUtils.formatChar(c) + ": " + merged);
//					System.out.println("OLD " + StringUtils.formatChar(c) + ": " + p.matchCase[c]);
//				}
				if(merged == p || merged instanceof Choice) {
					/* this is a rare case where the selected choice is the parent choice */
					/* this cause the repeated calls of the same matchers */
					//System.out.println("NFA: " + StringUtils.formatChar(c) + ": " + merged);
					Verbose.printNFA(StringUtils.formatChar(c) + ": " + merged);
					inst = this.encodeUnoptimizedChoice((Choice)merged, next);
				}
				else {
					inst = encodeExpression(merged, next);
				}
				inst = this.replaceConsumeInstruction(inst);
				m.put(key, inst);
			}
			dispatch.setJumpTable(c, inst);
		}
		//System.out.println("end .. " + p);
		return dispatch;
	}
	
	Expression mergeChoice(Expression p, Expression p2) {
		if(p == null) {
			return p2;
		}
//		if(p instanceof Choice) {
//			Expression last = p.get(p.size() - 1);
//			Expression common = makeCommonChoice(last, p2);
//			if(common == null) {
//				return Factory.newChoice(null, p, p2);
//			}
//		}
		Expression common = makeCommonChoice(p, p2, true);
		if(common == null) {
			return Factory.newChoice(null, p, p2);
		}
		return common;
	}
	
	private final Expression makeCommonChoice(Expression e, Expression e2, boolean ignoredFirstChar) {
		int min = sizeAsSequence(e) < sizeAsSequence(e2) ? sizeAsSequence(e) : sizeAsSequence(e2);
		int commonIndex = -1;
		for(int i = 0; i < min; i++) {
			Expression p = retrieveAsList(e, i);
			Expression p2 = retrieveAsList(e2, i);
			if(ignoredFirstChar && i == 0) {
				if(Expression.isByteConsumed(p.optimize(this.option)) && Expression.isByteConsumed(p2.optimize(this.option))) {
					commonIndex = i + 1;
					continue;
				}
			}
			if(p.getId() != p2.getId()) {
				break;
			}
			commonIndex = i + 1;
		}
		if(commonIndex == -1) {
			return null;
		}
		UList<Expression> common = new UList<Expression>(new Expression[commonIndex]);
		for(int i = 0; i < commonIndex; i++) {
			common.add(retrieveAsList(e, i));
		}
		UList<Expression> l1 = new UList<Expression>(new Expression[sizeAsSequence(e)]);
		for(int i = commonIndex; i < sizeAsSequence(e); i++) {
			l1.add(retrieveAsList(e, i));
		}
		UList<Expression> l2 = new UList<Expression>(new Expression[sizeAsSequence(e2)]);
		for(int i = commonIndex; i < sizeAsSequence(e2); i++) {
			l2.add(retrieveAsList(e2, i));
		}
		UList<Expression> l3 = new UList<Expression>(new Expression[2]);
		Factory.addChoice(l3, Factory.newSequence(null, l1));
		Factory.addChoice(l3, Factory.newSequence(null, l2));
		Factory.addSequence(common, Factory.newChoice(null, l3));
		return Factory.newSequence(null, common);
	}
	
	private final Instruction encodePrefetchChoice(Choice p, Instruction next) {
		HashMap<Integer, Instruction> m = new HashMap<Integer, Instruction>();
		IDfaDispatch dispatch = new IDfaDispatch(p, null);
		for(int ch = 0; ch < p.matchCase.length; ch++) {
			Expression merged = p.matchCase[ch];
			Integer key = merged.getId();
			Instruction inst = m.get(key);
			if(inst == null) {
				//System.out.println("creating '" + (char)ch + "'("+ch+"): " + e);
				if(merged == p) {
					/* this is a rare case where the selected choice is the parent choice */
					/* this cause the repeated calls of the same matchers */
					Expression common = this.makeCommonPrefix(p);
					if(common != null) {
						//System.out.println("@common '" + (char)ch + "'("+ch+"): " + e + "\n=>\t" + common);
						inst = encodeExpression(common, next);
					}
					else {
						inst = this.encodeUnoptimizedChoice(p, next);
					}
				}
				else {
					if(merged instanceof Choice) {
						Expression common = this.makeCommonPrefix((Choice)merged);
						if(common != null) {
							//System.out.println("@common '" + (char)ch + "'("+ch+"): " + e + "\n=>\t" + common);
							inst = encodeExpression(common, next);
						}
						else {
							inst = this.encodeUnoptimizedChoice((Choice)merged, next);
						}
					}
					else {
						inst = encodeExpression(merged, next);
					}
				}
				m.put(key, inst);
			}
			dispatch.setJumpTable(ch, inst);
		}
		return dispatch;
	}
	
//	private final boolean checkStartingTerminal(Expression e, int ch) {
//		e = Factory.resolveNonTerminal(e);
//		if(e instanceof ByteChar) {
//			return (((ByteChar) e).byteChar == ch);
//		}
//		if(e instanceof Sequence) {
//			return checkStartingTerminal(e.get(0), ch);
//		}
//		if(e instanceof Choice) {
//			for(Expression p: e) {
//				if(!checkStartingTerminal(p, ch)) {
//					return false;
//				}
//			}
//			return true;
//		}
//		return false;
//	}
//
//	private final Expression trimStartingTerminal(Expression e, int ch) {
//		if(e instanceof Sequence) {
//			UList<Expression> l = new UList<Expression>(new Expression[e.size()-1]);
//			for(int i = 1; i < e.size(); i++) {
//				l.add(e.get(i));
//			}
//			return Factory.newSequence(null, l);
//		}
//		if(e instanceof Choice) {
//			UList<Expression> l = new UList<Expression>(new Expression[e.size()]);
//			for(Expression p: e) {
//				Factory.addChoice(l, p);
//			}
//			return Factory.newChoice(null, l);
//		}
//		assert(e instanceof ByteChar);
//		return Factory.newEmpty(null);
//	}
//
//	private final Expression trimStartingTerminal(Expression e) {
//		if(e instanceof Sequence) {
//			UList<Expression> l = new UList<Expression>(new Expression[e.size()-1]);
//			for(int i = 1; i < e.size(); i++) {
//				l.add(e.get(i));
//			}
//			return Factory.newSequence(null, l);
//		}
//		if(e instanceof Choice) {
//			UList<Expression> l = new UList<Expression>(new Expression[e.size()]);
//			for(Expression p: e) {
//				Factory.addChoice(l, p);
//			}
//			return Factory.newChoice(null, l);
//		}
//		assert(e instanceof ByteChar);
//		return Factory.newEmpty(null);
//	}

	private final Expression trimCommonPrefix(Expression e, Expression e2) {
		int min = sizeAsSequence(e) < sizeAsSequence(e2) ? sizeAsSequence(e) : sizeAsSequence(e2);
		int commonIndex = -1;
		for(int i = 0; i < min; i++) {
			Expression p = retrieveAsList(e, i);
			Expression p2 = retrieveAsList(e2, i);
			if(p.getId() != p2.getId()) {
				break;
			}
			commonIndex = i + 1;
		}
		if(commonIndex == -1) {
			return null;
		}
		UList<Expression> common = new UList<Expression>(new Expression[commonIndex]);
		for(int i = 0; i < commonIndex; i++) {
			common.add(retrieveAsList(e, i));
		}
		UList<Expression> l1 = new UList<Expression>(new Expression[sizeAsSequence(e)]);
		for(int i = commonIndex; i < sizeAsSequence(e); i++) {
			l1.add(retrieveAsList(e, i));
		}
		UList<Expression> l2 = new UList<Expression>(new Expression[sizeAsSequence(e2)]);
		for(int i = commonIndex; i < sizeAsSequence(e2); i++) {
			l2.add(retrieveAsList(e2, i));
		}
		UList<Expression> l3 = new UList<Expression>(new Expression[2]);
		Factory.addChoice(l3, Factory.newSequence(null, l1));
		Factory.addChoice(l3, Factory.newSequence(null, l2));
		Factory.addSequence(common, Factory.newChoice(null, l3));
		return Factory.newSequence(null, common);
	}

	private final Expression makeCommonPrefix(Choice p) {
		if(!UFlag.is(this.option, Grammar.CommonPrefix)) {
			return null;
		}
		int start = 0;
		Expression common = null;
		for(int i = 0; i < p.size() - 1; i++) {
			Expression e = p.get(i);
			Expression e2 = p.get(i+1);
			if(retrieveAsList(e,0).getId() == retrieveAsList(e2,0).getId()) {
				common = trimCommonPrefix(e, e2);
				start = i;
				break;
			}
		}
		if(common == null) {
			return null;
		}
		UList<Expression> l = new UList<Expression>(new Expression[p.size()]);
		for(int i = 0; i < start; i++) {
			Expression e = p.get(i);
			l.add(e);
		}
		for(int i = start + 2; i < p.size(); i++) {
			Expression e = p.get(i);
			if(retrieveAsList(common, 0).getId() == retrieveAsList(e,0).getId()) {
				e = trimCommonPrefix(common, e);
				if(e != null) {
					common = e;
					continue;
				}
			}
			l.add(common);
			common = e;
		}
		l.add(common);
		return Factory.newChoice(null, l);
	}

	private final int sizeAsSequence(Expression e) {
		if(e instanceof NonTerminal) {
			e = Factory.resolveNonTerminal(e);
		}
		if(e instanceof Sequence) {
			return e.size();
		}
		return 1;
	}

	private final Expression retrieveAsList(Expression e, int index) {
		if(e instanceof NonTerminal) {
			e = Factory.resolveNonTerminal(e);
		}
		if(e instanceof Sequence) {
			return e.get(index);
		}
		return e;
	}

	public final Instruction encodeUnoptimizedChoice(Choice p, Instruction next) {
		//System.out.println("@@@@@" + p);
		Instruction nextChoice = encodeExpression(p.get(p.size()-1), next);
		for(int i = p.size() -2; i >= 0; i--) {
			Expression e = p.get(i);
			nextChoice = new IFailPush(e, nextChoice, encodeExpression(e, new IFailPop(e, next)));
		}
		return nextChoice;
	}

	public final Instruction encodeNonTerminal(NonTerminal p, Instruction next) {
		Production r = p.getProduction();
		Expression pp = p.optimize(option);
		if(pp instanceof ByteChar || pp instanceof ByteMap || pp instanceof AnyChar) {
			Verbose.noticeOptimize("Inlining", p, pp);
			return encodeExpression(pp, next);
		}
		if(r.isInline() && UFlag.is(option, Grammar.Inlining)) {
			Verbose.noticeOptimize("Inlining", p, r.getExpression());
			return encodeExpression(r.getExpression(), next);
		}
		if(this.enablePackratParsing()) {
			if(!this.enableASTConstruction() || r.isPurePEG()) {
				Expression ref = Factory.resolveNonTerminal(r.getExpression());
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
	
	public final Instruction encodeLink(Link p, Instruction next) {
		if(this.enableASTConstruction()) {
			if(this.enablePackratParsing()) {
				Expression inner = Factory.resolveNonTerminal(p.get(0));
				MemoPoint m = this.issueMemoPoint(p.toString(), inner);
				if(m != null) {
					if(UFlag.is(option, Grammar.Tracing)) {
						IMonitoredSwitch monitor = new IMonitoredSwitch(p, encodeExpression(p.get(0), next));
						Instruction inside = encodeExpression(p.get(0), newMemoizeNode(p, monitor, m, next));
						monitor.setActivatedNext(newLookupNode(p, monitor, m, inside, next, new IMemoizeFail(p, monitor, m)));
						return monitor;
					}
					Instruction inside = encodeExpression(p.get(0), newMemoizeNode(p, IMonitoredSwitch.dummyMonitor, m, next));
					return newLookupNode(p, IMonitoredSwitch.dummyMonitor, m, inside, next, new IMemoizeFail(p, IMonitoredSwitch.dummyMonitor, m));
				}
			}
			return new INodePush(p, encodeExpression(p.get(0), new INodeStore(p, next)));
		}
		return encodeExpression(p.get(0), next);
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

	
//	public final Instruction encodeNewClosure(NewClosure p, Instruction next) {
//		if(this.enableASTConstruction()) {
//			return new INew(p, this.encodeSequence(p, new ICapture(p, next), dfa));
//		}
//		return this.encodeSequence(p, next, dfa);
//	}
//
//	public final Instruction encodeLeftNewClosure(LeftNewClosure p, Instruction next) {
//		if(this.enableASTConstruction()) {
//			return new ILeftNew(p, this.encodeSequence(p, new ICapture(p, next), dfa));
//		}
//		return this.encodeSequence(p, next, dfa);
//	}

	public final Instruction encodeNew(New p, Instruction next) {
		if(this.enableASTConstruction()) {
			return p.lefted ? new ILeftNew(p, next) : new INew(p, next);
		}
		return next;
	}

	public final Instruction encodeCapture(Capture p, Instruction next) {
		if(this.enableASTConstruction()) {
			return new ICapture(p, next);
		}
		return next;
	}
	
	public final Instruction encodeTagging(Tagging p, Instruction next) {
		if(this.enableASTConstruction()) {
			return new ITag(p, next);
		}
		return next;
	}

	public final Instruction encodeReplace(Replace p, Instruction next) {
		if(this.enableASTConstruction()) {
			return new IReplace(p, next);
		}
		return next;
	}
	
	public final Instruction encodeBlock(Block p, Instruction next) {
		Instruction failed = new ITablePop(p, new IFail(p));
		Instruction inner = encodeExpression(p.get(0), new IFailPop(p, new ITablePop(p, next)));
		return new ITablePush(p, new IFailPush(p, failed, inner));
	}
	
	public final Instruction encodeDefSymbol(DefSymbol p, Instruction next) {
		Instruction inner = encodeExpression(p.get(0), new IDefSymbol(p, next));
		return new IPosPush(p, inner);
	}
	
	public final Instruction encodeIsSymbol(IsSymbol p, Instruction next) {
		Instruction inner = encodeExpression(p.getSymbolExpression(), new IIsSymbol(p, p.checkLastSymbolOnly, next));
		return new IPosPush(p, inner);
	}
	
	public final Instruction encodeDefIndent(DefIndent p, Instruction next) {
		return new IDefIndent(p, next);
	}
	
	public final Instruction encodeIsIndent(IsIndent p, Instruction next) {
		return new IIsIndent(p, next);
	}

}
