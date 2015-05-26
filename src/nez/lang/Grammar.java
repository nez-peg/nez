package nez.lang;

import java.util.TreeMap;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeFactory;
import nez.ast.ParsingFactory;
import nez.main.Recorder;
import nez.main.Verbose;
import nez.util.ConsoleUtils;
import nez.util.UFlag;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.DeprecatedNezCompiler;
import nez.vm.Instruction;
import nez.vm.Machine;
import nez.vm.MemoPoint;
import nez.vm.MemoTable;
import nez.vm.NezCompiler;
import nez.vm.NezCompiler1;

public class Grammar {
	Production start;
	UList<Production>          productionList;
	UMap<Production>           productionMap;

	Grammar(Production start, int option) {
		this.start = start;
		this.productionList = new UList<Production>(new Production[4]);
		this.productionMap = new UMap<Production>();
		this.setOption(option);
		TreeMap<String, Boolean> conditionMap = new TreeMap<String, Boolean>(); 
		analyze(start, conditionMap);
		if(conditionMap.size() > 0) {
			new ConditionalAnalysis(this);
		}
	}

	public Production getStartProduction() {
		return this.start;
	}

	public UList<Production> getProductionList() {
		return this.productionList;
	}

//	void reshapeAll(GrammarReshaper m) {
//		for(Production p: productionList) {
//			p = this.productionMap.get(p.getUniqueName());
//			Expression shaped = p.e.reshape(m);
//			if(shaped != gp.e) {
//				//System.out.println(m.getClass().getSimpleName() + ": " + p.getLocalName() + " = " + shaped);
//				gp.e = shaped;
//			}
//		}
//	}
	
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
			conditionMap.put(((IfFlag) p).getFlagName(), false);
		}
		for(Expression se : p) {
			analyze(se, conditionMap);
		}
	}

	/* --------------------------------------------------------------------- */
	/* memoization configuration */
	
	private Instruction compiledCode = null;
	private int option;
	
	private void setOption (int option) {
		if(this.option != option) {
			this.compiledCode = null; // recompile
		}
		if(UFlag.is(option, PackratParsing) && this.defaultMemoTable == null) {
			this.defaultMemoTable = MemoTable.newElasticTable(0, 0, 0);
		}
		this.option = option;
	}

	public final void enable(int option) {
		setOption(this.option | option);
	}

	public final void disable(int option) {
		setOption(UFlag.unsetFlag(this.option, option));
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
			NezCompiler bc = new NezCompiler1(this.option);
			compiledCode = bc.encode(this).startPoint;
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
	
	public NezCompiler cc() {
		NezCompiler bc = new NezCompiler1(this.option);
		bc.encode(this);
		return bc;
	}
		
	public final boolean match(SourceContext s) {
		boolean matched;
		Instruction pc = this.compile();
		s.initJumpStack(64, getMemoTable(s));
		if(Verbose.Debug) {
			matched = Machine.debug(pc, s);
		}
		else {
			matched = Machine.run(pc, s);
		}
		if(matched) {
			s.newTopLevelNode();
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

	public Object parse(SourceContext sc, ParsingFactory treeFactory) {
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
		return (CommonTree)this.parse(sc, new CommonTreeFactory());
	}

	public final CommonTree parseAST(String str) {
		SourceContext sc = SourceContext.newStringContext(str);
		return (CommonTree)this.parse(sc, new CommonTreeFactory());
	}

	public final void record(Recorder rec) {
		if(rec != null) {
			this.enable(Grammar.Profiling);
			this.compile();
			rec.setFile("G.File", this.start.getNameSpace().getURN());
			rec.setCount("G.NonTerminals", this.productionMap.size());
			rec.setCount("G.Instruction", this.InstructionSize);
			rec.setCount("G.MemoPoint", this.memoPointSize);
		}
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
	
	/* --------------------------------------------------------------------- */
	/* Production Option */
	
	public final static int ClassicMode = 1;
	public final static int ASTConstruction = 1 << 1;
	public final static int PackratParsing  = 1 << 2;
	public final static int Optimization    = 1 << 3;
	public final static int Specialization  = 1 << 4;
	public final static int CommonPrefix    = 1 << 5;
	public final static int Inlining        = 1 << 6;
	public final static int Prediction      = 1 << 7;
	public final static int DFA             = 1 << 8;
	public final static int Tracing         = 1 << 9;	
	public final static int Binary          = 1 << 10;
	public final static int Utf8            = 1 << 11;	
	public final static int Profiling       = 1 << 12;

	public final static int DefaultOption = ASTConstruction | PackratParsing | Optimization 
											| Specialization | Inlining | CommonPrefix | Prediction /* | Tracing */;
	public final static int RegexOption = ASTConstruction | PackratParsing | Optimization
											| Specialization | Prediction /* | Tracing */;
	public final static int SafeOption = ASTConstruction | Optimization;
	public final static int ExampleOption = Optimization | Specialization | Inlining | CommonPrefix | Prediction;
	
	public final static int mask(int m) {
		return Binary & m;
	}
	
	public final static String stringfyOption(int option, String delim) {
		StringBuilder sb = new StringBuilder();
		if(UFlag.is(option, Grammar.ClassicMode)) {
			sb.append(delim);
			sb.append("classic");
		}
		if(UFlag.is(option, Grammar.ASTConstruction)) {
			sb.append(delim);
			sb.append("ast");
		}
		if(UFlag.is(option, Grammar.PackratParsing)) {
			sb.append(delim);
			sb.append("memo");
		}
		if(UFlag.is(option, Grammar.Optimization)) {
			sb.append(delim);
			sb.append("opt.");
		}
		if(UFlag.is(option, Grammar.Specialization)) {
			sb.append(delim);
			sb.append("spe.");
		}
		if(UFlag.is(option, Grammar.CommonPrefix)) {
			sb.append(delim);
			sb.append("com.");
		}
		if(UFlag.is(option, Grammar.Inlining)) {
			sb.append(delim);
			sb.append("inline");
		}
		if(UFlag.is(option, Grammar.Prediction)) {
			sb.append(delim);
			sb.append("pdt.");
		}
		if(UFlag.is(option, Grammar.Tracing)) {
			sb.append(delim);
			sb.append("tracing");
		}
		if(UFlag.is(option, Grammar.DFA)) {
			sb.append(delim);
			sb.append("dfa");
		}
		if(UFlag.is(option, Grammar.Profiling)) {
			sb.append(delim);
			sb.append("prof");
		}
		String s = sb.toString();
		if(s.length() > 0) {
			return s.substring(delim.length());
		}
		return s;
	}
}