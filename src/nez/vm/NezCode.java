package nez.vm;

import java.util.List;

import nez.main.Verbose;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class NezCode {
	final Instruction startPoint;
	final int instructionSize;
	final List<MemoPoint> memoPointList;

	public NezCode(Instruction instruction, int instSize, List<MemoPoint> memoPointList) {
		this.startPoint = instruction;
		this.instructionSize = instSize;
		this.memoPointList = memoPointList;
	}

	public final Instruction getStartPoint() {
		return startPoint;
	}

	public final int getInstructionSize() {
		return instructionSize;
	}

	public final int getMemoPointSize() {
		return this.memoPointList != null ? this.memoPointList.size() : 0;
	}
	
	public final void dumpMemoPointList() {
		if(this.memoPointList != null) {
			ConsoleUtils.println("ID\tPEG\tCount\tHit\tFail\tMean");
			for(MemoPoint p: this.memoPointList) {
				String s = String.format("%d\t%s\t%d\t%f\t%f\t%f", p.id, p.label, p.count(), p.hitRatio(), p.failHitRatio(), p.meanLength());
				ConsoleUtils.println(s);
			}
			ConsoleUtils.println("");
		}
	}

	
}
