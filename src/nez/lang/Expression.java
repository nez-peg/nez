package nez.lang;
import java.util.AbstractList;
import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UList;
import nez.util.UMap;

public abstract class Expression extends AbstractList<Expression> {
	public final static boolean ClassicMode = false;
	SourcePosition s = null;
	int    internId   = 0;
	
	public Expression optimized;
	int    optimizedOption = -1;
	
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
	
	final boolean isInterned() {
		return (this.internId > 0);
	}
	
	final Expression intern() {
		return Factory.intern(this);
	}

	public abstract String getPredicate();
	public abstract String key();
	public abstract Expression reshape(Manipulator m);
	
	public final boolean isAlwaysConsumed() {
		return this.checkAlwaysConsumed(null, null, null);
	}
	public abstract boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack);
	
	public abstract boolean isConsumed(Stacker stacker);
	
	
//	void checkPhase1(GrammarChecker checker, String ruleName, UMap<String> visited, int depth) {}
//	void checkPhase2(GrammarChecker checker) {}
	boolean setOuterLefted(Expression outer) { return false; }
	
	public final int inferTypestate() {
		return this.inferTypestate(null);
	}
	
	public abstract int inferTypestate(UMap<String> visited);
//	public abstract Expression checkTypestate(GrammarChecker checker, Typestate c);
	
//	public final static boolean CreateNonTerminal = true;
//	public final static boolean RemoveOnly = false;

//  public abstract Expression removeASTOperator(boolean newNonTerminal);
//	public abstract Expression removeFlag(TreeMap<String, String> undefedFlags);
	
	public abstract short acceptByte(int ch, int option);
//	public  boolean predict(int option, int ch, boolean k) {return false;}  // 
//	public abstract void predict(int option, boolean[] dfa);
	
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

	public abstract Instruction encode(RuntimeCompiler bc, Instruction next);

	protected abstract int pattern(GEP gep);
	protected abstract void examplfy(GEP gep, StringBuilder sb, int p);

	// test
	
	public static final boolean isByteConsumed(Expression e) {
		return (e instanceof ByteChar || e instanceof ByteMap || e instanceof AnyChar);
	}

	public static final boolean isPositionIndependentOperation(Expression e) {
		return (e instanceof Tagging || e instanceof Replace);
	}

}
