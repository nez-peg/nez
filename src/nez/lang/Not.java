package nez.lang;

import nez.NezOption;
import nez.ast.Source;
import nez.ast.SourcePosition;
import nez.util.UFlag;
import nez.util.UList;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Not extends Unary {
	Not(SourcePosition s, Expression e) {
		super(s, e);
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		if(o instanceof Not) {
			return this.get(0).equalsExpression(o.get(0));
		}
		return false;
	}
	@Override
	public String getPredicate() { 
		return "!";
	}
	@Override
	public String key() { 
		return "!" ;
	}
	@Override
	protected final void format(StringBuilder sb) {
		this.formatUnary(sb, "!", this.inner);
	}
	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeNot(this);
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
		return PossibleAcceptance.acceptNot(this, ch);
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeNot(this, next, failjump);
	}
	@Override
	protected int pattern(GEP gep) {
		int max = 0;
		for(Expression p: this) {
			int c = p.pattern(gep);
			if(c > max) {
				max = c;
			}
		}
		return max;
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		
	}



}