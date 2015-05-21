package nez.lang;

import java.util.TreeMap;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeFactory;
import nez.ast.ParsingFactory;
import nez.ast.SourcePosition;
import nez.main.Recorder;
import nez.main.Verbose;
import nez.runtime.Instruction;
import nez.runtime.MemoPoint;
import nez.runtime.MemoTable;
import nez.runtime.NezCompiler;
import nez.runtime.NezCompiler0;
import nez.util.ConsoleUtils;
import nez.util.UFlag;
import nez.util.UList;
import nez.util.UMap;

class GProduction {
	int ref = 1;
	Production p;
	Expression e;
	GProduction(Production p) {
		this.p = p;
		this.e = p.getExpression();
	}
}

public class Grammar {
	Production start;
	UMap<GProduction>          productionMap;
	UList<Production>          productionList;

	Grammar(Production start, int option) {
		this.start = start;
		this.productionList = new UList<Production>(new Production[4]);
		this.productionMap = new UMap<GProduction>();
		this.setOption(option);
		TreeMap<String, Boolean> conditionMap = new TreeMap<String, Boolean>(); 
		analyze(start, conditionMap);
		if(!UFlag.is(option, Grammar.ASTConstruction)) {
			reshapeAll(Manipulator.RemoveAST);
		}
		if(conditionMap.size() > 0) {
			new ConditionalAnalysis(this);
		}
		new GrammarOptimizer(option).optimize(this);

	}

	public Production getStartProduction() {
		return this.start;
	}

	public UList<Production> getProductionList() {
		return this.productionList;
	}

	void reshapeAll(Manipulator m) {
		for(Production p: productionList) {
			GProduction gp = this.productionMap.get(p.getUniqueName());
			gp.e = gp.e.reshape(m);
		}
	}
	
