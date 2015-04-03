package nez.expr;

import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;

public class Failure extends Unconsumed {
	Failure(SourcePosition s) {
		super(s);
	}
	@Override
	public String getPredicate() {
		return "fail";
	}
	@Override
	public String getInterningKey() {
		return "!!";
	}
	@Override
	public short acceptByte(int ch, int option) {
		return Prediction.Reject;
	}
	@Override
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
		return bc.encodeFail(this);
	}
	@Override
	protected int pattern(GEP gep) {
		return 1;
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		sb.append("\0");
	}

}