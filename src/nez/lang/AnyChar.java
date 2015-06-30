package nez.lang;

import nez.ast.SourcePosition;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class AnyChar extends Char implements Consumed {
	AnyChar(SourcePosition s, boolean binary) {
		super(s, binary);
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		if(o instanceof AnyChar) {
			return this.binary == ((AnyChar)o).isBinary();
		}
		return false;
	}

	@Override
	protected final void format(StringBuilder sb) {
		sb.append(".");
	}

	@Override
	public String getPredicate() {
		return "any";
	}
	
	@Override
	public String key() { 
		return binary ? "b." : ".";
	}
	
	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeAnyChar(this);
	}

	@Override
	public boolean isConsumed() {
		return true;
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.BooleanType;
	}
	
	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.acceptAny(binary, ch);
	}
	
	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeAnyChar(this, next, failjump);
	}
	@Override
	protected int pattern(GEP gep) {
		return 1;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		sb.append(".");
	}
}