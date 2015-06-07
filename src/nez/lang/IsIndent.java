package nez.lang;

import nez.ast.SourcePosition;
import nez.util.UList;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class IsIndent extends Terminal implements Contextual {
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
	public short acceptByte(int ch, int option) {
		if (ch == '\t' || ch == ' ') {
			return Acceptance.Accept;
		}
		return Acceptance.Unconsumed;
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeIsIndent(this, next, failjump);
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