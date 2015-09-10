package nez.vm;

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
		this.refcount = 0;
		this.p = p;
	}

	public void setExpression(Expression e) {
		this.e = e;
	}

	public void count() {
		this.refcount++;
	}

}