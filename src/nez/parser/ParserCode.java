package nez.parser;

import java.util.HashMap;
import java.util.Map;

import nez.lang.Grammar;
import nez.lang.Production;
import nez.lang.Productions;
import nez.lang.Typestate;
import nez.util.UList;
import nez.util.Verbose;

public abstract class ParserCode<T extends Instruction> {

	protected final Grammar grammar;
	protected UList<T> codeList;

	protected ParserCode(Grammar grammar, T[] initArray) {
		this.grammar = grammar;
		this.funcMap = new HashMap<>();
		this.codeList = initArray != null ? new UList<>(initArray) : null;
	}

	public final Grammar getCompiledGrammar() {
		return this.grammar;
	}

	public abstract void layoutCode(T inst);

	public final T getStartInstruction() {
		return codeList.get(0);
	}

	public final int getInstSize() {
		return codeList.size();
	}

	public abstract Object exec(ParserContext context);

	/* ParserFunc */

	protected final HashMap<String, ProductionCode<T>> funcMap;

	public static class ProductionCode<T extends Instruction> {
		private T compiled;

		public ProductionCode(T inst) {
			this.compiled = inst;
		}

		public void setCompiled(T inst) {
			this.compiled = inst;
		}

		public final T getCompiled() {
			return this.compiled;
		}
	}

	protected int getCompiledProductionSize() {
		return funcMap.size();
	}

	public ProductionCode<T> getProductionCode(Production p) {
		return funcMap.get(p.getUniqueName());
	}

	public void setProductionCode(Production p, ProductionCode<T> f) {
		funcMap.put(p.getUniqueName(), f);
	}

	/* MemoPoint */

	protected Map<String, MemoPoint> memoPointMap = null;

	public void initMemoPoint() {
		final Typestate.Analyzer typestate = new Typestate.Analyzer();
		memoPointMap = new HashMap<>();
		Map<String, Integer> refs = Productions.countNonTerminalReference(grammar);
		for (Production p : grammar) {
			String uname = p.getUniqueName();
			Integer cnt = refs.get(uname);
			Typestate ts = typestate.inferTypestate(p);
			if (cnt != null && (cnt > 2 && ts != Typestate.TreeMutation)) {
				MemoPoint memoPoint = new MemoPoint(this.memoPointMap.size(), uname, p.getExpression(), ts, false);
				this.memoPointMap.put(uname, memoPoint);
			}
		}
	}

	public final MemoPoint getMemoPoint(String uname) {
		if (memoPointMap != null) {
			return this.memoPointMap.get(uname);
		}
		return null;
	}

	public final int getMemoPointSize() {
		return this.memoPointMap != null ? this.memoPointMap.size() : 0;
	}

	public final void dumpMemoPoints() {
		if (this.memoPointMap != null) {
			Verbose.println("ID\tPEG\tCount\tHit\tFail\tMean");
			for (String key : this.memoPointMap.keySet()) {
				MemoPoint p = this.memoPointMap.get(key);
				String s = String.format("%d\t%s\t%d\t%f\t%f\t%f", p.id, p.label, p.count(), p.hitRatio(), p.failHitRatio(), p.meanLength());
				Verbose.println(s);
			}
			Verbose.println("");
		}
	}

}
//
// public abstract class ParserCode<I extends NezInst> {
// final protected ParserGrammar gg;
// final protected List<MemoPoint> memoPointList;
//
// public ParserCode(ParserGrammar pgrammar, List<MemoPoint> memoPointList) {
// this.gg = pgrammar;
// this.memoPointList = memoPointList;
// }
//
// public abstract int getInstSize();
//
// public final int getMemoPointSize() {
// return this.memoPointList != null ? this.memoPointList.size() : 0;
// }
//
// public final void dumpMemoPoints() {
// if (this.memoPointList != null) {
// Verbose.println("ID\tPEG\tCount\tHit\tFail\tMean");
// for (MemoPoint p : this.memoPointList) {
// String s = String.format("%d\t%s\t%d\t%f\t%f\t%f", p.id, p.label, p.count(),
// p.hitRatio(), p.failHitRatio(), p.meanLength());
// Verbose.println(s);
// }
// Verbose.println("");
// }
// }
//
// public abstract Object exec(ParserContext context);
//
// }
