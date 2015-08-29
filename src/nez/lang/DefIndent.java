package nez.lang;

import nez.ast.SourcePosition;
import nez.util.StringUtils;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class DefIndent extends Unconsumed {
	DefIndent(SourcePosition s) {
		super(s);
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		return (o instanceof DefSymbol);
	}
	@Override
	public String getPredicate() {
		return "defindent";
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeDefIndent(this, next, failjump);
	}
	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeUndefined(this);
	}

}