package nez.lang;

import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.NezCompiler;

public class Failure extends Unconsumed {
	Failure(SourcePosition s) {
		super(s);
	}
	@Override
	public String getPredicate() {
		return "fail";
	}
	@Override
	public String key() {
		return "!!";
	}
	@Override
	public Expression reshape(Manipulator m) {
		return m.reshapeFailure(this);
	}
	@Override
	public boolean isConsumed(Stacker stacker) {
		return true;
	}

	@Override
	public short acceptByte(int ch, int option) {
		return Prediction.Reject;
	}
	@Override
	public Instruction encode(NezCompiler bc, Instruction next) {
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