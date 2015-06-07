package nez.lang;
import java.util.AbstractList;
import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.util.UList;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public abstract class Expression extends AbstractList<Expression> {
	public final static boolean ClassicMode = false;

	SourcePosition s = null;
	int    internId   = 0;
	
	Expression(SourcePosition s) {
		this.s = s;
		this.internId = 0;
	}
	
	public abstract boolean equalsExpression(Expression o) ;
	
	public final boolean equals(Object o) {
		if(o instanceof Expression) {
			return this.equalsExpression((Expression)o);
		}
		return false;
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

	public Expression getFirst() {
		return this;
	}

	public Expression getLast() {
		return null;
	}

	public abstract Expression reshape(GrammarReshaper m);
	
//	public final boolean isAlwaysConsumed() {
//		return this.checkAlwaysConsumed(null, null, null);
//	}
//	public abstract boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack);
	
	public abstract boolean isConsumed();
	
	boolean setOuterLefted(Expression outer) { return false; }
	
	public final int inferTypestate() {
		return this.inferTypestate(null);
	}
	
	public abstract int inferTypestate(Visa v);
	
	public abstract short acceptByte(int ch, int option);
	
	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		format(sb);
		return sb.toString();
	}

	protected void format(StringBuilder sb) {
		sb.append("<");
		sb.append(this.getPredicate());
		for(Expression se : this) {
			sb.append(" ");
			se.format(sb);
		}
		sb.append(">");
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

	public abstract Instruction encode(NezEncoder bc, Instruction next, Instruction failjump);


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
	
	// convinient interface
	
	public final Expression newEmpty() {
		return GrammarFactory.newEmpty(this.getSourcePosition());
	}

	public final Expression newFailure() {
		return GrammarFactory.newFailure(this.getSourcePosition());
	}

	public final Expression newSequence(Expression e, Expression e2) {
		return GrammarFactory.newSequence(this.getSourcePosition(), e, e2);
	}

	public final Expression newSequence(UList<Expression> l) {
		return GrammarFactory.newSequence(this.getSourcePosition(), l);
	}

	public final Expression newChoice(Expression e, Expression e2) {
		return GrammarFactory.newChoice(this.getSourcePosition(), e, e2);
	}

	public final Expression newChoice(UList<Expression> l) {
		return GrammarFactory.newChoice(this.getSourcePosition(), l);
	}

	
	

}
