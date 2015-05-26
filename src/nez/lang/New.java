package nez.lang;

import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Instruction;
import nez.vm.NezCompiler;

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
	public String getPredicate() { 
		return "new";
	}
	@Override
	public String key() {
		String s = lefted ? "{@" : "{";
		return (shift != 0) ? s + "[" + shift + "]" : s;
	}
	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeNew(this);
	}

	@Override
	public boolean isConsumed(Stacker stacker) {
		return false;
	}

	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
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
	public int inferTypestate(UMap<String> visited) {
		return Typestate.ObjectType;
	}
	@Override
	public Instruction encode(NezCompiler bc, Instruction next, Instruction failjump) {
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
