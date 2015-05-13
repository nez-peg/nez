package nez.lang;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeFactory;
import nez.ast.ParsingFactory;
import nez.main.Recorder;
import nez.main.Verbose;
import nez.runtime.Instruction;
import nez.runtime.MemoPoint;
import nez.runtime.MemoTable;
import nez.runtime.RuntimeCompiler;
import nez.util.ConsoleUtils;
import nez.util.UFlag;
import nez.util.UList;
import nez.util.UMap;

public class Grammar {
	Production start;
	UMap<Production>           ruleMap;
	UList<Production>          subProductionList;

	Grammar(Production start, int option) {
		this.start = start;
		this.subProductionList = new UList<Production>(new Production[4]);
		this.ruleMap = new UMap<Production>();
		this.setOption(option);
		add(0, start);
		//dump();
	}

	public Production getStartProduction() {
		return this.start;
	}

	public UList<Production> getSubProductionList() {
		return this.subProductionList;
	}
	
	private void add(int pos, Production r) {
		if(!ruleMap.hasKey(r.getUniqueName())) {
			subProductionList.add(r);
			ruleMap.put(r.getUniqueName(), r);
			add(pos, r.getExpression());
		}
	}
	
	private Expression rep = null;
	
	private void add(int pos, Expression expr) {
		if(expr instanceof NonTerminal) {
			//System.out.println("call " + ((NonTerminal) expr).getUniqueName() + " pos=" + pos + " redundant? " + checkRedundantCall(expr, pos));
			path.add(new Trace(expr, pos));
			add(pos, ((NonTerminal) expr).getProduction());
		}
		if(rep == null && expr instanceof nez.lang.Repetition) {
			rep = expr;
			//System.out.println("top level repetition: " + expr);
			add(pos, expr.get(0));
			rep = null;
		}
		for(Expression se : expr) {
			add(pos, se);
			if(!(expr instanceof nez.lang.Choice)) {
				pos += count(se);
			}
		}
	}

	class Trace {
		Expression e;
		int pos;
		int count = 0;
		boolean redundant = false;
		Trace(Expression e, int pos) {
			this.e = e;
			this.pos = pos;
		}
		@Override
		public String toString() {
			return e + " pos=" + pos + " redundant? " + redundant;
		}
	}

	UList<Trace> path = new UList<Trace>(new Trace[128]);
	
	void dump() {
		for(Trace t : this.path) {
			System.out.println(t);
		}
	}

	boolean checkRedundantCall(Expression e, int pos) {
		boolean r = false;
		for(Trace t : this.path) {
			if(t.e == e && t.pos >= pos) {
				t.redundant = true;
				r = true;
			}
		}
		return r;
	}
	
//	boolean isRecursivelyVisited(NonTerminal e) {
//		for(int i = path.size() - 1; i >= 0; i--) {
//			if(path.ArrayValues[i].e == e) {
//				path.ArrayValues[i].count += 1;
//				return true;
//			}
//		}
//		return false;
//	}
//	
//	void push(Expression e, int pos) {
//		path.add(new Trace(e, pos));
//	}
	
	int count(Expression e) {
		return (e.isAlwaysConsumed()) ? 1 : 0;
	}

	void checkBacktrack(Expression e, int pos) {
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
			RuntimeCompiler bc = new RuntimeCompiler(this.option);
			compiledCode = bc.encode(this.subProductionList);
			this.InstructionSize  = bc.getInstructionSize();
			this.memoPointSize = bc.getMemoPointSize();
			if(Verbose.PackratParsing) {
				this.memoPointList = bc.getMemoPointList();
			}
			if(Verbose.VirtualMachine) {
				bc.dump(this.subProductionList);
			}
		}
		return compiledCode;
	}
	
	public RuntimeCompiler cc() {
		RuntimeCompiler bc = new RuntimeCompiler(this.option);
		bc.encode(subProductionList);
		return bc;
	}
		
	public final boolean match(SourceContext s) {
		boolean matched;
		Instruction pc = this.compile();
		s.initJumpStack(64, getMemoTable(s));
		if(Verbose.Debug) {
			matched = Instruction.debug(pc, s);
		}
		else {
			matched = Instruction.run(pc, s);
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
			rec.setCount("G.NonTerminals", this.ruleMap.size());
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
	public final static int ExampleOption = Optimization | Specialization | Inlining | CommonPrefix | Prediction;
	public final static int RegexOption = ASTConstruction | PackratParsing | Optimization
											| Specialization | Prediction /* | Tracing */;
	public final static int SafeOption = ASTConstruction | Optimization;
	
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
