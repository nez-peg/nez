package nez.lang;

import java.util.List;
import java.util.TreeMap;

import nez.NezOption;
import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeTransducer;
import nez.ast.TreeTransducer;
import nez.main.Command;
import nez.main.NezProfier;
import nez.main.Verbose;
import nez.util.ConsoleUtils;
import nez.util.UFlag;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Instruction;
import nez.vm.Machine;
import nez.vm.MemoPoint;
import nez.vm.MemoTable;
import nez.vm.NezCompiler;
import nez.vm.NezDebugger;
import nez.vm.NezCompiler1;
import nez.vm.NezCompiler2;

public class Grammar {
	Production start;
	UList<Production>          productionList;
	UMap<Production>           productionMap;
	TreeMap<String, Boolean>   conditionMap;
	
	Grammar(Production start, NezOption option) {
		this.start = start;
		this.productionList = new UList<Production>(new Production[4]);
		this.productionMap = new UMap<Production>();
		this.setOption(option);
		
		conditionMap = start.isConditional() ? new TreeMap<String, Boolean>() : null; 
		analyze(start, conditionMap);
		if(conditionMap != null) {
			assert(conditionMap.size() > 0);
			//Verbose.debug("condition flow analysis: " + conditionMap.keySet());
			this.start = new ConditionalAnalysis(conditionMap).newStart(start);
			this.productionList = new UList<Production>(new Production[4]);
			this.productionMap = new UMap<Production>();
			analyze(this.start, conditionMap);
		}
	}	
	
	public final Production getStartProduction() {
		return this.start;
	}

	public final List<Production> getProductionList() {
		return this.productionList;
	}
		
	private void analyze(Production p, TreeMap<String, Boolean> conditionMap) {
		String uname = p.getUniqueName();
		if(productionMap.hasKey(uname)) {
			return;
		}
		productionList.add(p);
		productionMap.put(p.getUniqueName(), p);
		analyze(p.getExpression(), conditionMap);
	}
	
	private void analyze(Expression p, TreeMap<String, Boolean> conditionMap) {
		if(p instanceof NonTerminal) {
			analyze(((NonTerminal) p).getProduction(), conditionMap);
		}
		if(p instanceof IfFlag) {
			conditionMap.put(((IfFlag) p).getFlagName(), true);
		}
		for(Expression se : p) {
			analyze(se, conditionMap);
		}
	}
	
	/* --------------------------------------------------------------------- */
	/* profiler */

	private NezProfier prof = null;
	
	public void setProfiler(NezProfier prof) {
		this.prof = prof;
		if(prof != null) {
			this.compile();
			prof.setFile("G.File", this.start.getGrammarFile().getURN());
			prof.setCount("G.Production", this.productionMap.size());
			prof.setCount("G.Instruction", this.InstructionSize);
			prof.setCount("G.MemoPoint", this.memoPointSize);
		}
	}

	public NezProfier getProfiler() {
		return this.prof;
	}

	public void logProfiler() {
		if(prof != null) {
			prof.log();
		}
	}

	/* --------------------------------------------------------------------- */
	/* memoization configuration */
	
	private Instruction compiledCode = null;
	private NezOption option;
	
	public final NezOption getNezOption() {
		return this.option;
	}

	private void setOption (NezOption option) {
		this.option = option;
		this.compiledCode = null;
	}

	private MemoTable defaultMemoTable;
	private int windowSize = 32;
	private int memoPointSize;
	private int InstructionSize;
	private UList<MemoPoint> memoPointList = null;

	public void config(MemoTable memoTable, int windowSize) {
		this.windowSize = windowSize;
		this.defaultMemoTable = memoTable;
	}
	
	private MemoTable getMemoTable(SourceContext sc) {
		if(memoPointSize == 0) {
			return MemoTable.newNullTable(sc.length(), this.windowSize, this.memoPointSize);
		}
		return this.defaultMemoTable.newMemoTable(sc.length(), this.windowSize, this.memoPointSize);
	}

	public final Instruction compile() {
		if(compiledCode == null) {
			NezCompiler bc = Command.ReleasePreview ? new NezCompiler2(this.option) : new NezCompiler1(this.option);
			compiledCode = bc.compile(this).startPoint;
//			this.InstructionSize  = bc.getInstructionSize();
//			this.memoPointSize = bc.getMemoPointSize();
//			if(Verbose.PackratParsing) {
//				this.memoPointList = bc.getMemoPointList();
//			}
//			if(Verbose.VirtualMachine) {
//				bc.dump(this.productionList);
//			}
		}
		return compiledCode;
	}
	
//	public NezCompiler cc() {
//		NezCompiler bc = Command.ReleasePreview ? new NezCompiler2(this.option) : new NezCompiler1(this.option);
//		bc.encode(this);
//		return bc;
//	}
		
	public final boolean match(SourceContext s) {
		boolean matched;
		Instruction pc;
		s.initJumpStack(64, getMemoTable(s));
// REAL FIXME
//		if(this.option == NezOption.DebugOption) { // FIXME
//			NezCompiler c = new NezCompiler1(this.option);
//			pc = c.compile(this).startPoint;
//			NezDebugger debugger = new NezDebugger(this, pc, s);
//			matched = debugger.exec();
//		}
		pc = this.compile();
		if(prof != null) {
			s.startProfiling(prof);
		}
		if(Verbose.Debug) {
			matched = Machine.debug(pc, s);
		}
		else {
			matched = Machine.run(pc, s);
		}
		if(matched) {
			s.newTopLevelNode();
		}
		if(prof != null) {
			s.doneProfiling(prof);
		}
		return matched;
	}

	/* --------------------------------------------------------------------- */
		
	public final boolean match(String str) {
		SourceContext sc = SourceContext.newStringContext(str);
		if(match(sc)) {
			return (!sc.hasUnconsumed());
		}
		return false;
	}

	public Object parse(SourceContext sc, TreeTransducer treeFactory) {
		long startPosition = sc.getPosition();
		sc.setFactory(treeFactory);
		if(!this.match(sc)) {
			return null;
		}
		Object node = sc.getParsingObject();
		if(node == null) {
			node = treeFactory.newNode(null, sc, startPosition, sc.getPosition(), 0, null);
		}
//		else {
//			sc.commitConstruction(0, node);
//		}
		return treeFactory.commit(node);
	}

	public final CommonTree parse(SourceContext sc) {
		return (CommonTree)this.parse(sc, new CommonTreeTransducer());
	}

	public final CommonTree parseAST(String str) {
		SourceContext sc = SourceContext.newStringContext(str);
		return (CommonTree)this.parse(sc, new CommonTreeTransducer());
	}

	public final void verboseMemo() {
		if(Verbose.PackratParsing && this.memoPointList != null) {
			ConsoleUtils.println("ID\tPEG\tCount\tHit\tFail\tMean");
			for(MemoPoint p: this.memoPointList) {
				String s = String.format("%d\t%s\t%d\t%f\t%f\t%f", p.id, p.label, p.count(), p.hitRatio(), p.failHitRatio(), p.meanLength());
				ConsoleUtils.println(s);
			}
			ConsoleUtils.println("");
		}
	}


}