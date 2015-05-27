package nez.lang;
import java.util.AbstractList;
import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Instruction;
import nez.vm.NezCompiler;

public abstract class Expression extends AbstractList<Expression> {
	public final static boolean ClassicMode = false;

	SourcePosition s = null;
	int    internId   = 0;
	
	int    optimizedOption = -1;
	
	public Expression optimized;
	Expression(SourcePosition s) {
		this.s = s;
		this.internId = 0;
		this.optimized = this;
	}
	
	public final SourcePosition getSourcePosition() {
		return this.s;
	}

	public final int getId() {
		return this.internId;
	}
	
	public final boolean isInterned() {
		return (this.internId > 0);
	}
	
	final Expression intern() {
		return GrammarFactory.intern(this);
	}

	public abstract String getPredicate();
	public String key() { return this.getPredicate(); }
	
	@Override
	public Expression get(int index) {
		return null;
	}
	@Override
	public int size() {
		return 0;
	}
	
	public abstract Expression reshape(GrammarReshaper m);
	
	public final boolean isAlwaysConsumed() {
		return this.checkAlwaysConsumed(null, null, null);
	}
	public abstract boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack);
	
	public abstract boolean isConsumed(Stacker stacker);
	
	
	boolean setOuterLefted(Expression outer) { return false; }
	
	public final int inferTypestate() {
		return this.inferTypestate(null);
	}
	
	public abstract int inferTypestate(UMap<String> visited);
	
	public abstract short acceptByte(int ch, int option);
	
	public final Expression optimize(int option) {
		option = Grammar.mask(option);
		if(this.optimizedOption != option) {
			optimizeImpl(option);
			this.optimizedOption = option;
		}
		return this.optimized;
	}
	
	void optimizeImpl(int option) {
		this.optimized = this;
	}

	@Override
	public String toString() {
		return new GrammarFormatter().format(this);
	}

	public final UList<Expression> toList() {
		UList<Expression> l = new UList<Expression>(new Expression[this.size()]);
		if(this.size() > 1) {
			for(Expression e : this) {
				l.add(e);
			}
		}
		else {
			l.add(this);
		}
		return l;
	}

	public final void visit(GrammarVisitor visitor) {
		visitor.visit(this);
	}

	public abstract Instruction encode(NezCompiler bc, Instruction next, Instruction failjump);


	protected int pattern(GEP gep) {
		return 0;
	}

	protected void examplfy(GEP gep, StringBuilder sb, int p) {

	}
//
//	protected abstract int pattern(GEP gep);
//	protected abstract void examplfy(GEP gep, StringBuilder sb, int p);

	
	// test
	
	public static final boolean isByteConsumed(Expression e) {
		return (e instanceof ByteChar || e instanceof ByteMap || e instanceof AnyChar);
	}

	public static final boolean isPositionIndependentOperation(Expression e) {
		return (e instanceof Tagging || e instanceof Replace);
	}
	
	
	

}
