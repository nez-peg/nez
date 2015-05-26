package nez.lang;

import nez.ast.SourcePosition;
import nez.vm.Instruction;
import nez.vm.NezCompiler;

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
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeEmpty(this);
	}

	@Override
	public boolean isConsumed(Stacker stacker) {
		return false;
	}


	@Override
	public Instruction encode(NezCompiler bc, Instruction next, Instruction failjump) {
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
