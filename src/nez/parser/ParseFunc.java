package nez.parser;

import nez.lang.Expression;
import nez.lang.Production;

public class ParseFunc {
	String name;
	Production p;
	Expression e;

	int refcount;
	boolean inlining;
	boolean state;
	MemoPoint memoPoint = null;

	Instruction compiled;

	public ParseFunc(String uname, Production p) {
		this.name = uname;
		this.refcount = 0;
		this.p = p;
	}

	public void setExpression(Expression e) {
		this.e = e;
	}

	public void count() {
		this.refcount++;
	}

	public final int getRefCount() {
		return this.refcount;
	}

	public final void update(Expression e, int refcount) {
		this.setExpression(e);
		this.refcount = refcount;
	}

}