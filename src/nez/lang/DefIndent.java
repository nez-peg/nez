package nez.lang;

import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.NezCompiler;
import nez.util.StringUtils;

public class DefIndent extends Unconsumed {
	DefIndent(SourcePosition s) {
		super(s);
	}
	@Override
	public String getPredicate() {
		return "defindent";
	}

	@Override
	public boolean isConsumed(Stacker stacker) {
		return false;
	}

	@Override
	public Instruction encode(NezCompiler bc, Instruction next) {
		return bc.encodeDefIndent(this, next);
	}
	@Override
	protected int pattern(GEP gep) {
		return 1;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		String token = gep.addIndent();
		sb.append(token);
	}
	@Override
	public Expression reshape(Manipulator m) {
		return m.reshapeUndefined(this);
	}

}