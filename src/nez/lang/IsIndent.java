package nez.lang;

import nez.ast.SourcePosition;
import nez.util.UList;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class IsIndent extends Expression implements Contextual {
	IsIndent(SourcePosition s) {
		super(s);
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		return (o instanceof IsIndent);
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
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeIsIndent(this);
	}
	
	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.BooleanType;
	}

	@Override
	public short acceptByte(int ch) {
		if (ch == '\t' || ch == ' ') {
			return PossibleAcceptance.Accept;
		}
		return PossibleAcceptance.Unconsumed;
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeIsIndent(this, next, failjump);
	}
}