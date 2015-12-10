package nez.parser;

import java.util.List;

import nez.Verbose;

public abstract class ParserCode {
	final protected ParserGrammar gg;
	final protected List<MemoPoint> memoPointList;

	public ParserCode(ParserGrammar pgrammar, List<MemoPoint> memoPointList) {
		this.gg = pgrammar;
		this.memoPointList = memoPointList;
	}

	public abstract int getInstSize();

	public final int getMemoPointSize() {
		return this.memoPointList != null ? this.memoPointList.size() : 0;
	}

	public final void dumpMemoPoints() {
		if (this.memoPointList != null) {
			Verbose.println("ID\tPEG\tCount\tHit\tFail\tMean");
			for (MemoPoint p : this.memoPointList) {
				String s = String.format("%d\t%s\t%d\t%f\t%f\t%f", p.id, p.label, p.count(), p.hitRatio(), p.failHitRatio(), p.meanLength());
				Verbose.println(s);
			}
			Verbose.println("");
		}
	}

	public abstract Object exec(ParserContext context);

}
