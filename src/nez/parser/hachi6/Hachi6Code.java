package nez.parser.hachi6;

import java.util.List;

import nez.Parser;
import nez.Verbose;
import nez.parser.ByteCoder;
import nez.parser.GenerativeGrammar;
import nez.parser.MemoPoint;
import nez.parser.moz.MozInst;
import nez.parser.moz.MozCode;
import nez.parser.moz.NezCompiler;
import nez.parser.moz.PackratCompiler;
import nez.util.UList;

public class Hachi6Code {
	final GenerativeGrammar gg;
	final UList<MozInst> codeList;
	final List<MemoPoint> memoPointList;

	public Hachi6Code(GenerativeGrammar gg, UList<MozInst> codeList, List<MemoPoint> memoPointList) {
		this.gg = gg;
		this.codeList = codeList;
		this.memoPointList = memoPointList;
	}

	public final MozInst getStartPoint() {
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

	public final static void writeMozCode(Parser parser, String path) {
		NezCompiler compile = new PackratCompiler(parser.getStrategy());
		MozCode code = compile.compile(parser.getGrammar());
		ByteCoder c = new ByteCoder();
		code.encode(c);
		Verbose.println("generating " + path);
		c.writeTo(path);
	}

}
