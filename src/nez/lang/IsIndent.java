package nez.lang;

import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UList;

public class IsIndent extends Terminal {
	IsIndent(SourcePosition s) {
		super(s);
	}
	@Override
	public String getPredicate() {
		return "indent";
	}
	@Override
	public String key() {
		return "indent";
	}
	
	@Override
	public Expression reshape(Manipulator m) {
		return m.reshapeIsIndent(this);
	}
	
	@Override
	public boolean isConsumed(Stacker stacker) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return false;
	}
	@Override
	public short acceptByte(int ch, int option) {
		if (ch == '\t' || ch == ' ') {
			return Prediction.Accept;
		}
		return Prediction.Unconsumed;
	}

	@Override
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
		return bc.encodeIsIndent(this, next);
	}
	@Override
	protected int pattern(GEP gep) {
		return 1;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		String token = gep.getSymbol(NezTag.Indent);
		sb.append(token);
	}
}