package nez.parser;

import nez.lang.Expression;
import nez.lang.Production;

public class ParseFunc {
	String name;
	Production grammarProduction;
	Production parserProduction;

	int refcount;
	boolean inlining;
	boolean state;
	MemoPoint memoPoint = null;

	NezInst compiled;

	public ParseFunc(String uname, Production p, Production pp, int init) {
		this.name = uname;
		this.refcount = 0;
		this.grammarProduction = p;
		this.parserProduction = pp;
		this.refcount = init;
	}

	public Expression getExpression() {
		return this.parserProduction.getExpression();
	}

	public void incCount() {
		this.refcount++;
	}

	public void resetCount() {
		this.refcount = 0;
	}

	public final int getCount() {
		return this.refcount;
	}

	public final MemoPoint getMemoPoint() {
		return this.memoPoint;
	}

	public final boolean isStateful() {
		return this.state;
	}

	public final boolean isInlined() {
		return this.inlining;
	}

	public final NezInst getCompiled() {
		return this.compiled;
	}

	public final void setCompiled(NezInst compiled) {
		this.compiled = compiled;
	}
}