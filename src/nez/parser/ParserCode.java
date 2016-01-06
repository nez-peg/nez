package nez.parser;

import java.util.HashMap;
import java.util.Map;

import nez.lang.Grammar;
import nez.lang.Production;
import nez.lang.Productions;
import nez.lang.Typestate;
import nez.util.UList;
import nez.util.Verbose;

public abstract class ParserCode<T extends NezInst> {

	protected final Grammar grammar;
	protected final HashMap<String, ParseFunc<T>> funcMap;
	protected UList<T> codeList;

	protected ParserCode(Grammar grammar, T[] initArray) {
		this.grammar = grammar;
		this.funcMap = new HashMap<>();
		this.codeList = initArray != null ? new UList<>(initArray) : null;
	}

	public final Grammar getCompiledGrammar() {
		return this.grammar;
	}

	public abstract Object exec(ParserContext context);

	/* ParserFunc */

	public static class ParseFunc<T extends NezInst> {
		Production p;
		T compiled;

		public ParseFunc(Production p, T inst) {
			this.p = p;
			this.compiled = inst;
		}

		public final T getCompiled() {
			return this.compiled;
		}
	}

	protected int getParseFuncSize() {
		return funcMap.size();
	}

	public ParseFunc<T> getParseFunc(Production p) {
		return funcMap.get(p.getUniqueName());
	}

	public void setParseFunc(Production p, ParseFunc<T> f) {
		funcMap.put(p.getUniqueName(), f);
	}

	public abstract void layoutCode(T inst);

	public final T getStartPoint() {
		return codeList.get(0);
	}

	public final int getInstSize() {
		return codeList.size();
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
