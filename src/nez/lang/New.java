package nez.lang;

import nez.ast.SourcePosition;
import nez.util.UList;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class New extends Unconsumed {
	public boolean lefted;
	public Expression outer = null;
	public int shift  = 0;
	New(SourcePosition s, boolean lefted, int shift) {
		super(s);
		this.lefted = lefted;
		this.shift  = shift;
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		if(o instanceof New) {
			New s = (New)o;
			return (this.lefted == s.lefted && this.shift == s.shift);
		}
		return false;
	}

	@Override
	public String getPredicate() { 
		return "new";
	}
	@Override
	public String key() {
		String s = lefted ? "{@" : "{";
		return (shift != 0) ? s + "[" + shift + "]" : s;
	}
	@Override
	protected final void format(StringBuilder sb) {
		sb.append(lefted ? "{@" : "{");
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeNew(this);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}
	
	@Override 
	boolean setOuterLefted(Expression outer) { 
		if(this.lefted) {
			this.outer = outer;
			return false;
		}
		return false; 
	}
	@Override
	public int inferTypestate(Visa v) {
		return Typestate.ObjectType;
	}
	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeNew(this, next);
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
		for(Expression e: this) {
			e.examplfy(gep, sb, p);
		}
	}
}
