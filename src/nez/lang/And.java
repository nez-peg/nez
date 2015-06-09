package nez.lang;

import nez.ast.SourcePosition;
import nez.util.UList;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class And extends Unary {
	And(SourcePosition s, Expression e) {
		super(s, e);
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		if(o instanceof And) {
			return this.get(0).equalsExpression(o.get(0));
		}
		return false;
	}

	@Override
	public String getPredicate() {
		return "&";
	}
	@Override
	protected final void format(StringBuilder sb) {
		this.formatUnary(sb, "&", this.inner);
	}
	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeAnd(this);
	}
	@Override
	public boolean isConsumed() {
		return false;
	}
	@Override
	public int inferTypestate(Visa v) {
		int t = this.inner.inferTypestate(v);
		if(t == Typestate.ObjectType) {  // typeCheck needs to report error
			return Typestate.BooleanType;
		}
		return t;
	}

	@Override
	public short acceptByte(int ch, int option) {
		short r = this.inner.acceptByte(ch, option);
		if(r == Acceptance.Accept || r == Acceptance.Unconsumed) {
			return Acceptance.Unconsumed;
		}
		return r;
	}
	
	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeAnd(this, next, failjump);
	}
	
	@Override
	protected int pattern(GEP gep) {
		return -1;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		// TODO Auto-generated method stub
	}
	
}