	private void analyze(Production p, TreeMap<String, Boolean> conditionMap) {
		String uname = p.getUniqueName();
		GProduction gp = productionMap.get(uname);
		if(gp != null) {
			gp.ref++;
			return;
		}
		gp =  new GProduction(p);
		productionList.add(p);
		productionMap.put(p.getUniqueName(), gp);
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

//	private void analyze(int pos, Production r) {
//		if(!productionMap.hasKey(r.getUniqueName())) {
//			productionList.add(r);
//			productionMap.put(r.getUniqueName(), new GProduction(r));
//			add(pos, r.getExpression());
//		}
//	}
//	
//	private Expression rep = null;
//	
//	private void add(int pos, Expression expr) {
//		if(expr instanceof NonTerminal) {
//			//System.out.println("call " + ((NonTerminal) expr).getUniqueName() + " pos=" + pos + " redundant? " + checkRedundantCall(expr, pos));
//			path.add(new Trace(expr, pos));
//			analyze(pos, ((NonTerminal) expr).getProduction());
//		}
//		if(rep == null && expr instanceof nez.lang.Repetition) {
//			rep = expr;
//			//System.out.println("top level repetition: " + expr);
//			add(pos, expr.get(0));
//			rep = null;
//		}
//		for(Expression se : expr) {
//			add(pos, se);
//			if(!(expr instanceof nez.lang.Choice)) {
//				pos += count(se);
//			}
//		}
//	}
//
//	class Trace {
//		Expression e;
//		int pos;
//		int count = 0;
//		boolean redundant = false;
//		Trace(Expression e, int pos) {
//			this.e = e;
//			this.pos = pos;
//		}
//		@Override
//		public String toString() {
//			return e + " pos=" + pos + " redundant? " + redundant;
//		}
//	}
//
//	UList<Trace> path = new UList<Trace>(new Trace[128]);
//	
//	void dump() {
//		for(Trace t : this.path) {
//			System.out.println(t);
//		}
//	}
//
//	boolean checkRedundantCall(Expression e, int pos) {
//		boolean r = false;
//		for(Trace t : this.path) {
//			if(t.e == e && t.pos >= pos) {
//				t.redundant = true;
//				r = true;
//			}
//		}
//		return r;
//	}
//	
////	boolean isRecursivelyVisited(NonTerminal e) {
////		for(int i = path.size() - 1; i >= 0; i--) {
////			if(path.ArrayValues[i].e == e) {
////				path.ArrayValues[i].count += 1;
////				return true;
////			}
////		}
////		return false;
////	}
////	
////	void push(Expression e, int pos) {
////		path.add(new Trace(e, pos));
////	}
//	
//	int count(Expression e) {
//		return (e.isAlwaysConsumed()) ? 1 : 0;
//	}
//
//	void checkBacktrack(Expression e, int pos) {
//	}

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
			NezCompiler0 bc = new NezCompiler0(this.option);
			compiledCode = bc.encode(this.productionList);
			this.InstructionSize  = bc.getInstructionSize();
			this.memoPointSize = bc.getMemoPointSize();
			if(Verbose.PackratParsing) {
				this.memoPointList = bc.getMemoPointList();
			}
			if(Verbose.VirtualMachine) {
				bc.dump(this.productionList);
			}
		}
		return compiledCode;
	}
	
	public NezCompiler cc() {
		NezCompiler0 bc = new NezCompiler0(this.option);
		bc.encode(productionList);
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

class GrammarOptimizer extends Manipulator {

	int option;
	Grammar grammar = null;
	
	public GrammarOptimizer(int option) {
		this.option = option;
	}

	public void optimize(Grammar grammar) {
		this.grammar = grammar;
		grammar.reshapeAll(this);
		this.grammar = null;
	}

	@Override
	public Expression reshapeChoice(Choice p) {
		UList<Expression> l = new UList<Expression>(new Expression[p.size()]);
		flatten(p, l);
		if(UFlag.is(option, Grammar.Optimization)) {
			Expression o = newOptimizedByteMap(p.s, l);
			if(o != null) {
				return o;
			}
		}
		return Factory.newChoice(p.getSourcePosition(), l);
//		if(UFlag.is(option, Grammar.Prediction) && !UFlag.is(option, Grammar.DFA)) {
//			Expression fails = Factory.newFailure(s);
//			this.matchCase = new Expression[257];
//			for(int ch = 0; ch <= 256; ch++) {
//				Expression selected = selectChoice(ch, fails, option);
//				matchCase[ch] = selected;
//			}
//		}
	}
	
	private void flatten(Choice parentExpression, UList<Expression> l) {
		for(Expression subExpression: parentExpression) {
			subExpression = subExpression.reshape(this);
//			e = resolveNonTerminal(e);
			if(subExpression instanceof Choice) {
				flatten((Choice)subExpression, l);
			}
			else {
				l.add(subExpression);
			}
		}
	}
	
	public final static Expression resolveNonTerminal(Expression e) {
		while(e instanceof NonTerminal) {
			NonTerminal nterm = (NonTerminal) e;
			e = nterm.deReference();
		}
		return e;
	}
	
	@Override
	public Expression reshapeNonTerminal(NonTerminal p) {
		Expression e = p;
		while(e instanceof NonTerminal) {
			NonTerminal nterm = (NonTerminal) e;
			e = nterm.deReference().optimize(option);
		}
		return e;
	}

	@Override
	public Expression reshapeSequence(Sequence parentExpression) {
		UList<Expression> l = new UList<Expression>(new Expression[parentExpression.size()]);
		for(Expression subExpression: parentExpression) {
			Factory.addSequence(l, subExpression.reshape(this));
		}
		reorderSequence(l);
		if(UFlag.is(option, Grammar.Optimization)) {
			int loc = findNotAny(0, l);
			if(loc != -1) {
				UList<Expression> nl = new UList<Expression>(new Expression[l.size()]);
				joinNotAny(0, loc, l, nl);
				l = nl;
			}
		}
		return Factory.newSequence(parentExpression.getSourcePosition(), l);
	}
	
	/**
	 * Sequence otimization
	 * // #t 'a' 'b' => 'a' #t 'b'
	 */

	private void reorderSequence(UList<Expression> l) {
		for(int i = 1; i < l.size(); i++) {
			Expression p = l.ArrayValues[i-1];
			Expression e = l.ArrayValues[i];
			if(Expression.isByteConsumed(e)) {   // #t 'a' 'b' => 'a' #t 'b'
				if(Expression.isPositionIndependentOperation(p)) {
					l.ArrayValues[i-1] = e;
					l.ArrayValues[i]   = p;
					continue;
				}
				if(p instanceof New) {
					New n = (New)p;
					l.ArrayValues[i-1] = e;
					if(n.isInterned()) {
						l.ArrayValues[i] =  Factory.newNew(n.s, n.lefted, n.shift - 1);
					}
					else {
						n.shift -= 1;
						l.ArrayValues[i]   = n;
					}
					continue;
				}
				if(p instanceof Capture) {
					Capture n = (Capture)p;
					l.ArrayValues[i-1] = e;
					if(n.isInterned()) {
						l.ArrayValues[i] =  Factory.newCapture(n.s, n.shift - 1);
					}
					else {
						n.shift -= 1;
						l.ArrayValues[i]   = n;
					}
					continue;
				}
			}
		}
	}

	private int findNotAny(int s, UList<Expression> l) {
		for(int i = s; i < l.size(); i++) {
			Expression p = l.ArrayValues[i];
			if(p instanceof Not) {
				if(findAny(i, l) != -1) {
					return i;
				}
			}
		}
		return -1;
	}

	private int findAny(int s, UList<Expression> l) {
		for(int i = s; i < l.size(); i++) {
			Expression p = l.ArrayValues[i];
			if(p instanceof Not) {
				continue;
			}
			if(p instanceof AnyChar) {
				return i;
			}
			break;
		}
		return -1;
	}

	private void joinNotAny(int s, int loc, UList<Expression> l, UList<Expression> nl) {
		for(int i = s; i < loc; i++) {
			nl.add(l.ArrayValues[i]);
		}
		int e = findAny(loc, l);
		assert(e != -1);
		Not not = (Not)l.ArrayValues[loc];
		AnyChar any = (AnyChar)l.ArrayValues[e];
		if(loc + 1 < e) {
			UList<Expression> sl = new UList<Expression>(new Expression[4]);
			for(int i = loc; i < e; i++) {
				Factory.addChoice(sl, l.ArrayValues[i]);
			}
			not = Factory.newNot(not.s, Factory.newChoice(not.s, sl).reshape(this));
		}
		if(not.inner instanceof ByteChar) {
			boolean[] byteMap = ByteMap.newMap(true);
			byteMap[((ByteChar) not.inner).byteChar] = false;
			if(!UFlag.is(option, Grammar.Binary)) {
				byteMap[0] = false;
			}
			nl.add(Factory.newByteMap(not.s, byteMap));
		}
		else if(not.inner instanceof ByteMap) {
			boolean[] byteMap = ByteMap.newMap(false);
			ByteMap.appendBitMap(byteMap, ((ByteMap) not.inner).byteMap);
			ByteMap.reverse(byteMap, option);
			nl.add(Factory.newByteMap(not.s, byteMap));
		}
		else {
			nl.add(not);
			nl.add(any);
		}
		loc = findNotAny(e+1, l);
		if(loc != -1) {
			joinNotAny(e+1, loc, l, nl);
			return;
		}
		for(int i = e+1; i < l.size(); i++) {
			nl.add(l.ArrayValues[i]);
		}
	}
	
	public Expression reshapeLink(Link p) {
		if(p.get(0) instanceof Choice) {
			Expression inner = p.get(0);
			UList<Expression> l = new UList<Expression>(new Expression[inner.size()]);
			for(Expression subChoice: inner) {
				subChoice = subChoice.reshape(this);
				l.add(Factory.newLink(p.getSourcePosition(), subChoice, p.index));
			}			
			return Factory.newChoice(inner.getSourcePosition(), l);
		}
		return super.reshapeLink(p);
	}
	
	public static Expression newOptimizedByteMap(SourcePosition s, UList<Expression> l) {
		boolean byteMap[] = ByteMap.newMap(false);
		for(Expression e : l) {
			if(e instanceof ByteChar) {
				byteMap[((ByteChar) e).byteChar] = true;
				continue;
			}
			if(e instanceof ByteMap) {
				ByteMap.appendBitMap(byteMap, ((ByteMap)e).byteMap);
				continue;
			}
			if(e instanceof AnyChar) {
				return e;
			}
			return null;
		}
		return (ByteMap)Factory.newByteMap(s, byteMap);
	}
	
}