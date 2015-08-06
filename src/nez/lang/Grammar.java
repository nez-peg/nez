package nez.lang;

import java.util.List;
import java.util.TreeMap;

import nez.NezOption;
import nez.SourceContext;
import nez.ast.AbstractTree;
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
import nez.vm.NezCode;
import nez.vm.NezCompiler;
import nez.vm.NezDebugger;
import nez.vm.OptimizedCompiler;
import nez.vm.PlainCompiler;
import nez.vm.PackratCompiler;

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
			prof.setCount("G.Instruction", this.compiledCode.getInstructionSize());
			prof.setCount("G.MemoPoint", this.compiledCode.getMemoPointSize());
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
	
	private NezOption option;
	private NezCode compiledCode = null;
	
	public final NezOption getNezOption() {
		return this.option;
	}

	private void setOption (NezOption option) {
		this.option = option;
		this.compiledCode = null;
	}
	
	private MemoTable getMemoTable(SourceContext sc) {
		return MemoTable.newTable(option, sc.length(), 32, this.compiledCode.getMemoPointSize());
	}

	public final Instruction compile() {
		if(compiledCode == null) {
//			NezCompiler bc = Command.ReleasePreview ? new PackratCompiler(this.option) : new PlainCompiler(this.option);
			NezCompiler bc = new OptimizedCompiler(this.option);
			compiledCode = bc.compile(this);
//			if(Verbose.VirtualMachine) {
//				bc.dump(this.productionList);
//			}
		}
		return compiledCode.getStartPoint();
	}
			
	public final boolean perform(Machine machine, SourceContext s, TreeTransducer treeTransducer) {
		Instruction pc = this.compile();
		s.init(getMemoTable(s), treeTransducer);
		if(prof != null) {
			s.startProfiling(prof);
			boolean matched = machine.run(pc,  s);
			s.doneProfiling(prof);
			if(Verbose.PackratParsing) {
				this.compiledCode.dumpMemoPointList();
			}
			return matched;
		}
		return machine.run(pc,  s);
	}

	public final boolean debug(SourceContext s) {
		boolean matched;
		Instruction pc;
		s.init(getMemoTable(s), null);
		NezCompiler c = new PlainCompiler(this.option);
		pc = c.compile(this).getStartPoint();
		NezDebugger debugger = new NezDebugger(this, pc, s);
		matched = debugger.exec();
		return matched;
	}

	/* --------------------------------------------------------------------- */
		
	public final boolean match(SourceContext s) {
		return perform(new Machine(), s, null);
	}

	public final boolean match(String str) {
		SourceContext sc = SourceContext.newStringContext(str);
		if(perform(new Machine(), sc, null)) {
			return (!sc.hasUnconsumed());
		}
		return false;
	}

	public Object parse(SourceContext sc, TreeTransducer treeTransducer) {
		long startPosition = sc.getPosition();
		if(!this.perform(new Machine(), sc, treeTransducer)) {
			return null;
		}
		return sc.getParseResult(startPosition, sc.getPosition());
	}

	public final CommonTree parseCommonTree(SourceContext sc) {
		return (CommonTree)this.parse(sc, new CommonTreeTransducer());
	}
	
	public final CommonTree parseCommonTree(String str) {
		SourceContext sc = SourceContext.newStringContext(str);
		return (CommonTree)this.parse(sc, new CommonTreeTransducer());
	}

}