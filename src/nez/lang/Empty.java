package nez.lang;

import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.NezCompiler;

public class Empty extends Unconsumed {
	Empty(SourcePosition s) {
		super(s);
	}	
	@Override
	public String getPredicate() {
		return "empty";
	}
	@Override
	public String key() {
		return "";
	}
	@Override
	public Expression reshape(Manipulator m) {
		return m.reshapeEmpty(this);
	}

	@Override
	public boolean isConsumed(Stacker stacker) {
		return false;
	}


	@Override
	public Instruction encode(NezCompiler bc, Instruction next) {
		return next;
	}

	@Override
	protected int pattern(GEP gep) {
		return 0;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
	}

}
