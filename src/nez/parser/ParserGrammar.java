package nez.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import nez.lang.ByteConsumption;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.OldGrammarChecker;
import nez.lang.Production;
import nez.lang.Typestate;
import nez.util.UList;
import nez.util.Verbose;

public class ParserGrammar extends Grammar {
	HashMap<String, ParseFunc> funcMap;
	public List<MemoPoint> memoPointList = null;

	public ParserGrammar(Production start, ParserStrategy strategy, TreeMap<String, Boolean> boolMap) {
		this.funcMap = new HashMap<String, ParseFunc>();
		new OldGrammarChecker(this, boolMap, start, strategy);
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

	private final Typestate.Analyzer typestate = new Typestate.Analyzer();

	public final Typestate typeState(Production p) {
		return typestate.inferTypestate(p);
	}

	public final Typestate typeState(Expression e) {
		return typestate.inferTypestate(e);
	}

	/* Acceptance */

	public ParseFunc getParseFunc(String name) {
		return this.funcMap.get(name);
	}

	public ParseFunc setParseFunc(String uname, Production p, Production parserProduction, int init) {
		ParseFunc f = new ParseFunc(uname, p, parserProduction, init);
		this.funcMap.put(uname, f);
		return f;
	}

	public void setParseFunc(ParseFunc f) {
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
			for (Entry<String, ParseFunc> e : funcMap.entrySet()) {
				this.checkInlining(e.getValue());
			}
		}
		if (memoPointList != null) {
			for (Entry<String, ParseFunc> e : funcMap.entrySet()) {
				this.checkMemoizing(e.getValue());
			}
		}
	}

	void checkInlining(ParseFunc f) {
		// if (f.refcount == 1 || GrammarOptimizer2.isSingleCharacter(f.e)) {
		// if (Verbose.PackratParsing) {
		// Verbose.println("Inlining: " + f.name);
		// }
		// f.inlining = true;
		// }
	}

	void checkMemoizing(ParseFunc f) {
		if (f.inlining || f.memoPoint != null) {
			return;
		}
		Production p = f.parserProduction;
		if (f.refcount > 1 && typeState(p) != Typestate.TreeMutation) {
			int memoId = memoPointList.size();
			f.memoPoint = new MemoPoint(memoId, p.getLocalName(), f.getExpression(), false); // FIXME
																								// p.isContextual());
			memoPointList.add(f.memoPoint);
			if (Verbose.PackratParsing) {
				Verbose.println("MemoPoint: " + f.memoPoint + " ref=" + f.refcount + " typestate? " + typeState(p));
			}
		}
	}

}
