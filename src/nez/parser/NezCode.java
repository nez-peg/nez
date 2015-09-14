package nez.parser;

import java.util.List;

import nez.main.Verbose;
import nez.util.UList;

public class NezCode {
	// final Instruction startPoint;
	// final int instructionSize;
	final GenerativeGrammar gg;
	final UList<Instruction> codeList;
	final List<MemoPoint> memoPointList;

	public NezCode(GenerativeGrammar gg, UList<Instruction> codeList, List<MemoPoint> memoPointList) {
		this.gg = gg;
		this.codeList = codeList;
		this.memoPointList = memoPointList;
	}

	public final Instruction getStartPoint() {
		return codeList.get(0);
	}

	public final int getInstructionSize() {
		return codeList.size();
	}

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

	public final void encode(ByteCoder coder) {
		if (coder != null) {
			coder.setHeader(codeList.size(), this.gg.size(), memoPointList == null ? 0 : memoPointList.size());
			coder.setInstructions(codeList.ArrayValues, codeList.size());
		}
	}

}
