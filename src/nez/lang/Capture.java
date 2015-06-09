package nez.lang;

import nez.ast.SourcePosition;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Capture extends ASTOperation {
	public int shift;
	Capture(SourcePosition s, int shift) {
		super(s);
		this.shift = shift;
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		return (o instanceof Capture && this.shift == ((Capture)o).shift);
	}
	@Override
	public String getPredicate() { 
		return "}";
	}
	@Override
	public String key() {
		return shift == 0 ? "}" : "}["+shift+"]";
	}
	@Override
	protected final void format(StringBuilder sb) {
		sb.append("}");
	}
	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeCapture(this);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}
	
	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeCapture(this, next);
	}
}
