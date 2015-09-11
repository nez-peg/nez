package nez.vm;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import nez.Grammar;
import nez.NezOption;
import nez.lang.GrammarChecker;
import nez.lang.GrammarOptimizer;
import nez.lang.Production;
import nez.lang.Typestate;
import nez.main.Verbose;
import nez.util.UList;

//class StaticParserContext extends TreeMap<String, Boolean> {
//	private static final long serialVersionUID = 1L;
//
//	StaticParserContext(boolean noAST) {
//		if (noAST) {
//			on("@");
//		}
//	}
//
//	public void on(String name) {
//		this.put(name, true);
//	}
//
//	public void off(String name) {
//		this.remove(name);
//	}
//
//	public String name(String uname) {
//		return uname;
//	}
//}

public class ParserGrammar extends Grammar {
	HashMap<String, ParseFunc> funcMap;
	public List<MemoPoint> memoPointList = null;

	public ParserGrammar(Production start, NezOption option, TreeMap<String, Boolean> flagMap) {
		this.funcMap = new HashMap<String, ParseFunc>();
		new GrammarChecker(this, !option.enabledASTConstruction, flagMap, start, option);
		memo(option);
	}

	public ParseFunc getParseFunc(String name) {
		return this.funcMap.get(name);
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

	void memo(NezOption option) {
		memoPointList = null;
		if (option.enabledMemoization || option.enabledPackratParsing) {
			memoPointList = new UList<MemoPoint>(new MemoPoint[4]);
		}
		if (option.enabledInlining) {
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
		if (f.refcount == 1 || GrammarOptimizer.isSingleCharacter(f.e)) {
			if (Verbose.PackratParsing) {
				Verbose.println("Inlining: " + f.name);
			}
			f.inlining = true;
		}
	}

	void checkMemoizing(ParseFunc f) {
		if (f.inlining || f.memoPoint != null) {
			return;
		}
		Production p = f.p;
		if (f.refcount > 1 && p.inferTypestate(null) != Typestate.OperationType) {
			int memoId = memoPointList.size();
			f.memoPoint = new MemoPoint(memoId, p.getLocalName(), f.e, p.isContextual());
			memoPointList.add(f.memoPoint);
			if (Verbose.PackratParsing) {
				Verbose.println("MemoPoint: " + f.memoPoint + " ref=" + f.refcount + " pure? " + p.isNoNTreeConstruction() + " rec? " + p.isRecursive());
			}
		}
	}

}
