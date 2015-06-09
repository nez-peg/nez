package nez.lang;

import nez.ast.SourcePosition;
import nez.util.UList;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Option extends Unary {
	Option(SourcePosition s, Expression e) {
		super(s, e);
		e.setOuterLefted(this);
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		if(o instanceof Option) {
			return this.get(0).equalsExpression(o.get(0));
		}
		return false;
	}
	@Override
	public String getPredicate() { 
		return "?";
	}
	@Override
	public String key() { 
		return "?";
	}
	@Override
	protected final void format(StringBuilder sb) {
		this.formatUnary(sb, this.inner, "?");
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeOption(this);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}
	
	@Override
	public int inferTypestate(Visa v) {
		int t = this.inner.inferTypestate(v);
		if(t == Typestate.ObjectType) {
			return Typestate.BooleanType;
		}
		return t;
	}
	
	@Override public short acceptByte(int ch, int option) {
		return Acceptance.acceptOption(this, ch, option);
	}
	
	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeOption(this, next);
	}

	@Override
	protected int pattern(GEP gep) {
		return 2;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		if(p % 2 == 0) {
			this.inner.examplfy(gep, sb, p);
		}
	}

}