package nez.parser.moz;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import nez.lang.ByteConsumption;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Production;
import nez.lang.Typestate;
import nez.lang.Typestate.TypestateAnalyzer;
import nez.parser.MemoPoint;
import nez.parser.ParserStrategy;
import nez.util.UList;
import nez.util.Verbose;

public class ParserGrammar extends Grammar {
	HashMap<String, ParserGrammarFunc> funcMap;
	public List<MemoPoint> memoPointList = null;

	public ParserGrammar(Production start, ParserStrategy strategy, TreeMap<String, Boolean> boolMap) {
		this.funcMap = new HashMap<String, ParserGrammarFunc>();
		new MozGrammarChecker(this, boolMap, start, strategy);
		memo(strategy);
	}

	/* Consumed */

	private final ByteConsumption consumed = new ByteConsumption();

	public final boolean isConsumed(Production p) {
		return consumed.isConsumed(p);
	}

	public final boolean isConsumed(Expression e) {
		return consumed.isConsumed(e);
	}

	/* Typestate */

	private final TypestateAnalyzer typestate = Typestate.newAnalyzer();

	public final Typestate typeState(Production p) {
		return typestate.inferTypestate(p);
	}

	public final Typestate typeState(Expression e) {
		return typestate.inferTypestate(e);
	}

	/* Acceptance */

	public ParserGrammarFunc getParseFunc(String name) {
		return this.funcMap.get(name);
	}

	public ParserGrammarFunc setParseFunc(String uname, Production p, Production parserProduction, int init) {
		ParserGrammarFunc f = new ParserGrammarFunc(uname, p, parserProduction, init);
		this.funcMap.put(uname, f);
		return f;
	}

	public void setParseFunc(ParserGrammarFunc f) {
		this.funcMap.put(f.name, f);
	}

	public void removeParseFunc(String name) {
		if (this.prodMap != null) {
			this.prodMap.remove(name);
		}
		this.funcMap.remove(name);
	}

	public void updateProductionList(UList<Production> prodList) {
		this.prodList = prodList;
		if (this.prodMap != null) {
			this.prodMap = new HashMap<String, Production>();
			for (Production p : prodList) {
				this.prodMap.put(p.getLocalName(), p);
			}
		}
	}

	void memo(ParserStrategy strategy) {
		memoPointList = null;
		if (strategy.PackratParsing) {
			memoPointList = new UList<MemoPoint>(new MemoPoint[4]);
		}
		if (strategy.Oinline) {
			for (Entry<String, ParserGrammarFunc> e : funcMap.entrySet()) {
				this.checkInlining(e.getValue());
			}
		}
		if (memoPointList != null) {
			for (Entry<String, ParserGrammarFunc> e : funcMap.entrySet()) {
				this.checkMemoizing(e.getValue());
			}
		}
	}

	void checkInlining(ParserGrammarFunc f) {
		// if (f.refcount == 1 || GrammarOptimizer2.isSingleCharacter(f.e)) {
		// if (Verbose.PackratParsing) {
		// Verbose.println("Inlining: " + f.name);
		// }
		// f.inlining = true;
		// }
	}

	void checkMemoizing(ParserGrammarFunc f) {
		if (f.inlining || f.memoPoint != null) {
			return;
		}
		Production p = f.parserProduction;
		if (f.refcount > 1 && typeState(p) != Typestate.TreeMutation) {
			int memoId = memoPointList.size();
			f.memoPoint = new MemoPoint(memoId, p.getLocalName(), f.getExpression(), typeState(p), false); // FIXME
			// p.isContextual());
			memoPointList.add(f.memoPoint);
			if (Verbose.PackratParsing) {
				Verbose.println("MemoPoint: " + f.memoPoint + " ref=" + f.refcount + " typestate? " + typeState(p));
			}
		}
	}

}
