package nez.parser;

import java.util.HashMap;
import java.util.Map;

import nez.ast.Tree;
import nez.lang.Grammar;
import nez.lang.Production;
import nez.lang.Productions;
import nez.lang.Productions.NonterminalReference;
import nez.lang.Typestate;
import nez.lang.Typestate.TypestateAnalyzer;
import nez.parser.vm.MozInst;
import nez.parser.vm.ParserMachineContext;
import nez.util.UList;
import nez.util.Verbose;

public abstract class ParserCode<T extends Instruction> {

	protected final Grammar grammar;
	protected UList<T> codeList;
	protected final boolean RecognitionMode;

	protected ParserCode(Grammar grammar, T[] initArray) {
		this.grammar = grammar;
		this.funcMap = new HashMap<>();
		this.codeList = initArray != null ? new UList<>(initArray) : null;
		TypestateAnalyzer typestate = Typestate.newAnalyzer();
		this.RecognitionMode = typestate.inferTypestate(grammar.getStartProduction()) == Typestate.Unit;
	}

	public final Grammar getCompiledGrammar() {
		return this.grammar;
	}

	public abstract void layoutCode(T inst);

	public final T getStartInstruction() {
		return codeList.get(0);
	}

	public final int getInstructionSize() {
		return codeList.size();
	}

	public final Tree<?> exec(ParserMachineContext ctx) {
		int ppos = (int) ctx.getPosition();
		MozInst code = (MozInst) this.getStartInstruction();
		boolean result = false;
		try {
			while (true) {
				code = code.exec2(ctx);
			}
		} catch (TerminationException e) {
			result = e.status;
		}
		if (RecognitionMode && result) {
			ctx.left = ctx.newTree(null, ppos, (int) ctx.getPosition(), 0, null);
		}
		return result ? ctx.left : null;

	}

	public abstract Object exec(ParserInstance context);

	/* ProductionCode */

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
		final TypestateAnalyzer typestate = Typestate.newAnalyzer();
		memoPointMap = new HashMap<>();
		NonterminalReference refs = Productions.countNonterminalReference(grammar);
		for (Production p : grammar) {
			String uname = p.getUniqueName();
			Typestate ts = typestate.inferTypestate(p);
			if (refs.count(uname) > 2 && ts != Typestate.TreeMutation) {
				Verbose.println("MemoPoint: %s refc=%d", uname, refs.count(uname));
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
